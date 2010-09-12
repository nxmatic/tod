package tod.impl.evdbng.db.fieldwriteindex;

import java.io.File;

import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.IntSlot;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.PagedFile;

public class OnDiskIndex
{
	private static final int N_LOG2_OBJECTSPERPAGE = 10;
	
	private final PagedFile itsFile;
	
	private Directory itsDirectory;
	
	private PageStack itsEmptyPagesStack;
	private PageStack[] itsSharedPagesStack = new PageStack[N_LOG2_OBJECTSPERPAGE]; 
	
	public OnDiskIndex(PagedFile aFile, Page aRootPage)
	{
		itsFile = aFile;
		itsDirectory = new Directory(aRootPage);

		itsEmptyPagesStack = new PageStack(itsDirectory.getEmptyPagesStackSlot());
		for(int i=0;i<N_LOG2_OBJECTSPERPAGE;i++)
			itsSharedPagesStack[i] = new PageStack(itsDirectory.getIncompleteSharedPagesStackSlot(i));
	}
	
	private class Directory
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
				Page thePrevPage = itsPrevPageSlot.getPage(true);
				setCurrentPage(thePrevPage);
				theCount = itsCountSlot.get();
				assert theCount == ITEMS_PER_PAGE;
			}
			
			theCount--;
			itsCountSlot.set(theCount);
			return itsCurrentPage.readInt(POS_DATA + 4*theCount);
		}
	}
}
