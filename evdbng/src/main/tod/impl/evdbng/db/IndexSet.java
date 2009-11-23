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

import static tod.impl.evdbng.DebuggerGridConfigNG.DB_PAGE_BUFFER_SIZE;
import static tod.impl.evdbng.DebuggerGridConfigNG.DB_PAGE_SIZE;

import java.util.ArrayList;
import java.util.List;

import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.file.BTree;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.Tuple;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.tools.ConcurrentMRUBuffer;
import zz.utils.Utils;
import zz.utils.list.NakedLinkedList.Entry;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;
import zz.utils.primitive.IntArray;

/**
 * A set of indexes for a given attribute. Within a set,
 * there is one index per possible attribute value.
 * @author gpothier
 */
public abstract class IndexSet<T extends Tuple>
{
	
	/**
	 * This dummy entry is used in {@link #itsIndexes} to differenciate
	 * entries that never existed (null value) from entries that were
	 * discarded and that are available in the file. 
	 */
	private static final Entry DISCARDED_ENTRY = new Entry(null);
	
	private static int itsNextId;
	
	private synchronized static int nextId()
	{
		return itsNextId++;
	}
	
	private final int itsId = nextId();
	
	/**
	 * The global index manager.
	 */
	private final IndexManager itsIndexManager;
	
	private final List<Entry<BTreeWrapper<T>>> itsIndexes;
	
	/**
	 * The page ids of all the pages that are used to store discarded indexes.
	 */
	private final IntArray itsIndexPages;
	
	/**
	 * Number of discarded indexes that fit in a page. 
	 */
	private final int itsIndexesPerPage;
	
	/**
	 * Name of this index set (for monitoring)
	 */
	private final String itsName;
	
	private final PagedFile itsFile;
	
	private int itsIndexCount = 0;
	
	private int itsDiscardCount = 0;
	private int itsLoadCount = 0;

	
	public IndexSet(IndexManager aIndexManager, String aName, PagedFile aFile)
	{
		itsIndexManager = aIndexManager;
		itsName = aName;
		itsFile = aFile;
		itsIndexes = new ArrayList<Entry<BTreeWrapper<T>>>();
		
		// Init discarded index page directory.
		itsIndexesPerPage = PagedFile.PAGE_SIZE/BTree.getSerializedSize();
		itsIndexPages = new IntArray();
		
		Monitor.getInstance().register(this);
	}
	
	/**
	 * Each {@link IndexSet} has a sequential id (for {@link DBExecutor}).
	 */
	protected int getId()
	{
		return itsId;
	}
	
	public String getName()
	{
		return itsName;
	}

	public void dispose()
	{
		Monitor.getInstance().unregister(this);		
	}

	public abstract BTree<T> createIndex(int aIndex);
	
	public abstract BTree<T> loadIndex(int aIndex, PageIOStream aStream);
	
	/**
	 * Returns the file used by the indexes of this set.
	 */
	public PagedFile getFile()
	{
		return itsFile;
	}
	
	/**
	 * Retrieved the index corresponding to the specified... index.
	 */
	public BTree<T> getIndex(int aIndex)
	{
		Entry<BTreeWrapper<T>> theEntry = Utils.listGet(itsIndexes, aIndex);
		BTreeWrapper<T> theIndex;
		
		if (theEntry == null)
		{
			// The index never existed.
			BTree<T> theTree = createIndex(aIndex);
			theIndex = new BTreeWrapper<T>(theTree, this, aIndex);
			theEntry = new Entry<BTreeWrapper<T>>(theIndex);
			
			Utils.listSet(itsIndexes, aIndex, theEntry);
			itsIndexManager.use((Entry) theEntry);
			itsIndexCount++;
		}
		else if (theEntry == DISCARDED_ENTRY)
		{
			// The index was written to the disk and discarded
			PageIOStream theStream = getIndexPage(aIndex);
			
			BTree<T> theTree = loadIndex(aIndex, theStream);
			theIndex = new BTreeWrapper<T>(theTree, this, aIndex);
			theEntry = new Entry<BTreeWrapper<T>>(theIndex);
			
			Utils.listSet(itsIndexes, aIndex, theEntry);
			itsIndexManager.use((Entry) theEntry);
			itsLoadCount++;
		}
		else 
		{
			theIndex = theEntry.getValue();
			if (theIndex.shouldUse()) itsIndexManager.use((Entry) theEntry);
		}
		
		
		return theIndex.getTree();
	}
	
	/**
	 * Returns the {@link PageIOStream} corresponding to the given index,
	 * positionned right before where the index is stored.
	 */
	private PageIOStream getIndexPage(int aIndex)
	{
		int thePageId = itsIndexPages.get(aIndex/itsIndexesPerPage);
		
		Page thePage;
		if (thePageId == 0)
		{
			thePage = itsFile.create();
			itsIndexPages.set(aIndex/itsIndexesPerPage, thePage.getPageId());
		}
		else
		{
			thePage = itsFile.get(thePageId);
		}
		
		PageIOStream theBitStruct = thePage.asIOStream();
		theBitStruct.setPos((aIndex % itsIndexesPerPage) * BTree.getSerializedSize());
		
		return theBitStruct;
	}
	
	private void discardIndex(int aIndex)
	{
		Entry<BTreeWrapper<T>> theEntry = Utils.listGet(itsIndexes, aIndex);
		assert theEntry != null : itsName+": "+aIndex;
		assert theEntry != DISCARDED_ENTRY : itsName+": "+aIndex;
		assert theEntry.getValue() != null : itsName+": "+aIndex;
		BTree<T> theIndex = theEntry.getValue().getTree();
		
		theIndex.writeTo(getIndexPage(aIndex));
		Utils.listSet(itsIndexes, aIndex, DISCARDED_ENTRY);
		itsDiscardCount++;
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
	public static <T extends Tuple> IBidiIterator<T> createFilteredIterator(IBidiIterator<T> aIterator)
	{
		return new DuplicateFilterIterator<T>(aIterator);
	}

	/**
	 * The index manager ensures that least-frequently-used indexes
	 * are discarded so that they do not waste memory.
	 * @author gpothier
	 */
	public static class IndexManager extends ConcurrentMRUBuffer<Integer, BTreeWrapper<? extends Tuple>>
	{
		private boolean itsDisposed = false;
		
		public IndexManager()
		{
			super((int) ((DB_PAGE_BUFFER_SIZE/DB_PAGE_SIZE) / 1), false, DebuggerGridConfigNG.DB_TASK_SIZE);
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
		protected void dropped(BTreeWrapper<? extends Tuple> aValue)
		{
			if (itsDisposed) return;
			aValue.getIndexSet().discardIndex(aValue.getIndex());
		}
		
		@Override
		public Entry<BTreeWrapper<? extends Tuple>> getEntry(Integer aKey, boolean aFetch)
		{
			if (itsDisposed) throw new IllegalStateException();
			return super.getEntry(aKey, aFetch);
		}
		
		@Override
		protected boolean isStillValid(Entry<BTreeWrapper< ? extends Tuple>> aEntry)
		{
			BTreeWrapper<? extends Tuple> theWrapper = aEntry.getValue();
			return Utils.listGet(theWrapper.getIndexSet().itsIndexes, theWrapper.getIndex()) != DISCARDED_ENTRY;
		}

		@Override
		protected BTreeWrapper<? extends Tuple> fetch(Integer aId)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected Integer getKey(BTreeWrapper<? extends Tuple> aValue)
		{
			throw new UnsupportedOperationException();
		}
	}
	
	private static class BTreeWrapper<T extends Tuple>
	{
		private final BTree<T> itsTree;
		private final IndexSet<T> itsIndexSet;
		
		/**
		 * The position of this index within the set.
		 */
		private final int itsIndex;
		
		private int itsUseCount = 0;
		
		public BTreeWrapper(
				BTree<T> aTree, 
				IndexSet<T> aIndexSet, 
				int aIndex)
		{
			itsTree = aTree;
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

		public BTree<T> getTree()
		{
			return itsTree;
		}
		
		/**
		 * Increments the use count, and returns true and resets the count if threshold is
		 * reached.
		 */
		public boolean shouldUse()
		{
			if (itsUseCount++ > DebuggerGridConfigNG.DB_USE_THRESHOLD)
			{
				itsUseCount = 0;
				return true;
			}
			else return false;
		}
		
		@Override
		public String toString()
		{
			return "BTreeWrapper "+itsIndexSet.getName()+"["+itsIndex+"]";
		}

	}

	protected static class DuplicateFilterIterator<T extends Tuple> extends AbstractFilteredBidiIterator<T, T>
		{
			private long itsLastKey;
			private int itsDirection = 0;
			
			public DuplicateFilterIterator(IBidiIterator<T> aIterator)
			{
				super(aIterator);
				itsLastKey = -1;
			}
			
			@Override
			protected T fetchNext()
			{
				if (itsDirection != 1) itsLastKey = -1;
				itsDirection = 1;
				return super.fetchNext();
			}
			
			@Override
			protected T fetchPrevious()
			{
				if (itsDirection != -1) itsLastKey = -1;
				itsDirection = -1;
				return super.fetchPrevious();
			}
			
			@Override
			protected Object transform(T aIn)
			{
				if (aIn.getKey() == itsLastKey) return REJECT;
				itsLastKey = aIn.getKey();
				
				return aIn;
			}
		}
	
}
