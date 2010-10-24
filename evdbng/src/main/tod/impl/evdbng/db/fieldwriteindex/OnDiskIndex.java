package tod.impl.evdbng.db.fieldwriteindex;

import gnu.trove.TIntArrayList;
import gnu.trove.TLongArrayList;
import tod.impl.evdbng.db.Stats;
import tod.impl.evdbng.db.file.DeltaBTree;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.BooleanSlot;
import tod.impl.evdbng.db.file.Page.IntSlot;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.Page.UnsignedByteSlot;
import tod.impl.evdbng.db.file.PagedFile;
import tod.utils.BitBuffer;
import zz.utils.Utils;
import zz.utils.cache.MRUBuffer;

public class OnDiskIndex
{
	private static final int[] OBJECTS_PER_SHAREDPAGE = {128, 64, 32, 16, 8, 4, 2};
	private static final int N_LOG2_OBJECTSPERPAGE = OBJECTS_PER_SHAREDPAGE.length;
	
	private static final boolean LOG = false;
	private static final boolean CHECK = false;
	
	private final PagedFile itsFile;
	private Directory itsDirectory;
	private PageStack itsEmptyPagesStack;
	
	/**
	 * Stacks of non-full shared pages.
	 */
	private PageStack[] itsSharedPagesStack = new PageStack[N_LOG2_OBJECTSPERPAGE]; 

	private final ObjectBTree itsObjectBTree;
	private final ObjectAccessStoreCache itsObjectAccessStoreCache = new ObjectAccessStoreCache();
	
	private BitBuffer itsSinglesBuffer = BitBuffer.allocate((PagedFile.PAGE_SIZE-4)*8);
	private SinglesPage itsCurrentSinglesPage;

	public OnDiskIndex(PidSlot aDirectoryPageSlot)
	{
		itsFile = aDirectoryPageSlot.getFile();
		itsDirectory = new Directory(aDirectoryPageSlot.getPage(true));
		
		itsObjectBTree = new ObjectBTree("objects map", itsDirectory.getObjectMapSlot());

		itsEmptyPagesStack = new PageStack(itsDirectory.getEmptyPagesStackSlot());
		for(int i=0;i<N_LOG2_OBJECTSPERPAGE;i++)
			itsSharedPagesStack[i] = new PageStack(itsDirectory.getIncompleteSharedPagesStackSlot(i));
		
		itsCurrentSinglesPage = new SinglesPage(this, itsSinglesBuffer);
	}
	
	private PagedFile getFile()
	{
		return itsFile;
	}
	
	public ObjectAccessStore getStore(long aObjectFieldId, boolean aReadOnly)
	{
		return itsObjectAccessStoreCache.get(aReadOnly ? -aObjectFieldId-1 : aObjectFieldId);
	}
	
	public void appendSingle(long aObjectFieldId, long aBlockId, int aThreadId)
	{
		ObjectPageSlot theSlot = itsObjectBTree.getSlot(aObjectFieldId);
		if (! theSlot.isNull()) 
		{
			ObjectAccessStore theStore = getStore(aObjectFieldId, false);
			theStore.append(new long[] {aBlockId}, new int[] {aThreadId}, 0, 1);
		}
		else
		{
			if (itsCurrentSinglesPage.isFull())
			{
				itsCurrentSinglesPage.dump();
				itsCurrentSinglesPage = new SinglesPage(this, itsSinglesBuffer);
			}
			itsCurrentSinglesPage.append(aObjectFieldId, aBlockId, aThreadId);
			theSlot.setSinglesPage(itsCurrentSinglesPage);
		}
	}
	
	/**
	 * Really creates the {@link ObjectAccessStore}; only called when the OAS is not
	 * found in the cache.
	 * @param aObjectFieldId Negative value means readonly.
	 */
	private ObjectAccessStore createStore(long aObjectFieldId)
	{
		if (aObjectFieldId >= 0)
		{
			ObjectPageSlot theSlot = itsObjectBTree.getSlot(aObjectFieldId);
			return new ObjectAccessStore(aObjectFieldId, theSlot);
		}
		else
		{
			// Read-only
			int thePid = itsObjectBTree.get(-aObjectFieldId-1);
			if (thePid == 0) return new ObjectAccessStore(0, null);
			else throw new UnsupportedOperationException("Not yet");
		}
	}
	
	private Page getEmptyPage()
	{
		int thePid = itsEmptyPagesStack.pop();
		if (thePid == 0) return getFile().create();
		else return getFile().get(thePid);
	}
	
	/**
	 * Returns a non-full shared page of the specified size. 
	 * @param aLogMaxCount An index into {@link #OBJECTS_PER_SHAREDPAGE} that indicates 
	 * the number of objects per page
	 */
	private SharedPage getSharedPage(int aLogMaxCount)
	{
		assert aLogMaxCount >= 0 && aLogMaxCount < N_LOG2_OBJECTSPERPAGE;
		PageStack theStack = itsSharedPagesStack[aLogMaxCount];
		Page thePage;
		SharedPage theSharedPage;
		if (theStack.isEmpty())
		{
			thePage = getEmptyPage();
			thePage.writeByte(0, aLogMaxCount);
			theStack.push(thePage.getPageId());
//			if (LOG) Utils.println("New shared page: %d [%d]", thePage.getPageId(), aLogMaxCount);
			theSharedPage = new SharedPage(this, thePage);
			theSharedPage.markFree(true);
		}
		else
		{
			int thePid = theStack.peek();
			thePage = getFile().get(thePid);
//			if (LOG) Utils.println("Reusing shared page: %d [%d]", thePage.getPageId(), aLogMaxCount);
			theSharedPage = new SharedPage(this, thePage);
			assert theSharedPage.isMarkedFree();
			assert theSharedPage.hasFreeIds();
		}
		return theSharedPage;
	}
	
	private void unfreeSharedPage(SharedPage aPage)
	{
		assert ! aPage.hasFreeIds();
		if (! aPage.isMarkedFree()) return;
//		if (LOG) Utils.println("Unfree shared page: %d", aPage.getPageId());
		int theLogMaxCount = aPage.getLogMaxCount();
		PageStack theStack = itsSharedPagesStack[theLogMaxCount];
		int thePid = theStack.pop();
		assert thePid == aPage.getPageId();
		aPage.markFree(false);
	}
	
	private void freeSharedPage(SharedPage aPage)
	{
		assert aPage.hasFreeIds();
		if (aPage.isMarkedFree()) return;
//		if (LOG) Utils.println("Free shared page: %d", aPage.getPageId());
		int theLogMaxCount = aPage.getLogMaxCount();
		PageStack theStack = itsSharedPagesStack[theLogMaxCount];
		theStack.push(aPage.getPageId());
		aPage.markFree(true);
	}
	
	private static class Directory
	{
		private static final int POS_PAGECOUNT = 0;
		private static final int POS_EMPTYPAGESSTACK = 4;
		private static final int POS_OBJECTMAP = 8;
		private static final int POS_SHAREDPAGES_START = 12;
		
		private final Page itsPage;
		private final IntSlot itsCountSlot;
		private final PidSlot itsEmptyPagesSlot;
		private final PidSlot itsObjectMapSlot;
		private final PidSlot[] itsSharedPagesSlots;
		
		public Directory(Page aPage)
		{
			itsPage = aPage;
			
			itsCountSlot = new IntSlot(itsPage, POS_PAGECOUNT);
			itsEmptyPagesSlot = new PidSlot(itsPage, POS_EMPTYPAGESSTACK);
			itsObjectMapSlot = new PidSlot(itsPage, POS_OBJECTMAP);
			
			itsSharedPagesSlots = new PidSlot[N_LOG2_OBJECTSPERPAGE];
			for(int i=0;i<itsSharedPagesSlots.length;i++)
				itsSharedPagesSlots[i] = new PidSlot(itsPage, POS_SHAREDPAGES_START+4*i);
		}

		public IntSlot getCountSlot()
		{
			return itsCountSlot;
		}

		public PidSlot getEmptyPagesStackSlot()
		{
			return itsEmptyPagesSlot;
		}

		public PidSlot getObjectMapSlot()
		{
			return itsObjectMapSlot;
		}

		public PidSlot getIncompleteSharedPagesStackSlot(int aObjectsPerPage_Log2)
		{
			return itsSharedPagesSlots[aObjectsPerPage_Log2];
		}
	}
	
	
	/**
	 * Maintains a stack of page ids.
	 * Format: 
	 *   previous page id: int
	 *   next page id: int
	 *   count: int
	 *   ids: int[]
	 * @author gpothier
	 */
	private class PageStack
	{
		private static final int POS_PREV_ID = 0;
		private static final int POS_NEXT_ID = 4;
		private static final int POS_COUNT = 8;
		private static final int POS_DATA = 12;
		
		private static final int ITEMS_PER_PAGE = (PagedFile.PAGE_SIZE - POS_DATA)/4;
		
		private final PidSlot itsCurrentPageSlot;
		private Page itsCurrentPage;
		
		private PidSlot itsNextPageSlot = new PidSlot();
		private PidSlot itsPrevPageSlot = new PidSlot();
		private IntSlot itsCountSlot = new IntSlot();

		public PageStack(PidSlot aCurrentPageSlot)
		{
			itsCurrentPageSlot = aCurrentPageSlot;
			setCurrentPage(itsCurrentPageSlot.getPage(true));
		}
		
		private void setCurrentPage(Page aPage)
		{
			itsCurrentPage = aPage;
			itsNextPageSlot.setup(itsCurrentPage, POS_NEXT_ID);
			itsPrevPageSlot.setup(itsCurrentPage, POS_PREV_ID);
			itsCountSlot.setup(itsCurrentPage, POS_COUNT);
			
			itsCurrentPageSlot.setPage(aPage);
		}
		
		public boolean isEmpty()
		{
			return itsCountSlot.get() == 0 && ! itsPrevPageSlot.hasPage();
		}
		
		public void push(int aPid)
		{
			int theCount = itsCountSlot.get();
			if (theCount >= ITEMS_PER_PAGE)
			{
				Page theNextPage = itsNextPageSlot.getPage(false);
				if (theNextPage == null)
				{
					theNextPage = itsFile.create();
					theNextPage.writeInt(POS_PREV_ID, itsCurrentPage.getPageId());
					itsNextPageSlot.setPage(theNextPage);
				}
				setCurrentPage(theNextPage);
				theCount = itsCountSlot.get();
				assert theCount == 0;
			}
			
			itsCurrentPage.writeInt(POS_DATA + 4*theCount, aPid);
			itsCountSlot.set(theCount+1);
		}
		
		public int pop()
		{
			int theCount = itsCountSlot.get();
			if (theCount <= 0)
			{
				Page thePrevPage = itsPrevPageSlot.getPage(false);
				if (thePrevPage == null) return 0;
				else
				{
					setCurrentPage(thePrevPage);
					theCount = itsCountSlot.get();
					assert theCount == ITEMS_PER_PAGE;
				}
			}
			
			theCount--;
			itsCountSlot.set(theCount);
			return itsCurrentPage.readInt(POS_DATA + 4*theCount);
		}
		
		public int peek()
		{
			int theCount = itsCountSlot.get();
			if (theCount <= 0)
			{
				Page thePrevPage = itsPrevPageSlot.getPage(false);
				setCurrentPage(thePrevPage);
				theCount = itsCountSlot.get();
				assert theCount == ITEMS_PER_PAGE;
			}
			
			theCount--;
			return itsCurrentPage.readInt(POS_DATA + 4*theCount);
		}
	}
	
	/**
	 * A slot that permits to store a pointer to a data structure that
	 * stores the accesses of a particular object.
	 * There are two kinds of such data structures:
	 * - For objects that have few accesses, a shared page is used
	 * - For objects that have many accesses, a {@link DeltaBTree} is used.
	 * To differentiate the structures we alter the page id that is actually
	 * stored by this slot. We extend {@link PidSlot} so that the {@link DeltaBTree}
	 * can store its current root page without being concerned about how to store
	 * the page id.
	 * @author gpothier
	 */
	public static class ObjectPageSlot extends PidSlot
	{
		private static final int PID_TYPE_BITS = 2; 
		private static final int PID_MASK = 0x3;
		
		private static final int PID_TYPE_SINGLES = 1;
		private static final int PID_TYPE_SHARED = 2;
		private static final int PID_TYPE_TREE = 3;
		
		public ObjectPageSlot(PageIOStream aStream)
		{
			super(aStream.getPage(), aStream.getPos());
		}
		
		@Override
		public Page getPage(boolean aCreateIfNull)
		{
			// This method should be called only if the store for the object is a delta BTree
			assert ! aCreateIfNull;
			int thePid = getPid() >>> PID_TYPE_BITS;
			return getFile().get(thePid);
		}
		
		@Override
		public void setPage(Page aPage)
		{
			// This method should be called only if the store for the object is a delta BTree
			setPid((aPage.getPageId() << PID_TYPE_BITS) + PID_TYPE_TREE);
		}

		public boolean isNull()
		{
			return getPid() == 0;
		}
		
		private int getPidType()
		{
			return getPid() & PID_MASK;
		}
		
		public boolean isSinglesPage()
		{
			return getPidType() == PID_TYPE_SINGLES;
		}
		
		public boolean isSharedPage()
		{
			return getPidType() == PID_TYPE_SHARED;
		}
		
		public boolean isBTree()
		{
			return getPidType() == PID_TYPE_TREE;
		}
		
		public SinglesPage getSinglesPage(OnDiskIndex aIndex)
		{
			int thePid = getPid() >>> PID_TYPE_BITS;
			Page thePage = getFile().get(thePid);
			return new SinglesPage(aIndex, thePage);
		}
		
		public void setSinglesPage(SinglesPage aPage)
		{
			setPid((aPage.getPageId() << PID_TYPE_BITS) + PID_TYPE_SINGLES);
		}
		
		public SharedPage getSharedPage(OnDiskIndex aIndex)
		{
			int thePid = getPid() >>> PID_TYPE_BITS;
			Page thePage = getFile().get(thePid);
			return new SharedPage(aIndex, thePage);
		}
		
		public void setSharedPage(SharedPage aPage)
		{
			setPid((aPage.getPageId() << PID_TYPE_BITS) + PID_TYPE_SHARED);
		}
		
		public DeltaBTree getBTree()
		{
			return new DeltaBTree("oas", getFile(), this);
		}
		
		public DeltaBTree toBTree()
		{
			Page theRoot = getFile().create();
			setPid((theRoot.getPageId() << PID_TYPE_BITS) + PID_TYPE_TREE);
			return new DeltaBTree("oas", getFile(), this);
		}
	}
	
	/**
	 * Stores entries for single objects, ie. objects that appear in a single block 
	 * (up to the moment they are first registered).
	 * If an object later happens to have more than a single access, it is moved to a larger store,
	 * but it is not removed from the singles page (for efficiency reasons).
	 * @author gpothier
	 */
	public static class SinglesPage
	{
		private final OnDiskIndex itsIndex;
		private final Page itsPage;
		
		private long itsLastObjectFieldId;
		private long itsLastBlockId;
		private int itsLastThreadId;
		
		private int itsEntriesCount = 0;
		
		private BitBuffer itsBuffer;
		private DecodedSinglesPage itsDecodedEntries;
		
		public SinglesPage(OnDiskIndex aIndex, BitBuffer aBuffer)
		{
			itsIndex = aIndex;
			itsPage = aIndex.getFile().create();
			
			itsBuffer = aBuffer;
			itsBuffer.erase();
			itsBuffer.position(0);
		}
		
		public SinglesPage(OnDiskIndex aIndex, Page aPage)
		{
			itsIndex = aIndex;
			itsPage = aPage;
			
			itsBuffer = BitBuffer.allocate((PagedFile.PAGE_SIZE-4)*8);
			itsEntriesCount = aPage.readInt(0);
			for(int i=4;i<PagedFile.PAGE_SIZE;i+=4)
				itsBuffer.put(itsPage.readInt(i), 32);

			itsBuffer.position(0);
			itsDecodedEntries = new DecodedSinglesPage(itsEntriesCount, itsBuffer);
		}
		
		public Entry getEntry(long aObjectFieldId)
		{
			return itsDecodedEntries.getEntry(aObjectFieldId);
		}

		
		public boolean isFull()
		{
			return itsBuffer.remaining() < 2*(64+64+32); // The maximum size of an entry (2* is because of gamma codes)
		}
		
		public void dump()
		{
			assert itsDecodedEntries == null; // decoded means read-only

			itsPage.writeInt(0, itsEntriesCount);
			itsBuffer.position(0);
			for(int i=4;i<PagedFile.PAGE_SIZE;i+=4)
				itsPage.writeInt(i, itsBuffer.getInt(32));
		}
		
		public void append(long aObjectFieldId, long aBlockId, int aThreadId)
		{
			assert itsDecodedEntries == null; // decoded means read-only
			
			itsBuffer.putGamma(aObjectFieldId-itsLastObjectFieldId);
			itsBuffer.putGamma(aBlockId-itsLastBlockId);
			itsBuffer.putGamma(aThreadId-itsLastThreadId);
			
			itsLastObjectFieldId = aObjectFieldId;
			itsLastBlockId = aBlockId;
			itsLastThreadId = aThreadId;
			
			itsEntriesCount++;
		}
		
		public int getPageId()
		{
			return itsPage.getPageId();
		}
	}
	
	public static class DecodedSinglesPage
	{
		private long[] itsObjectFieldIds;
		private long[] itsBlockIds;
		private int[] itsThreadIds;
		
		public DecodedSinglesPage(int aCount, BitBuffer aBuffer)
		{
			itsObjectFieldIds = new long[aCount];
			itsBlockIds = new long[aCount];
			itsThreadIds = new int[aCount];
			
			long theLastObjectFieldId = 0;
			long theLastBlockId = 0;
			int theLastThreadId = 0;

			for(int i=0;i<aCount;i++)
			{
				long theObjectFieldId = theLastObjectFieldId + aBuffer.getGammaLong();
				long theBlockId = theLastBlockId + aBuffer.getGammaLong();
				int theThreadId = theLastThreadId + aBuffer.getGammaInt();
				
				itsObjectFieldIds[i] = theObjectFieldId;
				itsBlockIds[i] = theBlockId;
				itsThreadIds[i] = theThreadId;
				
				theLastObjectFieldId = theObjectFieldId;
				theLastBlockId = theBlockId;
				theLastThreadId = theThreadId;
			}
		}
		
		public Entry getEntry(long aObjectFieldId)
		{
			for(int i=0;i<itsObjectFieldIds.length;i++)
			{
				if (itsObjectFieldIds[i] == aObjectFieldId)
					return new Entry(aObjectFieldId, itsBlockIds[i], itsThreadIds[i]);
			}
			return null;
		}
	}
	
	public static class Entry
	{
		public final long objectFieldId;
		public final long blockId;
		public final int threadId;
		
		public Entry(long aObjectFieldId, long aBlockId, int aThreadId)
		{
			objectFieldId = aObjectFieldId;
			blockId = aBlockId;
			threadId = aThreadId;
		}
	}
	
	public static class SharedPage
	{
		private final OnDiskIndex itsIndex;
		private final Page itsPage;
		
		/**
		 * An index into {@link OnDiskIndex#OBJECTS_PER_SHAREDPAGE}, that indicates 
		 * the maximum number of objects stored in this page.
		 */
		private final int itsLogMaxCount;
		
		/**
		 * Number of objects currently stored in this page 
		 */
		private UnsignedByteSlot itsCurrentCount;
		
		private BooleanSlot itsMarkedFree;
		
		private long itsLastObjectFieldId = 0;
		private int itsLastIndex = -1;
		
		public SharedPage(OnDiskIndex aIndex, Page aPage)
		{
			itsIndex = aIndex;
			itsPage = aPage;
			itsLogMaxCount = aPage.readByte(0);
			assert itsLogMaxCount >= 0 && itsLogMaxCount < N_LOG2_OBJECTSPERPAGE : ""+itsLogMaxCount;
			itsCurrentCount = new UnsignedByteSlot(aPage, 1);
			itsMarkedFree = new BooleanSlot(aPage, 2);
			if (LOG) Utils.println("Shared page (<init>) %d: %d/%d", getPageId(), getCurrentCount(), getMaxCount());
			check();
		}
		
		private void check()
		{
			if (! CHECK) return;
			assert itsPage.readByte(3) == 0;
		}
		
		private void checkCount()
		{
			if (! CHECK) return;
			int theCount = 0;
			for(int i=0;i<getMaxCount();i++)
			{
				long theId = getId(i);
				if (theId != 0) theCount++;
			}
			assert theCount == getCurrentCount(): theCount+", "+getCurrentCount();
		}
		
		public boolean isMarkedFree()
		{
			return itsMarkedFree.get();
		}
		
		public void markFree(boolean aFree)
		{
			itsMarkedFree.set(aFree);
		}
		
		public int getPageId()
		{
			return itsPage.getPageId();
		}
		
		public int getLogMaxCount()
		{
			return itsLogMaxCount;
		}
		
		/**
		 * Maximum number of ids in the page
		 */
		public int getMaxCount()
		{
			return getMaxCount(itsLogMaxCount);
		}
		
		private static int getMaxCount(int aLogMaxCount)
		{
			return OnDiskIndex.OBJECTS_PER_SHAREDPAGE[aLogMaxCount];
		}
		
		/**
		 * Current number of ids in the page.
		 */
		public int getCurrentCount()
		{
			return itsCurrentCount.get();
		}
		
		/**
		 * Whether this page has free id slots. 
		 */
		public boolean hasFreeIds()
		{
			return getCurrentCount() < getMaxCount();
		}
		
		public void setCurrentCount(int aCount)
		{
			itsCurrentCount.set(aCount);
			check();
		}
		
		private static int getIdsOffset()
		{
			return 4;
		}
		
		private static int getCountsOffset(int aLogMaxCount)
		{
			return getIdsOffset()+getMaxCount(aLogMaxCount)*8;
		}
		
		private int getValuesOffset()
		{
			return getCountsOffset(itsLogMaxCount)+getMaxCount();
		}
		
		private static int getValuesOffsetForLogMaxCount(int aLogMaxCount)
		{
			return getCountsOffset(aLogMaxCount)+getMaxCount(aLogMaxCount);
		}
		
		/**
		 * Size of a single value
		 */
		private static int getValueSize()
		{
			return 12;
		}
		
		/**
		 * Maximum number of values for each id
		 */
		private static int getMaxValuesCount(int aLogMaxCount)
		{
			return ((PagedFile.PAGE_SIZE-getValuesOffsetForLogMaxCount(aLogMaxCount))/getMaxCount(aLogMaxCount))/getValueSize();
		}
		
		private int getValuesOffset(int aIndex)
		{
			return getValuesOffset()+aIndex*getValueSize()*getMaxValuesCount(itsLogMaxCount);
		}
		
		private long getId(int aIndex)
		{
			return itsPage.readLong(getIdsOffset()+8*aIndex);
		}
		
		private void setId(int aIndex, long aId)
		{
			if (aId == 0 && aIndex == itsLastIndex)
			{
				assert getId(aIndex) == itsLastObjectFieldId;
				itsLastObjectFieldId = 0;
				itsLastIndex = -1;
			}
			itsPage.writeLong(getIdsOffset()+8*aIndex, aId);
			check();
			checkCount();
		}
		
		/**
		 * Number of values corresponding to the id at the given index.
		 */
		private int getValuesCount(int aIndex)
		{
			return itsPage.readByte(getCountsOffset(itsLogMaxCount)+aIndex) & 0xff;
		}
		
		private void setValuesCount(int aIndex, int aCount)
		{
			itsPage.writeByte(getCountsOffset(itsLogMaxCount)+aIndex, aCount);
			check();
		}
		
		/**
		 * Returns the index of the given id.
		 * If it is not found and there are still empty indexes,
		 * an empty index is used.
		 */
		public int indexOf(long aObjectFieldId)
		{
			assert aObjectFieldId != 0;
			if (aObjectFieldId == itsLastObjectFieldId) return itsLastIndex;
			
			itsLastObjectFieldId = aObjectFieldId;
			int theFreeIndex = -1;
			for(int i=0;i<getMaxCount();i++)
			{
				long theId = getId(i);
				if (theId == 0 && theFreeIndex == -1) theFreeIndex = i;
				else if (theId == aObjectFieldId) 
				{
					itsLastIndex = i;
					return i;
				}
			}
			if (theFreeIndex >= 0)
			{
				setCurrentCount(getCurrentCount()+1);
				setId(theFreeIndex, aObjectFieldId);
				itsLastIndex = theFreeIndex;
				return theFreeIndex;
			}
			assert ! hasFreeIds();
			itsLastIndex = -1;
			return -1;
		}
		
		public Object addTuple(ObjectPageSlot aSlot, long aObjectFieldId, long aBlockId, int aThreadId)
		{
			int theIndex = indexOf(aObjectFieldId);
			if (theIndex == -1) throw new RuntimeException("Shared page has no free indexes");
			assert getCurrentCount() > 0;
		
			int theCount = getValuesCount(theIndex);
			if (theCount == getMaxValuesCount(itsLogMaxCount)) return dumpToLargerStore(aSlot, theIndex, aObjectFieldId, aBlockId, aThreadId);
			else
			{
				int theOffset = getValuesOffset(theIndex) + theCount*getValueSize();
				itsPage.writeLI(theOffset, aBlockId, aThreadId);
				setValuesCount(theIndex, theCount+1);
				check();
				return null;
			}
		}
		
		private Object dumpToLargerStore(ObjectPageSlot aSlot, int aIndex, long aObjectFieldId, long aBlockId, int aThreadId)
		{
			if (itsLogMaxCount < OBJECTS_PER_SHAREDPAGE.length-1)
			{
				// Dump to a larger shared page
				SharedPage theSharedPage = itsIndex.getSharedPage(itsLogMaxCount+1);
				int theValuesCount = getValuesCount(aIndex);
				int theValuesOffset = getValuesOffset(aIndex);
				for(int i=0;i<theValuesCount;i++)
				{
					long theBlockId = itsPage.readLong(theValuesOffset);
					theValuesOffset += 8;
					int theThreadId = itsPage.readInt(theValuesOffset);
					theValuesOffset += 4;
					Object theDump = theSharedPage.addTuple(null, aObjectFieldId, theBlockId, theThreadId);
					assert theDump == null;
				}
				Object theDump = theSharedPage.addTuple(null, aObjectFieldId, aBlockId, aThreadId);
				assert theDump == null;
				setValuesCount(aIndex, 0);
				setCurrentCount(getCurrentCount()-1);
				setId(aIndex, 0);
				aSlot.setSharedPage(theSharedPage);
				check();

				if (! theSharedPage.hasFreeIds()) itsIndex.unfreeSharedPage(theSharedPage);
				itsIndex.freeSharedPage(this);
				
				if (LOG) Utils.println("Moved object %d from p.%d [%d/%d] to p.%d [%d/%d]", 
						aObjectFieldId, 
						getPageId(), 
						getCurrentCount(), getMaxCount(),
						theSharedPage.getPageId(),
						theSharedPage.getCurrentCount(), theSharedPage.getMaxCount());
				
				return theSharedPage;
			}
			else
			{
				assert getCurrentCount() > 0;
				DeltaBTree theBTree = aSlot.toBTree();
				int theValuesCount = getValuesCount(aIndex);
				int theValuesOffset = getValuesOffset(aIndex);
				for(int i=0;i<theValuesCount;i++)
				{
					long theBlockId = itsPage.readLong(theValuesOffset);
					theValuesOffset += 8;
					int theThreadId = itsPage.readInt(theValuesOffset);
					theValuesOffset += 4;
					theBTree.insertLeafTuple(theBlockId, theThreadId);
				}
				theBTree.insertLeafTuple(aBlockId, aThreadId);

				setValuesCount(aIndex, 0);
				setCurrentCount(getCurrentCount()-1);
				setId(aIndex, 0);
				check();

				itsIndex.freeSharedPage(this);

				if (LOG) Utils.println("Moved object %d from page %d to btree", aObjectFieldId, getPageId());

				return theBTree;
			}
		}
		
		@Override
		public String toString()
		{
			return String.format(
					"ids off.: %d, counts off.: %d, values off.: %d, values count: %d, max count: %d", 
					getIdsOffset(),
					getCountsOffset(itsLogMaxCount),
					getValuesOffset(),
					getMaxValuesCount(itsLogMaxCount),
					getMaxCount());
		}
	}
	
	private static long[] insert(long aValue, long[] aArray, int aOffset, int aCount)
	{
		long[] theResult = new long[aCount+1];
		theResult[0] = aValue;
		System.arraycopy(aArray, aOffset, theResult, 1, aCount);
		return theResult;
	}

	private static int[] insert(int aValue, int[] aArray, int aOffset, int aCount)
	{
		int[] theResult = new int[aCount+1];
		theResult[0] = aValue;
		System.arraycopy(aArray, aOffset, theResult, 1, aCount);
		return theResult;
	}
	
	
	public class ObjectAccessStore 
	{
		/**
		 * Id of the object/field whose accesses are stored here.
		 * Negative value means read-only.
		 */
		private long itsObjectFieldId;
		
		private ObjectPageSlot itsSlot;
		
		private SinglesPage itsSinglesPage;
		private SharedPage itsSharedPage;
		private DeltaBTree itsDeltaBTree;

		public ObjectAccessStore(long aObjectFieldId, ObjectPageSlot aSlot)
		{
			itsObjectFieldId = aObjectFieldId;
			itsSlot = aSlot;
			
			if (itsSlot.isNull())
			{
				assert itsObjectFieldId > 0;
				// Initialize later, but must keep the check
			}
			else if (itsSlot.isSinglesPage())
			{
				if (Stats.COLLECT) Stats.NO_LONGER_SINGLES++;
				itsSinglesPage = itsSlot.getSinglesPage(OnDiskIndex.this);
			}
			else if (itsSlot.isSharedPage())
			{
				// Shared page
				itsSharedPage = itsSlot.getSharedPage(OnDiskIndex.this);
				if (LOG) Utils.println("Object %d on p.%d [%d/%d]", aObjectFieldId, itsSharedPage.getPageId(), itsSharedPage.getCurrentCount(), itsSharedPage.getMaxCount());
				assert itsSharedPage.indexOf(aObjectFieldId) != -1;
			}
			else
			{
				// DeltaBTree
				itsDeltaBTree = itsSlot.getBTree();
				if (LOG) Utils.println("Object %d on btree", aObjectFieldId);
			}
		}

		public long getObjectFieldId()
		{
			return itsObjectFieldId;
		}
		
		private void init(int aInitialTupleCount)
		{
			// Check if we can use a shared page
			for(int i=0;i<N_LOG2_OBJECTSPERPAGE;i++)
			{
				if (aInitialTupleCount < SharedPage.getMaxValuesCount(i)*8/10)
				{
					itsSharedPage = getSharedPage(i);
					itsSlot.setSharedPage(itsSharedPage);
					assert itsSharedPage.hasFreeIds();
					itsDeltaBTree = null;
					if (LOG) Utils.println("Object %d on p.%d [%d/%d] (initial)", itsObjectFieldId, itsSharedPage.getPageId(), itsSharedPage.getCurrentCount(), itsSharedPage.getMaxCount());
					return;
				}
			}
			itsSharedPage = null;
			itsDeltaBTree = itsSlot.toBTree();
		}
		
		
		public void append(long[] aBlockIds, int[] aThreadIds, int aOffset, int aCount)
		{
			if (itsSinglesPage != null)
			{
				Entry theEntry = itsSinglesPage.getEntry(itsObjectFieldId);
				assert theEntry.objectFieldId == itsObjectFieldId;
				aBlockIds = insert(theEntry.blockId, aBlockIds, aOffset, aCount);
				aThreadIds = insert(theEntry.threadId, aThreadIds, aOffset, aCount);
				aCount++;
				aOffset = 0;
			}
			
			if (itsSharedPage == null && itsDeltaBTree == null) init(aCount);
			
			assert itsObjectFieldId > 0;
			while(aCount > 0)
			{
				if (itsSharedPage != null)
				{
					Object theDump = itsSharedPage.addTuple(itsSlot, itsObjectFieldId, aBlockIds[aOffset], aThreadIds[aOffset]);
					if (! itsSharedPage.hasFreeIds()) unfreeSharedPage(itsSharedPage);
	
					if (theDump instanceof DeltaBTree)
					{
						itsDeltaBTree = (DeltaBTree) theDump;
						itsSharedPage = null;
					}
					else if (theDump instanceof SharedPage) 
					{
						itsSharedPage = (SharedPage) theDump;
						itsDeltaBTree = null;
					}
					else assert theDump == null;
					
					aOffset++;
					aCount--;
				}
				else
				{
					itsDeltaBTree.insertLeafTuples(aBlockIds, aThreadIds, aOffset, aCount);
					break;
				}
			}				
		}
		
		public int[] getThreadIds(long aBlockId)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * Returns the ids of threads that accessed the object in the block that
		 * preceedes the given block.
		 */
		public int[] getPrevThreadIds(long aBlockId)
		{
			throw new UnsupportedOperationException();
		}
		
		private void flush()
		{
			if (itsDeltaBTree != null) itsDeltaBTree.flush();
		}
	}
	
	private class ObjectAccessStoreCache extends MRUBuffer<Long, ObjectAccessStore>
	{
		public ObjectAccessStoreCache()
		{
			super(64);
		}
		
		@Override
		protected Long getKey(ObjectAccessStore aValue)
		{
			return aValue.getObjectFieldId();
		}

		@Override
		protected ObjectAccessStore fetch(Long aId)
		{
			long theId = aId;
			return createStore(theId);
		}
		
		@Override
		protected void dropped(ObjectAccessStore aValue)
		{
			if (aValue.getObjectFieldId() > 0) aValue.flush();
		}
	}

}
