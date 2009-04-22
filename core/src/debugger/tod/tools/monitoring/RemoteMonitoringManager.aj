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
package tod.tools.monitoring;

import tod.core.DebugFlags;
import tod.tools.monitoring.MonitoringClient.MonitorId;
import zz.utils.srpc.IRemote;
import zz.utils.srpc.SRPCServer;

public aspect RemoteMonitoringManager
{
	pointcut monitoredCall(IRemote aTarget, MonitorId aId): 
		call(* IRemote+.*(MonitorId, ..)) 
		&& !within(tod.tools.monitoring.**)
		&& args(aId, ..) && target(aTarget);
	
	/**
	 * Obtains an id for the current monitor and transmits this id
	 * to the server side.
	 */
	Object around(IRemote aTarget, MonitorId aId): monitoredCall(aTarget, aId)
	{
		if (SRPCServer.isLocal(aTarget)) return proceed(aTarget, aId);

		MonitorId theRealId = null;
		try
		{
			TaskMonitor theMonitor = TaskMonitor.current();

			if (theMonitor != null)
			{
				RIMonitoringServer theServer = RMIGroupManager.get().getServer(aTarget);
				theRealId = MonitoringClient.get().createId(theMonitor, theServer);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		
		try
		{
			return proceed(aTarget, theRealId);
		}
		finally
		{
			if (theRealId != null) MonitoringClient.get().destroyId(theRealId);
		}
	}
	
	pointcut monitoredExec(IRemote aSubject, MonitorId aId): 
		execution(* IRemote+.*(MonitorId, ..)) 
		&& !within(tod.tools.monitoring.**)
		&& args(aId, ..) && this(aSubject);
	
	/**
	 * Receives an id from the client and initializes a monitor.
	 */
	before(IRemote aSubject, MonitorId aId): monitoredExec(aSubject, aId)
	{
		if (SRPCServer.isLocal(aSubject)) return;

		try
		{
			TaskMonitor theMonitor = TaskMonitoring.start();
			MonitoringServer.get().assign(aId, theMonitor);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Cleans up the monitor for this call.
	 */
	after(IRemote aSubject, MonitorId aId): monitoredExec(aSubject, aId)
	{
		if (SRPCServer.isLocal(aSubject)) return;

		try
		{
			MonitoringServer.get().delete(aId);
			TaskMonitoring.stop();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	pointcut remoteCall(IRemote aTarget): 
//		call(IRemote+ IRemote+.*(..))
//		&& !call(* java..*(..)) // avoids trapping calls to Registry
		call(@RemoteLinker IRemote+ *.*(..))
		&& target(aTarget);
	
	/**
	 * Set up the Remote objects groups.
	 */
	after(IRemote aTarget) returning(IRemote aResult): remoteCall(aTarget)
	{
		try
		{
			if (DebugFlags.TRACE_MONITORING) System.out.println("[RemoteMonitoringManager] At: "+thisJoinPoint.toLongString());
			
			if (SRPCServer.isLocal(aTarget))
			{
				if (DebugFlags.TRACE_MONITORING) System.out.println("[RemoteMonitoringManager] Object is local, skipping: "+aTarget);
			}
			else
			{
				if (DebugFlags.TRACE_MONITORING) System.out.println("[RemoteMonitoringManager] Linking "+aTarget+" -> "+aResult);
				RMIGroupManager.get().addLink(aTarget, aResult);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
}
