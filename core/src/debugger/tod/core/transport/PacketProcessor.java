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
package tod.core.transport;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import tod.core.DebugFlags;
import tod2.agent.AgentConfig;
import tod2.agent.AgentDebugFlags;
import tod2.agent.Command;
import zz.utils.Utils;
import zz.utils.notification.IEvent;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;

/**
 * Processes events received from the debugged application, or read from a file.
 * @author gpothier
 */
public abstract class PacketProcessor 
{
	private ILogReceiverMonitor itsMonitor = null;
	private boolean itsEof = false;

	/**
	 * Number of commands received.
	 */
	private long itsMessageCount = 0;
	
	private final ByteBuffer itsHeaderBuffer;
	private final ByteBuffer itsDataBuffer;
	
	/**
	 * This map contains buffers that are used to reassemble long packets.
	 * It only contains the entry corresponding to a given thread if a long packet is currently being
	 * processed.
	 */
	private Map<Integer, ThreadPacketBuffer> itsThreadPacketBuffers = new HashMap<Integer, ThreadPacketBuffer>();
	
	private IFireableEvent<Throwable> eException = new SimpleEvent<Throwable>();

	public PacketProcessor()
	{
		itsHeaderBuffer = ByteBuffer.allocate(9);
		itsHeaderBuffer.order(ByteOrder.nativeOrder());
		
		itsDataBuffer = ByteBuffer.allocate(AgentConfig.COLLECTOR_BUFFER_SIZE);
		itsDataBuffer.order(ByteOrder.nativeOrder());
	}
	
	/**
	 * An event that is fired when an exception occurs in packet processing.
	 */
	public IEvent<Throwable> eException()
	{
		return eException;
	}
	
	
	public void setMonitor(ILogReceiverMonitor aMonitor)
	{
		itsMonitor = aMonitor;
	}
	
	protected ILogReceiverMonitor getMonitor()
	{
		return itsMonitor;
	}
	
	/**
	 * Returns the total number of messages received by this receiver.
	 */
	public long getMessageCount()
	{
		return itsMessageCount;
	}

	protected synchronized void eof()
	{
		itsEof = true;
		processFlush();
		notifyAll();
	}
	
	public boolean isEof()
	{
		return itsEof;
	}
	
	/**
	 * Waits until the input stream terminates
	 */
	public synchronized void waitEof()
	{
		try
		{
			while (! itsEof) wait();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected boolean readPackets(DataInputStream aDataIn, boolean aPrintProgress) throws IOException
	{
		long theProcessedBytes = 0;
		long theLastPrinted = 0; // Last printed quantity 
		
		while(aDataIn.available() != 0)
		{
			try
			{
				// Read and decode meta-packet header 
				aDataIn.readFully(itsHeaderBuffer.array());
				itsHeaderBuffer.position(0);
				itsHeaderBuffer.limit(9);
				theProcessedBytes += 9;
				
				int theThreadId = itsHeaderBuffer.getInt(); 
				int theSize = itsHeaderBuffer.getInt(); 
				int theFlags = itsHeaderBuffer.get();
				
				// These flags indicate if the beginning (resp. end) of the metapacket
				// correspond to the beginning (resp. end) of a real packet.
				// (otherwise, it means the real packets span several metapackets).
				boolean theCleanStart = (theFlags & 2) != 0;
				boolean theCleanEnd = (theFlags & 1) != 0;
				
//				Utils.println("[LogReceiver] Packet: th: %d, sz: %d, cs: %s, ce: %s", theThreadId, theSize, theCleanStart, theCleanEnd);
				
				aDataIn.readFully(itsDataBuffer.array(), 0, theSize);
				itsDataBuffer.position(0);
				itsDataBuffer.limit(theSize);
				theProcessedBytes += theSize;

				if (aPrintProgress)
				{
					int n = 10;
					long thePrint = theProcessedBytes/(1024*1024*n); // Print every 10MB
					if (thePrint > theLastPrinted)
					{
						theLastPrinted = thePrint;
						Utils.println("Processed %dMB", thePrint*n);
					}
				}
				
				if (! theCleanEnd)
				{
					ThreadPacketBuffer theBuffer = itsThreadPacketBuffers.get(theThreadId);
					if (theBuffer == null)
					{
						assert theCleanStart;
						
						theBuffer = new ThreadPacketBuffer();
						
						if (AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG)
							System.out.println("[LogReceiver] Starting long packet for thread "+theThreadId);
						
						itsThreadPacketBuffers.put(theThreadId, theBuffer);
					}
					else
					{
						assert ! theCleanStart;
					}
					
					if (AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG)
						System.out.println("[LogReceiver] Long packet for thread "+theThreadId+", appending "+theSize+" bytes");
					
					theBuffer.append(itsDataBuffer.array(), 0, itsDataBuffer.remaining());
				}
				else
				{
					ThreadPacketBuffer theBuffer = itsThreadPacketBuffers.remove(theThreadId);
					if (theBuffer != null)
					{
						// Process outstanding long packet.
						assert ! theCleanStart;
						
						if (AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG)
							System.out.println("[LogReceiver] Long packet for thread "+theThreadId+", appending "+theSize+" bytes");
						
						theBuffer.append(itsDataBuffer.array(), 0, itsDataBuffer.remaining());
						
						BufferDataInput theStream = new BufferDataInput(theBuffer.toByteBuffer());
						
						if (AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG)
							System.out.println("[LogReceiver] Starting to process long packet for thread "+theThreadId+": "+theBuffer);
						
						processThreadPackets(theThreadId, theStream, AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG);
					}
					else
					{
						assert theCleanStart;
						
						BufferDataInput theStream = new BufferDataInput(itsDataBuffer);
						processThreadPackets(theThreadId, theStream, false);
					}
				}
			}
			catch (EOFException e)
			{
				System.err.println("LogReceiver: EOF (msg #"+itsMessageCount+")");
				eof();
				break;
			}
			catch (Throwable e)
			{
				System.err.println("Exception in LogReceiver.process (msg #"+itsMessageCount+"):");
				e.printStackTrace();
				eException.fire(e);
				eof();
				break;
			}
		}
		
		return true;
	}

	protected void processThreadPackets(
			int aThreadId, 
			BufferDataInput aStream, 
			boolean aLogPackets)
	throws IOException
	{
		while(aStream.hasMore())
		{
			int theMessage = aStream.readByte();
//			System.out.println("[LogReceiver] Command: "+theCommand);
			itsMessageCount++;

			if (DebugFlags.MAX_EVENTS > 0 && itsMessageCount > DebugFlags.MAX_EVENTS)
			{
				eof();
				break;
			}
			
			if (theMessage >= Command.BASE)
			{
				switch (theMessage)
				{
				case Command.DBCMD_FLUSH:
					System.out.println("[LogReceiver] Received flush request.");
					processFlush();
					break;
					
				case Command.DBCMD_CLEAR:
					System.out.println("[LogReceiver] Received clear request.");
					processFlush();
					processClear();
					break;
					
				case Command.DBCMD_END:
					System.out.println("[LogReceiver] Received end request.");
					processFlush();
					processEnd();
					break;
					
				case Command.DBEV_CAPTURE_ENABLED:
					boolean theEnabled = aStream.readByte() != 0;
					System.out.println("[LogReceiver] Received capture enabled event: "+theEnabled);
					processEvCaptureEnabled(theEnabled);
					break;
					
				default: throw new RuntimeException("Not handled: "+theMessage); 
				}

			}
			else
			{
				if (aLogPackets) System.out.println("[LogReceiver] Processing "+theMessage+" (remaining: "+aStream.remaining()+")");
				processEvent(aThreadId, (byte) theMessage, aStream);
				if (aLogPackets) System.out.println("[LogReceiver] Done processing "+theMessage+" (remaining: "+aStream.remaining()+")");
			}
			
			if (itsMonitor != null 
					&& DebugFlags.RECEIVER_PRINT_COUNTS > 0 
					&& itsMessageCount % DebugFlags.RECEIVER_PRINT_COUNTS == 0)
			{
				itsMonitor.processedMessages(itsMessageCount);
			}
		}
	}
	
	/**
	 * Reads and processes an incoming event packet for the given thread.
	 */
	protected abstract void processEvent(int aThreadId, byte aMessage, DataInput aStream) throws IOException;
	
	/**
	 * Flushes buffered events.
	 * @return Number of flushed events
	 */
	protected abstract int processFlush();
	
	/**
	 * Clears the database.
	 */
	protected abstract void processClear();
	
	/**
	 * Called when an END command is received.
	 */
	protected abstract void processEnd();
	
	/**
	 * Called when the DBEV_CAPTURE_ENABLED event is received.
	 */
	protected abstract void processEvCaptureEnabled(boolean aEnabled);
	
	
	/**
	 * This is a buffer for long packets, ie. packets that span more than
	 * one meta-packet.
	 * @author gpothier
	 */
	private static class ThreadPacketBuffer
	{
		private final ByteArrayOutputStream itsBuffer = new ByteArrayOutputStream();
		
		public void append(byte[] aBuffer, int aOffset, int aLength)
		{
			itsBuffer.write(aBuffer, aOffset, aLength);
		}
		
		public ByteBuffer toByteBuffer()
		{
			ByteBuffer theBuffer = ByteBuffer.wrap(itsBuffer.toByteArray());
			theBuffer.order(ByteOrder.nativeOrder());
			return theBuffer;
		}
		
		@Override
		public String toString()
		{
			return "ThreadPacketBuffer: "+itsBuffer.size()+" bytes";
		}
	}
	
	public interface ILogReceiverMonitor
	{
		public void started();
		public void processedMessages(long aCount);
	}
	
	/**
	 * Wraps a {@link ByteBuffer} in a {@link DataInput}.
	 * @author gpothier
	 */
	private static class BufferDataInput implements DataInput
	{
		private final ByteBuffer itsBuffer;

		public BufferDataInput(ByteBuffer aBuffer)
		{
			itsBuffer = aBuffer;
		}

		public boolean hasMore()
		{
			return itsBuffer.remaining() > 0;
		}
		
		public int remaining()
		{
			return itsBuffer.remaining();
		}
		
		public boolean readBoolean()
		{
			return itsBuffer.get() != 0;
		}

		public byte readByte() 
		{
			return itsBuffer.get();
		}

		public char readChar() 
		{
			return itsBuffer.getChar();
		}

		public double readDouble() 
		{
			return itsBuffer.getDouble();
		}

		public float readFloat() 
		{
			return itsBuffer.getFloat();
		}

		public void readFully(byte[] aB, int aOff, int aLen) 
		{
			itsBuffer.get(aB, aOff, aLen);
		}

		public void readFully(byte[] aB) 
		{
			readFully(aB, 0, aB.length);
		}

		public int readInt() 
		{
			return itsBuffer.getInt();
		}

		public String readLine() 
		{
			throw new UnsupportedOperationException();
		}

		public long readLong() 
		{
			return itsBuffer.getLong();
		}

		public short readShort() 
		{
			return itsBuffer.getShort();
		}

		public int readUnsignedByte() 
		{
			throw new UnsupportedOperationException();
		}

		public int readUnsignedShort() 
		{
			throw new UnsupportedOperationException();
		}

		public String readUTF() 
		{
			int theSize = readInt();
			char[] theChars = new char[theSize];
			for(int i=0;i<theSize;i++) theChars[i] = readChar();
			return new String(theChars);
		}

		public int skipBytes(int aN) 
		{
			throw new UnsupportedOperationException();
		}
		
	}
}