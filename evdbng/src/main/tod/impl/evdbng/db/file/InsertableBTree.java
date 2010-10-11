package tod.impl.evdbng.db.file;

import static tod.impl.evdbng.DebuggerGridConfigNG.DB_MAX_INDEX_LEVELS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import tod.core.DebugFlags;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import zz.utils.Utils;

/**
 * A B+Tree that supports insertion of keys in any order
 * @author gpothier
 */
public abstract class InsertableBTree<T extends Tuple>
{
	private static final boolean CHECKS = false;
	private static final boolean LOG = false;
	
	/**
	 * Whether leaves are sorted. Because we are less concerned about retrievel performance than
	 * about insertion performance, we experiment with leaving the leaves unsorted.
	 */
	private static final boolean SORT_LEAVES = true;
	
	/**
	 * The name of this btree (the index it represents).
	 */
	private final String itsName;
	
	private PidSlot itsRootPageSlot;
	
	/**
	 * Size of data associated to each tuple.
	 */
	private final TupleBufferFactory<T> itsTupleBufferFactory = getTupleBufferFactory();
	
	private static PrintWriter itsLogWriter;
	
	static
	{
		if (DebugFlags.DB_LOG_DIR != null) 
		{
			try
			{
				itsLogWriter = new PrintWriter(new File(DebugFlags.DB_LOG_DIR+"/btree.log"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Write everything to the same file otherwise we can get too many open files.
	 */
	private static void log(String aName, long aKey, String aExtradata)
	{
		if (DebugFlags.DB_LOG_DIR != null)
		{
			synchronized(StaticBTree.class)
			{
				itsLogWriter.print(aName+" - "+aKey);
				if (aExtradata != null) itsLogWriter.print(" "+aExtradata);
				itsLogWriter.println();
				itsLogWriter.flush();
			}
		}
	}
	
	public InsertableBTree(String aName, PidSlot aRootPageSlot)
	{
		itsName = aName;
		itsRootPageSlot = aRootPageSlot;
	}
	
	private Page getRootPage()
	{
		Page thePage = itsRootPageSlot.getPage(false);
		if (thePage == null)
		{
			thePage = getFile().create();
			setPageHeader_Level(thePage, 0);
			itsRootPageSlot.setPage(thePage);
		}
		return thePage;
	}
	
	private int getRootLevel()
	{
		return getPageHeader_Level(getRootPage());
	}
	
	protected void logLeafTuple(long aKey, String aExtradata)
	{
		log(itsName, aKey, aExtradata);
	}
	
	public PagedFile getFile()
	{
		return itsRootPageSlot.getFile();
	}

	/**
	 * Returns the tuple buffer factory for this btree's leaf nodes.
	 * Note that this method is called only once, during the initialization
	 * of the BTree. It should return a constant. 
	 */
	protected abstract TupleBufferFactory<T> getTupleBufferFactory();
	
	/**
	 * Returns the offset at which keys or key/value pairs start in a page.
	 * This leaves rooms for pointers and stuff.
	 */
	protected int getPageHeaderSize()
	{
		return 4;
	}
	
	private int getPageHeader_TupleCount(Page aPage)
	{
		return aPage.readShort(0);
	}
	
	private void setPageHeader_TupleCount(Page aPage, int aCount)
	{
		aPage.writeShort(0, aCount);
	}
	
	private boolean getPageHeader_PageSorted(Page aPage)
	{
		return aPage.readBoolean(2);
	}
	
	private void setPageHeader_PageSorted(Page aPage, boolean aSorted)
	{
		aPage.writeBoolean(2, aSorted);
	}
	
	private int getPageHeader_Level(Page aPage)
	{
		return aPage.readByte(3);
	}
	
	private void setPageHeader_Level(Page aPage, int aLevel)
	{
		aPage.writeByte(3, aLevel);
	}
	
	private int getTupleSize(int aLevel)
	{
		TupleBufferFactory<?> theFactory = aLevel == 0 ? itsTupleBufferFactory : INTERNAL;
		return 8+theFactory.getDataSize();
	}
	
	private long getKeyAt(Page aPage, int aLevel, int aIndex)
	{
		int theOffset = getPageHeaderSize() + aIndex*getTupleSize(aLevel);
		return aPage.readLong(theOffset);
	}
	
	private void setInternalKeyAt(Page aPage, int aIndex, long aKey)
	{
		int theOffset = getPageHeaderSize() + aIndex*getTupleSize(1);
		aPage.writeLong(theOffset, aKey);
	}
	
	private int getInternalPidAt(Page aPage, int aIndex)
	{
		int theOffset = getPageHeaderSize() + aIndex*getTupleSize(1);
		return aPage.readInt(theOffset+8);
	}
	
	private int getTuplesPerPage(int aLevel)
	{
		return (PagedFile.PAGE_SIZE-getPageHeaderSize())/getTupleSize(aLevel);
	}
	
	/**
	 * Returns the index of the given key in the page, or -1-i, where i is the 
	 * insertion point, if the key is absent.
	 */
	private int indexOf(Page aPage, int aLevel, long aKey)
	{
        int low = 0;
        int high = getPageHeader_TupleCount(aPage)-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = getKeyAt(aPage, aLevel, mid);

            if (midVal < aKey)
                low = mid + 1;
            else if (midVal > aKey)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
	}
	
	public T getTupleAt(long aKey)
	{
		if (LOG) System.out.println("Looking up: "+aKey);
		int theLevel = getRootLevel();
		Page thePage = getRootPage();
		while(theLevel > 0)
		{
//			if (LOG) System.out.println(internalPageToString(thePage));
			int theIndex = indexOf(thePage, theLevel, aKey);
			if (theIndex < 0)
			{
				theIndex = -theIndex-2;
				if (theIndex < 0) return null; // No such key
			}
			int theChildPid = getInternalPidAt(thePage, theIndex);
			thePage = getFile().get(theChildPid);
			theLevel--;
		}
		
		// Reached leaf page
		ensureSorted(thePage);
		int theIndex = indexOf(thePage, 0, aKey);

		if (theIndex < 0) return null;
		PageIOStream theStream = thePage.asIOStream();
		theStream.setPos(getPageHeaderSize() + theIndex*(8+itsTupleBufferFactory.getDataSize()) + 8);
		return itsTupleBufferFactory.readTuple(aKey, theStream);
	}
	
	private String internalPageToString(Page aPage)
	{
		StringBuilder theBuilder = new StringBuilder();
		int theTupleCount = getPageHeader_TupleCount(aPage);

		theBuilder.append("Page id: "+aPage.getPageId()+"\n");
		theBuilder.append("Tuple count: "+theTupleCount+"\n");
		for(int i=0;i<theTupleCount;i++)
		{
			theBuilder.append(getKeyAt(aPage, 1, i));
			theBuilder.append(" -> ");
			theBuilder.append(getInternalPidAt(aPage, i));
			theBuilder.append("\n");
		}
		
		return theBuilder.toString();
	}
	
	private void clearTuple(PageIOStream aStream)
	{
		int thePosition = aStream.getPos();
		itsTupleBufferFactory.clearTuple(aStream);
		aStream.setPos(thePosition);
	}
	
	/**
	 * Adds a key to the tree, and returns a {@link PageIOStream}
	 * to which the extra data can be written.
	 */
	protected PageIOStream insertLeafKey(long aKey, boolean aAllowOverwrite)
	{
		// If we allow overwrite we must sort the leaves, otherwise search is inefficient.
		if (aAllowOverwrite && ! SORT_LEAVES) throw new UnsupportedOperationException();
		
		int[] theIndexes = new int[DB_MAX_INDEX_LEVELS]; // MAX instead of root level in hope the compiler can optimize better.
		Page[] thePages = new Page[DB_MAX_INDEX_LEVELS];
		
		int theLevel = getRootLevel();
		Page thePage = getRootPage();
		while(theLevel > 0)
		{
			int theIndex = indexOf(thePage, theLevel, aKey);
			if (theIndex < 0)
			{
				theIndex = -theIndex-2;
				if (theIndex < 0) 
				{
					// Extend the tree to the left
					setInternalKeyAt(thePage, 0, aKey);
					theIndex = 0;
				}
			}
			theIndexes[theLevel] = theIndex+1;
			thePages[theLevel] = thePage;
			int theChildPid = getInternalPidAt(thePage, theIndex);
			thePage = getFile().get(theChildPid);
			theLevel--;
		}
		
		// Reached leaf page
		if (SORT_LEAVES)
		{
			int theIndex = indexOf(thePage, 0, aKey);
			if (theIndex >= 0) 
			{
				if (aAllowOverwrite)
				{
					int theTupleSize = getTupleSize(0);
					PageIOStream theStream = thePage.asIOStream();
					theStream.setPos(getPageHeaderSize() + theIndex*theTupleSize + 8);
					return theStream;
				}
				else throw new RuntimeException("Key already present: "+aKey);
			}
			PageIOStream theStream = insertKey(thePage, 0, thePages, theIndexes, -theIndex-1, aKey);
			clearTuple(theStream);
			return theStream;
		}
		else
		{
			int theTupleCount = getPageHeader_TupleCount(thePage);
			PageIOStream theStream = insertKey(thePage, 0, thePages, theIndexes, theTupleCount, aKey);
			clearTuple(theStream);
			return theStream;
		}		
	}
	
	private void checkKeysSorted(Page aPage, int aLevel)
	{
		if (aLevel == 0 && !getPageHeader_PageSorted(aPage)) return;
		int theTupleCount = getPageHeader_TupleCount(aPage);
		long theLastKey = Long.MIN_VALUE;
		for(int i=0;i<theTupleCount;i++)
		{
			long theKey = getKeyAt(aPage, aLevel, i);
			assert theKey > theLastKey;
			theLastKey = theKey;
		}
	}
	
	private PageIOStream insertKey(Page aPage, int aLevel, Page[] aPages, int[] aIndexes, int aIndex, long aKey)
	{
		int theTupleCount = getPageHeader_TupleCount(aPage);
		int theMaxCount = getTuplesPerPage(aLevel);
		int theTupleSize = getTupleSize(aLevel);
		
		if (theTupleCount < theMaxCount)
		{
			if (CHECKS) checkKeysSorted(aPage, aLevel);
			int theOffset = getPageHeaderSize() + aIndex*theTupleSize;
			if (theTupleCount != aIndex) aPage.move(theOffset, (theTupleCount-aIndex)*theTupleSize, theTupleSize);
			PageIOStream theStream = aPage.asIOStream();
			theStream.setPos(theOffset);
			theStream.writeLong(aKey);
			if (LOG) Utils.println("Wrote key: %d to page %d.", aKey, aPage.getPageId());
			setPageHeader_TupleCount(aPage, theTupleCount+1);
			if (! SORT_LEAVES && aLevel == 0) setPageHeader_PageSorted(aPage, false);
			if (CHECKS) checkKeysSorted(aPage, aLevel);
			return theStream;
		}
		else
		{
			Page theNewPage = splitPage(aPage, aLevel, aPages, aIndexes);
			long theRightKey = getKeyAt(theNewPage, aLevel, 0);
			if (theRightKey == aKey) throw new RuntimeException("Key already present: "+aKey);
			
			int theLeftTuples = getPageHeader_TupleCount(aPage);
			int theRightTuples = getPageHeader_TupleCount(theNewPage);
			if (aKey < theRightKey) 
			{
				if (! SORT_LEAVES && aLevel == 0) aIndex = theLeftTuples;
				return insertKey(aPage, aLevel, aPages, aIndexes, aIndex, aKey);
			}
			else
			{
				if (! SORT_LEAVES && aLevel == 0) aIndex = theRightTuples;
				else 
				{
					aIndex -= theLeftTuples;
					if (CHECKS)
					{
						int theCheckIndex = indexOf(theNewPage, aLevel, aKey);
						assert aIndex == -theCheckIndex-1;
					}
				}
				return insertKey(theNewPage, aLevel, aPages, aIndexes, aIndex, aKey);
			}
		}
	}
	
	private void ensureSorted(Page aPage)
	{
		if (SORT_LEAVES) return;
		if (getPageHeader_PageSorted(aPage)) return;
		
		int theTupleCount = getPageHeader_TupleCount(aPage);

		// Decode page into tuple buffer
		TupleBuffer<T> theBuffer = itsTupleBufferFactory.create(theTupleCount, 0, 0);
		PageIOStream theStream = aPage.asIOStream();
		theStream.setPos(getPageHeaderSize());
		for(int i=0;i<theTupleCount;i++)
		{
			long theKey = theStream.readLong();
			theBuffer.read(theKey, theStream);
		}
		
		// Sort tuple buffer
		theBuffer.sort();
		
		// Re-encode the sorted buffer
		theStream.setPos(getPageHeaderSize());
		theBuffer.setPosition(0);
		for(int i=0;i<theTupleCount;i++)
		{
			theStream.writeLong(theBuffer.getKey(i));
			theBuffer.write(theStream);
		}
		
		setPageHeader_PageSorted(aPage, true);
	}
	
	private Page splitPage(Page aPage, int aLevel, Page[] aPages, int[] aIndexes)
	{
		if (aLevel == 0) ensureSorted(aPage);
		int theTupleCount = getTuplesPerPage(aLevel);
		assert theTupleCount == getPageHeader_TupleCount(aPage);
		
		int theLeftTuples = theTupleCount/2;
		int theRightTuples = theTupleCount-theLeftTuples;

		Page theNewPage = getFile().create();
		setPageHeader_TupleCount(aPage, theLeftTuples);
		setPageHeader_TupleCount(theNewPage, theRightTuples);
		
		if (! SORT_LEAVES && aLevel == 0)
		{
			setPageHeader_PageSorted(aPage, true);
			setPageHeader_PageSorted(theNewPage, true);
		}
		
		long theRightKey = getKeyAt(aPage, aLevel, theLeftTuples);
		int theTupleSize = getTupleSize(aLevel);
		aPage.copy(getPageHeaderSize()+theLeftTuples*theTupleSize, theNewPage, getPageHeaderSize(), theRightTuples*theTupleSize);
		aPage.clear(getPageHeaderSize()+theLeftTuples*theTupleSize, theRightTuples*theTupleSize);

		int theRootLevel = getRootLevel();
		assert aLevel <= theRootLevel;
		if (aLevel == theRootLevel)
		{
			// Create a new root
			Page theNewRoot = getFile().create();
			setPageHeader_TupleCount(theNewRoot, 2);
			setPageHeader_Level(theNewRoot, theRootLevel+1);
			itsRootPageSlot.setPage(theNewRoot);

			long theLeftKey = getKeyAt(aPage, aLevel, 0);
			PageIOStream theStream = theNewRoot.asIOStream();
			theStream.setPos(getPageHeaderSize());
			theStream.writeLong(theLeftKey);
			theStream.writePagePointer(aPage.getPageId());
			theStream.writeLong(theRightKey);
			theStream.writePagePointer(theNewPage.getPageId());
			if (LOG) Utils.println("Splitting page: %d. (lt: %d, rt: %d, rk: %d, np: %d, nr: %d) ", aPage.getPageId(), theLeftTuples, theRightTuples, theRightKey, theNewPage.getPageId(), theNewRoot.getPageId());
		}
		else
		{
			Page theParentPage = aPages[aLevel+1];
			PageIOStream theStream = insertKey(theParentPage, aLevel+1, aPages, aIndexes, aIndexes[aLevel+1], theRightKey);
			theStream.writePagePointer(theNewPage.getPageId());
			if (LOG) Utils.println("Splitting page: %d. (lt: %d, rt: %d, rk: %d, np: %d)", aPage.getPageId(), theLeftTuples, theRightTuples, theRightKey, theNewPage.getPageId());
		}
		
		
		return theNewPage;
	}
	
	/**
	 * Tuple for internal nodes of the {@link StaticBTree}
	 * @author gpothier
	 */
	private static class InternalTuple extends Tuple
	{
		private final int itsPageId;
		
		public InternalTuple(long aKey, int aPageId)
		{
			super(aKey);
			assert aPageId != 0;
			itsPageId = aPageId;
		}

		public int getPageId()
		{
			return itsPageId;
		}
	}

	private static class InternalTupleBuffer extends TupleBuffer<InternalTuple>
	{
		private int[] itsPageIdBuffer;
		
		public InternalTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsPageIdBuffer = new int[aSize];
		}

		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsPageIdBuffer[aPosition] = aStream.readPagePointer();
		}

		@Override
		public void write0(int aPosition, PageIOStream aStream)
		{
			aStream.writePagePointer(itsPageIdBuffer[aPosition]);
		}

		@Override
		public InternalTuple getTuple(int aPosition)
		{
			return new InternalTuple(
					getKey(aPosition), 
					itsPageIdBuffer[aPosition]);
		}

		@Override
		protected void swap(int a, int b)
		{
			swap(itsPageIdBuffer, a, b);
		}
	}

	private static final TupleBufferFactory<InternalTuple> INTERNAL = new TupleBufferFactory<InternalTuple>()
	{
		@Override
		public InternalTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new InternalTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}
		
		@Override
		public int getDataSize()
		{
			return 4;
		}

		@Override
		public InternalTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new InternalTuple(aKey, aStream.readPagePointer());
		}

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writePagePointer(0);
		}
	};

}
