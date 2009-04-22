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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import tod.core.DebugFlags;

import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;

public class MonitoringClient implements RIMonitoringClient
{
	private static final MonitoringClient INSTANCE = new MonitoringClient();

	/**
	 * Retrieves the singleton instance.
	 */
	public static MonitoringClient get()
	{
		return INSTANCE;
	}
	
	private Map<MonitorId, MonitorWrapper> itsMonitorsMap = 
		new HashMap<MonitorId, MonitorWrapper>();
	
	private static int itsCurrentId;

	private synchronized int nextId()
	{
		return itsCurrentId++;
	}

	public MonitorId createId(TaskMonitor aMonitor, RIMonitoringServer aServer)
	{
		MonitorId theId = new MonitorId(nextId());
		if (DebugFlags.TRACE_MONITORING) System.out.println("[MonitoringClient] "+Thread.currentThread()+" created: "+theId);
		itsMonitorsMap.put(theId, new MonitorWrapper(theId, aMonitor, aServer));
		return theId;
	}
	
	public void destroyId(MonitorId aId)
	{
		if (DebugFlags.TRACE_MONITORING) System.out.println("[MonitoringClient] "+Thread.currentThread()+" destroyed: "+aId);
		itsMonitorsMap.remove(aId);
	}
	
	private static class MonitorWrapper implements IEventListener<Void>
	{
		private final MonitorId itsId;
		private final TaskMonitor itsMonitor;
		private final RIMonitoringServer itsServer;
		
		private MonitorWrapper(MonitorId aId, TaskMonitor aMonitor, RIMonitoringServer aServer)
		{
			itsId = aId;
			itsMonitor = aMonitor;
			itsServer = aServer;
			itsMonitor.eCancelled().addListener(this);
		}

		public void fired(IEvent< ? extends Void> aEvent, Void aData)
		{
			if (DebugFlags.TRACE_MONITORING) System.out.println("Monitor canceled: "+itsId);
			itsServer.monitorCancelled(itsId);
		}
	}
	
	/**
	 * Identifies a monitor across RMI process boundaries.
	 * To call a method that expects a MonitorId as a first argument,
	 * simply pass {@link MonitorId#get()}; the {@link RemoteMonitorWrapper}
	 * aspect takes care of providing a correct monitor id and doing
	 * the bookkeeping.
	 * @author gpothier
	 */
	public static class MonitorId implements Serializable
	{
		private static final long serialVersionUID = 1776540222874612876L;
		
		public final int id;

		private MonitorId(int aId)
		{
			id = aId;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MonitorId other = (MonitorId) obj;
			if (id != other.id) return false;
			return true;
		}
		
		@Override
		public String toString()
		{
			return "MonitorId: "+id;
		}
		
		/**
		 * MonitorId.get() should be passed to the methods that expect a {@link MonitorId}
		 * as a parameter. The actual value will be provided by RemoteMonitorWrapper.
		 */
		public static MonitorId get()
		{
			return null;
		}
	}
}
