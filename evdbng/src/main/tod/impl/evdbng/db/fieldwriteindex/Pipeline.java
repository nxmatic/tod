package tod.impl.evdbng.db.fieldwriteindex;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.naming.OperationNotSupportedException;

import tod.gui.kit.AbstractNavButton;
import tod.impl.evdbng.Indexer.EventRef;
import tod.impl.evdbng.db.Stats;
import tod.impl.evdbng.db.fieldwriteindex.OnDiskIndex.ObjectAccessStore;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.Sorter;
import tod.impl.evdbng.db.file.Page.PidSlot;
import zz.utils.Utils;

import gnu.trove.TLongHashSet;

public class Pipeline
{
	private static final int KB = 1024;
	private static final int MB = 1024*KB;
	
	private static final int NTHREADS = 8;
	private static final int QUEUE_SIZE = 128;
	private static final int KEEPALIVE_TIME_MS = 10000;

	private static final int COMPACTED_BLOCKS_THRESHOLD = 32*MB;
	private static final int COMPACTED_BLOCKS_TOPICK = COMPACTED_BLOCKS_THRESHOLD*60/100;
	
	private static final boolean COMPACT_SORTED_BLOCKS = false;
	
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
	
	private int itsSubmittedJobs = 0;
	private final ThreadPoolExecutor itsPool = createThreadPoolExecutor();
	
	private final OnDiskIndex itsIndex;
	private final Object itsSortedBlocksMonitor = new Object();
	
	private final List<PerThreadIndex> itsPerThreadIndexes = new ArrayList<Pipeline.PerThreadIndex>();
	
	/**
	 * Stores compacted id blocks. The blocks can be compressed.
	 */
	private ArrayList<AbstractBlockData> itsSortedBlocks = new ArrayList<AbstractBlockData>();
	
	private int itsSortedBlocksSize = 0;
	
	public Pipeline(PidSlot aDirectoryPageSlot)
	{
		itsIndex = new OnDiskIndex(aDirectoryPageSlot);
	}
	
	public PerThreadIndex getIndex(int aThreadId)
	{
		PerThreadIndex theIndex = Utils.listGet(itsPerThreadIndexes, aThreadId);
		if (theIndex == null)
		{
			theIndex = new PerThreadIndex(aThreadId);
			Utils.listSet(itsPerThreadIndexes, aThreadId, theIndex);
		}
		return theIndex;
	}
	
	private synchronized void incSubmittedJobs()
	{
		itsSubmittedJobs++;
	}
	
	private synchronized void decSubmittedJobs()
	{
		itsSubmittedJobs--;
	}
	
	private synchronized void waitJobs()
	{
		try
		{
			while(itsSubmittedJobs > 0) wait(1000);
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void flush()
	{
		for(PerThreadIndex theIndex : itsPerThreadIndexes) if (theIndex != null) theIndex.flush();
		waitJobs();
		synchronized (itsSortedBlocksMonitor)
		{
			Collections.sort(itsSortedBlocks);
			postInvertBlocks(itsSortedBlocks);
		}
		waitJobs();
	}
	
	private void submit(final Runnable aTask)
	{
		incSubmittedJobs();
		itsPool.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					aTask.run();
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
				decSubmittedJobs();
			}
		});
	}
	
	private void postBlockSort(RawBlockData aData)
	{
		submit(new SortBlockTask(aData));
	}
	
	private void postCompactBlock(RawBlockData aData)
	{
		submit(new CompactBlockTask(aData));
	}
	
	private void postInvertBlocks(ArrayList<AbstractBlockData> aBlocks)
	{
		submit(new InvertBlocksTask(aBlocks));
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
		throw new UnsupportedOperationException();
	}
	
	private static long[] decompressSortedIds(byte[] aData)
	{
		throw new UnsupportedOperationException();
	}
	
	public ThreadIds inspect(long aSlotId, EventRef aReferenceEventRef)
	{
		ObjectAccessStore theStore = itsIndex.getStore(aSlotId, true);
		return theStore.getThreadIds(aReferenceEventRef.blockId);
	}
	
	/**
	 * Two arrays of thread ids that represent the result of a query.
	 * @author gpothier
	 */
	public static class ThreadIds
	{
		public final int[] sameBlockThreadIds;
		public final long prevBlockId;
		public final int[] prevBlockThreadIds;
		
		public ThreadIds(int[] aSameBlockThreadIds)
		{
			this(aSameBlockThreadIds, 0, null);
		}
		
		public ThreadIds(int[] aSameBlockThreadIds, long aPrevBlockId, int[] aPrevBlockThreadIds)
		{
			sameBlockThreadIds = aSameBlockThreadIds;
			prevBlockId = aPrevBlockId;
			prevBlockThreadIds = aPrevBlockThreadIds;
		}
	}
	
	public class PerThreadIndex
	{
		private final int itsThreadId;
		private TLongHashSet itsSet = new TLongHashSet();
		private long itsCurrentBlockId = 1;
		
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
			assert aObjectId >= 0;
			itsSet.add(aObjectId);
		}
		
		public void startBlock(long aBlockId)
		{
			post();
			itsCurrentBlockId = aBlockId;
		}
		
		public void flush()
		{
			post();
		}
		
		private synchronized void post()
		{
			long[] theValues = itsSet.toArray();
			
			if (theValues.length > 0)
			{
				itsSet.clear();
				postBlockSort(new RawBlockData(itsThreadId, itsCurrentBlockId, theValues));
			}
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
			assert aBlockId > 0;
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
	
	
	private class InvertBlocksTask extends Sorter.Sortable implements Runnable
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
			
			// We store the PIVOT at index 0, hence the +1
			itsObjectIds = new long[theCount+1];
			itsBlockIds = new long[theCount+1];
			itsThreadIds = new int[theCount+1];
			
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
					itsObjectIds[theCount+1] = theObjectId;
					itsBlockIds[theCount+1] = theBlockId;
					itsThreadIds[theCount+1] = theThreadId;
					theCount++;
				}
			}
			
			// Sort
			Sorter.sort(this, 0, itsObjectIds.length-1);
			
			Utils.println("Preparing to write %d entries to disk.", theCount);

			// Store
			synchronized(itsIndex)
			{
				Utils.println("Writing (%d entries).", theCount);
				ObjectAccessStore theStore = null;
				long theLastObjectId = itsObjectIds[1];
				int theOffset = 1;
				int theSubCount = 0;
				for(int i=1;i<=theCount+1;i++)
				{
					long theObjectId = i == theCount+1 ? 0 : itsObjectIds[i]; //  objId = 0 to force the last range to storage.
					if (theObjectId != theLastObjectId)
					{
						if (theSubCount == 1)
						{
							itsIndex.appendSingle(itsObjectIds[theOffset], itsBlockIds[theOffset], itsThreadIds[theOffset]);
						}
						else
						{
							theStore = itsIndex.getStore(theLastObjectId, false);
							theStore.append(itsBlockIds, itsThreadIds, theOffset, theSubCount);
						}
						
						if (Stats.COLLECT) Stats.sub(theSubCount);
						
						theOffset = i;
						theSubCount = 0;
						theLastObjectId = theObjectId;
					}
					theSubCount++;
				}
				Utils.println("Done (%d entries).", theCount);
			}
		}

		@Override
		protected void setPivot(int aIndex)
		{
			itsObjectIds[0] = itsObjectIds[aIndex+1];
			itsBlockIds[0] = itsBlockIds[aIndex+1];
			itsThreadIds[0] = itsThreadIds[aIndex+1];
		}

		@Override
		protected int compare(int a, int b)
		{
			long oa = itsObjectIds[a+1];
			long ob = itsObjectIds[b+1];
			
			if (oa > ob) return 1;
			else if (oa < ob) return -1;
			else
			{
				long ba = itsBlockIds[a+1];
				long bb = itsBlockIds[b+1];

				if (ba > bb) return 1;
				else if (ba < bb) return -1;
				else
				{
					int ta = itsThreadIds[a+1];
					int tb = itsThreadIds[b+1];
					return ta - tb;
				}
			}
		}

		@Override
		protected void swap(int a, int b)
		{
			assert a != PIVOT;
			assert b != PIVOT;
			Sorter.swap(itsObjectIds, a+1, b+1);
			Sorter.swap(itsBlockIds, a+1, b+1);
			Sorter.swap(itsThreadIds, a+1, b+1);
		}
	}
}
