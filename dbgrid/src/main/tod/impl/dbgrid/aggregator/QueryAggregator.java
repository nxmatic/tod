///*
//TOD - Trace Oriented Debugger.
//Copyright (c) 2006-2008, Guillaume Pothier
//All rights reserved.
//
//This program is free software; you can redistribute it and/or 
//modify it under the terms of the GNU General Public License 
//version 2 as published by the Free Software Foundation.
//
//This program is distributed in the hope that it will be useful, 
//but WITHOUT ANY WARRANTY; without even the implied warranty of 
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License 
//along with this program; if not, write to the Free Software 
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
//MA 02111-1307 USA
//
//Parts of this work rely on the MD5 algorithm "derived from the 
//RSA Data Security, Inc. MD5 Message-Digest Algorithm".
//*/
//package tod.impl.dbgrid.aggregator;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import tod.impl.database.AbstractBidiIterator;
//import tod.impl.database.BufferedBidiIterator;
//import tod.impl.database.IBidiIterator;
//import tod.impl.dbgrid.DebuggerGridConfig;
//import tod.impl.dbgrid.GridMaster;
//import tod.impl.dbgrid.IGridEventFilter;
//import tod.impl.dbgrid.db.RIEventIterator;
//import tod.impl.dbgrid.db.RINodeEventIterator;
//import tod.impl.dbgrid.dispatch.RINodeConnector;
//import tod.impl.dbgrid.merge.DisjunctionIterator;
//import tod.impl.dbgrid.messages.GridEvent;
//import tod.tools.monitoring.MonitoringClient.MonitorId;
//import zz.utils.Future;
//
///**
// * Aggregates the partial results of a query obtained from the nodes.
// * @author gpothier
// */
//public class QueryAggregator implements RIQueryAggregator
//{
//	private final GridMaster itsMaster;
//	private final IGridEventFilter itsCondition;
//	private AbstractBidiIterator<GridEvent> itsMergeIterator;
//
//	public QueryAggregator(GridMaster aMaster, IGridEventFilter aCondition)
//	{
//		itsMaster = aMaster;
//		itsCondition = aCondition;
//		initIterators(0);
//	}
//	
//	private void initIterators(final long aTimestamp)
//	{
//		final List<RINodeConnector> theNodes = itsMaster.getNodes();
//		
//		if (theNodes.size() == 1)
//		{
//			// Don't use futures if there is only one node.
//			RINodeConnector theNode = theNodes.get(0);
//			RINodeEventIterator theIterator = theNode.getIterator(itsCondition);
//			theIterator.setNextTimestamp(aTimestamp);
//			itsMergeIterator = new EventIterator(theIterator);
//		}
//		else
//		{
//			final EventIterator[] theIterators = new EventIterator[theNodes.size()];
//			List<Future<EventIterator>> theFutures = new ArrayList<Future<EventIterator>>();
//			
//			for (int i=0;i<theNodes.size();i++)
//			{
//				final int i0 = i;
//				theFutures.add(new Future<EventIterator>()
//						{
//							@Override
//							protected EventIterator fetch() throws Throwable
//							{
//								RINodeConnector theNode = theNodes.get(i0);
//								try
//								{
//									RINodeEventIterator theIterator = theNode.getIterator(itsCondition);
//									
//									theIterator.setNextTimestamp(aTimestamp);
//									theIterators[i0] = new EventIterator(theIterator);
//									
//									return theIterators[i0];
//								}
//								catch (Exception e)
//								{
//									throw new RuntimeException(
//											"Exception catched in initIterators for node "+theNode.getNodeId(), 
//											e);
//								}
//							}
//						});
//				
//			}
//	
//			// Ensure all futures have completed
//			for (Future<EventIterator> theFuture : theFutures) theFuture.get();
//			
//			itsMergeIterator = new MyMergeIterator(theIterators);
//		}
//		
//	}
//
//	private static GridEvent[] toArray(List<GridEvent> aList)
//	{
//		return aList.size() > 0 ?
//				aList.toArray(new GridEvent[aList.size()])
//				: null;
//	}
//	
//	public GridEvent[] next(MonitorId aMonitorId, int aCount)
//	{
//		List<GridEvent> theList = new ArrayList<GridEvent>(aCount);
//		for (int i=0;i<aCount;i++)
//		{
//			if (itsMergeIterator.hasNext()) theList.add(itsMergeIterator.next());
//			else break;
//		}
//
//		return toArray(theList);
//	}
//
//	public void setNextTimestamp(long aTimestamp)
//	{
//		initIterators(aTimestamp);
//	}
//
//	public GridEvent[] previous(MonitorId aMonitorId, int aCount)
//	{
//		List<GridEvent> theList = new ArrayList<GridEvent>(aCount);
//		for (int i=0;i<aCount;i++)
//		{
//			if (itsMergeIterator.hasPrevious()) theList.add(itsMergeIterator.previous());
//			else break;
//		}
//
//		int theSize = theList.size();
//		if (theSize == 0) return null;
//		
//		GridEvent[] theResult = new GridEvent[theSize];
//		for (int i=0;i<theSize;i++) theResult[i] = theList.get(theSize-i-1);
//		
//		return theResult;
//	}
//
//	public void setPreviousTimestamp(long aTimestamp)
//	{
//		initIterators(aTimestamp);
//		GridEvent theNext = itsMergeIterator.peekNext();
//		
//		if (theNext != null && theNext.getTimestamp() > aTimestamp) return;
//		
//		while(itsMergeIterator.hasNext())
//		{
//			itsMergeIterator.next();
//			theNext = itsMergeIterator.peekNext();
//			GridEvent thePrevious = itsMergeIterator.peekPrevious();
//			if (theNext == null || theNext.getTimestamp() > aTimestamp) break;
//		}
//	}
//	
//	public boolean setNextEvent(long aTimestamp, int aThreadId)
//	{
//		setNextTimestamp(aTimestamp);
//		while(itsMergeIterator.hasNext())
//		{
//			GridEvent theNext = itsMergeIterator.next();
//			
//			if (theNext.getTimestamp() > aTimestamp) break;
//			
//			if (theNext.getTimestamp() == aTimestamp
//					&& theNext.getThread() == aThreadId)
//			{
//				itsMergeIterator.previous();
//				return true;
//			}
//		}
//
//		setNextTimestamp(aTimestamp);
//		return false;
//	}
//
//	public boolean setPreviousEvent(long aTimestamp, int aThreadId)
//	{
//		setPreviousTimestamp(aTimestamp);
//		while(itsMergeIterator.hasPrevious())
//		{
//			GridEvent thePrevious = itsMergeIterator.previous();
//			
//			if (thePrevious.getTimestamp() < aTimestamp) break;
//			
//			if (thePrevious.getTimestamp() == aTimestamp
//					&& thePrevious.getThread() == aThreadId) 
//			{
//				itsMergeIterator.next();
//				return true;
//			}
//		}
//		
//		setPreviousTimestamp(aTimestamp);
//		return false;		
//	}
//
//	
//	
//	public long[] getEventCounts(
//			final long aT1,
//			final long aT2, 
//			final int aSlotsCount,
//			final boolean aForceMergeCounts)
//	{
////		System.out.println("Aggregating counts...");
//		
//		long t0 = System.currentTimeMillis();
//
//		// Sum results from all nodes.
//		List<RINodeConnector> theNodes = itsMaster.getNodes();
//		List<Future<long[]>> theFutures = new ArrayList<Future<long[]>>();
//		
//		for (RINodeConnector theNode : theNodes)
//		{
//			final RINodeConnector theNode0 = theNode;
//			theFutures.add (new Future<long[]>()
//			{
//				@Override
//				protected long[] fetch() throws Throwable
//				{
//					try
//					{
//						long[] theEventCounts = theNode0.getEventCounts(
//								itsCondition,
//								aT1,
//								aT2, 
//								aSlotsCount,
//								aForceMergeCounts);
//						return theEventCounts;
//					}
//					catch (Exception e)
//					{
//						throw new RuntimeException("Exception in node "+theNode0.getNodeId(), e);
//					}
//				}
//			});
//		}
//
//		long theTotal = 0;
//		long[] theCounts = new long[aSlotsCount];
//		for (Future<long[]> theFuture : theFutures)
//		{
//			long[] theNodeCounts = theFuture.get();
//			for(int i=0;i<aSlotsCount;i++) 
//			{
//				long c = theNodeCounts[i];
//				theCounts[i] += c;
//				theTotal += c;
//			}
//		}
//		
//		long t1 = System.currentTimeMillis();
//		
//		System.out.println("Computed counts for "+itsCondition+" - found: "+theTotal);
//		
////		System.out.println("Computed counts in "+(t1-t0)+"ms.");
//		
//		return theCounts;
//	}
//
//	/**
//	 * A real iterator that wraps a {@link RIEventIterator}
//	 * @author gpothier
//	 */
//	private static class EventIterator extends BufferedBidiIterator<GridEvent[], GridEvent>
//	{
//		private RIEventIterator itsIterator;
//
//		public EventIterator(RIEventIterator aIterator)
//		{
//			itsIterator = aIterator;
//		}
//		
//		@Override
//		protected GridEvent[] fetchNextBuffer()
//		{
//			return itsIterator.next(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
//		}
//
//		@Override
//		protected GridEvent[] fetchPreviousBuffer()
//		{
//			return itsIterator.previous(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
//		}
//
//		@Override
//		protected int getSize(GridEvent[] aBuffer)
//		{
//			return aBuffer.length;
//		}
//
//		@Override
//		protected GridEvent get(GridEvent[] aBuffer, int aIndex)
//		{
//			return aBuffer[aIndex];
//		}
//	}
//	
//	/**
//	 * The iterator that merges results from all the nodes
//	 * @author gpothier
//	 */
//	private static class MyMergeIterator extends DisjunctionIterator<GridEvent>
//	{
//		public MyMergeIterator(IBidiIterator<GridEvent>[] aIterators)
//		{
//			super(aIterators);
//		}
//
//		@Override
//		protected long getKey(GridEvent aItem)
//		{
//			return aItem.getTimestamp();
//		}
//
//		@Override
//		protected boolean sameEvent(GridEvent aItem1, GridEvent aItem2)
//		{
//			return aItem1.getThread() == aItem2.getThread()
//				&& aItem1.getTimestamp() == aItem2.getTimestamp();
//		}
//		
//		@Override
//		protected boolean parallelFetch()
//		{
//			return true;
//		}
//	}
//}
