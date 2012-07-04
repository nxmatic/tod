package tod.impl.evdbng.db.cflowindex;

import tod.impl.evdbng.db.Stats;
import tod.impl.evdbng.db.file.LongInsertableBTree;
import tod.impl.evdbng.db.file.LongInsertableBTree.LongTuple;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.LongSlot;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.RangeMinMaxTree;
import zz.utils.Utils;

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
		
		itsTree = new RangeMinMaxTree(Stats.ACC_CFLOW, itsDirectory.getTreeSlot());
		itsBlockToRankTree = new LongInsertableBTree("block to rank", Stats.ACC_CFLOW, itsDirectory.getBlockToRankTreeSlot());
		itsRankToBlockTree = new LongInsertableBTree("rank to block", Stats.ACC_CFLOW, itsDirectory.getRankToBlockTreeSlot());
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
			itsTreeSlot = new PidSlot(Stats.ACC_CFLOW, itsPage, theOffset);
			theOffset += PidSlot.size();
			itsBlockToRankTreeSlot = new PidSlot(Stats.ACC_CFLOW, itsPage, theOffset);
			theOffset += PidSlot.size();
			itsRankToBlockTreeSlot = new PidSlot(Stats.ACC_CFLOW, itsPage, theOffset);
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
	
	public void snapshot(long aBlockId)
	{
//		Utils.println("cflow snapshot: bid %d, sz %d", aBlockId, itsTree.size());
		long theCurrentRank = itsTree.size();
		if (theCurrentRank == itsDirectory.getCurrentRankSlot().get()) return;
		itsBlockToRankTree.add(aBlockId, theCurrentRank);
		itsRankToBlockTree.add(theCurrentRank, aBlockId);
		itsDirectory.getCurrentRankSlot().set(theCurrentRank);
	}
	
	public long size()
	{
		return itsTree.size();
	}
	
	public boolean isOpen(long aPosition)
	{
		return itsTree.isOpen(aPosition);
	}
	
	public boolean isClose(long aPosition)
	{
		return itsTree.isClose(aPosition);
	}
	
	public long getClose(long aPosition)
	{
		return itsTree.getClose(aPosition);
	}
	
	public long getOpen(long aPosition)
	{
		return itsTree.getOpen(aPosition);
	}
	
	public long getParent(long aPosition)
	{
		return itsTree.parent(aPosition);
	}
	
	public long getBlockStartPosition(long aBlockId)
	{
		assert aBlockId >= 0;
		return aBlockId > 0 ? itsBlockToRankTree.get(aBlockId) : 0;
	}

	/**
	 * Returns a tuple whose data is the id of the block that contains the given position.
	 * The key of the tuple is the position of the beginning of the block.
	 */
	public LongTuple getContainingBlock(long aPosition)
	{
		return itsRankToBlockTree.getTupleAt(aPosition, false);
	}
}
