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
package tod.scheduling;

import tod.Util;
import tod.tools.monitoring.Monitored;
import tod.tools.monitoring.MonitoringServer;
import tod.tools.monitoring.RIMonitoringServer;
import tod.tools.monitoring.MonitoringClient.MonitorId;
import zz.utils.srpc.SRPCRegistry;

public class Server implements RIServer
{
	public RIMonitoringServer getMonitoringServer()
	{
		return MonitoringServer.get();
	}

	public int doTask(MonitorId aId, int aParam) 
	{
		System.out.println("Server.doTask()...");
		try
		{
			int theResult = 0;
			for(int i=0;i<aParam;i++)
			{
				theResult += subtask();
				Thread.sleep(1000);
			}
			
			return theResult;
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			System.out.println("Server.doTask() - Done.");
		}
	}
	
	@Monitored
	private int subtask()
	{
		return 1;
	}
	
	public static void main(String[] args) throws Exception
	{
		SRPCRegistry theRoot = Util.getLocalSRPCRegistry();
		theRoot.bind("server", new Server());
		System.out.println("Bound server");
	}

}
