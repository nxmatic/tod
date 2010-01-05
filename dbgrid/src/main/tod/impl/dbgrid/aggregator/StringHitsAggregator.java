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
//import tod.impl.database.BufferedBidiIterator;
//import tod.impl.database.IBidiIterator;
//import tod.impl.dbgrid.DebuggerGridConfig;
//import tod.impl.dbgrid.GridMaster;
//import tod.impl.dbgrid.db.RIBufferIterator;
//import tod.impl.dbgrid.dispatch.RINodeConnector;
//import tod.impl.dbgrid.dispatch.RINodeConnector.StringSearchHit;
//import tod.impl.dbgrid.merge.DisjunctionIterator;
//import tod.tools.monitoring.MonitoringClient.MonitorId;
//import zz.utils.Future;
//
///**
// * Aggregates string hits provided by the {@link LeafEventDispatcher}s.
// * @author gpothier
// */
//public class StringHitsAggregator implements RIBufferIterator<StringSearchHit[]>
//{
//	private final GridMaster itsMaster;
//	private final String itsSearchText;
//	private MergeIterator itsMergeIterator;
//
//	public StringHitsAggregator(GridMaster aMaster, String aSearchText)
//	{
//		itsMaster = aMaster;
//		itsSearchText = aSearchText;
//		initIterators();
//	}
//	
//	private void initIterators()
//	{
//		final List<RINodeConnector> theNodes = itsMaster.getNodes();
//		final SearchHitIterator[] theIterators = new SearchHitIterator[theNodes.size()];
//		
//		List<Future<SearchHitIterator>> theFutures = new ArrayList<Future<SearchHitIterator>>();
//		
//		for (int i=0;i<theNodes.size();i++)
//		{
//			final int i0 = i;
//			theFutures.add(new Future<SearchHitIterator>()
//					{
//						@Override
//						protected SearchHitIterator fetch() throws Throwable
//						{
//							RINodeConnector theNode = theNodes.get(i0);
//							RIBufferIterator<StringSearchHit[]> theIterator = 
//								theNode.searchStrings(itsSearchText);
//							theIterators[i0] = new SearchHitIterator(theIterator);
//							
//							return theIterators[i0];
//						}
//					});
//			
//		}
//
//		// Ensure all futures have completed
//		for (Future<SearchHitIterator> theFuture : theFutures) theFuture.get();
//		
//		itsMergeIterator = new MergeIterator(theIterators);
//	}
//
//	private static StringSearchHit[] toArray(List<StringSearchHit> aList)
//	{
//		return aList.size() > 0 ?
//				aList.toArray(new StringSearchHit[aList.size()])
//				: null;
//	}
//	
//	public StringSearchHit[] next(MonitorId aMonitorId, int aCount)
//	{
//		List<StringSearchHit> theList = new ArrayList<StringSearchHit>(aCount);
//		for (int i=0;i<aCount;i++)
//		{
//			if (itsMergeIterator.hasNext()) theList.add(itsMergeIterator.next());
//			else break;
//		}
//
//		return toArray(theList);
//	}
//
//	public StringSearchHit[] previous(MonitorId aMonitorId, int aCount)
//	{
//		List<StringSearchHit> theList = new ArrayList<StringSearchHit>(aCount);
//		for (int i=0;i<aCount;i++)
//		{
//			if (itsMergeIterator.hasPrevious()) theList.add(itsMergeIterator.previous());
//			else break;
//		}
//
//		int theSize = theList.size();
//		if (theSize == 0) return null;
//		
//		StringSearchHit[] theResult = new StringSearchHit[theSize];
//		for (int i=0;i<theSize;i++) theResult[i] = theList.get(theSize-i-1);
//		
//		return theResult;
//	}
//
//
//	/**
//	 * A real iterator that wraps a {@link RIBufferIterator}
//	 * @author gpothier
//	 */
//	private static class SearchHitIterator extends BufferedBidiIterator<StringSearchHit[], StringSearchHit>
//	{
//		private RIBufferIterator<StringSearchHit[]> itsIterator;
//
//		public SearchHitIterator(RIBufferIterator<StringSearchHit[]> aIterator)
//		{
//			assert aIterator != null;
//			itsIterator = aIterator;
//		}
//		
//		@Override
//		protected StringSearchHit[] fetchNextBuffer()
//		{
//			return itsIterator.next(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
//		}
//
//		@Override
//		protected StringSearchHit[] fetchPreviousBuffer()
//		{
//			return itsIterator.previous(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
//		}
//
//		@Override
//		protected int getSize(StringSearchHit[] aBuffer)
//		{
//			return aBuffer.length;
//		}
//
//		@Override
//		protected StringSearchHit get(StringSearchHit[] aBuffer, int aIndex)
//		{
//			return aBuffer[aIndex];
//		}
//	}
//	
//	/**
//	 * The iterator that merges results from all the nodes
//	 * @author gpothier
//	 */
//	private static class MergeIterator extends DisjunctionIterator<StringSearchHit>
//	{
//		public MergeIterator(IBidiIterator<StringSearchHit>[] aIterators)
//		{
//			super(aIterators);
//		}
//
//		@Override
//		protected long getKey(StringSearchHit aItem)
//		{
//			return aItem.getScore();
//		}
//
//		@Override
//		protected boolean sameEvent(StringSearchHit aItem1, StringSearchHit aItem2)
//		{
//			return aItem1.getObjectId() == aItem2.getObjectId();
//		}
//	}
//
//}
