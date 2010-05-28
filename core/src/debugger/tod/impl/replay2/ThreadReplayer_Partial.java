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
import tod.core.database.structure.IStructureDatabase;
import tod.impl.server.BufferStream;

public class ThreadReplayer_Partial extends ThreadReplayer
{
	public ThreadReplayer_Partial(
			ReplayerLoader aLoader,
			int aThreadId,
			TODConfig aConfig,
			IStructureDatabase aDatabase,
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		super(aLoader, aThreadId, aConfig, aDatabase, aCollector, aTmpIdManager, aBuffer);
		
	}

	@Override
	public boolean isSnapshotDue()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerSnapshot(LocalsSnapshot aSnapshot)
	{
		throw new UnsupportedOperationException();
	}

	public LocalsSnapshot createSnapshot(int aProbeId)
	{
		throw new UnsupportedOperationException();
	}

	public int getSnapshotSeq()
	{
		throw new UnsupportedOperationException();
	}

	public LocalsSnapshot getSnapshotForResume()
	{
		throw new UnsupportedOperationException();
	}

	public void replay()
	{
		throw new UnsupportedOperationException();
	}
}
