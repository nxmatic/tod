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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.tod.io._ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import tod.agent.Command;
import tod.core.DebugFlags;
import tod.core.config.TODConfig;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.database.structure.standard.HostInfo;
import tod.impl.database.structure.standard.StructureDatabaseUtils;
import tod.utils.TODUtils;

/**
 * Receives (low-level) events from the debugged application through a socket.
 * @author gpothier
 */
public abstract class LogReceiver extends PacketProcessor
{
	public static final ReceiverThread DEFAULT_THREAD = new ReceiverThread();
	
	private final TODConfig itsConfig;
	private final IStructureDatabase itsStructureDatabase;
	
	private ReceiverThread itsReceiverThread;
	
	private boolean itsStarted = false;
	
	/**
	 * Identification of the host that sends events
	 */
	private HostInfo itsHostInfo;
	
	private final InputStream itsInStream;
	private final OutputStream itsOutStream;

	private DataInputStream itsDataIn;
	private DataOutputStream itsDataOut;
	
	/**
	 * Output stream for storing events in two-phases mode.
	 */
	private final OutputStream itsFileOut;
	private final File itsEventsFile;
	private final File itsDbFile;
	
	public LogReceiver(
			TODConfig aConfig,
			IStructureDatabase aStructureDatabase,
			HostInfo aHostInfo,
			InputStream aInStream, 
			OutputStream aOutStream, 
			boolean aStart)
	{
		this(aConfig, aStructureDatabase, DEFAULT_THREAD, aHostInfo, aInStream, aOutStream, aStart);
	}
	
	public LogReceiver(
			TODConfig aConfig,
			IStructureDatabase aStructureDatabase,
			ReceiverThread aReceiverThread,
			HostInfo aHostInfo,
			InputStream aInStream, 
			OutputStream aOutStream, 
			boolean aStart)
	{
		itsConfig = aConfig;
		itsStructureDatabase = aStructureDatabase;
		itsReceiverThread = aReceiverThread;
		itsHostInfo = aHostInfo;
		itsInStream = aInStream;
		itsOutStream = aOutStream;
		
		itsDataIn = new DataInputStream(itsInStream);
		itsDataOut = new DataOutputStream(itsOutStream);
		
		itsReceiverThread.register(this);
		
		if (itsConfig.get(TODConfig.DB_TWOPHASES))
		{
			try
			{
				itsEventsFile = new File(itsConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");
				itsEventsFile.delete();
				
				itsDbFile = new File(itsConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db.raw");
				itsDbFile.delete();
				
				itsFileOut = new BufferedOutputStream(new FileOutputStream(itsEventsFile));
			}
			catch (FileNotFoundException e)
			{
				System.err.println("FATAL: Cannot open raw events directory: "+itsConfig.get(TODConfig.DB_RAW_EVENTS_DIR));
				e.printStackTrace();
				System.exit(1);
				throw new RuntimeException(); // For compiler, because of final field
			}
		}
		else
		{
			itsEventsFile = null;
			itsDbFile = null;
			itsFileOut = null;
		}
		
		if (aStart) start();
	}
	
	public void start()
	{
		itsStarted = true;
		synchronized (itsReceiverThread)
		{
			itsReceiverThread.notifyAll();
		}
	}
	
	public void disconnect()
	{
		try
		{
			itsInStream.close();
			itsOutStream.close();
			eof();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private boolean isStarted()
	{
		return itsStarted;
	}
	
	/**
	 * Returns the identification of the currently connected host.
	 */
	public HostInfo getHostInfo()
	{
		return itsHostInfo;
	}

	/**
	 * Returns the name of the currently connected host, or null
	 * if there is no connected host.
	 */
	public String getHostName()
	{
		return itsHostInfo != null ? itsHostInfo.getName() : null;
	}
	
	private synchronized void setHostName(String aHostName)
	{
		itsHostInfo.setName(aHostName);
		notifyAll();
	}
	
	@Override
	protected synchronized void eof()
	{
		super.eof();
		try
		{
			itsInStream.close();
			itsOutStream.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Waits until the host name is available, and returns it.
	 * See {@link #getHostName()}
	 */
	public synchronized String waitHostName()
	{
		try
		{
			while (getHostName() == null) wait();
			return getHostName();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Processes currently pending data.
	 * @return Whether there was data to process.
	 */
	private boolean process() throws IOException
	{
		return process(itsDataIn);
	}
	
	protected synchronized void sendCommand(Command aCommand) throws IOException
	{
		itsDataOut.writeByte(aCommand.ordinal() + Command.BASE);
	}
	
	public synchronized void sendEnableCapture(boolean aEnable)
	{
		try
		{
			System.out.println("Sending enabled capture command ("+aEnable+")");
			sendCommand(Command.AGCMD_ENABLECAPTURE);
			itsDataOut.writeBoolean(aEnable);
			itsDataOut.flush();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected boolean process(DataInputStream aDataIn) throws IOException
	{
		if (DebugFlags.SWALLOW)
		{
			byte[] theBuffer = new byte[4096];
			while(true)
			{
				int theRead = aDataIn.read(theBuffer);
				if (theRead == -1)
				{
					eof();
					return true;
				}
			}
		}
		
		try
		{
			if (aDataIn.available() == 0) 
			{
				if (DebugFlags.REPLAY_MODE) eof();
				return false;
			}
		}
		catch (IOException e1)
		{
			eof();
		}
		
		if (getHostName() == null)
		{
			setHostName(_ByteBuffer.getString(aDataIn));
			if (getMonitor() != null) getMonitor().started();
		}
		
		if (itsFileOut != null) 
		{
			TODUtils.log(0, "Storing raw events to file.");
			storePackets(aDataIn);
			StructureDatabaseUtils.saveDatabase(itsStructureDatabase, itsDbFile);
			TODUtils.logf(0, "Client terminated. Stored %.2fMB", 1f*itsEventsFile.length()/(1024*1024));
			
			readPackets(
					new DataInputStream(new BufferedInputStream(new FileInputStream(itsEventsFile), 32*1024*1024)), 
					true);
			
			processFlush();
			
			TODUtils.log(0, "Trace imported.");
			
			return true;
		}
		else return readPackets(aDataIn, false);
	}
	
	private void storePackets(DataInputStream aDataIn) throws IOException
	{
		byte[] theBuffer = new byte[4096];
		while(true)
		{
			int theRead;
			try
			{
				theRead = aDataIn.read(theBuffer);
			}
			catch (IOException e)
			{
				System.err.println(e.getMessage());
				theRead = -1;
			}

			if (theRead == 0) continue;
			else if (theRead > 0) itsFileOut.write(theBuffer, 0, theRead);
			else
			{
				eof();
				break;
			}
		}
	}
	
	@Override
	protected void processEnd()
	{
//		disconnect();
	}
	
	/**
	 * This is the thread that actually processes the streams.
	 * Having a unique thread permits to avoid synchronization
	 * problems further in the stream.
	 * @author gpothier
	 */
	public static class ReceiverThread extends Thread
	{
		public ReceiverThread()
		{
			super("LogReceiver.ReceiverThread");
			start();
		}
		
		private List<LogReceiver> itsReceivers = new ArrayList<LogReceiver>();
		
		public synchronized void register(LogReceiver aReceiver)
		{
			itsReceivers.add(aReceiver);
			notifyAll();
		}
		
		@Override
		public void run()
		{
			try
			{
				int theWait = 1;
				while(true)
				{
					// We don't use an iterator so as to avoid concurrent modif. exceptions
					for(int i=0;i<itsReceivers.size();i++)
					{
						LogReceiver theReceiver = itsReceivers.get(i);
						if (! theReceiver.isStarted()) continue;
						
						try
						{
							if (theReceiver.process()) theWait = 1;
						}
						catch (IOException e)
						{	
							System.err.println("Exception while processing receiver of "+theReceiver.getHostName());
							e.printStackTrace();
							theReceiver.processFlush();
						}
						
						if (theReceiver.isEof()) itsReceivers.remove(i);
					}
					
					if (theWait > 1) 
					{
						synchronized (this)
						{
							wait(theWait);
						}
					}
					
					theWait *= 2;
					theWait = Math.min(theWait, 100);
				}
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}