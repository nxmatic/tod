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
package tod.impl.evdb1.db;

import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_BUFFER_SIZE;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_SIZE;
import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.evdb1.db.RoleIndexSet.RoleTuple;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.IndexTuple;
import tod.impl.evdb1.db.file.TupleCodec;
import tod.impl.evdb1.db.file.HardPagedFile.Page;
import tod.impl.evdb1.db.file.HardPagedFile.PageBitStruct;
import zz.utils.bit.BitStruct;
import zz.utils.cache.SyncMRUBuffer;
import zz.utils.list.NakedLinkedList.Entry;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;

/**
 * A set of indexes for a given attribute. Within a set,
 * there is one index per possible attribute value.
 * @author gpothier
 */
public abstract class IndexSet<T extends IndexTuple>
{
	
	/**
	 * This dummy entry is used in {@link #itsIndexes} to differenciate
	 * entries that never existed (null  value) from entries that were
	 * discarded and that are available in the file. 
	 */
	private static Entry DISCARDED_ENTRY = new Entry(null);
	
	private final IndexManager itsIndexManager;
	private final Entry<MyHierarchicalIndex<T>>[] itsIndexes;
	
	/**
	 * The page ids of all the pages that are used to store discarded indexes.
	 */
	private final long[] itsIndexPages;
	
	/**
	 * Number of discarded indexes that fit in a page. 
	 */
	private final int itsIndexesPerPage;
	
	/**
	 * Name of this index set (for monitoring)
	 */
	private final String itsName;
	
	private final HardPagedFile itsFile;
	
	private int itsIndexCount = 0;
	
	private int itsDiscardCount = 0;
	private int itsLoadCount = 0;

	
	public IndexSet(
			String aName,
			IndexManager aIndexManager,
			HardPagedFile aFile, 
			int aIndexCount)
	{
		itsName = aName;
		itsIndexManager = aIndexManager;
		itsFile = aFile;
		itsIndexes = new Entry[aIndexCount];
		
		// Init discarded index page directory.
		itsIndexesPerPage = aFile.getPageSize()*8/HierarchicalIndex.getSerializedSize(itsFile);
		int theNumPages = (aIndexCount+itsIndexesPerPage-1)/itsIndexesPerPage;
		itsIndexPages = new long[theNumPages];
		
		System.out.println("Created index "+itsName+" with "+aIndexCount+" entries.");
		Monitor.getInstance().register(this);
	}
	
	public void dispose()
	{
		Monitor.getInstance().unregister(this);		
	}

	/**
	 * Returns the tuple codec used for the level 0 of the indexes of this set.
	 */
	public abstract TupleCodec<T> getTupleCodec();
	
	/**
	 * Returns the file used by the indexes of this set.
	 */
	public HardPagedFile getFile()
	{
		return itsFile;
	}
	
	/**
	 * Retrieved the index corresponding to the specified... index.
	 */
	public HierarchicalIndex<T> getIndex(int aIndex)
	{
		if (aIndex >= itsIndexes.length) throw new IndexOutOfBoundsException("Index overflow for "+itsName+": "+aIndex+" >= "+itsIndexes.length);
		
		Entry<MyHierarchicalIndex<T>> theEntry = itsIndexes[aIndex];
		MyHierarchicalIndex<T> theIndex;
		
		if (theEntry == null)
		{
			theIndex = new MyHierarchicalIndex<T>(
					itsName+" = "+aIndex,
					getTupleCodec(), 
					getFile(), 
					this, 
					aIndex);
			
			theEntry = new Entry<MyHierarchicalIndex<T>>(theIndex);
			itsIndexes[aIndex] = theEntry;
			itsIndexCount++;
		}
		else if (theEntry == DISCARDED_ENTRY)
		{
			theIndex = new MyHierarchicalIndex<T>(
					itsName+" = "+aIndex,
					getTupleCodec(), 
					getFile(), 
					getIndexStruct(aIndex), 
					this, 
					aIndex);
			
			theEntry = new Entry<MyHierarchicalIndex<T>>(theIndex);
			itsIndexes[aIndex] = theEntry;
			itsLoadCount++;
		}
		else theIndex = theEntry.getValue();
		
		itsIndexManager.use((Entry) theEntry);
		
		return theIndex;
	}
	
	/**
	 * Returns the bit struct corresponding to the given index,
	 * positionned right before where the index is stored 
	 */
	private BitStruct getIndexStruct(int aIndex)
	{
		long thePageId = itsIndexPages[aIndex/itsIndexesPerPage];
		
		Page thePage;
		if (thePageId == 0)
		{
			thePage = itsFile.create();
			itsIndexPages[aIndex/itsIndexesPerPage] = thePage.getPageId()+1;
		}
		else
		{
			thePage = itsFile.get(thePageId-1);
		}
		
		PageBitStruct theBitStruct = thePage.asBitStruct();
		theBitStruct.setPos((aIndex % itsIndexesPerPage) * HierarchicalIndex.getSerializedSize(itsFile));
		
		return theBitStruct;
	}
	
	private void discardIndex(int aIndex)
	{
		Entry<MyHierarchicalIndex<T>> theEntry = itsIndexes[aIndex];
		HierarchicalIndex<T> theIndex = theEntry.getValue();
		
		theIndex.writeTo(getIndexStruct(aIndex));
		itsIndexes[aIndex] = DISCARDED_ENTRY;
		itsDiscardCount++;
	}

	public void addTuple(int aIndex, T aTuple)
	{
		getIndex(aIndex).add(aTuple);
	}
	
	@Probe(key = "index count", aggr = AggregationType.SUM)
	public long getIndexCount()
	{
		return itsIndexCount;
	}
	
	@Probe(key = "index discard count", aggr = AggregationType.SUM)
	public int getDiscardCount()
	{
		return itsDiscardCount;
	}

	@Probe(key = "index reload count", aggr = AggregationType.SUM)
	public int getLoadCount()
	{
		return itsLoadCount;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName()+": "+itsName;
	}
	
	/**
	 * Creates an iterator that filters out duplicate tuples, which is useful when the 
	 * role is not checked: for instance if a behavior call event has the same called
	 * and executed method, it would appear twice in the behavior index with
	 * a different role.
	 */
	public static <T extends StdTuple> IBidiIterator<T> createFilteredIterator(IBidiIterator<T> aIterator)
	{
		return new DuplicateFilterIterator<T>(aIterator);
	}
	
	private static class DuplicateFilterIterator<T extends StdTuple> extends AbstractFilteredBidiIterator<T, T>
	{
		private long itsLastEventPointer;
		private int itsDirection = 0;
		
		public DuplicateFilterIterator(IBidiIterator<T> aIterator)
		{
			super(aIterator);
			itsLastEventPointer = -1;
		}
		
		@Override
		protected T fetchNext()
		{
			if (itsDirection != 1) itsLastEventPointer = -1;
			itsDirection = 1;
			return super.fetchNext();
		}
		
		@Override
		protected T fetchPrevious()
		{
			if (itsDirection != -1) itsLastEventPointer = -1;
			itsDirection = -1;
			return super.fetchPrevious();
		}
		
		@Override
		protected Object transform(T aIn)
		{
			if (aIn.getEventPointer() == itsLastEventPointer) return REJECT;
			itsLastEventPointer = aIn.getEventPointer();
			
			return aIn;
		}
	}
	

	
	/**
	 * The index manager ensures that least-frequently-used indexes
	 * are discarded so that they do not waste memory.
	 * @author gpothier
	 */
	public static class IndexManager extends SyncMRUBuffer<Integer, MyHierarchicalIndex>
	{
		private boolean itsDisposed = false;
		
		public IndexManager()
		{
			super((int) ((DB_PAGE_BUFFER_SIZE/DB_PAGE_SIZE) / 1), false);
		}
		
		/**
		 * Disposes this index manager by dropping all entries.
		 */
		public void dispose()
		{
			itsDisposed = true;
			dropAll();
		}
		
		@Override
		protected void dropped(MyHierarchicalIndex aValue)
		{
			if (itsDisposed) return;
			aValue.getIndexSet().discardIndex(aValue.getIndex());
		}
		
		@Override
		public Entry<MyHierarchicalIndex> getEntry(Integer aKey, boolean aFetch)
		{
			if (itsDisposed) throw new IllegalStateException();
			return super.getEntry(aKey, aFetch);
		}

		@Override
		protected MyHierarchicalIndex fetch(Integer aId)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected Integer getKey(MyHierarchicalIndex aValue)
		{
			throw new UnsupportedOperationException();
		}
	}
	
	private static class MyHierarchicalIndex<T extends IndexTuple> 
	extends HierarchicalIndex<T>
	{
		private final IndexSet<T> itsIndexSet;
		
		/**
		 * The position of this index within the set.
		 */
		private final int itsIndex;
		
		
		public MyHierarchicalIndex(
				String aName,
				TupleCodec<T> aTupleCodec, 
				HardPagedFile aFile,
				IndexSet<T> aIndexSet, 
				int aIndex)
		{
			super(aName, aTupleCodec, aFile);
			itsIndexSet = aIndexSet;
			itsIndex = aIndex;
		}

		public MyHierarchicalIndex(
				String aName,
				TupleCodec<T> aTupleCodec, 
				HardPagedFile aFile,
				BitStruct aStoredIndexStruct, 
				IndexSet<T> aIndexSet, 
				int aIndex)
		{
			super(aName, aTupleCodec, aFile, aStoredIndexStruct);
			itsIndexSet = aIndexSet;
			itsIndex = aIndex;
		}

		public int getIndex()
		{
			return itsIndex;
		}

		public IndexSet<T> getIndexSet()
		{
			return itsIndexSet;
		}

	}
	
}
