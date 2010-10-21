package tod.impl.evdbng.db.file;

import static tod.impl.evdbng.DebuggerGridConfigNG.DB_MAX_INDEX_LEVELS;

import gnu.trove.TIntArrayList;
import gnu.trove.TLongArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.utils.BitBuffer;
import zz.utils.Utils;
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
	}
	
	private void loadCurrentLeafPage()
	{
		Page thePage = itsCurrentPages[0];
		itsCurrentLastKey = getPageHeader_LastKey(thePage);
		itsCurrentLastValue = getPageHeader_LastValue(thePage);
		itsCurrentTupleCount = getPageHeader_TupleCount(thePage);
		int theCurrentOffset = getPageHeader_CurrentOffset(thePage);
		loadLeafPage(itsCurrentBuffer, thePage);
		itsCurrentBuffer.position(theCurrentOffset);
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
		int theCurrentOffset = itsCurrentBuffer.position();
		itsCurrentBuffer.position(0);
		for(int i=getPageHeaderSize();i<PagedFile.PAGE_SIZE;i+=4)
			thePage.writeInt(i, itsCurrentBuffer.getInt(32));
		setPageHeader_LastKey(thePage, itsCurrentLastKey);
		setPageHeader_LastValue(thePage, itsCurrentLastValue);
		setPageHeader_TupleCount(thePage, itsCurrentTupleCount);
		setPageHeader_CurrentOffset(thePage, theCurrentOffset);
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
		return 20;
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
	
	private static int getPageHeader_CurrentOffset(Page aPage)
	{
		return aPage.readShort(16);
	}
	
	private static void setPageHeader_CurrentOffset(Page aPage, int aOffset)
	{
		aPage.writeShort(16, aOffset);
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
	
	private void newLeafPage(List<Page> aFreePages)
	{
		saveCurrentLeafPage();
		itsCurrentBuffer.position(0);
		itsCurrentBuffer.limit(itsCurrentBuffer.capacity());

		Page theNewPage = aFreePages != null && ! aFreePages.isEmpty() ? 
				aFreePages.remove(aFreePages.size()-1)
				: getFile().create(); 
		setPageHeader_Level(theNewPage, 0);
		setPageHeader_TupleCount(theNewPage, 0);
		// Last key/value are saved when the page is finished

		appendInternalTuple(1, itsCurrentLastKey, itsCurrentPages[0].getPageId());
		itsCurrentPages[0] = theNewPage;
		itsCurrentTupleCount = 0;
	}
	
	private static long min(long[] aValues, int aOffset, int aCount)
	{
		long theMin = Long.MAX_VALUE;
		for(int i=0;i<aCount;i++)
		{
			long theValue = aValues[aOffset+i];
			if (theValue < theMin) theMin = theValue;
		}
		return theMin;
	}
	
	private boolean isSorted(long[] aValues, int aOffset, int aCount)
	{
		if (aCount < 2) return true;
		long theLastValue = aValues[aOffset];
		for(int i=1;i<aCount;i++)
		{
			long theValue = aValues[aOffset+i];
			if (theValue < theLastValue) return false;
			else theLastValue = theValue;
		}
		return true;
	}
	
	private void collectSubtree(List<Page> aTarget, int aLevel, Page aPage)
	{
		aTarget.add(aPage);
		if (aLevel > 0)
		{
			assert aLevel > 0;
			int theTupleCount = getPageHeader_TupleCount(aPage);
			for(int i=0;i<theTupleCount;i++)
			{
				int thePid = getPidAt_Internal(aPage, i);
				Page theChild = getFile().get(thePid);
				collectSubtree(aTarget, aLevel-1, theChild);
			}
		}
	}
	
	/**
	 * Trims the btree so that the last key is the one that precedes
	 * that identified by the pages/indexes.
	 * @return A list of pages that are free after the trimming.
	 */
	private List<Page> trim(Page[] aPages, int[] aIndexes)
	{
		List<Page> theResult = new ArrayList<Page>();
		
		for(int i=0;i<=itsCurrentRootLevel;i++)
		{
			Page thePage = aPages[i];
			int theIndex = aIndexes[i];
			
			if (i > 0 && theIndex != -1)
			{
				assert theIndex >= 0;
				int theTupleCount = getPageHeader_TupleCount(thePage);
				for(int j=theIndex+1;j<theTupleCount;j++)
				{
					int thePid = getPidAt_Internal(thePage, j);
					Page theChild = getFile().get(thePid);
					collectSubtree(theResult, i-1, theChild);
				}
			}

			if (theIndex < 0)
			{
				if (i == 0)
				{
					theIndex = -theIndex-1;
				}
				else
				{
					assert theIndex == -1;
					assert Page.same(aPages[i-1], itsCurrentPages[i-1]);
					continue;
				}
			}
			setPageHeader_TupleCount(thePage, theIndex);
			if (i == 0) 
			{
				itsCurrentTupleCount = theIndex;
				DecodedLeafPage theDecodedPage = getDecodedPage(thePage);
				if (theIndex > 0)
				{
					itsCurrentLastKey = theDecodedPage.getKeyAt(theIndex-1);
					itsCurrentLastValue = theDecodedPage.getValueAt(theIndex-1);
				}
				else
				{
					itsCurrentLastKey = theDecodedPage.getPrevLastKey();
					itsCurrentLastValue = theDecodedPage.getPrevLastValue();
				}
				setPageHeader_LastKey(thePage, itsCurrentLastKey);
				setPageHeader_LastValue(thePage, itsCurrentLastValue);
			}
			else
			{
				// Not necessary to update the last key/value for the page
				// because it will be restored next time a tuple is added,
				// and (I think) it is not needed for appending 
			}
			
			if (itsCurrentPages[i].getPageId() != thePage.getPageId())
			{
				collectSubtree(theResult, i, itsCurrentPages[i]);
				itsCurrentPages[i] = thePage;
			}
		}
		
		return theResult;
	}
	
	/**
	 * Inserts a key in the middle of the tree.
	 * This is achieved by removing all the keys that follow the added key
	 * and re-inserting them, so this is inefficient as hell. But it should
	 * not be called often.
	 */
	private void refill(long[] aKeys, int[] aValues, int aOffset, int aCount)
	{
		assert aCount > 0;
		assert isSorted(aKeys, aOffset, aCount);
	
		flush();
		Utils.println("Key insertion: %d-%d (last: %d, count: %d)", aKeys[aOffset], aKeys[aOffset+aCount-1], itsCurrentLastKey, aCount);
		
		Page[] thePages = new Page[DB_MAX_INDEX_LEVELS];
		int[] theIndexes = new int[DB_MAX_INDEX_LEVELS];
		DecodedLeafPage theDecodedPage = drillTo(aKeys[0], thePages, theIndexes);
		
		Page[] theInsertPages = thePages.clone();
		int[] theInsertIndexes = theIndexes.clone();
		
		TLongArrayList theKeys = new TLongArrayList();
		TIntArrayList theValues = new TIntArrayList();
		
		int theIndex = theIndexes[0];
		Page thePage = thePages[0];
		if (theIndex < 0) theIndex = -theIndex-1;
		
		// Extract the values from the BTree and at the same time merge with the new values.
		while(true)
		{
			long theKey = theDecodedPage.getKeyAt(theIndex);
			int theValue = theDecodedPage.getValueAt(theIndex);
			
			while(aCount > 0 && aKeys[aOffset] <= theKey)
			{
				theKeys.add(aKeys[aOffset]);
				theValues.add(aValues[aOffset]);
				aOffset++;
				aCount--;
			}
			
			theKeys.add(theKey);
			theValues.add(theValue);
			theIndex++;
			if (theIndex >= theDecodedPage.getTupleCount())
			{
				thePage = moveToNextPage(0, thePages, theIndexes);
				if (thePage == null) break;
				theDecodedPage = getDecodedPage(thePage);
				theIndex = 0;
			}
		} 
		
		while(aCount > 0)
		{
			theKeys.add(aKeys[aOffset]);
			theValues.add(aValues[aOffset]);
			aOffset++;
			aCount--;
		}

		Utils.println("Shifting %d tuples", theValues.size());
		List<Page> theFreedPages = trim(theInsertPages, theInsertIndexes);
		loadCurrentLeafPage();
		
		// Forward the buffer until it is positioned right after the last tuple
		itsCurrentBuffer.position(0);
		for(int i=0;i<itsCurrentTupleCount;i++)
		{
			long kd = itsCurrentBuffer.getGammaLong();
			int vd = itsCurrentBuffer.getGammaInt();
		}
		
		int theCount = theKeys.size();
		for(int i=0;i<theCount;i++)
		{
			long theKey = theKeys.get(i);
			int theValue = theValues.get(i);
			
			appendLeafTuple(theKey, theValue, theFreedPages);
		}
		
		assert theFreedPages.size() == 0 : ""+theFreedPages.size();
		itsDecodedPagesCache.dropAll(); // That's a bit more than strictly necessary, but we're on the safe side.
	}
	
	public static long itsKeysBits;
	public static long itsValuesBits;
	public static long itsEntriesCount;
	
	private static final boolean STATS = false;
	
	
	private void appendLeafTuple(long aKey, int aValue, List<Page> aFreePages)
	{
		int p0 = 0;
		int p1 = 0;
		int p2 = 0;
		
		if (itsCurrentBuffer.remaining() < getMaxTupleBits_Leaf()) newLeafPage(aFreePages);
		if (STATS) p0 = itsCurrentBuffer.position();
		itsCurrentBuffer.putGamma(aKey-itsCurrentLastKey);
		if (STATS) p1 = itsCurrentBuffer.position();
		itsCurrentBuffer.putGamma(aValue-itsCurrentLastValue);
		if (STATS) p2 = itsCurrentBuffer.position();
		
		if (STATS) 
		{
			itsKeysBits += p1-p0;
			itsValuesBits += p2-p1;
			itsEntriesCount++;
		}
		
		itsCurrentLastKey = aKey;
		itsCurrentLastValue = aValue;
		itsCurrentTupleCount++;
	}
	
	/**
	 * Adds a (key, value) tuple to the tree.
	 */
	public void insertLeafTuple(long aKey, int aValue)
	{
		itsDecodedPagesCache.dropAll(); // That's a bit more than strictly necessary, but we're on the safe side.
		if (aKey >= itsCurrentLastKey) appendLeafTuple(aKey, aValue, null);
		else refill(new long[] {aKey}, new int[] {aValue}, 0, 1);
	}

	public void insertLeafTuples(long[] aKeys, int[] aValues, int aOffset, int aCount)
	{
		itsDecodedPagesCache.dropAll(); // That's a bit more than strictly necessary, but we're on the safe side.
		
		while(aCount > 0)
		{
			long theKey = aKeys[aOffset];
			int theValue = aValues[aOffset];
			
			if (theKey >= itsCurrentLastKey)
			{
				appendLeafTuple(theKey, theValue, null);
				aOffset++;
				aCount--;
			}
			else
			{
				refill(aKeys, aValues, aOffset, aCount);
				break;
			}
		}
	}

	private Page moveToNextPage(int aLevel, Page[] aPages, int[] aIndexes)
	{
		assert aLevel <= itsCurrentRootLevel;
		if (Page.same(aPages[aLevel], itsCurrentPages[aLevel])) return null;
		
		Page theNextPage;
		Page theParentPage = aPages[aLevel+1];
		int theParentIndex = aIndexes[aLevel+1];
		int theParentTupleCount = getPageHeader_TupleCount(theParentPage);
		if (theParentIndex < theParentTupleCount-1)
		{
			theParentIndex++;
			aIndexes[aLevel+1] = theParentIndex;
			int thePid = getPidAt_Internal(theParentPage, theParentIndex);
			theNextPage = getFile().get(thePid);
		}
		else if (theParentTupleCount == getTuplesPerPage_Internal())
		{
			theParentPage = moveToNextPage(aLevel+1, aPages, aIndexes);
			if (theParentPage == null)
			{
				theNextPage = itsCurrentPages[aLevel];
			}
			else
			{
				aIndexes[aLevel+1] = 0;
				aPages[aLevel+1] = theParentPage;
				int thePid = getPidAt_Internal(theParentPage, 0);
				theNextPage = getFile().get(thePid);
			}
		}
		else theNextPage = itsCurrentPages[aLevel];
		
		aPages[aLevel] = theNextPage;
		return theNextPage;
	}
	
	private DecodedLeafPage getDecodedPage(Page aPage)
	{
		return itsDecodedPagesCache.get(aPage.getPageId());
	}
	
	private DecodedLeafPage drillTo(long aKey, Page[] aPages, int[] aIndexes)
	{
		int theLevel = itsCurrentRootLevel;
		Page thePage = getRootPage();
		while(theLevel > 0)
		{
			assert thePage != null;
			aPages[theLevel] = thePage;
			long theLastKey = getPageHeader_LastKey(thePage);
			if (aKey <= theLastKey)
			{
				int theIndex = indexOf_Internal(thePage, aKey);
				if (theIndex < 0) theIndex = -theIndex-1;
				aIndexes[theLevel] = theIndex;
				int thePid = getPidAt_Internal(thePage, theIndex);
				thePage = getFile().get(thePid);
			}
			else
			{
				thePage = itsCurrentPages[theLevel-1];
				aIndexes[theLevel] = -1;
			}
			theLevel--;
		}
		
		assert thePage != null;
		aPages[0] = thePage;
		DecodedLeafPage theDecodedPage = getDecodedPage(thePage);
		int theIndex = theDecodedPage.indexOf(aKey);
		while(theIndex > 0 && theDecodedPage.getKeyAt(theIndex-1) == aKey) theIndex--;
		
		aIndexes[0] = theIndex;
		return theDecodedPage;
	}
	
	public int[] getValues(long aKey)
	{
		Page[] thePages = new Page[DB_MAX_INDEX_LEVELS];
		int[] theIndexes = new int[DB_MAX_INDEX_LEVELS];
		DecodedLeafPage theDecodedPage = drillTo(aKey, thePages, theIndexes);
		
		int theIndex = theIndexes[0];
		Page thePage = thePages[0];
		if (theIndex < 0) return null;
		
		TIntArrayList theResult = new TIntArrayList(4);
		do
		{
			theResult.add(theDecodedPage.getValueAt(theIndex));
			theIndex++;
			if (theIndex >= theDecodedPage.getTupleCount())
			{
				thePage = moveToNextPage(0, thePages, theIndexes);
				if (thePage == null) break;
				theDecodedPage = getDecodedPage(thePage);
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
		
		private final long itsPrevLastKey;
		private final int itsPrevLastValue;
		
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
				itsPrevLastKey = -theKeysDelta;
				itsPrevLastValue = -theValuesDelta;
			}
			else
			{
				itsPrevLastKey = theLastKey;
				itsPrevLastValue = theLastValue;
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
		
		public long getPrevLastKey()
		{
			return itsPrevLastKey;
		}
		
		public int getPrevLastValue()
		{
			return itsPrevLastValue;
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
