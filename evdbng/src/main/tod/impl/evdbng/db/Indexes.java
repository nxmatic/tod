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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.ObjectCodecNG;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.IndexSet.IndexManager;
import tod.impl.evdbng.db.file.BTree;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.RoleTree;
import tod.impl.evdbng.db.file.SequenceTree;
import tod.impl.evdbng.db.file.SimpleTree;
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
	private RoleIndexSet itsBehaviorIndexSet;
	private SimpleIndexSet itsAdviceSourceIdIndexSet;
	private SimpleIndexSet itsAdviceCFlowIndexSet; // Distinct from above to implement pointcut history.
	private SimpleIndexSet itsRoleIndexSet;
	private SimpleIndexSet itsFieldIndexSet;
	private SimpleIndexSet itsVariableIndexSet;
	
	/**
	 * (Split) index sets for array indexes 
	 */
	private SimpleIndexSet[] itsArrayIndexIndexSets;
	
	/**
	 * (Split) index sets for object ids.
	 */
	private RoleIndexSet[] itsObjectIndexeSets;
	
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
		
		itsTimestampTree = new SequenceTree("[EventDatabase] timestamp tree", aFile);
		
		itsTypeIndexSet = new SimpleIndexSet(itsIndexManager, "type", aFile);
		itsThreadIndexSet = new SimpleIndexSet(itsIndexManager, "thread", aFile);
		itsDepthIndexSet = new SimpleIndexSet(itsIndexManager, "depth", aFile);
		itsLocationIndexSet = new SimpleIndexSet(itsIndexManager, "bytecodeLoc.", aFile);
		itsAdviceSourceIdIndexSet = new SimpleIndexSet(itsIndexManager, "advice src id", aFile);
		itsAdviceCFlowIndexSet = new SimpleIndexSet(itsIndexManager, "advice cflow", aFile);
		itsRoleIndexSet = new SimpleIndexSet(itsIndexManager, "role", aFile);
		itsBehaviorIndexSet = new RoleIndexSet(itsIndexManager, "behavior", aFile);
		itsFieldIndexSet = new SimpleIndexSet(itsIndexManager, "field", aFile);
		itsVariableIndexSet = new SimpleIndexSet(itsIndexManager, "variable", aFile);

		itsArrayIndexIndexSets = createSplitIndex("arIndex", SimpleIndexSet.class, aFile);
		itsObjectIndexeSets = createSplitIndex("object", RoleIndexSet.class, aFile);
	}
	
	/**
	 * Creates all the sub indexes for a split index.
	 * @param aName Name base of the indexes
	 * @param aIndexClass Class of each index
	 */
	private <T extends IndexSet> T[] createSplitIndex(
			String aName, 
			Class<T> aIndexClass, 
			PagedFile aFile)
	{
		try
		{
			Constructor<T> theConstructor = aIndexClass.getConstructor(
					IndexManager.class,
					String.class,
					PagedFile.class);
			
			T[] theResult = (T[]) Array.newInstance(aIndexClass, 2);
			theResult[0] = theConstructor.newInstance(itsIndexManager, aName+"_0", aFile);
			theResult[1] = theConstructor.newInstance(itsIndexManager, aName+"_1", aFile);
			
			return theResult;
		}
		catch (InvocationTargetException e)
		{
			throw new RuntimeException(e.getCause());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
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
		itsBehaviorIndexSet.dispose();
		itsFieldIndexSet.dispose();
		itsVariableIndexSet.dispose();

		for (int i=0;i<itsArrayIndexIndexSets.length;i++)
		{
			itsArrayIndexIndexSets[i].dispose();
		}
		
		for (int i=0;i<itsObjectIndexeSets.length;i++)
		{
			itsObjectIndexeSets[i].dispose();
		}
		
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
		itsBehaviorIndexSet.flushTasks();
		itsFieldIndexSet.flushTasks();
		itsVariableIndexSet.flushTasks();

		for (int i=0;i<itsArrayIndexIndexSets.length;i++)
		{
			itsArrayIndexIndexSets[i].flushTasks();
		}
		
		for (int i=0;i<itsObjectIndexeSets.length;i++)
		{
			itsObjectIndexeSets[i].flushTasks();
		}
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
	
	public void indexBehavior(int aIndex, long aEventId, byte aRole)
	{
		itsBehaviorIndexSet.add(aIndex, aEventId, aRole);
	}
	
	public RoleTree getBehaviorIndex(int aIndex)
	{
		return itsBehaviorIndexSet.getIndex(aIndex);
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
	
	public void indexArrayIndex(int aIndex, long aEventId)
	{
		SplittedConditionHandler.INDEXES.add(itsArrayIndexIndexSets, aIndex, aEventId, (byte) 0);
	}
	
	public SimpleTree getArrayIndexIndex(int aPart, int aPartialKey)
	{
		return itsArrayIndexIndexSets[aPart].getIndex(aPartialKey);
	}
	
	public void indexObject(Object aObject, long aEventId, byte aRole)
	{
		long theId = ObjectCodecNG.getObjectId(aObject, false);
		indexObject(theId, aEventId, aRole);
	}

	public void indexObject(long aIndex, long aEventId, byte aRole)
	{
		if (aIndex > Integer.MAX_VALUE) throw new RuntimeException("Object index overflow: "+aIndex);
		int theId = (int) aIndex;
		itsMaxObjectId = Math.max(itsMaxObjectId, aIndex);
		SplittedConditionHandler.OBJECTS.add(itsObjectIndexeSets, theId, aEventId, aRole);
	}
	
	public RoleTree getObjectIndex(int aPart, int aPartialKey)
	{
		return itsObjectIndexeSets[aPart].getIndex(aPartialKey);
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
			BTree<T> aIndex,
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
