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

import java.util.Map;
import java.util.WeakHashMap;

import zz.utils.srpc.IRemote;

/**
 * Manages groups of {@link IRemote} objects.
 * The representative of the group is an instance of {@link RIMonitoringServerProvider}.
 * Whenever a call to a method of an object of the group returns a remote object,
 * this object is made part of the group.
 * The idea is that each group should correspond to one remote JVM.
 * @author gpothier
 */
public class RMIGroupManager
{
	private static RMIGroupManager INSTANCE = new RMIGroupManager();

	/**
	 * Retrieves the singleton instance.
	 */
	public static RMIGroupManager get()
	{
		return INSTANCE;
	}
	
	private RMIGroupManager()
	{
	}

	
	/**
	 * Maps remote objects to their group representative.
	 */
	private Map<IRemote, RIMonitoringServerProvider> itsGroupdMap =
		new WeakHashMap<IRemote, RIMonitoringServerProvider>();
	
	private Map<RIMonitoringServerProvider, RIMonitoringServer> itsProvidersMap =
		new WeakHashMap<RIMonitoringServerProvider, RIMonitoringServer>();
	
	/**
	 * Retrieves the server corresponding to the given provider.
	 * If this is the first time we see this server, we register the
	 * local {@link MonitoringClient}.
	 */
	private synchronized RIMonitoringServer getServer(RIMonitoringServerProvider aProvider)
	{
		RIMonitoringServer theServer = itsProvidersMap.get(aProvider);
		if (theServer == null)
		{
			theServer = aProvider.getMonitoringServer();
			itsProvidersMap.put(aProvider, theServer);
		}
		return theServer;
	}
	
	
	/**
	 * Adds a link from called to result, meaning result was returned 
	 * by a call to called. 
	 */
	public void addLink(IRemote aCalled, IRemote aResult)
	{
		if (aCalled instanceof RIMonitoringServerProvider)
		{
			RIMonitoringServerProvider theProvider = (RIMonitoringServerProvider) aCalled;
			itsGroupdMap.put(aResult, theProvider);
		}
		else 
		{
			RIMonitoringServerProvider theProvider = itsGroupdMap.get(aCalled);
			if (theProvider == null) throw new RuntimeException("Called remote object "+aCalled+", which was not registered.");
			itsGroupdMap.put(aResult, theProvider);
		}
	}
	
	/**
	 * Returns the server provided by the representative of the given remote object, if available.
	 */
	public RIMonitoringServer getServer(IRemote aObject)
	{
		RIMonitoringServerProvider theProvider;
		if (aObject instanceof RIMonitoringServerProvider) theProvider = (RIMonitoringServerProvider) aObject;
		else theProvider = itsGroupdMap.get(aObject);
		
		if (theProvider == null) throw new RuntimeException("Remote object not registered: "+aObject);
		return getServer(theProvider);
	}
}
