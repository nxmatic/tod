/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package java.tod.transport;

import java.nio.channels.ByteChannel;
import java.tod.AgentReady;
import java.tod.EventCollector;
import java.tod.TOD;
import java.tod.TracedMethods;
import java.tod.io._ByteBuffer;
import java.tod.io._EOFException;
import java.tod.io._IO;
import java.tod.io._IOException;
import java.tod.io._SocketChannel;
import java.tod.util._ArrayList;
import java.tod.util._SyncRingBuffer;

import tod.agent.Command;

/**
 * This class implements the thread that communicates with the TOD database.
 * - Sends the packets buffered by a set of {@link PacketBuffer} to a given
 * {@link ByteChannel}.
 * Packets of a given thread are sent together, prefixed by a header that indicates the 
 * size of the "meta-packet", the corresponding thread id, and whether the meta-packet contains
 * split packets.
 * - Receives commands from the database.
 * 
 * Because we are using native iostream calls to perform IO, we can't have read operations and 
 * write operations in different threads. 
 * @author gpothier
 */
public class IOThread extends Thread
{
	private final EventCollector itsEventCollector;
	
	private final _SocketChannel itsChannel;
	
	/**
	 * Used to send the header of each meta-packet.
	 */
	private final _ByteBuffer itsHeaderBuffer;
	
	/**
	 * Buffers that are waiting to be sent.
	 */
	private final _SyncRingBuffer<PacketBuffer> itsPendingBuffers = new _SyncRingBuffer<PacketBuffer>(1000);
	
	/**
	 * This list contains all the packet buffers created by this sender.
	 */
	private final _ArrayList<PacketBuffer> itsBuffers = new _ArrayList<PacketBuffer>();

	private final MyShutdownHook itsShutdownHook;
	
	private volatile boolean itsShutdownStarted = false;
	
	/**
	 *  Time at which last stale buffer check was performed
	 */
	private long itsCheckTime;
	
	/**
	 * Last observed value of the Capture Enabled flag
	 */
	private boolean itsLastEnabled; 
	
	/**
	 *  Number of buffers that were sent since last timestamp was taken (taking timestamps is costly)
	 */
	private int itsSentBuffers;
	
	/**
	 * A buffer for the command reader
	 */
	private _ByteBuffer its1b = _ByteBuffer.allocate(1);



	public IOThread(EventCollector aEventCollector, _SocketChannel aChannel)
	{
		super("[TOD] Packet buffer sender");
		setDaemon(true);
		assert aChannel != null;
		itsEventCollector = aEventCollector;
		itsChannel = aChannel;
		
		itsHeaderBuffer = _ByteBuffer.allocate(9);
		
		itsShutdownHook = new MyShutdownHook();
		Runtime.getRuntime().addShutdownHook(itsShutdownHook);
		
		start();
	}
	
	public boolean hasShutdownStarted()
	{
		return itsShutdownStarted;
	}
	
	/**
	 * Assertions don't seem to work in bootclasspath code...
	 */
	public static void _assert(boolean aValue)
	{
		if (! aValue) throw new AssertionError();
	}

	@Override
	public void run()
	{
		try
		{
			itsCheckTime = System.currentTimeMillis();
			itsLastEnabled = ! AgentReady.CAPTURE_ENABLED; 
			itsSentBuffers = 0;
			
			while(true)
			{
//				_IO.out("PacketBufferSender.run() - sentBuffers: "+sentBuffers);
				PacketBuffer thePendingBuffer = popBuffer();
				
				if (thePendingBuffer != null)
				{
					itsSentBuffers++;
					sendBuffer(thePendingBuffer);
				}
				
				if (thePendingBuffer == null || itsSentBuffers > 100)
				{
					// Check stale buffers at a regular interval
					checkStaleBuffers();
					itsSentBuffers = 0;
				}
				
				readCommands();
			}
		}
		catch (Exception e)
		{
			if (itsShutdownStarted && (e instanceof _IOException))
			{
				// Broken pipe, just exit
			}
			else
			{
				_IO.err("[TOD] FATAL:");
				e.printStackTrace();
				Runtime.getRuntime().removeShutdownHook(itsShutdownHook);
				System.exit(1);
			}
		}
	}
	
	private void sendBuffer(PacketBuffer aBuffer) throws _IOException
	{
		_ByteBuffer theByteBuffer = aBuffer.getPendingBuffer();
		_assert (theByteBuffer != null);
		
		int theSize = theByteBuffer.position();
		int theId = aBuffer.getThreadId();

		itsHeaderBuffer.clear();
		itsHeaderBuffer.putInt(theId);
		itsHeaderBuffer.putInt(theSize);
		
		int theFlags = 
			(aBuffer.hasCleanStart() ? 2 : 0) 
			| (aBuffer.hasCleanEnd() ? 1 : 0);
		
//		_IO.out(String.format(
//				"[TOD-IOThread] Sending packet (th: %d, sz: %d, cs: %s, ce: %s)",
//				theId,
//				theSize,
//				aBuffer.hasCleanStart(),
//				aBuffer.hasCleanEnd()));
		
		itsHeaderBuffer.put((byte) theFlags);
		
		itsHeaderBuffer.flip();
		itsChannel.write(itsHeaderBuffer);
		
		theByteBuffer.flip();
		itsChannel.write(theByteBuffer);
		
		theByteBuffer.clear();
		
		aBuffer.sent();
	}
	
	private void checkStaleBuffers()
	{
		long t = System.currentTimeMillis();
		long delta = t - itsCheckTime;
		
		if (delta > 100)
		{
			itsCheckTime = t;
			
			PacketBuffer[] theArray;
			synchronized(this)
			{
				// Might deadlock if we synchronize the pleaseSwaps, so copy the list
				theArray = itsBuffers.toArray(new PacketBuffer[itsBuffers.size()]);
			}
			
			for (PacketBuffer theBuffer : theArray) theBuffer.pleaseSwap();
			
			// Notify of capture enabled state
			if (itsLastEnabled != AgentReady.CAPTURE_ENABLED)
			{
				itsEventCollector.evCaptureEnabled(AgentReady.CAPTURE_ENABLED);
				itsLastEnabled = AgentReady.CAPTURE_ENABLED;
			}
		}
	}
	
	private byte readByte() throws _IOException
	{
		its1b.clear();
		int theCount = itsChannel.read(its1b);
		if (theCount == -1) throw new _EOFException();
		return its1b.get();
	}
	
	private boolean readBoolean() throws _IOException
	{
		return readByte() != 0;
	}

	private int readInt() throws _IOException
	{
		byte b0 = readByte();
		byte b1 = readByte();
		byte b2 = readByte();
		byte b3 = readByte();
		
		return _ByteBuffer.makeInt(b3, b2, b1, b0);
	}

	
	private void readCommands() throws _IOException
	{
		while(itsChannel.hasInput())
		{
			byte theMessage = readByte();
			if (theMessage >= Command.BASE)
			{
				switch (theMessage)
				{
				case Command.AGCMD_ENABLECAPTURE:
					processEnableCapture();
					break;
					
				case Command.AGCMD_SETMONITORINGMODE:
					processSetMonitoringMode();
					break;
					
				default: throw new RuntimeException("Not handled: "+theMessage); 
				}
			}
		}
	}
	
	/**
	 * Processes the {@link Command#AGCMD_ENABLECAPTURE} command.
	 */
	private void processEnableCapture() throws _IOException
	{
		boolean theEnable = readBoolean();
		if (theEnable) 
		{
			_IO.out("[TOD] Enable capture request received.");
			TOD.enableCapture();
		}
		else 
		{
			_IO.out("[TOD] Disable capture request received.");
			TOD.disableCapture();
		}
	}

	/**
	 * Processes the {@link Command#AGCMD_SETMONITORINGMODE} command
	 */
	private void processSetMonitoringMode() throws _IOException
	{
		int theCount = readInt();
		for (int i=0;i<theCount;i++)
		{
			int theBehaviorId = readInt();
			byte theMode = readByte();
			TracedMethods.setMode(theBehaviorId, theMode);
		}
	}
	
	/**
	 * Pushes the given packet buffer to the pending queue.
	 */
	synchronized void pushBuffer(PacketBuffer aBuffer) 
	{
		itsPendingBuffers.add(aBuffer);
		notifyAll();
	}
	
	/**
	 * Pops a buffer from the stack, waiting for up to 100ms if none is available.
	 */
	synchronized PacketBuffer popBuffer() throws InterruptedException
	{
		if (itsPendingBuffers.isEmpty()) wait(100);
		return itsPendingBuffers.poll();
	}
	
	public synchronized PacketBuffer createBuffer(int aThreadId)
	{
		PacketBuffer thePacketBuffer = new PacketBuffer(this, aThreadId);
		itsBuffers.add(thePacketBuffer);
		return thePacketBuffer;
	}
	
	private class MyShutdownHook extends Thread
	{
		public MyShutdownHook() 
		{
			super("Shutdown hook (SocketCollector)");
		}

		@Override
		public void run()
		{
			itsShutdownStarted = true;
			_IO.out("[TOD] Flushing buffers...");
			
			EventCollector.INSTANCE.end();
			
			for (int i=0;i<itsBuffers.size();i++) itsBuffers.get(i).swapBuffers();
			
			try
			{
				int thePrevSize = itsPendingBuffers.size();
				while(thePrevSize > 0)
				{
					Thread.sleep(200);
					int theNewSize = itsPendingBuffers.size();
					if (theNewSize == thePrevSize)
					{
						_IO.err("[TOD] Buffers are not being sent, shutting down anyway ("+theNewSize+" buffers remaining).");
						break;
					}
					thePrevSize = theNewSize;
				}
				
				// Give some more time to allow for the buffers to be sent
				Thread.sleep(3000);
				
				itsChannel.close();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			
			_IO.out("[TOD] Shutting down.");
		}
	}
	

}
