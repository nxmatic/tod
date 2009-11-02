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
package tod.impl.evdbng.db.file.classic;

import static tod.impl.evdbng.DebuggerGridConfigNG.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.file.classic.ClassicPagedFile.FilePage;
import zz.utils.Utils;
import zz.utils.list.NakedLinkedList;
import zz.utils.list.NakedLinkedList.Entry;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;
import zz.utils.primitive.FixedIntStack;

/**
 * Manages the shared buffer of {@link ClassicPagedFile}s.
 * @author gpothier
 */
public class BufferManager
{
	private static BufferManager INSTANCE = new BufferManager();

	public static BufferManager getInstance()
	{
		return INSTANCE;
	}

	private final ReentrantLock itsLock = new ReentrantLock();
	
	/**
	 * This buffer spans all the pages of this page manager.
	 */
	private final ByteBuffer itsBuffer;
	
	private final int itsPageSize;
	private final int itsBufferCount;

	/**
	 * The page attached to each buffer.
	 */
	private final FilePage[] itsAttachedPages;
	
	private long itsReadCount = 0;
	private long itsWriteCount = 0;
	private long itsCollisions = 0;
	
	private PageReplacementAlgorithm itsPageReplacementAlgorithm;

	private BufferManager()
	{
		itsPageSize = DB_PAGE_SIZE;
		itsBufferCount = (int) (DB_PAGE_BUFFER_SIZE/DB_PAGE_SIZE);
		
		int theSize = itsPageSize*itsBufferCount;
		try
		{
			Utils.println("Allocating %d buffers (%d bytes).", itsBufferCount, theSize);
			itsBuffer = ByteBuffer.allocateDirect(theSize);
		}
		catch (Throwable e)
		{
			throw new RuntimeException("Cannot allocate buffer of size: "+theSize, e);
		}
		itsBuffer.order(ByteOrder.nativeOrder());
		
		itsAttachedPages = new FilePage[itsBufferCount];
		
		itsPageReplacementAlgorithm = new LRUAlgorithm(itsBufferCount);
		
		Monitor.getInstance().register(this);
	}
	
	public ByteBuffer getBuffer()
	{
		return itsBuffer;
	}
	
	public int getPageSize()
	{
		return itsPageSize;
	}
	
	/**
	 * Returns a view of the main buffer with position and limit set to the beginning
	 * and end of the buffer for the specified id.
	 */
	private ByteBuffer getPageData(int aBufferId)
	{
		assert aBufferId >= 0 && aBufferId < itsBufferCount : String.format("bid: %d, count: %d", aBufferId, itsBufferCount);
		ByteBuffer thePageData = itsBuffer.duplicate();
		int theBufferPos = aBufferId * itsPageSize;
		assert theBufferPos >= 0 && theBufferPos < itsBuffer.limit(): String.format("bufferPos: %d, limit: %d", theBufferPos, itsBuffer.limit());
		thePageData.position(theBufferPos);
		thePageData.limit(theBufferPos+itsPageSize);
		return thePageData;
	}
	
	/**
	 * Returns the id of a free buffer.
	 * @param aLock This lock will be locked once the freed buffer is known.
	 * Note that many things can happen before the freed buffer is known, such as
	 * writing pages, etc. It is guaranteed that when this method exits normally,
	 * the lock is taken.
	 */
	private int getFreeBuffer(ReentrantLock aLock)
	{
		return itsPageReplacementAlgorithm.getFreeBuffer(aLock);
	}

	/**
	 * Frees the specified buffer, saves it to the file if dirty, and notifies the attached
	 * page.
	 * @return Whether the operation succeeded. The operation fails if the page or its 
	 * {@link ClassicPagedFile} is in use in another thread.
	 */
	private boolean freeBuffer(int aBufferId)
	{		
		try
		{
			itsLock.lock();

			FilePage thePage = itsAttachedPages[aBufferId];
			if (thePage == null) return true; // already free.

			// If the page is being used, don't free the buffer (yes, this happens)
			if (! thePage.tryLock()) return false;
			try
			{
				if (! thePage.getFile().tryLock()) return false;
				
				try
				{
					assert thePage.getBufferId() == aBufferId;
					
					if (thePage.isDirty())
					{
						assert thePage.getBufferId() >= 0 : "Page #"+thePage.getPageId()+" @"+aBufferId;
						thePage.getFile().write(thePage);
					}

					thePage.pagedOut();
					itsAttachedPages[aBufferId] = null;
					
					itsPageReplacementAlgorithm.bufferFreed(aBufferId);
					return true;
				}
				finally
				{
					thePage.getFile().unlock();
				}
			}
			finally
			{
				thePage.unlock();
			}
		}
		finally
		{
			itsLock.unlock();
		}
	}
	
	/**
	 * Creates a new page.
	 */
	public FilePage create(ClassicPagedFile aFile, int aPageId)
	{
		try
		{
			int theBufferId = getFreeBuffer(itsLock);
			ByteBuffer thePageData = getPageData(theBufferId);
			
			// Clear the page
			LongBuffer theLongBuffer = thePageData.asLongBuffer();
			for (int i=0;i<itsPageSize/8;i++) theLongBuffer.put(0);
			
			FilePage thePage = aFile.new FilePage(theBufferId, aPageId);
			assert itsAttachedPages[theBufferId] == null;
			itsAttachedPages[theBufferId] = thePage;
			
			return thePage;
		}
		finally
		{
			itsLock.unlock(); // Lock taken by getFreeBuffer
		}
	}

	/**
	 * Flushes all dirty buffers to disk
	 */
	public void flush()
	{
		try
		{
			itsLock.lock();

			for (int i=0;i<itsBufferCount;i++)
			{
				if (itsAttachedPages[i] != null) freeBuffer(i);
			}
		}
		finally
		{
			itsLock.unlock();
		}
	}
	
	/**
	 * Flushed all the buffers that pertain to the given file.
	 */
	public void flush(ClassicPagedFile aFile)
	{
		try
		{
			itsLock.lock();

			for (int i=0;i<itsBufferCount;i++)
			{
				FilePage thePage = itsAttachedPages[i];
				if (thePage != null && thePage.getFile() == aFile) freeBuffer(i);
			}
		}
		finally
		{
			itsLock.unlock();
		}
	}
	
	/**
	 * Invalidates all the pages of the specified file.
	 */
	public void invalidatePages(ClassicPagedFile aFile)
	{
		try
		{
			itsLock.lock();

			for (FilePage thePage : itsAttachedPages) 
			{
				if (thePage != null && thePage.getFile() == aFile) thePage.invalidate();
			}
		}
		finally
		{
			itsLock.unlock();
		}

	}

	/**
	 * Reloads a page from the disk. It is assumed that no buffer already holds this page.
	 */
	void loadPage(FilePage aPage)
	{
		while (! aPage.getFile().tryLock())
		{
			Utils.sleep(1);
			itsCollisions++;
		}
			
		try
		{
			int theBufferId = getFreeBuffer(itsLock);
			aPage.getFile().read(aPage, theBufferId);
			
			assert itsAttachedPages[theBufferId] == null;
			itsAttachedPages[theBufferId] = aPage;
			aPage.pagedIn(theBufferId);
		}
		finally
		{
			aPage.getFile().unlock();
			itsLock.unlock();
		}
	}
	
	/**
	 * Registers an access of the given buffer.
	 */
	public void use(int aBufferId)
	{
		itsPageReplacementAlgorithm.use(aBufferId);
	}
	
	/**
	 * Indicates to the page manager that this page is not going to be used anymore.
	 * This is optional, not calling it has no adverse effects, and the effect of calling
	 * it is a potiential increase in efficiency.
	 */
	public void free(FilePage aPage)
	{
		int theBufferId = aPage.getBufferId();
		if (theBufferId != -1) itsPageReplacementAlgorithm.free(theBufferId);
	}
	
	private void printBuffer(
			String aLabel, 
			FilePage aPage, 
			int thePhysicalPageId,
			int aBufferId)
	{
		if (true) return;
		
		StringBuilder theBuilder = new StringBuilder(String.format(
				"%s [%s] pid: %d, ppid: %d, bid: %d - ",
				aLabel,
				aPage.getFile().getName(),
				aPage.getPageId(),
				thePhysicalPageId,
				aBufferId));
		
		ByteBuffer theBuffer = getPageData(aBufferId);

		int p = theBuffer.position();
		for (int i = 0; i < itsPageSize; i++)
		{
			if (i % 16 == 0) theBuilder.append("["+i+"] ");
			
			String theHexString = Integer.toHexString(theBuffer.get(p+i) & 0xff);
			if (theHexString.length() == 1) theBuilder.append('0');
			theBuilder.append(theHexString);
			theBuilder.append(' ');
		}

		System.out.println(theBuilder.toString());
	}
	
	/**
	 * Writes a particular page to the disk
	 * @param aPhysPageId Physical id of the page (location on disk), 
	 * might be different from the logical page id stored in the Page.
	 */
	public void write(FilePage aPage, int aPhysPageId)
	{
		try
		{
			itsLock.lock();

			ByteBuffer thePageData = getPageData(aPage.getBufferId());
			printBuffer("w", aPage, aPhysPageId, aPage.getBufferId());
			
			long thePagePos = ((long) aPhysPageId) * ((long) itsPageSize);
			int theRemaining = itsPageSize;
			try
			{
				while (theRemaining > 0)
				{
					int theWritten = aPage.getFile().getChannel().write(thePageData, thePagePos);
					theRemaining -= theWritten;
					thePagePos += theWritten;
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
			itsWriteCount++;
		}
		finally
		{
			itsLock.unlock();
		}
	}
	
	/**
	 * Reads a page to the disk
	 * @param aPhysPageId Id of the page on disk
	 */
	public void read(FilePage aPage, int aPhysPageId, int aBufferId)
	{
		try
		{
			itsLock.lock();

			ByteBuffer thePageData = getPageData(aBufferId);
			
			long thePagePos = ((long) aPhysPageId) * ((long) itsPageSize);
			assert thePagePos >= 0 : thePagePos;
			int theRemaining = itsPageSize;
			try
			{
				while (theRemaining > 0)
				{
					int theRead = aPage.getFile().getChannel().read(thePageData, thePagePos);
					assert theRead >= 0 : "theRead: "+theRead+", thePagePos: "+thePagePos+", aPageId: "+aPhysPageId;
					theRemaining -= theRead;
					thePagePos += theRead;
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			
			printBuffer("r", aPage, aPhysPageId, aBufferId);
			itsReadCount++;
		}
		finally
		{
			itsLock.unlock();
		}
	}

	@Probe(key = "buffer count", aggr = AggregationType.SUM)
	public long getBufferCount()
	{
		return itsBufferCount;
	}
	
	@Probe(key = "buffer space", aggr = AggregationType.SUM)
	public long getBufferSpace()
	{
		return itsBufferCount*itsPageSize;
	}
	
	@Probe(key = "collisions", aggr = AggregationType.SUM)
	public long getCollisions()
	{
		return itsCollisions;
	}
	
//	private static class ConcurrentBitSet
//	{
//		private final BitSet itsDelegate = new BitSet();
//		
//		private final ReentrantReadWriteLock itsLock = new ReentrantReadWriteLock();		
//		private final List<FixedIntStack> itsStacks = new ArrayList<FixedIntStack>();
//		private final ThreadLocal<FixedIntStack> itsLocalStacks = new ThreadLocal<FixedIntStack>()
//		{
//			@Override
//			protected FixedIntStack initialValue()
//			{
//				FixedIntStack theStack = new FixedIntStack(DebuggerGridConfigNG.DB_DIRTYPAGES_TMPSIZE);
//				synchronized (itsStacks)
//				{
//					itsStacks.add(theStack);
//				}
//				return theStack;
//			}
//		};
//
//		public void set(int aIndex)
//		{
//			FixedIntStack theStack = itsLocalStacks.get();
//			
//			try
//			{
//				itsLock.readLock().lock();
//				if (theStack.isEmpty() || theStack.peek() != aIndex) theStack.push(aIndex);
//			}
//			finally
//			{
//				itsLock.readLock().unlock();
//			}
//			
//			if (theStack.isFull()) commit(theStack);
//		}
//		
//		private void set0(int aIndex)
//		{
//			itsDelegate.set(aIndex);
//		}
//		
//		private void commit(FixedIntStack aStack)
//		{
//			try
//			{
//				itsLock.writeLock().lock();
//				while(! aStack.isEmpty()) set0(aStack.pop());
//			}
//			finally
//			{
//				itsLock.writeLock().unlock();
//			}
//		}
//		
//		private void commit()
//		{
//			for (FixedIntStack theStack : itsStacks) commit(theStack);
//		}
//		
//		/**
//		 * Clears the given bit, and returns whether it was set or not.
//		 */
//		public boolean getAndClear(int aIndex)
//		{
//			try
//			{
//				itsLock.writeLock().lock();
//				commit();
//				boolean theSet = itsDelegate.get(aIndex);
//				itsDelegate.clear(aIndex);
//				return theSet;
//			}
//			finally
//			{
//				itsLock.writeLock().unlock();
//			}
//		}
//	}
//	
	/**
	 * Abstract paging algorithm. Decides which buffers to page out when
	 * new buffers are requested.
	 * @author gpothier
	 */
	private abstract class PageReplacementAlgorithm
	{
		
		protected boolean freeBuffer(int aBufferId)
		{
			return BufferManager.this.freeBuffer(aBufferId);
		}

		/**
		 * Indicates that the specified buffer has been accessed. This method
		 * is called very often so it should execute fast.
		 */
		public abstract void use(int aBufferId);
		
		/**
		 * Indicates that the specified buffer will not be used in the near future.
		 */
		public abstract void free(int aBufferId);
		
		/**
		 * Returns a free buffer, paging out other buffers if necessary.
		 */
		public abstract int getFreeBuffer(ReentrantLock aLock);
		
		/**
		 * Called when a buffer has been freed so as to update the algorithm's state.
		 */
		public abstract void bufferFreed(int aBufferId);
		
		/**
		 * Clears all stored data.
		 */
		public abstract void clear();
	}

	/**
	 * From Wikipedia: http://en.wikipedia.org/wiki/Page_replacement_algorithm#Aging
	 * @author gpothier
	 */
	private class AgingAlgorithm extends PageReplacementAlgorithm
	{
		/**
		 * Number of page accesses between each "clock cycle" (aging algorithm).
		 */
		private static final int ACCESSES_PER_CYCLE = 100000;
		
		/**
		 * Ids of free pages
		 */
		private final FixedIntStack itsFreeBuffersIds;
		
		/**
		 * Buffers accessed during the last "clock cycle" are marked with a bit
		 * in this set (aging algorithm). 
		 */
		private final BitSet itsAccessedBuffers = new BitSet();
		
		/**
		 * There is a counter for each buffer. At the end of each "clock cycle", the counters are
		 * updated:
		 * c = (c >>> 1) | (accessed ? 0x80 : 0)
		 * Thus the least frequently & recently used buffers have the lowest counter values
		 * (aging algorithm).
		 */
		private final byte[] itsCounters;
		
		/**
		 * Number of counters for each possible counter value. 
		 * At the end of each "clock cycle", when the counters are updated the counter bins
		 * are updated accordingly. This is used to speed up the collection of free
		 * pages.
		 */
		private final int[] itsCounterBins = new int[256];
		
		private final int itsBufferCount;

		public AgingAlgorithm(int aBufferCount)
		{
			itsBufferCount = aBufferCount;
			
			itsCounters = new byte[itsBufferCount];
			itsFreeBuffersIds = new FixedIntStack(itsBufferCount);
		}
		
		@Override
		public void clear()
		{
			// Mark all buffers free and clear counters
			itsFreeBuffersIds.clear();
			for (int i=0;i<itsBufferCount;i++) 
			{
				itsFreeBuffersIds.push(i);
				itsCounters[i] = 0;
			}
			
			// Reset counter bins
			for (int i=0;i<itsCounterBins.length;i++) itsCounterBins[i] = 0;
			
			itsAccessedBuffers.clear();
		}
		
		@Override
		public void use(int aBufferId)
		{
			itsAccessedBuffers.set(aBufferId);
		}
		
		@Override
		public void free(int aBufferId)
		{
			itsAccessedBuffers.clear(aBufferId);
			byte c = itsCounters[aBufferId];
			itsCounterBins[c & 0xff]--;
			itsCounters[aBufferId] = 0;
			itsCounterBins[0]++;
		}
		
		@Override
		public int getFreeBuffer(ReentrantLock aLock)
		{
			itsLock.lock(); // TODO: this is safe for semantics but might deadlock
			if (itsFreeBuffersIds.isEmpty())
			{
				freeNBuffers((itsBufferCount/20) + 1);
			}
			return itsFreeBuffersIds.pop();
		}
		
		@Override
		public void bufferFreed(int aBufferId)
		{
			itsAccessedBuffers.clear(aBufferId);
			byte c = itsCounters[aBufferId];
			itsCounterBins[c & 0xff]--;
			itsCounters[aBufferId] = 0;
			itsCounterBins[0]++;
			
			itsFreeBuffersIds.push(aBufferId);
		}

		private synchronized void updateUsage()
		{
//			if (itsRemainingAccesses > 0) return; // In case of concurrent invocation, don't reexecute
//			itsRemainingAccesses = ACCESSES_PER_CYCLE;
		
			long t0 = System.currentTimeMillis();
			for(int i=0;i<itsCounterBins.length;i++) itsCounterBins[i] = 0;
			
			for(int i=0;i<itsBufferCount;i++)
			{
				byte c = itsCounters[i];
				c = (byte) ((c & 0xff) >>> 1);
				boolean theAccessed = itsAccessedBuffers.get(i);
				if (theAccessed) c |= 0x80;
				itsCounters[i] = c;
			
				itsCounterBins[c & 0xff]++;
			}
			
			itsAccessedBuffers.clear();
			
			long t1 = System.currentTimeMillis();
			
			
			long t = t1-t0;
			
//			System.out.println("updateUsage executed in "+t+"ms.");
		}
		
		/**
		 * Frees at least N of the least used buffers.
		 * @param aCount The minumum number of buffers to free
		 */
		private void freeNBuffers(int aCount)
		{
			updateUsage();
			
			int theSum = 0;
			int theTreshold = 0;
			for (int i=0;i<256;i++)
			{
				theTreshold = i;
				theSum += itsCounterBins[i];
				if (theSum >= aCount) break;
			}
			
			int theFreed = 0;
			for (int i=0;i<itsBufferCount;i++) 
			{
				byte c = itsCounters[i];
				if (c <= theTreshold) 
				{
					if (freeBuffer(i)) theFreed++;
				}
				if (theFreed >= aCount) break;
			}
		}
	}
	
	private class LRUAlgorithm extends PageReplacementAlgorithm
	{
		/**
		 * Most recently used items go to the tail of the list.
		 */
		private final NakedLinkedList<Integer> itsLRUList = new NakedLinkedList<Integer>();

		private final Entry<Integer>[] itsEntries;
		
		private final ReentrantReadWriteLock itsLock = new ReentrantReadWriteLock();
		private final List<LRUStacks> itsStacks = new ArrayList<LRUStacks>();
		private final ThreadLocal<LRUStacks> itsLocalStacks = new ThreadLocal<LRUStacks>()
		{
			@Override
			protected LRUStacks initialValue()
			{
				LRUStacks theStacks = new LRUStacks();
				try
				{
					itsLock.writeLock().lock();
					itsStacks.add(theStacks);
				}
				finally
				{
					itsLock.writeLock().unlock();
				}
				
				return theStacks;
			}
		};
		
		public LRUAlgorithm(int aBufferCount)
		{
			itsEntries = new Entry[aBufferCount];
			for (int i=0;i<itsEntries.length;i++) 
			{
				Entry<Integer> theEntry = itsLRUList.createEntry(i);
				assert theEntry.getValue() != null;
				itsEntries[i] = theEntry;
				itsLRUList.addFirst(theEntry);
			}
		}

		@Override
		public void clear()
		{
			// It is not necessary to reset the lru list
		}

		@Override
		public void use(int aBufferId)
		{
			LRUStacks theStacks = itsLocalStacks.get();
			
			try
			{
				itsLock.readLock().lock();
				if (theStacks.useStack.isEmpty() || theStacks.useStack.peek() != aBufferId)
					theStacks.useStack.push(aBufferId);
			}
			finally
			{
				itsLock.readLock().unlock();
			}
			
			if (theStacks.useStack.isFull()) commit(theStacks);
		}
		
		private void use0(int aBufferId)
		{
			Entry<Integer> theEntry = itsEntries[aBufferId];
			
			assert theEntry.isAttached();
			itsLRUList.moveLast(theEntry);
		}
		
		@Override
		public void free(int aBufferId)
		{
			LRUStacks theStacks = itsLocalStacks.get();
			
			try
			{
				itsLock.readLock().lock();
				if (theStacks.freeStack.isEmpty() || theStacks.freeStack.peek() != aBufferId)
					theStacks.freeStack.push(aBufferId);
			}
			finally
			{
				itsLock.readLock().unlock();
			}
			
			if (theStacks.freeStack.isFull()) commit(theStacks);
		}
		
		private void free0(int aBufferId)
		{
			Entry<Integer> theEntry = itsEntries[aBufferId];
			
			assert theEntry.isAttached();
			itsLRUList.moveFirst(theEntry);
		}
		
		private void commit(LRUStacks aStacks)
		{
			try
			{
				itsLock.writeLock().lock();
				while(! aStacks.freeStack.isEmpty()) free0(aStacks.freeStack.pop());
				while(! aStacks.useStack.isEmpty()) use0(aStacks.useStack.pop());
			}
			finally
			{
				itsLock.writeLock().unlock();
			}
		}
		
		private void commit()
		{
			for (LRUStacks theStacks : itsStacks) commit(theStacks);
		}
		
		@Override
		public void bufferFreed(int aBufferId)
		{
		}

		@Override
		public int getFreeBuffer(ReentrantLock aLock)
		{
			while(true)
			{
				try
				{
					itsLock.writeLock().lock();
					commit();
				
					Entry<Integer> theEntry = itsLRUList.getFirstEntry();
				
					for(int i=0;i<16;i++) 
					{
						int theBufferId = theEntry.getValue();
						if (freeBuffer(theBufferId))
						{
							assert theEntry.isAttached();
							aLock.lock();
							itsLRUList.moveLast(theEntry);
							
							return theBufferId;
						}

						itsCollisions++;
						theEntry = itsLRUList.getNextEntry(theEntry);
					}
				}
				finally
				{
					itsLock.writeLock().unlock();
				}
				
				Utils.sleep(1);
			}
		}
	}


	private static class LRUStacks
	{
		public final FixedIntStack useStack = new FixedIntStack(DebuggerGridConfigNG.DB_TASK_SIZE);
		public final FixedIntStack freeStack = new FixedIntStack(DebuggerGridConfigNG.DB_TASK_SIZE);
	}
}
