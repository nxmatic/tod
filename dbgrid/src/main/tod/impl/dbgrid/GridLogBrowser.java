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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.IVariablesInspector;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ObjectId;
import tod.core.session.ISession;
import tod.impl.common.LogBrowserUtils;
import tod.impl.common.ObjectInspector;
import tod.impl.common.VariablesInspector;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.aggregator.GridEventBrowser;
import tod.impl.dbgrid.aggregator.StringHitsIterator;
import tod.impl.dbgrid.db.ObjectsDatabase.Decodable;
import tod.impl.dbgrid.queries.EventIdCondition;
import zz.utils.Utils;
import zz.utils.cache.MRUBuffer;

/**
 * Implementation of {@link ILogBrowser} for the grid backend.
 * This is the client-side object that interfaces with the {@link GridMaster}
 * for executing queries.
 * Note: it is remote because it must be accessed by the master.
 * @author gpothier
 */
public abstract class GridLogBrowser 
implements ILogBrowser, IScheduled
{
	private static final long serialVersionUID = -5101014933784311102L;

	private final ISession itsSession;
		
	private RIGridMaster itsMaster;
	private IStructureDatabase itsStructureDatabase;
	
	private long itsEventsCount;
	private long itsDroppedEventsCount;
	private long itsFirstTimestamp;
	private long itsLastTimestamp;
	
	private List<IThreadInfo> itsThreads;
	private List<IThreadInfo> itsPackedThreads;
	private List<IHostInfo> itsHosts;
	private List<IHostInfo> itsPackedHosts;
	
	private Map<String, IHostInfo> itsHostsMap = new HashMap<String, IHostInfo>();
		
	private QueryResultCache itsQueryResultCache = new QueryResultCache();
	private ObjectInspectorCache itsObjectInspectorCache = new ObjectInspectorCache();
	private StaticInspectorCache itsStaticInspectorCache = new StaticInspectorCache();
	
	protected GridLogBrowser(
			ISession aSession,
			RIGridMaster aMaster,
			IStructureDatabase aStructureDatabase)
	{
		itsSession = aSession;
		itsMaster = aMaster;
		itsStructureDatabase = aStructureDatabase;
		System.out.println("[GridLogBrowser] Instantiated.");
	}
	
	public ISession getSession()
	{
		return itsSession;
	}
	
	public ILogBrowser getKey()
	{
		return this;
	}
	
	public void clear()
	{
		itsMaster.clear();
	}
	
	private List<EventIdCondition> getIdConditions(IEventFilter[] aFilters)
	{
		List<EventIdCondition> theIdConditions = new ArrayList<EventIdCondition>();
		for (IEventFilter theFilter : aFilters)
		{
			if (theFilter instanceof EventIdCondition)
			{
				EventIdCondition theCondition = (EventIdCondition) theFilter;
				theIdConditions.add(theCondition);
			}
		}

		return theIdConditions;
	}
	
	private List<IEventFilter> getNonIdConditions(IEventFilter[] aFilters)
	{
		List<IEventFilter> theIdConditions = new ArrayList<IEventFilter>();
		for (IEventFilter theFilter : aFilters)
		{
			if (theFilter instanceof EventIdCondition) continue;
			theIdConditions.add(theFilter);
		}
		
		return theIdConditions;
	}

	
	public ICompoundFilter createIntersectionFilter(IEventFilter... aFilters)
	{
		// If at least one filter is an EventIdCondition,
		// we create the intersection "manually"
		List<EventIdCondition> theIdConditions = getIdConditions(aFilters);
		if (theIdConditions.size() > 0)
		{
			// Check that all event id conditions point to the same event
			ILogEvent theEvent = null;
			for (EventIdCondition theCondition : theIdConditions)
			{
				// An event id condition with a null event has an empty
				// result set, so return an empty union filter.
				if (theCondition.getEvent() == null) return theCondition;
				
				if (theEvent == null) theEvent = theCondition.getEvent();
				else if (theEvent != theCondition.getEvent()) return new EventIdCondition(this, null);
			}
			
			// Check that the rest of the filter also match the event
			List<IEventFilter> theRemainingConditions = getNonIdConditions(aFilters);
			ICompoundFilter theRemainingFilter = createIntersectionFilter(theRemainingConditions.toArray(new IEventFilter[theRemainingConditions.size()]));
			IEventBrowser theRemainingBrowser = createBrowser(theRemainingFilter);
			
			if (LogBrowserUtils.hasEvent(theRemainingBrowser, theEvent)) 
			{
				return new EventIdCondition(this, theEvent);
			}
			else return new EventIdCondition(this, null);
		}
		else return createIntersectionFilter0(aFilters);
	}
	
	protected abstract ICompoundFilter createIntersectionFilter0(IEventFilter... aFilters);


	public ICompoundFilter createUnionFilter(IEventFilter... aFilters)
	{
		List<EventIdCondition> theIdConditions = getIdConditions(aFilters);
		if (theIdConditions.size() > 0) throw new UnsupportedOperationException();

		return createUnionFilter0(aFilters);
	}
	
	protected abstract ICompoundFilter createUnionFilter0(IEventFilter... aFilters);

	
	public Object getRegistered(ObjectId aId)
	{
		Decodable theDecodable = itsMaster.getRegisteredObject(aId.getId());
		return theDecodable != null ? theDecodable.decode() : null;
	}

	public long getEventsCount()
	{
		if (itsEventsCount == 0) updateStats(); 
		return itsEventsCount;
	}

	public long getDroppedEventsCount()
	{
		if (itsDroppedEventsCount == 0) updateStats(); 
		return itsDroppedEventsCount;
	}
	
	public long getFirstTimestamp()
	{
		if (itsFirstTimestamp == 0) updateStats();
		return itsFirstTimestamp;
	}

	public long getLastTimestamp()
	{
		if (itsFirstTimestamp == 0) updateStats();
		return itsLastTimestamp;
	}
	
	private synchronized void fetchThreads()
	{
		itsThreads = new ArrayList<IThreadInfo>();
		itsPackedThreads = itsMaster.getThreads();
		for (IThreadInfo theThread : itsPackedThreads)
		{
			Utils.listSet(itsThreads, theThread.getId(), theThread);
		}
	}
	
	private synchronized List<IThreadInfo> getThreads0()
	{
		if (itsThreads == null) fetchThreads();
		return itsThreads;
	}
	
	private synchronized void fetchHosts()
	{
		itsHosts = new ArrayList<IHostInfo>();
		itsPackedHosts = itsMaster.getHosts();
		for (IHostInfo theHost : itsPackedHosts)
		{
			Utils.listSet(itsHosts, theHost.getId(), theHost);
			itsHostsMap.put(theHost.getName(), theHost);
		}		
	}
	
	private synchronized List<IHostInfo> getHosts0()
	{
		if (itsHosts == null) fetchHosts();
		return itsHosts;
	}
	
	public synchronized Iterable<IThreadInfo> getThreads()
	{
		getThreads0();
		return itsPackedThreads;
	}
	
	public Iterable<IHostInfo> getHosts()
	{
		getHosts0();
		return itsPackedHosts;
	}
	
	public IHostInfo getHost(int aId)
	{
		return getHosts0().get(aId);
	}
	
	public IHostInfo getHost(String aName)
	{
		getHosts0(); // lazy init
		return itsHostsMap.get(aName);
	}

	public IThreadInfo getThread(int aThreadId)
	{
		return getThreads0().get(aThreadId);
	}

	public ILogEvent getEvent(ExternalPointer aPointer)
	{
		return LogBrowserUtils.getEvent(this, aPointer);
	}

	
	public IStructureDatabase getStructureDatabase()
	{
		return itsStructureDatabase;
	}
	
	public RIGridMaster getMaster()
	{
		return itsMaster;
	}

	
	public GridEventBrowser createBrowser()
	{
		ICompoundFilter theDisjunction = createUnionFilter();
		for (IThreadInfo theThread : getThreads())
		{
			theDisjunction.add(createThreadFilter(theThread));
		}
		
		return new GlobalEventBrowser(this, (IGridEventFilter) theDisjunction);
	}
	
	public IEventBrowser createBrowser(IEventFilter aFilter)
	{
		if (aFilter instanceof EventIdCondition)
		{
			EventIdCondition theCondition = (EventIdCondition) aFilter;
			return theCondition.createBrowser();
		}
		else return createBrowser0(aFilter);
	}
	
	protected abstract GridEventBrowser createBrowser0(IEventFilter aFilter);



	public IParentEvent getCFlowRoot(IThreadInfo aThread)
	{
		return LogBrowserUtils.createCFlowRoot(this, aThread);
	}

	public IObjectInspector createClassInspector(IClassInfo aClass)
	{
		return itsStaticInspectorCache.get(aClass);
	}

	protected IObjectInspector createStaticInspector0(IClassInfo aClass)
	{
		return new ObjectInspector(this, aClass);
	}

	public final IObjectInspector createObjectInspector(ObjectId aObjectId)
	{
		return itsObjectInspectorCache.get(aObjectId);
	}
	
	protected IObjectInspector createObjectInspector0(ObjectId aObjectId)
	{
		return new ObjectInspector(this, aObjectId);
	}

	public IVariablesInspector createVariablesInspector(IBehaviorCallEvent aEvent)
	{
		return new VariablesInspector(this, aEvent);
	}
	
	public IBidiIterator<Long> searchStrings(String aSearchText)
	{
		return new StringHitsIterator(itsMaster.searchStrings(aSearchText));
	}

	/**
	 * Clears cached information so that they are lazily retrieved.
	 */
	public synchronized void clearStats()
	{
		itsEventsCount = 0;
		itsDroppedEventsCount = 0;
		itsFirstTimestamp = 0;
		itsLastTimestamp = 0;
		itsThreads = null;
		itsHosts = null;
		itsHostsMap.clear();
	}
	
	private void updateStats()
	{
		itsEventsCount = itsMaster.getEventsCount();
		itsDroppedEventsCount = itsMaster.getDroppedEventsCount();
		itsFirstTimestamp = itsMaster.getFirstTimestamp();
		itsLastTimestamp = itsMaster.getLastTimestamp();
	}

	public <O> O exec(Query<O> aQuery)
	{
		return itsQueryResultCache.getResult(aQuery);
	}

	public long[] getEventCounts(IBehaviorInfo[] aBehaviors)
	{
		int[] theIds = new int[aBehaviors.length];
		for(int i=0;i<theIds.length;i++) theIds[i] = aBehaviors[i].getId();
		return getMaster().getEventCountAtBehaviors(theIds);
	}

	public long[] getEventCounts(IClassInfo[] aClasses)
	{
		int[] theIds = new int[aClasses.length];
		for(int i=0;i<theIds.length;i++) theIds[i] = aClasses[i].getId();
		return getMaster().getEventCountAtClasses(theIds);
	}
	
	/**
	 * An event browser that returns all events.
	 * @author gpothier
	 */
	private static class GlobalEventBrowser extends GridEventBrowser
	{
		public GlobalEventBrowser(
				GridLogBrowser aBrowser, 
				IGridEventFilter aFilter)
		{
			super(aBrowser, aFilter);
		}

		/**
		 * Optimization: as this browser returns all events we don't need
		 * a "real" intersection.
		 */
		@Override
		public IEventBrowser createIntersection(IEventFilter aFilter)
		{
			return getLogBrowser().createBrowser(aFilter);
		}
	}
	
	private static class QueryCacheEntry
	{
		private final Query query;
		
		private final Object result;
		
		/**
		 * Number of events in the database when the query was executed.
		 */
		private final long eventCount;

		public QueryCacheEntry(Query aQuery, Object aResult, long aEventCount)
		{
			query = aQuery;
			result = aResult;
			eventCount = aEventCount;
		}
	}
	
	private class QueryResultCache extends MRUBuffer<Query, QueryCacheEntry>
	{
		public QueryResultCache()
		{
			super(1000);
		}
		
		@Override
		protected Query getKey(QueryCacheEntry aValue)
		{
			return aValue.query;
		}

		@Override
		protected QueryCacheEntry fetch(Query aQuery)
		{
			Object theResult = itsMaster.exec(aQuery);
			return new QueryCacheEntry(aQuery, theResult, itsEventsCount);
		}
		
		public <O> O getResult(Query<O> aQuery)
		{
			QueryCacheEntry theEntry = get(aQuery);
			if (aQuery.recomputeOnUpdate() && theEntry.eventCount != itsEventsCount)
			{
				drop(aQuery);
				theEntry = get(aQuery);
			}
			return (O) theEntry.result;
		}
	}
	
	private class ObjectInspectorCache extends MRUBuffer<ObjectId, IObjectInspector>
	{
		public ObjectInspectorCache()
		{
			super(1000);
		}

		@Override
		protected IObjectInspector fetch(ObjectId aId)
		{
			return createObjectInspector0(aId);
		}

		@Override
		protected ObjectId getKey(IObjectInspector aValue)
		{
			return aValue.getObject();
		}
	}
	
	private class StaticInspectorCache extends MRUBuffer<IClassInfo, IObjectInspector>
	{
		public StaticInspectorCache()
		{
			super(1000);
		}
		
		@Override
		protected IObjectInspector fetch(IClassInfo aId)
		{
			return createStaticInspector0(aId);
		}
		
		@Override
		protected IClassInfo getKey(IObjectInspector aValue)
		{
			return (IClassInfo) aValue.getType();
		}
	}
}
