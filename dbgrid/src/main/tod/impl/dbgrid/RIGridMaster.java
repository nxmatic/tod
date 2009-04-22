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
package tod.impl.dbgrid;

import java.util.List;

import tod.core.config.TODConfig;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ITypeInfo;
import tod.impl.dbgrid.aggregator.QueryAggregator;
import tod.impl.dbgrid.aggregator.RIQueryAggregator;
import tod.impl.dbgrid.db.NodeRejectedException;
import tod.impl.dbgrid.db.RIBufferIterator;
import tod.impl.dbgrid.db.ObjectsDatabase.Decodable;
import tod.impl.dbgrid.dispatch.RINodeConnector;
import tod.impl.dbgrid.dispatch.RINodeConnector.StringSearchHit;
import tod.tools.monitoring.RIMonitoringServerProvider;
import tod.tools.monitoring.RemoteLinker;
import tod.utils.remote.RIStructureDatabase;
import zz.utils.ITask;
import zz.utils.monitoring.Monitor.MonitorData;
import zz.utils.srpc.IRemote;

/**
 * Remote interface of the grid master.
 * This is the entry point to the database grid.
 * Manages configuration and discovery of database nodes,
 * acts as a factory for {@link GridEventCollector}s
 * and {@link QueryAggregator}.
 * @author gpothier
 */
public interface RIGridMaster extends IRemote, RIMonitoringServerProvider
{
	/**
	 * Returns the configuration of this master.
	 */
	public TODConfig getConfig();
	
	/**
	 * Returns the configuration of this master.
	 */
	public void setConfig(TODConfig aConfig);
	
	/**
	 * Adds a listener to this master.
	 * Client: frontend
	 */
	public void addListener(RIGridMasterListener aListener);

	public void removeListener(RIGridMasterListener aListener);
	
	/**
	 * Clears the database managed by this master.
	 * Client: frontend
	 */
	public void clear();
	
	/**
	 * Ensures that all buffered data is pushed to the nodes.
	 */
	public void flush();
	
	/**
	 * Disconnects from currently connected debuggees.
	 */
	public void disconnect();
	
	/**
	 * Tells the agent to enable/disable trace capture.
	 */
	public void sendEnableCapture(boolean aEnable);

	/**
	 * Registers a node so that it can be used by the grid.
	 * @throws NodeRejectedException Thrown if the master refuses the new node
	 * @return The id assigned to the node.
	 */
	public int registerNode(RINodeConnector aNode, String aHostname) 
		throws NodeRejectedException;
	
	/**
	 * Dispatch nodes call this method when they encounter an exception.
	 * Client: dispatch nodes
	 */
	public void nodeException(NodeException aException);
	
	/**
	 * Dispatch nodes can periodically send monitoring data.
	 * Client: nodes
	 */
	public void pushMonitorData(int aNodeId, MonitorData aData);

	/**
	 * Returns a new query aggregator for the specified query
	 * Client: frontend 
	 */
	@RemoteLinker
	public RIQueryAggregator createAggregator(IGridEventFilter aFilter);
		
	/**
	 * Returns all the threads registered during the execution of the
	 * debugged program, in no particular order.
	 * Client: frontend 
	 */
	public List<IThreadInfo> getThreads();
	
	/**
	 * Returns all the hosts registered during the execution of the
	 * debugged program, in no particular order.
	 * Client: frontend 
	 */
	public List<IHostInfo> getHosts();
	
	/**
	 * Returns the number of events stored by the nodes of this master.
	 * Client: frontend 
	 */
	public long getEventsCount();
	
	/**
	 * Returns the number of dropped events.
	 * Client: frontend 
	 */
	public long getDroppedEventsCount();
	
	/**
	 * Returns the timestamp of the first event recorded in this log.
	 * Client: frontend 
	 */
	public long getFirstTimestamp();
	
	/**
	 * Returns the timestamp of the last event recorded in this log.
	 * Client: frontend 
	 */
	public long getLastTimestamp();
	
	/**
	 * Returns a remote locations repository.
	 * Client: frontend 
	 */
	@RemoteLinker
	public RIStructureDatabase getRemoteStructureDatabase();

	/**
	 * Returns an object registered by the database, or null
	 * if not found.
	 */
	public Decodable getRegisteredObject(long aId);
	
	/**
	 * Returns the type of an object registered by the database.
	 */
	public ITypeInfo getObjectType(long aId);

	/**
	 * See {@link ILogBrowser#exec(ITask)}
	 */
	public <O> O exec(ITask<ILogBrowser, O> aTask);
	
	/**
	 * Searches a text in the registered strings.
	 * @return An iterator that returns matching strings in order of relevance.
	 */
	@RemoteLinker
	public RIBufferIterator<StringSearchHit[]> searchStrings(String aSearchText);

	/**
	 * If the {@link TODConfig#MASTER_TIMEOUT} configuration attribute is set,
	 * the clients must periodically call this method to prevent the grid master from
	 * shutting down.
	 */
	public void keepAlive();

	/**
	 * Returns the id of the specified behavior (for exception processing).
	 * This metehod is called by the database nodes' exception resolver.
	 * @see NodeExceptionResolver 
	 */
	public int getBehaviorId(String aClassName, String aMethodName, String aMethodSignature);
	
	/**
	 * Returns the number of events that occurred within the given behavior.
	 */
	public long[] getEventCountAtBehaviors(int[] aBehaviorIds);

	/**
	 * Returns the number of events that occurred within the given class.
	 */
	public long[] getEventCountAtClasses(int[] aClassIds);
}
