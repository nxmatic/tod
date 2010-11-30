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
package tod.impl.evdbng.db;

import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.IndexSet.IndexManager;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.SequenceTree;
import tod.impl.evdbng.db.file.SimpleTree;
import tod.impl.evdbng.db.file.StaticBTree;
import tod.impl.evdbng.db.file.Tuple;
import tod.impl.evdbng.db.file.TupleFinder.NoMatch;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;
import zz.utils.primitive.IntArray;

/**
 * Groups all the indexes maintained by a database node.
 * @author gpothier
 */
public class Indexes 
{
	private IndexManager itsIndexManager = new IndexManager();
	
	/**
	 * This tree permits to retrieve the id of the event for a specified timestamp
	 */
	private SequenceTree itsTimestampTree;
	
	private SimpleIndexSet itsTypeIndexSet;
	private SimpleIndexSet itsThreadIndexSet;
	private SimpleIndexSet itsDepthIndexSet;
	private SimpleIndexSet itsLocationIndexSet;
	private SimpleIndexSet itsAdviceSourceIdIndexSet;
	private SimpleIndexSet itsAdviceCFlowIndexSet; // Distinct from above to implement pointcut history.
	private SimpleIndexSet itsRoleIndexSet;
	private SimpleIndexSet itsFieldIndexSet;
	private SimpleIndexSet itsVariableIndexSet;
	
	private long itsMaxObjectId = 0;
	
	private IntArray itsEventsAtBehavior = new IntArray(DebuggerGridConfigNG.STRUCTURE_BEHAVIOR_COUNT);

	
	/**
	 * Protected constructor for subclasses. Does not initialize indexes.
	 */
	protected Indexes()
	{
	}
	
	public Indexes(PagedFile aFile)
	{
		Monitor.getInstance().register(this);
		
		itsTimestampTree = new SequenceTree("[EventDatabase] timestamp tree", null, aFile);
		
		itsTypeIndexSet = new SimpleIndexSet(itsIndexManager, "type", null, aFile);
		itsThreadIndexSet = new SimpleIndexSet(itsIndexManager, "thread", null, aFile);
		itsDepthIndexSet = new SimpleIndexSet(itsIndexManager, "depth", null, aFile);
		itsLocationIndexSet = new SimpleIndexSet(itsIndexManager, "bytecodeLoc.", null, aFile);
		itsAdviceSourceIdIndexSet = new SimpleIndexSet(itsIndexManager, "advice src id", null, aFile);
		itsAdviceCFlowIndexSet = new SimpleIndexSet(itsIndexManager, "advice cflow", null, aFile);
		itsRoleIndexSet = new SimpleIndexSet(itsIndexManager, "role", null, aFile);
		itsFieldIndexSet = new SimpleIndexSet(itsIndexManager, "field", null, aFile);
		itsVariableIndexSet = new SimpleIndexSet(itsIndexManager, "variable", null, aFile);
	}
	
	/**
	 * Recursively disposes this object.
	 * Unregister all the indexes from the monitor.
	 */
	public void dispose()
	{
		Monitor.getInstance().unregister(this);

		itsTypeIndexSet.dispose();
		itsThreadIndexSet.dispose();
		itsDepthIndexSet.dispose();
		itsLocationIndexSet.dispose();
		itsAdviceSourceIdIndexSet.dispose();
		itsAdviceCFlowIndexSet.dispose();
		itsRoleIndexSet.dispose();
		itsFieldIndexSet.dispose();
		itsVariableIndexSet.dispose();

		itsIndexManager.dispose();
	}
	
	/**
	 * Flushes currently pending tasks
	 */
	public void flushTasks()
	{
		itsTimestampTree.flushTasks();
		itsTypeIndexSet.flushTasks();
		itsThreadIndexSet.flushTasks();
		itsDepthIndexSet.flushTasks();
		itsLocationIndexSet.flushTasks();
		itsAdviceSourceIdIndexSet.flushTasks();
		itsAdviceCFlowIndexSet.flushTasks();
		itsRoleIndexSet.flushTasks();
		itsFieldIndexSet.flushTasks();
		itsVariableIndexSet.flushTasks();
	}
	
	public void registerTimestamp(long aTimestamp)
	{
		itsTimestampTree.addAsync(aTimestamp);
	}
	
	public long getEventId(long aTimestamp) 
	{
		return itsTimestampTree.getTuplePosition(aTimestamp, NoMatch.AFTER);	
	}
	
	public void indexType(int aIndex, long aEventId)
	{
		itsTypeIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getTypeIndex(int aIndex)
	{
		return itsTypeIndexSet.getIndex(aIndex);
	}
	
	public void indexThread(int aIndex, long aEventId)
	{
		itsThreadIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getThreadIndex(int aIndex)
	{
		return itsThreadIndexSet.getIndex(aIndex);
	}
	
	public void indexDepth(int aIndex, long aEventId)
	{
		itsDepthIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getDepthIndex(int aIndex)
	{
		return itsDepthIndexSet.getIndex(aIndex);
	}
	
	public void indexLocation(int aIndex, long aEventId)
	{
		itsLocationIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getLocationIndex(int aIndex)
	{
		return itsLocationIndexSet.getIndex(aIndex);
	}
	
	public void indexAdviceSourceId(int aIndex, long aEventId)
	{
		itsAdviceSourceIdIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getAdviceSourceIdIndex(int aIndex)
	{
		return itsAdviceSourceIdIndexSet.getIndex(aIndex);
	}
	
	public void indexAdviceCFlow(int aIndex, long aEventId)
	{
		itsAdviceCFlowIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getAdviceCFlowIndex(int aIndex)
	{
		return itsAdviceCFlowIndexSet.getIndex(aIndex);
	}
	
	public void indexRole(int aIndex, long aEventId)
	{
		itsRoleIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getRoleIndex(int aIndex)
	{
		return itsRoleIndexSet.getIndex(aIndex);
	}
	
	public void indexField(int aIndex, long aEventId)
	{
		itsFieldIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getFieldIndex(int aIndex)
	{
		return itsFieldIndexSet.getIndex(aIndex);
	}
	
	public void indexVariable(int aIndex, long aEventId)
	{
		itsVariableIndexSet.add(aIndex, aEventId);
	}
	
	public SimpleTree getVariableIndex(int aIndex)
	{
		return itsVariableIndexSet.getIndex(aIndex);
	}
	
	@Probe(key = "max object id", aggr = AggregationType.MAX)
	public long getMaxObjectId()
	{
		return itsMaxObjectId;
	}
	
	/**
	 * Accounts for an event in the given probe
	 */
	public void eventAtProbe(ProbeInfo aProbeInfo)
	{
		int theBehaviorId = aProbeInfo.behaviorId;
		int theCount = itsEventsAtBehavior.get(theBehaviorId);
		if (theCount == Integer.MAX_VALUE) throw new IllegalStateException("It's time to use a LongArray...");
		itsEventsAtBehavior.set(theBehaviorId, theCount+1);
	}
	
	public long getEventsAtBehavior(int aBehaviorId)
	{
		return itsEventsAtBehavior.get(aBehaviorId);
	}
	
	public <T extends Tuple> long[] fastCounts(
			StaticBTree<T> aIndex,
			long aT1, 
			long aT2,
			int aSlotsCount)
	{
		long[] theCounts = new long[aSlotsCount];

		long ki1 = getEventId(aT1); 
		
		for (int i=0;i<aSlotsCount;i++)
		{
			long t2 = aT1 + ((i+1)*(aT2-aT1)/aSlotsCount);
			long ki2 = getEventId(t2);
			
			long c = ki1 != ki2 ? aIndex.countTuples(ki1, ki2) : 0;
			theCounts[i] = c;
			ki1 = ki2;
		}
		
		return theCounts;
	}


}
