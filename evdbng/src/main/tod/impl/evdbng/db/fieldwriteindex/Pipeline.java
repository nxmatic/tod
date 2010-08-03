package tod.impl.evdbng.db.fieldwriteindex;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tod.gui.kit.AbstractNavButton;
import tod.impl.evdbng.db.file.Sorter;

import gnu.trove.TLongHashSet;

public class Pipeline
{
	private static final int KB = 1024;
	private static final int MB = 1024*KB;
	
	private static final int NTHREADS = 8;
	private static final int QUEUE_SIZE = 128;
	private static final int KEEPALIVE_TIME_MS = 10000;

	private static final int COMPACTED_BLOCKS_THRESHOLD = 4*MB;
	private static final int COMPACTED_BLOCKS_TOPICK = COMPACTED_BLOCKS_THRESHOLD*75/100;
	
	private static final boolean COMPACT_SORTED_BLOCKS = true;
	
	private static ThreadPoolExecutor createThreadPoolExecutor()
	{
		return new ThreadPoolExecutor(
				NTHREADS, 
				NTHREADS, 
				KEEPALIVE_TIME_MS, 
				TimeUnit.MILLISECONDS, 
				new ArrayBlockingQueue<Runnable>(QUEUE_SIZE), 
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
	
	private final ThreadPoolExecutor itsPool = createThreadPoolExecutor();
	
	
	private final Object itsSortedBlocksMonitor = new Object();
	
	/**
	 * Stores compacted id blocks. The blocks can be compressed.
	 */
	private ArrayList<AbstractBlockData> itsSortedBlocks = new ArrayList<AbstractBlockData>();
	
	private int itsSortedBlocksSize = 0;
	
	private void postBlockSort(RawBlockData aData)
	{
		itsPool.execute(new SortBlockTask(aData));
	}
	
	private void postCompactBlock(RawBlockData aData)
	{
		itsPool.execute(new CompactBlockTask(aData));
	}
	
	private void postInvertBlocks(ArrayList<AbstractBlockData> aBlocks)
	{
		itsPool.execute(new InvertBlocksTask(aBlocks));
	}
	
	private void addSortedBlock(AbstractBlockData aData)
	{
		synchronized (itsSortedBlocksMonitor)
		{
			itsSortedBlocks.add(aData);
			itsSortedBlocksSize += aData.getSize();

			// When we reach the threshold, sort the blocks and pick the first X% for inversion.
			if (itsSortedBlocksSize > COMPACTED_BLOCKS_THRESHOLD)
			{
				Collections.sort(itsSortedBlocks);
				
				ArrayList<AbstractBlockData> thePicked = new ArrayList<AbstractBlockData>(itsSortedBlocks.size());
				ArrayList<AbstractBlockData> theRemaining = new ArrayList<AbstractBlockData>(itsSortedBlocks.size());
				
				int thePickedSize = 0;
				for(int i=0;i<itsSortedBlocks.size();i++)
				{
					AbstractBlockData theData = itsSortedBlocks.get(i);
					
					if (thePickedSize < COMPACTED_BLOCKS_TOPICK) 
					{
						thePicked.add(theData);
						thePickedSize += theData.getSize();
					}
					else theRemaining.add(theData);
					
				}
				itsSortedBlocksSize -= thePickedSize;
				
				itsSortedBlocks = theRemaining;
				postInvertBlocks(thePicked);
			}
		}
	}
	
	private static byte[] compressSortedIds(long[] aIds)
	{
	}
	
	private static long[] decompressSortedIds(byte[] aData)
	{
	}
	
	public class PerThreadIndex
	{
		private final int itsThreadId;
		private TLongHashSet itsSet = new TLongHashSet();
		private long itsCurrentBlockId;
		
		public PerThreadIndex(int aThreadId)
		{
			itsThreadId = aThreadId;
		}

		public int getThreadId()
		{
			return itsThreadId;
		}
		
		public void registerAccess(long aObjectId)
		{
			itsSet.add(aObjectId);
		}
		
		public void startBlock(long aBlockId)
		{
			long[] theValues = itsSet.toArray();
			itsSet.clear();
			
			postBlockSort(new RawBlockData(itsThreadId, itsCurrentBlockId, theValues));
			
			itsCurrentBlockId = aBlockId;
		}
	}

	/**
	 * Represents the sorted or unsorted data of a block (ie. the ids of objects accessed in the block).
	 * The subclasses decide if the data is stored directly or in compressed form
	 * @author gpothier
	 */
	private abstract class AbstractBlockData implements Comparable<AbstractBlockData>
	{
		private final int itsThreadId;
		private final long itsBlockId;
		
		public AbstractBlockData(int aThreadId, long aBlockId)
		{
			itsThreadId = aThreadId;
			itsBlockId = aBlockId;
		}
		
		public int getThreadId()
		{
			return itsThreadId;
		}
		
		public long getBlockId()
		{
			return itsBlockId;
		}
		
		public abstract long[] getObjectIds();
		
		/**
		 * Returns the size (in bytes) of this block data.
		 */
		public int getSize()
		{
			return 4+4+8; // Object header, thread id, block id
		}
		
		/**
		 * Returns the number of ids in the block.
		 */
		public abstract int getCount();

		public int compareTo(AbstractBlockData aOther)
		{
			if (itsBlockId == aOther.itsBlockId) return itsThreadId-aOther.itsThreadId;
			else if (itsBlockId > aOther.itsBlockId) return 1;
			else return -1;
		}
	}
	
	/**
	 * Non-compressed implementation of {@link AbstractBlockData}
	 * @author gpothier
	 */
	private class RawBlockData extends AbstractBlockData
	{
		private final long[] itsObjectIds;
		
		public RawBlockData(int aThreadId, long aBlockId, long[] aObjectIds)
		{
			super(aThreadId, aBlockId);
			itsObjectIds = aObjectIds;
		}
		
		@Override
		public long[] getObjectIds()
		{
			return itsObjectIds;
		}
		
		@Override
		public int getSize()
		{
			return super.getSize()+8*itsObjectIds.length;
		}
		
		@Override
		public int getCount()
		{
			return itsObjectIds.length;
		}
	}
	
	private class CompressedBlockData extends AbstractBlockData
	{
		private final int itsCount;
		private final byte[] itsData;
		
		public CompressedBlockData(int aThreadId, long aBlockId, long[] aObjectIds) 
		{
			super(aThreadId, aBlockId);
			itsCount = aObjectIds.length;
			itsData = compressSortedIds(aObjectIds);
		}

		@Override
		public long[] getObjectIds()
		{
			return decompressSortedIds(itsData);
		}
		
		@Override
		public int getSize()
		{
			return super.getSize()+itsData.length;
		}
		
		@Override
		public int getCount()
		{
			return itsCount;
		}
	}
	
	/**
	 * Sorts the ids of a {@link BlockData}, and passes it to the next pipeline step
	 * @author gpothier
	 */
	private class SortBlockTask implements Runnable
	{
		private final RawBlockData itsData;
		
		public SortBlockTask(RawBlockData aData)
		{
			itsData = aData;
		}

		public void run()
		{
			Arrays.sort(itsData.getObjectIds());
			
			if (COMPACT_SORTED_BLOCKS) postCompactBlock(itsData);
			else addSortedBlock(itsData);
		}
	}
	
	/**
	 * Compacts a block of sorted ids using some form of delta or gamma encoding
	 * @author gpothier
	 */
	private class CompactBlockTask implements Runnable
	{
		private final RawBlockData itsData;

		public CompactBlockTask(RawBlockData aData)
		{
			itsData = aData;
		}
		
		public void run()
		{
			addSortedBlock(new CompressedBlockData(itsData.getThreadId(), itsData.getBlockId(), itsData.getObjectIds()));
		}
	}
	
	private class InvertBlocksTask extends Sorter implements Runnable
	{
		private final ArrayList<AbstractBlockData> itsBlocks;
		
		private long[] itsObjectIds;
		private long[] itsBlockIds;
		private int[] itsThreadIds;

		public InvertBlocksTask(ArrayList<AbstractBlockData> aBlocks)
		{
			itsBlocks = aBlocks;
		}
		
		public void run()
		{
			// Count total number of entries
			int theCount = 0;
			for(int i=0;i<itsBlocks.size();i++)
			{
				AbstractBlockData theData = itsBlocks.get(i);
				theCount += theData.getCount();
			}
			
			itsObjectIds = new long[theCount];
			itsBlockIds = new long[theCount];
			itsThreadIds = new int[theCount];
			
			// Fill the arrays
			theCount = 0;
			for(int i=0;i<itsBlocks.size();i++)
			{
				AbstractBlockData theData = itsBlocks.get(i);
				long[] theBlockObjectIds = theData.getObjectIds();
				long theBlockId = theData.getBlockId();
				int theThreadId = theData.getThreadId();
				
				for(long theObjectId : theBlockObjectIds)
				{
					itsObjectIds[theCount] = theObjectId;
					itsBlockIds[theCount] = theBlockId;
					itsThreadIds[theCount] = theThreadId;
					theCount++;
				}
			}
			
			// Sort
			sort(itsObjectIds);
			
			// Compact
		}

		@Override
		protected void swap(int a, int b)
		{
			swap(itsBlockIds, a, b);
			swap(itsThreadIds, a, b);
		}
	}
}
