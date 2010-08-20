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
import tod.impl.server.BufferStream;

/**
 * Wraps a {@link ThreadReplayer} so as to solve the classloading issue (existing classes such
 * as {@link ReplayerFrame} are modified on the fly).
 * @author gpothier
 */
public class ReplayerWrapper
{
	private final ReplayerLoader itsLoader;
	private final Object itsReplayer;
	private Method itsReplayMethod;
	
	public ReplayerWrapper(
			ReplayerLoader aLoader,
			int aThreadId,
			LocalsSnapshot aSnapshot,
			TODConfig aConfig, 
			IMutableStructureDatabase aDatabase, 
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		try
		{
			itsLoader = aLoader;
			itsReplayer = itsLoader.createReplayer(aSnapshot, aThreadId, aCollector, aTmpIdManager, aBuffer);
			itsReplayMethod = itsReplayer.getClass().getMethod("replay");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}
	
	public void replay()
	{
		try
		{
			try
			{
				itsReplayMethod.invoke(itsReplayer);
			}
			catch (InvocationTargetException e)
			{
				String theExceptionName = e.getTargetException().getClass().getName();
				if (SkipThreadException.class.getName().equals(theExceptionName)) // Because of class loading, we must compare by name
					throw new SkipThreadException();
				else throw e.getTargetException();
			}
		}
		catch (SkipThreadException e)
		{
			System.out.println("Thread skipped");
		}
		catch (RuntimeException e)
		{
			System.err.println("Runtime exception catched by ReplayerWrapper: "+e.getClass()+": "+e.getMessage());
			e.printStackTrace();
			throw e;
		}
		catch (Throwable e)
		{
			System.err.println("Exception catched by ReplayerWrapper: "+e.getClass()+": "+e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	

}
