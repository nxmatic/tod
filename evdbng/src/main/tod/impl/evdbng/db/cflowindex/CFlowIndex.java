package tod.impl.evdbng.db.cflowindex;

import tod.impl.evdbng.db.file.LongInsertableBTree;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.LongSlot;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.RangeMinMaxTree;

public class CFlowIndex
{
	private final PidSlot itsDirectorySlot;
	private final Directory itsDirectory;
	
	private RangeMinMaxTree itsTree;
	private LongInsertableBTree itsBlockToRankTree;
	private LongInsertableBTree itsRankToBlockTree;
	
	public CFlowIndex(PidSlot aDirectorySlot)
	{
		itsDirectorySlot = aDirectorySlot;
		itsDirectory = new Directory(itsDirectorySlot.getPage(true));
		
		itsTree = new RangeMinMaxTree(itsDirectory.getTreeSlot());
		itsBlockToRankTree = new LongInsertableBTree("block to rank", itsDirectory.getBlockToRankTreeSlot());
		itsRankToBlockTree = new LongInsertableBTree("rank to block", itsDirectory.getRankToBlockTreeSlot());
	}
	
	public void flush()
	{
		itsTree.flush();
	}
	
	private static class Directory
	{
		private Page itsPage;
		private PidSlot itsTreeSlot;
		private PidSlot itsBlockToRankTreeSlot;
		private PidSlot itsRankToBlockTreeSlot;
		private LongSlot itsCurrentRankSlot;

		public Directory(Page aPage)
		{
			itsPage = aPage;
			int theOffset = 0;
			itsTreeSlot = new PidSlot(itsPage, theOffset);
			theOffset += PidSlot.size();
			itsBlockToRankTreeSlot = new PidSlot(itsPage, theOffset);
			theOffset += PidSlot.size();
			itsRankToBlockTreeSlot = new PidSlot(itsPage, theOffset);
			theOffset += PidSlot.size();
			itsCurrentRankSlot = new LongSlot(aPage, theOffset);
			theOffset += LongSlot.size();
		}

		public PidSlot getTreeSlot()
		{
			return itsTreeSlot;
		}

		public PidSlot getBlockToRankTreeSlot()
		{
			return itsBlockToRankTreeSlot;
		}

		public PidSlot getRankToBlockTreeSlot()
		{
			return itsRankToBlockTreeSlot;
		}
		
		public LongSlot getCurrentRankSlot()
		{
			return itsCurrentRankSlot;
		}
	}
	
	public void enter()
	{
		itsTree.open();
	}
	
	public void exit()
	{
		itsTree.close();
	}
	
	public void sync(long aTimestamp)
	{
		long theCurrentRank = itsTree.size();
		if (theCurrentRank == itsDirectory.getCurrentRankSlot().get()) return;
		itsBlockToRankTree.add(aTimestamp, theCurrentRank);
		itsRankToBlockTree.add(theCurrentRank, aTimestamp);
		itsDirectory.getCurrentRankSlot().set(theCurrentRank);
	}
}
