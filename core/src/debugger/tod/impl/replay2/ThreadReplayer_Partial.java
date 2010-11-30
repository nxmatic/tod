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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase.SnapshotProbeInfo;
import tod.impl.server.BufferStream;
import tod.impl.server.BufferStream.EndOfStreamException;
import tod2.agent.Message;
import zz.utils.Utils;

public class ThreadReplayer_Partial extends ThreadReplayer
{
	private LocalsSnapshot itsSnapshot;
	private boolean itsKillOnSnapshot = false;
	private int itsMessagesSinceLastSnapshot = 0;

	public ThreadReplayer_Partial(
			ReplayerLoader aLoader,
			int aThreadId,
			TODConfig aConfig,
			IMutableStructureDatabase aDatabase,
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer,
			LocalsSnapshot aSnapshot)
	{
		super(aLoader, aThreadId, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer);
		itsSnapshot = aSnapshot;
	}

	@Override
	public byte getNextMessage()
	{
		itsMessagesSinceLastSnapshot++;
		return super.getNextMessage();
	}

	@Override
	public void registerSnapshot(LocalsSnapshot aSnapshot)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public LocalsSnapshot createSnapshot(int aProbeId)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSnapshotSeq()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void checkSnapshotKill()
	{
		if (itsKillOnSnapshot) throw new EndOfStreamException();
	}
	
	@Override
	protected void processSync(long aTimestamp)
	{
		super.processSync(aTimestamp);
		if (itsMessagesSinceLastSnapshot >= MIN_MESSAGES_BETWEEN_SNAPSHOTS) 
			itsKillOnSnapshot = true;
	}

	@Override
	public LocalsSnapshot getSnapshotForResume()
	{
		LocalsSnapshot theSnapshot = itsSnapshot;
		itsSnapshot = null; // Resume only once
		return theSnapshot;
	}
	
	@Override
	public int getStartProbe()
	{
		return itsSnapshot.getProbeId();
	}

	@Override
	public void replay()
	{
		getBehIdReceiver().setCurrentValue(itsSnapshot.getBehIdCurrentValue());
		getObjIdReceiver().setCurrentValue(itsSnapshot.getObjIdCurrentValue());
		
		try
		{
			SnapshotProbeInfo theSnapshotProbeInfo = getDatabase().getSnapshotProbeInfo(itsSnapshot.getProbeId());
			assert theSnapshotProbeInfo != null : ""+itsSnapshot.getProbeId();
			
			String theClassName = 
				MethodReplayerGenerator.REPLAY_CLASS_PREFIX 
				+ theSnapshotProbeInfo.behaviorId
				+ "_" + theSnapshotProbeInfo.id;
			
			Class theClass = Class.forName(theClassName);
			Method theInvokeMethod = theClass.getDeclaredMethod("invoke_PartialReplay", ThreadReplayer.class);
			try
			{
				theInvokeMethod.invoke(null, this);
				while(true)
				{
					byte theMessage = peekNextMessage();
					if (theMessage != Message.INSCOPE_BEHAVIOR_ENTER_FROM_OUTOFSCOPE
							&& theMessage != Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_OUTOFSCOPE
							&& theMessage != Message.INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE) break;
					replay_OOS_loop();
				}
			}
			catch (InvocationTargetException e)
			{
				if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
				else throw e;
			}
		}
		catch (EndOfStreamException e)
		{
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally 
		{
			getStream().skipAll(); // Finishes 
		}
	}
	
}
