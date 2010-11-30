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
package tod.core.replay2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.replay2.EventCollector;
import tod.impl.replay2.LocalsSnapshot;
import tod.impl.replay2.ReifyEventCollector;
import tod.impl.replay2.ReifyEventCollector.Event;
import tod.impl.replay2.ReplayerLoader;
import tod.impl.server.DBSideIOThread;
import zz.utils.Utils;

public class PartialReplayTest
{
	private static List<Event> partialReplay(
			File aEventsFile, 
			TODConfig aConfig, 
			IMutableStructureDatabase aDatabase, 
			LocalsSnapshot aSnapshot,
			ReplayerLoader aLoader) throws IOException
	{
		Utils.println(
				"Partial replay of snapshot: %d (block id: %d, packet start offset: %d, packet offset: %d)", 
				aSnapshot.getProbeId(), 
				aSnapshot.getBlockId(),
				aSnapshot.getPacketStartOffset(),
				aSnapshot.getPacketOffset());
		
		FileInputStream fis = new FileInputStream(aEventsFile);
		long theBytesToSkip = aSnapshot.getPacketStartOffset();
		while(theBytesToSkip > 0) theBytesToSkip -= fis.skip(theBytesToSkip);
		
		final ReifyEventCollector theCollector = new ReifyEventCollector();
		final boolean[] theCollectorCreated = {false};
		
		DBSideIOThread theIOThread = new DBSideIOThread(aConfig, aDatabase, fis, aSnapshot, aLoader)
		{
			@Override
			protected EventCollector createCollector(int aThreadId)
			{
				if (theCollectorCreated[0]) throw new RuntimeException(); // Only one thread
				theCollectorCreated[0] = true;
				return theCollector;
			}
		};
		
		theIOThread.setInitialSkip(aSnapshot.getPacketOffset());
		theIOThread.run();
		
		return theCollector.getEvents();
	}
	
	private static int countCFlowEvents(List<Event> aEvents)
	{
		int theCount = 0;
		for(Event theEvent : aEvents) if (theEvent.isCFlowEvent()) theCount++;
		return theCount;
	}
	
	public static void main(String[] args) throws Exception
	{
		final List<MyEventCollector> theCollectors = new ArrayList<MyEventCollector>();
		TODConfig theConfig = new TODConfig();
		File theEventsFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");

		String theScopeMD5 = Utils.md5String(theConfig.get(TODConfig.SCOPE_TRACE_FILTER).getBytes());
		File theDbFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db-"+theScopeMD5+".raw");

		IMutableStructureDatabase theDatabase = StructureDatabase.create(theConfig, theDbFile, true);

		try
		{
			DBSideIOThread theIOThread = new DBSideIOThread(theConfig, theDatabase, new FileInputStream(theEventsFile), null)
			{
				@Override
				protected EventCollector createCollector(int aThreadId)
				{
					MyEventCollector theCollector = new MyEventCollector();
					theCollectors.add(theCollector);
					return theCollector;
				}
			};
			theIOThread.run();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		System.out.println("Replay done.");
		System.out.println("Threads: "+theCollectors.size());
		
		ReplayerLoader theLoader = new ReplayerLoader(DBSideIOThread.class.getClassLoader(), theConfig, theDatabase, false);

		int t = 1;
		for (MyEventCollector theCollector : theCollectors)
		{
			Utils.println("[%d] Replaying %d snapshots", t++, theCollector.getSnapshots().size());
			int i=0;
			int theTotalCount = 0;
			int theTotalCFlowCount = 0;
			for(LocalsSnapshot theSnapshot : theCollector.getSnapshots())
			{
				Utils.println("Snapshot #%d", i++);
				try
				{
					List<Event> theEvents = partialReplay(theEventsFile, theConfig, theDatabase, theSnapshot, theLoader);
					int theCount = theEvents.size();
					int theCFlowCount = countCFlowEvents(theEvents);
					theTotalCount += theCount;
					theTotalCFlowCount += theCFlowCount;
					Utils.println("Count: %d (cflow: %d), total: %d (cflow: %d)", theCount, theCFlowCount, theTotalCount, theTotalCFlowCount);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}

		Thread.sleep(1000);
		System.err.println("END");
	}
	
	private static class MyEventCollector extends EventCollector
	{
		private List<LocalsSnapshot> itsSnapshots = new ArrayList<LocalsSnapshot>();
		private int itsEnterExitCount = 0;
		private int itsEnterExitCountTotal = 0;
		private long itsCurrentBlockId;
		private int itsSnapshotSeq;
		
		public List<LocalsSnapshot> getSnapshots()
		{
			return itsSnapshots;
		}
		
		@Override
		public void localsSnapshot(LocalsSnapshot aSnapshot)
		{
			Utils.println("%d cflow events before snapshot %d.%d (%d total)", itsEnterExitCount, itsCurrentBlockId, itsSnapshotSeq, itsEnterExitCountTotal);
			itsSnapshotSeq++;
			itsEnterExitCount = 0;
			itsSnapshots.add(aSnapshot);
		}
		
		@Override
		public void sync(long aTimestamp)
		{
			Utils.println("%d snapshots since last sync", itsSnapshotSeq);
			itsCurrentBlockId = aTimestamp;
			itsSnapshotSeq = 0;
		}

		@Override
		public void enter(int aBehaviorId, int aArgsCount)
		{
			itsEnterExitCount++;
			itsEnterExitCountTotal++;
		}

		@Override
		public void exit()
		{
			itsEnterExitCount++;
			itsEnterExitCountTotal++;
		}

		@Override
		public void exitException()
		{
			itsEnterExitCount++;
			itsEnterExitCountTotal++;
		}
	}
}
