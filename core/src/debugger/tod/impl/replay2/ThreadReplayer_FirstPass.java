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
package tod.impl.replay2;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.server.BufferStream;
import zz.utils.Utils;

public class ThreadReplayer_FirstPass extends ThreadReplayer
{
	/**
	 * Minimum number of messages between snapshots.
	 * Usually snapshots are taken (roughly) after each SYNC, but if there are too few
	 * messages since the previous SYNC, the snapshot is deferred.
	 */
	private static final int MIN_MESSAGES_BETWEEN_SNAPSHOTS = 1000;
	
	private int itsSnapshotSeq = 1;
	private int itsMessagesSinceLastSnapshot = 0;
	
	public ThreadReplayer_FirstPass(
			ReplayerLoader aLoader,
			int aThreadId,
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		super(aLoader, aThreadId, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer);
	}

	@Override
	protected ReplayerGenerator createReplayerGenerator(
			ReplayerLoader aLoader,
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase)
	{
		return new ReplayerGenerator_FirstPass(aLoader, aConfig, aDatabase);
	}

	@Override
	public byte getNextMessage()
	{
		itsMessagesSinceLastSnapshot++;
		return super.getNextMessage();
	}
	
	int itsSnapshotCount = 0;
	
	@Override
	public LocalsSnapshot createSnapshot(int aProbeId)
	{
		Utils.println("Creating snapshot: probe %d, #%d.", aProbeId, itsSnapshotCount++);
		return new LocalsSnapshot(
				getStream().getPacketStartOffset(), 
				getStream().position(), 
				getStack().peek(),
				aProbeId,
				0, // TODO: compress stacks
				getStack().toArray(),
				getTracedMethodsVersion(),
				getBehIdReceiver().getCurrentValue(),
				getObjIdReceiver().getCurrentValue());
	}

	@Override
	public int getSnapshotSeq()
	{
		return itsSnapshotSeq;
	}
	
	@Override
	public LocalsSnapshot getSnapshotForResume()
	{
		return null;
	}
	
	@Override
	public int getStartProbe()
	{
		return 0;
	}
	
	@Override
	public void registerSnapshot(LocalsSnapshot aSnapshot)
	{
		getCollector().localsSnapshot(aSnapshot);
		itsMessagesSinceLastSnapshot = 0;
	}
	
	@Override
	public void replay()
	{
		UnmonitoredReplayerFrame theRootFrame = createUnmonitoredFrame(null, null, null);
		theRootFrame.setRootFrame(true);
		theRootFrame.invoke_OOS();		
		System.out.println("ThreadReplayer.replay()");
	}
	
	@Override
	protected void processSync(BufferStream aBuffer)
	{
		super.processSync(aBuffer);
		if (itsMessagesSinceLastSnapshot >= MIN_MESSAGES_BETWEEN_SNAPSHOTS) itsSnapshotSeq++;
	}

}
