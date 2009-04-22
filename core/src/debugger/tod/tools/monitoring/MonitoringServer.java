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
package tod.tools.monitoring;

import java.util.HashMap;
import java.util.Map;

import tod.core.DebugFlags;
import tod.tools.monitoring.MonitoringClient.MonitorId;

public class MonitoringServer implements RIMonitoringServer
{
	private static final MonitoringServer INSTANCE = new MonitoringServer();

	/**
	 * Retrieves the singleton instance.
	 */
	public static MonitoringServer get()
	{
		return INSTANCE;
	}
	
	private Map<MonitorId, TaskMonitor> itsMonitorsMap =
		new HashMap<MonitorId, TaskMonitor>();
	
	private MonitoringServer() 
	{
	}

	public void monitorCancelled(MonitorId aId)
	{
		TaskMonitor theMonitor = itsMonitorsMap.get(aId);
		if (theMonitor == null) return; // The monitored task has already finished
		if (DebugFlags.TRACE_MONITORING) System.out.println("Monitor cancelled: "+aId);
		theMonitor.cancel();
	}

	/**
	 * Assigns a monitor to a monitor id.
	 */
	public void assign(MonitorId aId, TaskMonitor aMonitor)
	{
		if (DebugFlags.TRACE_MONITORING) System.out.println("Assigning monitor "+aId);
		assert aId != null;
		assert aMonitor != null;
		itsMonitorsMap.put(aId, aMonitor);
	}
	
	/**
	 * Removes the monitor assigned to the given id.
	 */
	public void delete(MonitorId aId)
	{
		if (DebugFlags.TRACE_MONITORING) System.out.println("Deleting monitor "+aId);
		TaskMonitor theMonitor = itsMonitorsMap.remove(aId);
		if (theMonitor == null) throw new RuntimeException("No monitor for id: "+aId);		
	}
}
