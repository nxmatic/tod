package tod.impl.evdbng.db.file;

import static tod.impl.evdbng.DebuggerGridConfigNG.DB_MAX_INDEX_LEVELS;

import gnu.trove.TIntArrayList;

import java.util.Arrays;

import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.utils.BitBuffer;
import zz.utils.cache.MRUBuffer;

/**
 * A BTree where tuples in leaves is encoded using delta/gamma codes.
 * Keys are longs, values are ints.
 * The number of tuples in a leaf node is variable.
 * Keys can be duplicated.
 * The tree supports random insertion but this is innefficient.
 * @author gpothier
 */
public class DeltaBTree
{
	/**
	 * The name of this btree (the index it represents).
	 */
	private final String itsName;
	private final PagedFile itsFile;
	
	private PidSlot itsRootPageSlot;
	
	/**
	 * The last page of each level.
	 */
	private Page[] itsCurrentPages = new Page[DB_MAX_INDEX_LEVELS];
	private int itsCurrentRootLevel;
	private long itsCurrentLastKey;
	private int itsCurrentLastValue;
	private int itsCurrentTupleCount;
	
	/**
	 * The current page as a bit buffer.
	 * Work is done on the bit buffer, and written out to the page when the current page changes.
	 */
	private BitBuffer itsCurrentBuffer = BitBuffer.allocate((PagedFile.PAGE_SIZE-getPageHeaderSize())*8);
	
	private DecodedPagesCache itsDecodedPagesCache = new DecodedPagesCache();

	public DeltaBTree(String aName, PagedFile aFile, PidSlot aRootPageSlot)
	{
		itsName = aName;
		itsFile = aFile;
		itsRootPageSlot = aRootPageSlot;
		init();
	}
	
	public PagedFile getFile()
	{
		return itsFile;
	}

	private Page getRootPage()
	{
		Page thePage = itsRootPageSlot.getPage(false);
		if (thePage == null)
		{
			thePage = getFile().create();
			setPageHeader_Level(thePage, 0);
			setPageHeader_TupleCount(thePage, 0);
			setPageHeader_LastKey(thePage, 0);
			setPageHeader_LastValue(thePage, 0);
			itsRootPageSlot.setPage(thePage);
		}
		return thePage;
	}
	
	/**
	 * Sets up the current state (current page, buffer position, last key...)
	 */
	private void init()
	{
		Page thePage = getRootPage();
		int theLevel = getPageHeader_Level(thePage);
		itsCurrentRootLevel = theLevel;
		
		while(theLevel > 0)
		{
			itsCurrentPages[theLevel] = thePage;
			int theTupleCount = getPageHeader_TupleCount(thePage);
			int thePid = getPidAt_Internal(thePage, theTupleCount-1);
			thePage = getFile().get(thePid);
			theLevel--;
			assert getPageHeader_Level(thePage) == theLevel;
		}

		itsCurrentPages[theLevel] = thePage;
		loadCurrentLeafPage();
		
		// Forward the buffer until it is positioned right after the last tuple
		for(int i=0;i<itsCurrentTupleCount;i++)
		{
			long kd = itsCurrentBuffer.getGammaLong();
			int vd = itsCurrentBuffer.getGammaInt();
		}
	}
	
	private void loadCurrentLeafPage()
	{
		Page thePage = itsCurrentPages[0];
		itsCurrentLastKey = getPageHeader_LastKey(thePage);
		itsCurrentLastValue = getPageHeader_LastValue(thePage);
		itsCurrentTupleCount = getPageHeader_TupleCount(thePage);
		loadLeafPage(itsCurrentBuffer, thePage);
	}
	
	private static void loadLeafPage(BitBuffer aTarget, Page aPage)
	{
		aTarget.clear();
		for(int i=getPageHeaderSize();i<PagedFile.PAGE_SIZE;i+=4)
			aTarget.put(aPage.readInt(i), 32);
		aTarget.position(0);
		aTarget.limit(aTarget.capacity());
	}
	
	private void saveCurrentLeafPage()
	{
		Page thePage = itsCurrentPages[0];
		itsCurrentBuffer.position(0);
		for(int i=getPageHeaderSize();i<PagedFile.PAGE_SIZE;i+=4)
			thePage.writeInt(i, itsCurrentBuffer.getInt(32));
		setPageHeader_LastKey(thePage, itsCurrentLastKey);
		setPageHeader_LastValue(thePage, itsCurrentLastValue);
		setPageHeader_TupleCount(thePage, itsCurrentTupleCount);
	}
	
	public void flush()
	{
		int p = itsCurrentBuffer.position();
		saveCurrentLeafPage();
		itsCurrentBuffer.position(p);
	}
	
	/**
	 * Returns the offset at which keys or key/value pairs start in a page.
	 * This leaves rooms for pointers and stuff.
	 */
	private static int getPageHeaderSize()
	{
		return 16;
	}
	
	private static int getPageHeader_TupleCount(Page aPage)
	{
		return aPage.readShort(0);
	}
	
	private static void setPageHeader_TupleCount(Page aPage, int aCount)
	{
		aPage.writeShort(0, aCount);
	}
	
	private static boolean getPageHeader_PageSorted(Page aPage)
	{
		return aPage.readBoolean(2);
	}
	
	private static void setPageHeader_PageSorted(Page aPage, boolean aSorted)
	{
		aPage.writeBoolean(2, aSorted);
	}
	
	private static int getPageHeader_Level(Page aPage)
	{
		return aPage.readByte(3);
	}
	
	private static void setPageHeader_Level(Page aPage, int aLevel)
	{
		aPage.writeByte(3, aLevel);
	}
	
	private static long getPageHeader_LastKey(Page aPage)
	{
		return aPage.readLong(4);
	}
	
	private static void setPageHeader_LastKey(Page aPage, long aKey)
	{
		aPage.writeLong(4, aKey);
	}
	
	private static int getPageHeader_LastValue(Page aPage)
	{
		return aPage.readInt(12);
	}
	
	private static void setPageHeader_LastValue(Page aPage, int aValue)
	{
		aPage.writeInt(12, aValue);
	}
	
	private static int getTupleSize_Internal()
	{
		return 12; // Key (long), pid (int)
	}
	
	private static long getKeyAt_Internal(Page aPage, int aIndex)
	{
		int theOffset = getPageHeaderSize() + aIndex*getTupleSize_Internal();
		return aPage.readLong(theOffset);
	}
	
	private static void setTupleAt_Internal(Page aPage, int aIndex, long aKey, int aValue)
	{
		int theOffset = getPageHeaderSize() + aIndex*getTupleSize_Internal();
		aPage.writeLong(theOffset, aKey);
		aPage.writeInt(theOffset+8, aValue);
	}
	
	private static int getPidAt_Internal(Page aPage, int aIndex)
	{
		int theOffset = getPageHeaderSize() + aIndex*getTupleSize_Internal();
		return aPage.readInt(theOffset+8);
	}
	
	private static int getTuplesPerPage_Internal()
	{
		return (PagedFile.PAGE_SIZE-getPageHeaderSize())/getTupleSize_Internal();
	}
	
	/**
	 * Returns the index of the given key in the given internal page, or -1-i, where i is the 
	 * insertion point, if the key is absent.
	 */
	private static int indexOf_Internal(Page aPage, long aKey)
	{
        int low = 0;
        int high = getPageHeader_TupleCount(aPage)-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = getKeyAt_Internal(aPage, mid);

            if (midVal < aKey)
                low = mid + 1;
            else if (midVal > aKey)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
	}
	
	/**
	 * Maximum size (in bits) of a leaf tuple (key, value).
	 */
	private static int getMaxTupleBits_Leaf()
	{
		return 128+64;
	}
	
	/**
	 * Appends a tuple to the indicated internal level, indicating that
	 * the page identified as aPid contains keys up to aLastKey
	 */
	private void appendInternalTuple(int aLevel, long aLastKey, int aPid)
	{
		if (aLevel > itsCurrentRootLevel)
		{
			Page theNewRootPage = getFile().create();
			setPageHeader_Level(theNewRootPage, aLevel);
			setPageHeader_TupleCount(theNewRootPage, 0);
			itsCurrentPages[aLevel] = theNewRootPage;
			itsCurrentRootLevel++;
			itsRootPageSlot.setPage(theNewRootPage);
			assert itsCurrentRootLevel == aLevel;
		}

		Page thePage = itsCurrentPages[aLevel];
		int theTupleCount = getPageHeader_TupleCount(thePage);
		if (theTupleCount >= getTuplesPerPage_Internal())
		{
			appendInternalTuple(aLevel+1, getPageHeader_LastKey(thePage), thePage.getPageId());
			Page theNewPage = getFile().create();
			setPageHeader_Level(theNewPage, aLevel);
			setPageHeader_TupleCount(theNewPage, 0);
			itsCurrentPages[aLevel] = theNewPage;
			thePage = theNewPage;
			theTupleCount = 0;
		}

		setTupleAt_Internal(thePage, theTupleCount, aLastKey, aPid);
		setPageHeader_TupleCount(thePage, theTupleCount+1);
		setPageHeader_LastKey(thePage, aLastKey);
	}
	
	private void newLeafPage()
	{
		saveCurrentLeafPage();
		itsCurrentBuffer.position(0);
		itsCurrentBuffer.limit(itsCurrentBuffer.capacity());

		Page theNewPage = getFile().create();
		setPageHeader_Level(theNewPage, 0);
		setPageHeader_TupleCount(theNewPage, 0);
		// Last key/value are saved when the page is finished

		appendInternalTuple(1, itsCurrentLastKey, itsCurrentPages[0].getPageId());
		itsCurrentPages[0] = theNewPage;
		itsCurrentTupleCount = 0;
	}
	
	/**
	 * Adds a (key, value) tuple to the tree.
	 */
	public void insertLeafTuple(long aKey, int aValue)
	{
		itsDecodedPagesCache.dropAll(); // That's a bit more than strictly necessary, but we're on the safe side.
		if (aKey >= itsCurrentLastKey)
		{
			if (itsCurrentBuffer.remaining() < getMaxTupleBits_Leaf()) newLeafPage();
			itsCurrentBuffer.putGamma(aKey-itsCurrentLastKey);
			itsCurrentBuffer.putGamma(aValue-itsCurrentLastValue);
			itsCurrentLastKey = aKey;
			itsCurrentLastValue = aValue;
			itsCurrentTupleCount++;
		}
		else
		{
			throw new UnsupportedOperationException("Not yet supported");
		}
	}
	
	private Page moveToNextPage(int aLevel, Page[] aPages, int[] aIndexes)
	{
		if (aPages[aLevel] == itsCurrentPages[aLevel]) return null;
		
		Page theParentPage = aPages[aLevel+1];
		int theParentIndex = aIndexes[aLevel+1];
		int theParentTupleCount = getPageHeader_TupleCount(theParentPage);
		if (theParentIndex < theParentTupleCount-1)
		{
			theParentIndex++;
			aIndexes[aLevel+1] = theParentIndex;
			int thePid = getPidAt_Internal(theParentPage, theParentIndex);
			return getFile().get(thePid);
		}
		else if (theParentTupleCount == getTuplesPerPage_Internal())
		{
			theParentPage = moveToNextPage(aLevel+1, aPages, aIndexes);
			aIndexes[aLevel+1] = 0;
			aPages[aLevel+1] = theParentPage;
			int thePid = getPidAt_Internal(theParentPage, 0);
			return getFile().get(thePid);
		}
		else return itsCurrentPages[aLevel];
	}
	
	private DecodedLeafPage decodePage(Page aPage)
	{
		return itsDecodedPagesCache.get(aPage.getPageId());
	}
	
	public int[] getValues(long aKey)
	{
		Page[] thePages = new Page[DB_MAX_INDEX_LEVELS];
		int[] theIndexes = new int[DB_MAX_INDEX_LEVELS];
		int theLevel = itsCurrentRootLevel;
		Page thePage = getRootPage();
		while(theLevel > 0)
		{
			thePages[theLevel] = thePage;
			long theLastKey = getPageHeader_LastKey(thePage);
			if (aKey <= theLastKey)
			{
				int theIndex = indexOf_Internal(thePage, aKey);
				if (theIndex < 0) theIndex = -theIndex-1;
				theIndexes[theLevel] = theIndex;
				int thePid = getPidAt_Internal(thePage, theIndex);
				thePage = getFile().get(thePid);
			}
			else
			{
				thePage = itsCurrentPages[theLevel-1];
				theIndexes[theLevel] = -1;
			}
			theLevel--;
		}
		
		thePages[theLevel] = thePage;
		DecodedLeafPage theDecodedPage = decodePage(thePage);
		int theIndex = theDecodedPage.indexOf(aKey);
		if (theIndex < 0) return null;
		
		while(theIndex > 0 && theDecodedPage.getKeyAt(theIndex-1) == aKey) theIndex--;
		
		TIntArrayList theResult = new TIntArrayList(4);
		do
		{
			theResult.add(theDecodedPage.getValueAt(theIndex));
			theIndex++;
			if (theIndex >= theDecodedPage.getTupleCount())
			{
				thePage = moveToNextPage(0, thePages, theIndexes);
				if (thePage == null) break;
				theDecodedPage = decodePage(thePage);
				theIndex = 0;
			}
		} while(theDecodedPage.getKeyAt(theIndex) == aKey);
		
		return theResult.toNativeArray();
	}
	
	private static class DecodedLeafPage
	{
		private final int itsPageId;
		private final long[] itsKeys;
		private final int[] itsValues;
		
		public DecodedLeafPage(Page aPage)
		{
			itsPageId = aPage.getPageId();
			int theTupleCount = getPageHeader_TupleCount(aPage);
			long theLastKey = getPageHeader_LastKey(aPage);
			int theLastValue = getPageHeader_LastValue(aPage);
			
			itsKeys = new long[theTupleCount];
			itsValues = new int[theTupleCount];
			BitBuffer theBuffer = BitBuffer.allocate((PagedFile.PAGE_SIZE-getPageHeaderSize())*8);
			loadLeafPage(theBuffer, aPage);
			
			long theCurrentKey = 0;
			int theCurrentValue = 0;
			for(int i=0;i<theTupleCount;i++)
			{
				long kd = theBuffer.getGammaLong();
				int vd = theBuffer.getGammaInt();
				
				theCurrentKey += kd;
				itsKeys[i] = theCurrentKey;
				
				theCurrentValue += vd;
				itsValues[i] = theCurrentValue;
			}
			
			if (theTupleCount > 0)
			{
				// Fix keys and values
				long theKeysDelta = itsKeys[theTupleCount-1] - theLastKey;
				int theValuesDelta = itsValues[theTupleCount-1] - theLastValue;
				for(int i=0;i<theTupleCount;i++)
				{
					itsKeys[i] -= theKeysDelta;
					itsValues[i] -= theValuesDelta;
				}
			}
		}
		
		public int getPageId()
		{
			return itsPageId;
		}
		
		public int indexOf(long aKey)
		{
			return Arrays.binarySearch(itsKeys, aKey);
		}
		
		public int getTupleCount()
		{
			return itsKeys.length;
		}
		
		public long getKeyAt(int aIndex)
		{
			return itsKeys[aIndex];
		}
		
		public int getValueAt(int aIndex)
		{
			return itsValues[aIndex];
		}
	}
	
	private class DecodedPagesCache extends MRUBuffer<Integer, DecodedLeafPage>
	{
		public DecodedPagesCache()
		{
			super(64);
		}
		
		@Override
		protected Integer getKey(DecodedLeafPage aValue)
		{
			return aValue.getPageId();
		}

		@Override
		protected DecodedLeafPage fetch(Integer aId)
		{
			Page thePage = getFile().get(aId);
			return new DecodedLeafPage(thePage);
		}
	}
}
