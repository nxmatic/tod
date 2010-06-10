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
import tod.impl.server.DBSideIOThread;
import zz.utils.Utils;

public class PartialReplayTest
{
	
	private static void partialReplay(File aEventsFile, TODConfig aConfig, IMutableStructureDatabase aDatabase, LocalsSnapshot aSnapshot) throws IOException
	{
		System.out.println("Partial replay of snapshot at: "+aSnapshot.getProbeId());
		FileInputStream fis = new FileInputStream(aEventsFile);
		long theBytesToSkip = aSnapshot.getPacketStartOffset();
		while(theBytesToSkip > 0) theBytesToSkip -= fis.skip(theBytesToSkip);
		
		DBSideIOThread theIOThread = new DBSideIOThread(aConfig, aDatabase, fis, aSnapshot)
		{
			@Override
			protected EventCollector createCollector(int aThreadId)
			{
				MyEventCollector theCollector = new MyEventCollector();
//				theCollectors.add(theCollector);
				return theCollector;
			}
		};
		
		theIOThread.setInitialSkip(aSnapshot.getPacketOffset());
		theIOThread.run();
		System.out.println("Done");
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
		
		for (MyEventCollector theCollector : theCollectors)
		{
			int i=0;
			for(LocalsSnapshot theSnapshot : theCollector.getSnapshots())
			{
				Utils.println("Snapshot #%d", i++);
				partialReplay(theEventsFile, theConfig, theDatabase, theSnapshot);
			}
		}

		Thread.sleep(1000);
		System.err.println("END");
	}
	

	private static class MyEventCollector extends EventCollector
	{
		private List<LocalsSnapshot> itsSnapshots = new ArrayList<LocalsSnapshot>();
		
		public List<LocalsSnapshot> getSnapshots()
		{
			return itsSnapshots;
		}
		
		@Override
		public void localsSnapshot(LocalsSnapshot aSnapshot)
		{
			itsSnapshots.add(aSnapshot);
		}
		
	}
}
