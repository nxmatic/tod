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

import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tod.Util;
import tod.core.DebugFlags;
import tod.core.ILogCollector;
import tod.core.config.TODConfig;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.server.TODServer;
import tod.core.transport.LogReceiver;
import tod.impl.database.structure.standard.HostInfo;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.dbgrid.aggregator.QueryAggregator;
import tod.impl.dbgrid.aggregator.RIQueryAggregator;
import tod.impl.dbgrid.aggregator.StringHitsAggregator;
import tod.impl.dbgrid.db.DatabaseNode;
import tod.impl.dbgrid.db.NodeRejectedException;
import tod.impl.dbgrid.db.RIBufferIterator;
import tod.impl.dbgrid.db.ObjectsDatabase.Decodable;
import tod.impl.dbgrid.dispatch.DispatcherCollector;
import tod.impl.dbgrid.dispatch.NodeConnector;
import tod.impl.dbgrid.dispatch.NodeProxy;
import tod.impl.dbgrid.dispatch.RINodeConnector;
import tod.impl.dbgrid.dispatch.RINodeConnector.StringSearchHit;
import tod.tools.monitoring.MonitoringServer;
import tod.tools.monitoring.RIMonitoringServer;
import tod.utils.PrintThroughCollector;
import tod.utils.TODUtils;
import tod.utils.remote.RIStructureDatabase;
import tod.utils.remote.RemoteStructureDatabase;
import zz.utils.ITask;
import zz.utils.Utils;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Monitor.MonitorData;
import zz.utils.net.Server;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.properties.IProperty;
import zz.utils.properties.PropertyListener;
import zz.utils.srpc.SRPCRegistry;

/**
 * The entry point to the database grid. Manages configuration and discovery of
 * database nodes, acts as a factory for {@link GridEventCollector}s and
 * {@link QueryAggregator}.
 * 
 * @author gpothier
 */
public class GridMaster implements RIGridMaster
{
	public static final String READY_STRING = "[GridMaster] Ready.";
	public static final String SRPC_ID = "GridMaster";

	private TODConfig itsConfig;

	private List<ListenerData> itsListeners = new ArrayList<ListenerData>();
	
	private final boolean itsStartServer;

	private TODServer itsServer;

	private ILogCollector itsCollector;

	/**
	 * This is the same as {@link #itsCollector}, but only used in a
	 * distributed setting. For local setting, it is null.
	 */
	private DispatcherCollector itsDispatcherCollector;
	
	/**
	 * The socket server that accepts incoming connections from database nodes.
	 * We must keep a reference to it.
	 */
	private MyNodeServer itsNodeServer;

	/**
	 * Number of expected database nodes. If 0, we don't wait for all nodes to
	 * connect.
	 */
	private int itsExpectedNodes;

	private List<RINodeConnector> itsNodes = new ArrayList<RINodeConnector>();

	/**
	 * Set by {@link #keepAlive()}
	 */
	private long itsLastKeepAlive = System.currentTimeMillis();

	private IMutableStructureDatabase itsStructureDatabase;

	private RemoteStructureDatabase itsRemoteStructureDatabase;

	private long itsEventsCount;
	private long itsDroppedEventsCount = -1;
	private long itsObjectsStoreSize;

	private long itsFirstTimestamp;

	private long itsLastTimestamp;

	private int itsThreadCount;

	private List<IThreadInfo> itsThreads = new ArrayList<IThreadInfo>();

	private List<IHostInfo> itsHosts = new ArrayList<IHostInfo>();

	/**
	 * Maps node ids to {@link PrintWriter} objects that write to a log file
	 */
	private Map<Integer, PrintWriter> itsMonitorLogs = new HashMap<Integer, PrintWriter>();

	/**
	 * A log browser for {@link #exec(ITask)}.
	 */
	private GridLogBrowser itsLocalLogBrowser;

	private GridMaster(TODConfig aConfig, StructureDatabase aStructureDatabase, int aExpectedNodes, boolean aStartServer)
	{
		itsConfig = aConfig;
		itsStructureDatabase = aStructureDatabase;
		itsRemoteStructureDatabase = RemoteStructureDatabase.create(aStructureDatabase, true);
		itsExpectedNodes = aExpectedNodes;
		itsStartServer = aStartServer;

		itsLocalLogBrowser = DebuggerGridConfig.createLocalLogBrowser(null, this);

		createTimeoutThread();
	}

	/**
	 * Creates a master with a single database node.
	 */
	public static GridMaster createLocal(
			TODConfig aConfig, 
			StructureDatabase aStructureDatabase,
			DatabaseNode aDatabaseNode, 
			boolean aStartServer)
	{
		GridMaster theMaster = new GridMaster(aConfig, aStructureDatabase, 0, aStartServer);

		aDatabaseNode.connectedToMaster(theMaster, 0);
		theMaster.itsCollector = aDatabaseNode.createLogCollector(new HostInfo(0));
		
		if (DebugFlags.COLLECTOR_LOG) theMaster.itsCollector = new PrintThroughCollector(
				new HostInfo(0, "print"),
				theMaster.itsCollector,
				aStructureDatabase);
		
		theMaster.itsNodes.add(new NodeConnector(aDatabaseNode));

		return theMaster;
	}

	/**
	 * Initializes a grid master. After calling the constructor, the
	 * {@link #waitReady()} method should be called to wait for the nodes to
	 * connect.
	 */
	public static GridMaster create(
			TODConfig aConfig, 
			StructureDatabase aStructureDatabase, 
			int aExpectedNodes)
	{
		GridMaster theMaster = new GridMaster(aConfig, aStructureDatabase, aExpectedNodes, true);

		theMaster.itsNodeServer = theMaster.new MyNodeServer();
		theMaster.itsDispatcherCollector = new DispatcherCollector();
		theMaster.itsCollector = theMaster.itsDispatcherCollector;

		return theMaster;
	}

	public RIMonitoringServer getMonitoringServer()
	{
		return MonitoringServer.get();
	}

	/**
	 * For testing only.
	 */
	public ILogCollector _getCollector()
	{
		return itsCollector;
	}
	
	public GridLogBrowser _getLocalLogBrowser()
	{
		return itsLocalLogBrowser;
	}
	
	private void createTimeoutThread()
	{
		Integer theTimeout = getConfig().get(TODConfig.MASTER_TIMEOUT);
		if (theTimeout != null && theTimeout > 0)
		{
			new TimeoutThread(theTimeout * 1000).start();
		}
	}

	public TODConfig getConfig()
	{
		return itsConfig;
	}

	public void setConfig(TODConfig aConfig)
	{
		itsConfig = aConfig;
		itsServer.setConfig(aConfig);
		for (RINodeConnector theConnector : itsNodes) theConnector.setConfig(aConfig);
	}

	/**
	 * Creates the TOD server, which is in charge of accepting connections from
	 * clients.
	 */
	protected TODServer createServer()
	{
		TODServer theServer = TODServer.getFactory(getConfig()).create(
				getConfig(), 
				getStructureDatabase(),
				itsCollector);

		theServer.pConnected().addHardListener(new PropertyListener<Boolean>()
		{
			@Override
			public void propertyChanged(IProperty<Boolean> aProperty, Boolean aOldValue, Boolean aNewValue)
			{
				if (aNewValue == false)
				{
					flush();
				}
			}
		});
		
		theServer.eException().addListener(new IEventListener<Throwable>()
		{
			public void fired(IEvent< ? extends Throwable> aEvent, Throwable aData)
			{
				fireException(aData);
			}
		});
		
		theServer.pCaptureEnabled().addHardListener(new PropertyListener<Boolean>()
		{
			@Override
			public void propertyChanged(IProperty<Boolean> aProperty, Boolean aOldValue, Boolean aNewValue)
			{
				fireCaptureEnabled(aNewValue);
			}
		});

		return theServer;
	}

	/**
	 * Waits until all nodes and dispatchers are properly connected.
	 */
	public void waitReady()
	{
		// Wait for all expected database nodes to connect
		try
		{
			while (itsExpectedNodes > 0 && itsNodes.size() < itsExpectedNodes)
				wait(1000);
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}

		if (itsStartServer) itsServer = createServer();
		ready();
	}

	/**
	 * Called when the dispatching tree (dispatchers and db nodes) is set up.
	 */
	protected void ready()
	{
		Timer theTimer = new Timer(true);
		theTimer.schedule(new DataUpdater(), 5000, 3000);

		System.out.println(READY_STRING);
	}

	public void keepAlive()
	{
		itsLastKeepAlive = System.currentTimeMillis();
	}

	/**
	 * Stops accepting new connections from debuggees.
	 */
	public void stop()
	{
		itsServer.close();
	}

	public void disconnect()
	{
		itsServer.disconnect();
	}
	
	public void sendEnableCapture(boolean aEnable)
	{
		itsServer.pCaptureEnabled().set(aEnable);
	}


	public void addListener(RIGridMasterListener aListener)
	{
		System.out.println("[GridMaster] addListener...");
		ListenerData theListenerData = new ListenerData(aListener);
		itsListeners.add(theListenerData);
		// theListenerData.fireEventsReceived();
		System.out.println("[GridMaster] addListener done.");
	}
	
	

	public void removeListener(RIGridMasterListener aListener)
	{
		for (Iterator<ListenerData> theIterator = itsListeners.iterator(); theIterator.hasNext();)
		{
			ListenerData theListenerData = theIterator.next();
			if (theListenerData.getListener().equals(aListener))
			{
				theIterator.remove();
				break;
			}
		}
	}

	public void pushMonitorData(int aNodeId, MonitorData aData)
	{
		// System.out.println("Received monitor data from node
		// #"+aNodeId+"\n"+Monitor.format(aData, false));

		PrintWriter theWriter = itsMonitorLogs.get(aNodeId);
		if (theWriter == null)
		{
			try
			{
				theWriter = new PrintWriter(new FileWriter("log-" + aNodeId + ".txt"));
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			itsMonitorLogs.put(aNodeId, theWriter);
		}

		theWriter.println();
		theWriter.println(Monitor.format(aData, false));
		theWriter.flush();

		fireMonitorData(aNodeId, aData);
	}

	/**
	 * Fires the {@link RIGridMasterListener#eventsReceived()} message to all
	 * listeners.
	 */
	protected void fireEventsReceived()
	{
		for (Iterator<ListenerData> theIterator = itsListeners.iterator(); theIterator.hasNext();)
		{
			ListenerData theListenerData = theIterator.next();
			if (!theListenerData.fireEventsReceived())
			{
				System.out.println("Removing stale listener");
				theIterator.remove();
			}
		}
	}

	/**
	 * Fires the {@link RIGridMasterListener#eventsReceived()} message to all
	 * listeners.
	 */
	protected void fireMonitorData(int aNodeId, MonitorData aData)
	{
		for (Iterator<ListenerData> theIterator = itsListeners.iterator(); theIterator.hasNext();)
		{
			ListenerData theListenerData = theIterator.next();
			if (!theListenerData.fireMonitorData(aNodeId, aData))
			{
				System.out.println("Removing stale listener");
				theIterator.remove();
			}
		}
	}

	/**
	 * Fires the {@link RIGridMasterListener#exception(Throwable)} message to all
	 * listeners.
	 */
	public void fireException(Throwable aThrowable)
	{
		System.err.println("Exception catched in master, will be forwarded to clients.");
		aThrowable.printStackTrace();

		for (Iterator<ListenerData> theIterator = itsListeners.iterator(); theIterator.hasNext();)
		{
			ListenerData theListenerData = theIterator.next();
			if (!theListenerData.fireException(aThrowable))
			{
				System.out.println("Removing stale listener");
				theIterator.remove();
			}
		}
	}

	/**
	 * Fires the {@link RIGridMasterListener#captureEnabled(boolean)} message to all
	 * listeners.
	 */
	protected void fireCaptureEnabled(boolean aEnabled)
	{
		for (Iterator<ListenerData> theIterator = itsListeners.iterator(); theIterator.hasNext();)
		{
			ListenerData theListenerData = theIterator.next();
			if (!theListenerData.fireCaptureEnabled(aEnabled))
			{
				System.out.println("Removing stale listener");
				theIterator.remove();
			}
		}
	}

	public synchronized int registerNode(RINodeConnector aNode, String aHostname) throws NodeRejectedException
	{
		itsNodes.add(aNode);
		int theId = itsNodes.size();

		// Register the node in the RMI registry.
		Util.getLocalSRPCRegistry().bind("node-" + theId, aNode);

		return theId;
	}

	public synchronized void nodeException(NodeException aException)
	{
		System.err.println(String.format("Received exception %s from node %s", aException.getMessage(), aException
				.getNodeId()));
	}

	/**
	 * Returns the currently registered nodes.
	 */
	public List<RINodeConnector> getNodes()
	{
		return itsNodes;
	}

	/**
	 * Returns the number of registered nodes.
	 */
	public int getNodeCount()
	{
		return getNodes().size();
	}

	public void clear()
	{
		for (RINodeConnector theConnector : itsNodes)
			theConnector.clear();

		itsThreads.clear();
		itsHosts.clear();
		updateStats();

		itsLocalLogBrowser = DebuggerGridConfig.createLocalLogBrowser(null, this);
	}

	/**
	 * Ensures that all buffered data is pushed to the nodes.
	 */
	public void flush()
	{
		for (RINodeConnector theConnector : itsNodes)
			theConnector.flush();
	}

	/**
	 * Registers a thread. Should be used by the {@link LogReceiver} created by
	 * the root dispatcher
	 * 
	 * @see AbstractEventDispatcher#createLogReceiver(GridMaster,
	 *      tod.core.LocationRegistrer, Socket)
	 */
	public void registerThread(IThreadInfo aThreadInfo)
	{
		itsThreads.add(aThreadInfo);
		((HostInfo) aThreadInfo.getHost()).addThread(aThreadInfo);
	}

	/**
	 * Registers a host. Should be used by the {@link LogReceiver} created by
	 * the root dispatcher
	 * 
	 * @see AbstractEventDispatcher#createLogReceiver(GridMaster,
	 *      tod.core.LocationRegistrer, Socket)
	 */
	public void registerHost(IHostInfo aHostInfo)
	{
		itsHosts.add(aHostInfo);
	}

	public List<IThreadInfo> getThreads()
	{
		System.out.println("[GridMaster] getThreads - will return " + itsThreads.size() + " threads.");
		return itsThreads;
	}

	public List<IHostInfo> getHosts()
	{
		return itsHosts;
	}

	public RIQueryAggregator createAggregator(IGridEventFilter aCondition)
	{
		TODUtils.logf(2, "[GridMaster] Creating aggregator for conditions: %s", aCondition);
		return new QueryAggregator(this, aCondition);
	}

	public long getEventsCount()
	{
		if (itsEventsCount == 0) updateStats();
		return itsEventsCount;
	}

	public long getDroppedEventsCount()
	{
		if (itsDroppedEventsCount == -1) updateStats();
		return itsDroppedEventsCount;
	}
	
	public long getFirstTimestamp()
	{
		if (itsFirstTimestamp == 0) updateStats();
		return itsFirstTimestamp;
	}

	public long getLastTimestamp()
	{
		if (itsLastTimestamp == 0) updateStats();
		return itsLastTimestamp;
	}

	public Decodable getRegisteredObject(final long aId)
	{
		List<Decodable> theResults = Utils.fork(getNodes(), new ITask<RINodeConnector, Decodable>()
		{
			public Decodable run(RINodeConnector aInput)
			{
				return aInput.getRegisteredObject(aId);
			}
		});

		Decodable theObject = null;
		for (Decodable theResult : theResults)
		{
			if (theResult == null) continue;
			if (theObject != null) throw new RuntimeException("Object present in various nodes!");
			theObject = theResult;
		}

		return theObject;
	}
	
	public ITypeInfo getObjectType(final long aId)
	{
		List<ITypeInfo> theResults = Utils.fork(getNodes(), new ITask<RINodeConnector, ITypeInfo>()
		{
			public ITypeInfo run(RINodeConnector aInput)
			{
				return aInput.getObjectType(aId);
			}
		});
		
		ITypeInfo theType = null;
		for (ITypeInfo theResult : theResults)
		{
			if (theResult == null) continue;
			if (theType != null) throw new RuntimeException("Object present in various nodes!");
			theType = theResult;
		}
		
		return theType;
	}
	
	public RIBufferIterator<StringSearchHit[]> searchStrings(String aSearchText)
	{
		return new StringHitsAggregator(this, aSearchText);
	}

	protected void updateStats()
	{
		long theEventsCount = 0;
		long theDroppedEventsCount = 0;
		long theObjectsStoreSize = 0;
		long theFirstTimestamp = Long.MAX_VALUE;
		long theLastTimestamp = 0;

		for (RINodeConnector theNode : getNodes())
		{
			theEventsCount += theNode.getEventsCount();
			theDroppedEventsCount += theNode.getDroppedEventsCount();
			theObjectsStoreSize += theNode.getObjectsStoreSize();
			theFirstTimestamp = Math.min(theFirstTimestamp, theNode.getFirstTimestamp());
			theLastTimestamp = Math.max(theLastTimestamp, theNode.getLastTimestamp());
		}

		// When there are no nodes:
		if (theFirstTimestamp > theLastTimestamp) theFirstTimestamp = theLastTimestamp;
		itsEventsCount = theEventsCount;
		itsDroppedEventsCount = theDroppedEventsCount;
		itsObjectsStoreSize = theObjectsStoreSize;
		itsFirstTimestamp = theFirstTimestamp;
		itsLastTimestamp = theLastTimestamp;
	}

	public IMutableStructureDatabase getStructureDatabase()
	{
		return itsStructureDatabase;
	}

	public RIStructureDatabase getRemoteStructureDatabase()
	{
		System.out.println("GridMaster.getStructureDatabase()");
		return itsRemoteStructureDatabase;
	}

	public int getBehaviorId(String aClassName, String aMethodName, String aMethodSignature)
	{
		return itsStructureDatabase.getBehaviorId(aClassName, aMethodName, aMethodSignature);
	}

	public <O> O exec(ITask<ILogBrowser, O> aTask)
	{
		return aTask.run(itsLocalLogBrowser);
	}

	public long[] getEventCountAtBehaviors(final int[] aBehaviorIds)
	{
		List<long[]> theResults = Utils.fork(getNodes(), new ITask<RINodeConnector, long[]>()
		{
			public long[] run(RINodeConnector aInput)
			{
				return aInput.getEventCountAtBehaviors(aBehaviorIds);
			}
		});

		long[] theCounts = new long[aBehaviorIds.length];
		for (long[] theResult : theResults)
		{
			for(int i=0;i<theCounts.length;i++)
			{
				theCounts[i] += theResult[i];
			}
		}

		return theCounts;
	}
	
	public long[] getEventCountAtClasses(final int[] aClassIds)
	{
		List<long[]> theResults = Utils.fork(getNodes(), new ITask<RINodeConnector, long[]>()
		{
			public long[] run(RINodeConnector aInput)
			{
				return aInput.getEventCountAtClasses(aClassIds);
			}
		});

		long[] theCounts = new long[aClassIds.length];
		for (long[] theResult : theResults)
		{
			for(int i=0;i<theCounts.length;i++)
			{
				theCounts[i] += theResult[i];
			}
		}

		return theCounts;
	}
	
	/**
	 * A timer task that periodically updates aggregate data, and notifies
	 * listeners if data has changed since last update.
	 * 
	 * @author gpothier
	 */
	private class DataUpdater extends TimerTask
	{
		private long itsFirstTime = -1;
		private long itsFirstEventsCount = -1;

		private long itsPreviousTime;
		private long itsPreviousEventsCount;
		private long itsPreviousObjectsStoreSize;
		private long itsPreviousDroppedEventsCount;

		private long itsPreviousFirstTimestamp;

		private long itsPreviousLastTimestamp;

		private int itsPreviousThreadCount;

		@Override
		public void run()
		{
			try
			{
				run0();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
		
		private void run0()
		{
			updateStats();
			long theTime = System.currentTimeMillis();
			
			if (itsFirstTime == -1)
			{
				itsFirstTime = theTime;
				itsFirstEventsCount = itsEventsCount;
			}
			
			if (itsPreviousEventsCount != itsEventsCount
					|| itsPreviousDroppedEventsCount != itsDroppedEventsCount
					|| itsPreviousObjectsStoreSize != itsObjectsStoreSize
					|| itsPreviousFirstTimestamp != itsFirstTimestamp
					|| itsPreviousLastTimestamp != itsLastTimestamp
					|| itsPreviousThreadCount != itsThreadCount)
			{
				long theDelta = theTime - itsPreviousTime;
				long theTotalDelta = theTime - itsFirstTime;
				
				long theRate = theDelta != 0 ? (itsEventsCount-itsPreviousEventsCount)/theDelta : 0;
				long theTotalRate = theTotalDelta != 0 ? (itsEventsCount-itsFirstEventsCount)/theTotalDelta : 0;
				
				Utils.println("[DataUpdater] Recording rate: %dkEv/s (avg %dkEv/s)", theRate, theTotalRate);
				Utils.println("[DataUpdater] Event count: %,d", itsEventsCount);
				Utils.println("[DataUpdater] Objects store size: %,d", itsObjectsStoreSize);
				
				itsPreviousEventsCount = itsEventsCount;
				itsPreviousDroppedEventsCount = itsDroppedEventsCount;
				itsPreviousObjectsStoreSize = itsObjectsStoreSize;
				itsPreviousFirstTimestamp = itsFirstTimestamp;
				itsPreviousLastTimestamp = itsLastTimestamp;
				itsPreviousThreadCount = itsThreadCount;
				
				itsPreviousTime = theTime;

				fireEventsReceived();
			}
		}
	}

	/**
	 * A server that waits for database nodes to connect.
	 * 
	 * @author gpothier
	 */
	private class MyNodeServer extends Server
	{
		public MyNodeServer()
		{
			super(DebuggerGridConfig.MASTER_NODE_PORT);
		}

		@Override
		protected void accepted(Socket aSocket)
		{
			// Read node id
			int theNodeId;
			try
			{
				DataInputStream theInStream = new DataInputStream(aSocket.getInputStream());
				theNodeId = theInStream.readInt();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}

			// Retrieve RMI node
			RINodeConnector theNodeConnector = itsNodes.get(theNodeId - 1);

			itsDispatcherCollector.addProxy(new NodeProxy(theNodeConnector, aSocket));
		}
	}

	public static void main(String[] args) 
	{
		SRPCRegistry theRegistry = Util.getLocalSRPCRegistry();
		try
		{
			DBGridUtils.setupMaster(theRegistry, args);
			System.out.println("Master ready.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Data associated to listeners. Permits to identify and remove stale
	 * listeners.
	 * 
	 * @author gpothier
	 */
	private static class ListenerData
	{
		private RIGridMasterListener itsListener;

		private long itsFirstFailureTime;

		public ListenerData(RIGridMasterListener aListener)
		{
			itsListener = aListener;
		}
		
		public RIGridMasterListener getListener()
		{
			return itsListener;
		}

		public boolean fireEventsReceived()
		{
			try
			{
				itsListener.eventsReceived();
				return fire(false);
			}
			catch (Exception e)
			{
				return fire(true);
			}
		}

		public boolean fireException(Throwable aThrowable)
		{
			try
			{
				itsListener.exception(aThrowable);
				return fire(false);
			}
			catch (Exception e)
			{
				return fire(true);
			}
		}

		public boolean fireMonitorData(int aNodeId, MonitorData aData)
		{
			try
			{
				itsListener.monitorData(aNodeId, aData);
				return fire(false);
			}
			catch (Exception e)
			{
				return fire(true);
			}
		}

		public boolean fireCaptureEnabled(boolean aEnabled)
		{
			try
			{
				itsListener.captureEnabled(aEnabled);
				return fire(false);
			}
			catch (Exception e)
			{
				return fire(true);
			}
		}

		/**
		 * Returns false if the listener is no longer valid.
		 */
		private boolean fire(boolean aFailed)
		{
			if (aFailed)
			{
				long theTime = System.currentTimeMillis();
				if (itsFirstFailureTime == 0)
				{
					itsFirstFailureTime = theTime;
					return true;
				}
				else
				{
					long theDelta = theTime - itsFirstFailureTime;
					System.out.println("Listener stale for " + theDelta + "ms");
					return theDelta < 10000;
				}
			}
			else
			{
				itsFirstFailureTime = 0;
				return true;
			}
		}
	}

	/**
	 * This thread is in charge of exiting the database when no client is
	 * connected for a long time.
	 * 
	 * @author gpothier
	 */
	private class TimeoutThread extends Thread
	{
		private long itsTimeout;

		public TimeoutThread(long aTimeout)
		{
			itsTimeout = aTimeout;
		}

		@Override
		public void run()
		{
			System.out.println("[GridMaster] Timeout thread started.");
			while (true)
			{
				long theDelta = System.currentTimeMillis() - itsLastKeepAlive;
				if (theDelta > itsTimeout)
				{
					System.out.println("[GridMaster] Timeout, exiting");
					System.exit(0);
				}

				Utils.sleep(5000);
			}
		}
	}
}
