/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.server;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.replay2.EventCollector;
import tod.impl.replay2.LocalsSnapshot;
import tod.impl.replay2.ReplayerLoader;
import tod.impl.replay2.ReplayerWrapper;
import tod.impl.replay2.TmpIdManager;
import tod2.agent.Message;
import tod2.agent.io._ByteBuffer;
import zz.utils.Utils;

/**
 * The class that handles the traffic from the debugged VM.
 * @author gpothier
 */
public abstract class DBSideIOThread
{
	private static final int BUFFER_SIZE = 4096;
	
	private final TODConfig itsConfig;
	private final IMutableStructureDatabase itsDatabase;
	private final InputStream itsIn;
	private final LocalsSnapshot itsSnapshot;
	
	/**
	 * If not 0, the id of the only thread to replay.
	 */
	private int itsReplayThreadId;
	
	/**
	 * Number of bytes to skip from the first thread packet (for partial replay)
	 */
	private int itsInitialSkip;
	
	private final ReplayerLoader itsLoader;
	private final List<ThreadReplayerThread> itsReplayerThreads = new ArrayList<ThreadReplayerThread>();
	private final TmpIdManager itsTmpIdManager = new TmpIdManager();
	
	private long itsProcessedSize = 0;
	
	public DBSideIOThread(TODConfig aConfig, IMutableStructureDatabase aDatabase, InputStream aIn, LocalsSnapshot aSnapshot)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsIn = aIn;
		itsSnapshot = aSnapshot;
		itsLoader = new ReplayerLoader(getClass().getClassLoader(), aDatabase);
	}

	public void setInitialSkip(int aInitialSkip)
	{
		itsInitialSkip = aInitialSkip;
	}
	
	public void setReplayThreadId(int aReplayThreadId)
	{
		itsReplayThreadId = aReplayThreadId;
	}
	
	public void run()
	{
		try
		{
			Utils.println("Starting replay.");
			long t0 = System.currentTimeMillis();
			
			int thePacketCount = 0;
			
			loop:
			while(true)
			{
				int thePacketType = itsIn.read();
				itsProcessedSize++;
				
				switch(thePacketType)
				{
				case Message.PACKET_TYPE_THREAD: processThreadPacket(); break;
				case Message.PACKET_TYPE_STRING: processStringPacket(); break;
				case Message.PACKET_TYPE_MODECHANGES: processModeChangesPacket(); break;
				case -1: break loop;
				default: throw new RuntimeException("Not handled: "+thePacketType);
				}
				
				thePacketCount++;
				
				if (thePacketCount % 10000 == 0) Utils.println("Processed %d bytes (%d packets)", itsProcessedSize, thePacketCount);
			}
			
			for (ThreadReplayerThread theThread : itsReplayerThreads)
			{
				if (theThread != null) theThread.push(null);
			}
			
			for (ThreadReplayerThread theThread : itsReplayerThreads) if (theThread != null) theThread.join();

			long t1 = System.currentTimeMillis();
			Utils.println("Replay took %.3fs", 0.001f*(t1-t0));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected abstract EventCollector createCollector(int aThreadId);
	
	private ThreadReplayerThread getReplayerThread(int aThreadId)
	{
		ThreadReplayerThread theThread = Utils.listGet(itsReplayerThreads, aThreadId);
		if (theThread == null)
		{
			EventCollector theCollector = createCollector(aThreadId);
			theThread = new ThreadReplayerThread(aThreadId, theCollector);
			Utils.listSet(itsReplayerThreads, aThreadId, theThread);
		}
		return theThread;
	}
	
	private void readFully(byte[] aBuffer) throws IOException
	{
		readFully(aBuffer, aBuffer.length);
	}
	
	private void readFully(byte[] aBuffer, int aCount) throws IOException
	{
		int l = aCount;
		int c = 0;
		while(c < l) 
		{
			int r = itsIn.read(aBuffer, c, l-c);
			if (r == -1) throw new EOFException();
			c += r;
		}
	}
	
	private void processThreadPacket() throws IOException
	{
		int theThreadId = _ByteBuffer.getIntL(itsIn);
		int theLength = _ByteBuffer.getIntL(itsIn);
		
		PacketBuffer theBuffer = new PacketBuffer(new byte[BUFFER_SIZE], itsProcessedSize-1);
		readFully(theBuffer.array(), theLength);
		theBuffer.position(0);
		theBuffer.limit(theLength);
		
		if (itsInitialSkip > 0)
		{
			theBuffer.position(itsInitialSkip);
			itsInitialSkip = 0;
		}
		
		getReplayerThread(theThreadId).push(theBuffer);
		
		itsProcessedSize += 8 + theLength;
	}
	
	private void processStringPacket() throws IOException
	{
		long theObjectId = _ByteBuffer.getLongL(itsIn);
		String theString = _ByteBuffer.getString(itsIn);
		
		itsProcessedSize += 8 + 4 + theString.length()*2;
	}
	
	private void processModeChangesPacket() throws IOException
	{
		int theLength = _ByteBuffer.getIntL(itsIn);
		byte[] theData = new byte[theLength];
		Utils.readFully(itsIn, theData);
		
		_ByteBuffer b = _ByteBuffer.wrap(theData);
		while(b.remaining() > 0)
		{
			int theBehaviorId = b.getInt();
			byte theMode = b.get();
			
			ModeChangesList.add(theBehaviorId, theMode);
		}
		
		itsProcessedSize += 4 + theLength;
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		try
		{
			TODConfig theConfig = new TODConfig();
			File theEventsFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");

			String theScopeMD5 = Utils.md5String(theConfig.get(TODConfig.SCOPE_TRACE_FILTER).getBytes());
			File theDbFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db-"+theScopeMD5+".raw");

			IMutableStructureDatabase theDatabase = StructureDatabase.create(theConfig, theDbFile, true);
			
			final Map<Integer, EventCollector> theCollectors = new HashMap<Integer, EventCollector>();  
			
			DBSideIOThread theIOThread = new DBSideIOThread(theConfig, theDatabase, new FileInputStream(theEventsFile), null)
			{
				@Override
				protected EventCollector createCollector(int aThreadId)
				{
					EventCollector theCollector = new ObjectAccessDistributionEventCollector(aThreadId);
					theCollectors.put(aThreadId, theCollector);
					return theCollector;
				}
			};
			theIOThread.run();
			
			System.out.println("Collectors:");
			for(Map.Entry<Integer, EventCollector> theEntry : theCollectors.entrySet())
			{
				System.out.println(theEntry.getKey() + ": " + theEntry.getValue());
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		Thread.sleep(1000);
		System.err.println("END");
	}
	
	private class ThreadReplayerThread extends Thread
	{
		private final int itsThreadId;
		private final EventCollector itsCollector;
		
		private final BufferStream itsStream = new BufferStream();
		private ReplayerWrapper itsReplayer;
		
		public ThreadReplayerThread(int aThreadId, EventCollector aCollector)
		{
			super(ThreadReplayerThread.class.getName()+"-"+aThreadId);
			setDaemon(true);
			
			itsThreadId = aThreadId;
			itsCollector = aCollector;

			itsReplayer = new ReplayerWrapper(
					itsLoader, 
					itsThreadId, 
					itsSnapshot,
					itsConfig, 
					itsDatabase, 
					itsCollector, 
					itsTmpIdManager, 
					itsStream);

			start();
		}

		public void push(PacketBuffer aBuffer)
		{
//			if (itsThreadId != 1) return;
			if (itsReplayThreadId > 0 && itsReplayThreadId != itsThreadId) return;
			itsStream.pushBuffer(aBuffer);
		}
		
		@Override
		public void run()
		{
			itsReplayer.replay();
		}
	}
	
}
