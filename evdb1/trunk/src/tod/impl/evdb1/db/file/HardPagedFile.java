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
package tod.impl.evdb1.db.file;

import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_BUFFER_SIZE;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_POINTER_BITS;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_SIZE;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import tod.core.DebugFlags;
import zz.utils.ArrayStack;
import zz.utils.Stack;
import zz.utils.Utils;
import zz.utils.cache.SyncMRUBuffer;
import zz.utils.list.NakedLinkedList.Entry;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;

/**
 * A file organized in pages.
 * @author gpothier
 */
public class HardPagedFile extends PageBank
{
	private static PageDataManager itsPageDataManager = 
		new PageDataManager(DB_PAGE_SIZE, (int) (DB_PAGE_BUFFER_SIZE/DB_PAGE_SIZE));
	
	private final FileAccessor itsFileAccessor;
	private final int itsPageSize;
	
	/**
	 * Mapping of logical page ids to physical page offsets.
	 * TODO: Temporary. This should be stored on the disk.
	 */
	private int[] itsPhysicalPage = new int[10000000];
	
	/**
	 * Number of pages currently in the file.
	 */
	private long itsPagesCount;
	
	private String itsName;

	private int itsNum = num();

	public HardPagedFile(File aFile, int aPageSize) throws FileNotFoundException
	{
		assert aPageSize % 4 == 0;
		
		itsName = aFile.getName();
		Monitor.getInstance().register(this);
	
		itsPageSize = aPageSize;
		aFile.delete();
		itsFileAccessor = new FileAccessor(aFile);
		itsPagesCount = 0;
	}

	private static int n = 1;

	private static synchronized int num()
	{
		return n++;
	}
	
	public void dispose()
	{
		Monitor.getInstance().unregister(this);
		itsFileAccessor.dispose();
	}

	/**
	 * Page size, in bytes.
	 */
	public int getPageSize()
	{
		return itsPageSize;
	}
	
	/**
	 * Returns the size, in bits, of a page pointer
	 */
	public int getPagePointerSize()
	{
		return DB_PAGE_POINTER_BITS;
	}
	
	/**
	 * Returns the number of allocated pages.
	 */
	@Probe(key = "file page count", aggr = AggregationType.SUM)
	public long getPagesCount()
	{
		return itsPagesCount;
	}

	/**
	 * Returns the amount of storage, in bytes, occupied by this file.
	 */
	@Probe(key = "file storage", aggr = AggregationType.SUM)
	public long getStorageSpace()
	{
		return itsPagesCount * itsPageSize;
	}
	
	/**
	 * Returns the actual file size.
	 */
	@Probe(key = "file size", aggr = AggregationType.SUM)
	public long getFileSize()
	{
		try
		{
			return itsFileAccessor.itsFile.length();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Probe(key = "buffer count", aggr = AggregationType.MAX)
	public long getBufferCount()
	{
		return (int) (DB_PAGE_BUFFER_SIZE/DB_PAGE_SIZE);
	}
	
	@Probe(key = "buffer space", aggr = AggregationType.MAX)
	public long getBufferSpace()
	{
		return getBufferCount()*itsPageSize;
	}
	

	/**
	 * Returns a particular page of this file.
	 */
	public Page get(long aPageId)
	{
		PageKey theKey = new PageKey(this, aPageId);
		synchronized (itsPageDataManager)
		{
			PageData theData = itsPageDataManager.get(theKey, false);
			return theData != null ? theData.getAttachedPage() : new Page(theKey);
		}
	}
	
	@Override
	public void free(PageBank.Page aPage)
	{
		PageKey theKey = (PageKey) aPage.getKey();
		itsPageDataManager.drop(theKey);
	}
	
	/**
	 * Returns a page object suitable for overwriting the file page
	 * of the specified id. The data of the returned page is undefined.
	 * Moreover, the actual file contents can be considered as undefined
	 * once this method has been called, because of the way page caching works:
	 * it is not guaranteed that a client can retrieve the previous content of
	 * the page even is the page is not written out.
	 */
	public Page getPageForOverwrite(long aPageId)
	{
//		Reference<Page> thePageRef = itsPagesMap.get(aPageId);
//		Page thePage = thePageRef != null ? thePageRef.get() : null;
//		if (thePage == null)
//		{
//			int[] theBuffer = PageManager.getInstance().getFreeBuffer(itsPageSize, false);
//			thePage = new Page(theBuffer, aPageId);
//			itsPagesMap.put(aPageId, new WeakReference<Page>(thePage));
//		}
//		
//		return thePage;
		return get(aPageId);
	}
	
	/**
	 * Creates and returns a new page in this file.
	 */
	public Page create()
	{
		synchronized (itsPageDataManager)
		{
			itsPagesCount++;
			long thePageId = itsPagesCount-1;
			
			Entry<PageData> theData = itsPageDataManager.create(this, thePageId);
			Page thePage = new Page(theData);			
			return thePage; 
		}
	}
	
	private int[] loadPageData(long aId)
	{
//		System.out.println("Loading page: "+aId+" on "+itsName);
		if (aId >= itsPagesCount) throw new RuntimeException("Page does not exist: "+aId+" (page count: "+itsPagesCount+")");
		
		try
		{
			int[] theBuffer = itsPageDataManager.getFreeBuffer();
			
			if (! DebugFlags.DISABLE_STORE)
			{
				itsFileAccessor.read(aId, theBuffer);
			}
			
			return theBuffer;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void storePageData(long aId, int[] aData)
	{
//		System.out.println("Storing page: "+aId+" on "+itsName);
		try
		{
			if (! DebugFlags.DISABLE_STORE)
			{
				itsFileAccessor.write(aId, aData);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [" + itsName + "-" + itsNum + "]";
	}
	
	private class FileAccessor extends Thread
	{
		private boolean itsDisposed = false;
		private final RandomAccessFile itsFile;

		private ByteBuffer itsByteBuffer;
		private IntBuffer itsIntBuffer;
		private long itsPageId;
		private IOException itsException;
		
		private long itsReadCount = 0;
		private long itsWriteCount = 0;
		
		private long itsLastAccessedPage = -1;
		private long itsPageScattering = 0;
		
		private int itsPid = 1;
		
		public FileAccessor(File aFile) throws FileNotFoundException
		{
			Monitor.getInstance().register(this);
			itsFile = new RandomAccessFile(aFile, "rw");
			
			itsByteBuffer = ByteBuffer.allocate(itsPageSize);
			itsByteBuffer.order(ByteOrder.nativeOrder());
			itsIntBuffer = itsByteBuffer.asIntBuffer();
			
			itsPageId = -1;
			if (! DebugFlags.DISABLE_ASYNC_WRITES) start();
		}
		
		public void dispose()
		{
			Monitor.getInstance().unregister(this);
			try
			{
				itsFile.close();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			itsDisposed = true;
			System.out.println("[FileAccessor] Disposed ("+HardPagedFile.this+")");
		}
		
		public synchronized void read(long aId, int[] aBuffer) throws IOException
		{
			assert ! itsDisposed;
			aId = itsPhysicalPage[(int) aId];
			
			updateScattering(aId);
			
			itsReadCount++;
//			assert itsFile.length() >= (aId+1) * itsPageSize;
			if (! (itsFile.length() >= (aId+1) * itsPageSize))
			{
				throw new RuntimeException(String.format(
						"Read error: id: %d, page size: %d, storage: %d, length: %d",
						aId,
						itsPageSize,
						getStorageSpace(),
						itsFile.length()));
			}
					
			long theOffset = aId * itsPageSize;
			
			// Sometimes the readFully operation fails for no apparent reason, so we try
			// a brute-force workaround...
			int theRetries = 0;
			while (true)
			{
				try
				{
					itsFile.seek(theOffset);
					itsFile.readFully(itsByteBuffer.array());
					break;
				}
				catch (EOFException e)
				{
					System.err.println(String.format(
							"Read error: id: %d, page size: %d, storage: %d, length: %d, offset: %d, retry: %d",
							aId,
							itsPageSize,
							getStorageSpace(),
							itsFile.length(),
							theOffset,
							theRetries));
					
					Utils.sleep(500);
					theRetries++;
					if (theRetries == 50) throw e;
				}
			}
			
			itsIntBuffer.position(0);
			itsIntBuffer.get(aBuffer);
			
		}
		
		private void updateScattering(long aId)
		{
			if (itsLastAccessedPage >= 0)
			{
				long theDistance = Math.abs(itsLastAccessedPage - aId);
				itsPageScattering += theDistance;
			}
			itsLastAccessedPage = aId;
		}
		
		public synchronized void write(long aId, int[] aData) throws IOException
		{
			assert ! itsDisposed;

			int thePid = itsPhysicalPage[(int) aId];
			if (thePid == 0)
			{
				thePid = itsPid++;
				itsPhysicalPage[(int) aId] = thePid;
			}
			aId = thePid;
			
			updateScattering(aId);
			
			itsWriteCount++;
			if (DebugFlags.DISABLE_ASYNC_WRITES)
			{
				itsIntBuffer.position(0);
				itsIntBuffer.put(aData);
				itsFile.seek(aId * itsPageSize);
				itsFile.write(itsByteBuffer.array());
				return;
			}
			
			try
			{
				while (itsPageId >= 0) wait();
				itsPageId = aId;
				itsIntBuffer.position(0);
				itsIntBuffer.put(aData);
				IOException theException = itsException;
				itsException = null;
				notifyAll();
				
				if (theException != null) throw theException;
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public synchronized void run()
		{
			try
			{
				while (! itsDisposed)
				{
					while (itsPageId < 0) wait();
					try
					{
						itsFile.seek(itsPageId * itsPageSize);
						itsFile.write(itsByteBuffer.array());
					}
					catch (IOException e)
					{
						System.err.println("Exception in file writer thread");
						itsException = e;
					}
					itsPageId = -1;
					notifyAll();
				}
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Probe(key = "file written bytes", aggr = AggregationType.SUM)
		public long getWrittenBytes()
		{
			return itsWriteCount * itsPageSize;
		}

		@Probe(key = "file read bytes", aggr = AggregationType.SUM)
		public long getReadBytes()
		{
			return itsReadCount * itsPageSize;
		}
		
		@Probe(key = "file written pages", aggr = AggregationType.SUM)
		public long getWrittenPages()
		{
			return itsWriteCount;
		}
		
		@Probe(key = "file read pages", aggr = AggregationType.SUM)
		public long getReadPages()
		{
			return itsReadCount;
		}
		
		@Probe(key = "read/write ratio (%)", aggr = AggregationType.AVG)
		public float getRatio()
		{
			return 100f*itsReadCount/itsWriteCount;
		}
		
		@Probe(key = "page access scattering", aggr = AggregationType.SUM)
		public float getScattering()
		{
			return 1f * itsPageScattering / (itsReadCount+itsWriteCount);
		}
	}
	
	/**
	 * Clears all cached pages.
	 */
	public static void clearCache()
	{
		itsPageDataManager.dropAll();
	}
	
	/**
	 * Manages the collections of {@link PageData} instances.
	 * Ensures that dirty pages are saved before being discarded.
	 * @author gpothier
	 */
	private static class PageDataManager extends SyncMRUBuffer<PageKey, PageData>
	{
		private Stack<int[]> itsFreeBuffers = new ArrayStack<int[]>();
		private long itsCurrentSpace = 0;
		private final int itsPageSize;
		
		private PageDataManager(int aPageSize, int aBufferedPages)
		{
			super(aBufferedPages);
			itsPageSize = aPageSize;
			Monitor.getInstance().register(this);
		}
		
		public synchronized int[] getFreeBuffer()
		{
			if (itsFreeBuffers.isEmpty())
			{
				itsCurrentSpace += itsPageSize;
				return new int[itsPageSize/4];
			}
			else return itsFreeBuffers.pop();
		}
		
		private synchronized void addFreeBuffer(int[] aBuffer)
		{
			Utils.memset(aBuffer, 0);
			itsFreeBuffers.push(aBuffer);
		}
		
		public synchronized Entry<PageData> create(HardPagedFile aFile, long aId)
		{
			assert aFile.getPageSize() == itsPageSize;
			
			PageData theData = new PageData(
					new PageKey(aFile, aId),
					getFreeBuffer());
			
			return add(theData);
		}
		
		@Override
		protected void dropped(PageData aPageData)
		{
			addFreeBuffer(aPageData.detach());
		}

		@Override
		protected PageData fetch(PageKey aKey)
		{
			return aKey.load();
		}

		@Override
		protected PageKey getKey(PageData aValue)
		{
			return aValue.getKey();
		}

		@Probe(key = "page manager space", aggr = AggregationType.SUM)
		public long getCurrentSpace()
		{
			return itsCurrentSpace;
		}
	}
	
	public static class PageKey extends PageBank.PageKey
	{
		public PageKey(HardPagedFile aFile, long aPageId)
		{
			super(aFile, aPageId);
		}
		
		public HardPagedFile getFile()
		{
			return (HardPagedFile) getBank();
		}

		public PageData load()
		{
			int[] theData = getFile().loadPageData(getPageId());
			return new PageData(this, theData);
		}
		
		public void store(int[] aData)
		{
			getFile().storePageData(getPageId(), aData);
		}
	}
	
	/**
	 * Instances of this class hold the actual data of a page through a weak reference.
	 * @author gpothier
	 */
	private static class PageData
	{
		private PageKey itsKey;
		private List<Page> itsAttachedPages = new ArrayList<Page>(1);
		private int[] itsData;
		private boolean itsDirty = false;
		
		public PageData(PageKey aKey, int[] aData)
		{
			assert aData != null;
			itsKey = aKey;
			itsData = aData;
		}
		
		@Override
		protected void finalize() throws Throwable
		{
			if (itsDirty)
			{
				System.err.println("Finalizing dirty page: "+itsKey);
			}
		}
		
		public int[] getData()
		{
			assert itsData != null;
			return itsData;
		}
		
		public PageKey getKey()
		{
			return itsKey;
		}
		
		/**
		 * Returns the size, in bytes, of this page data.
		 */
		public int getSize()
		{
			return itsData.length * 4;
		}
		
		public void markDirty()
		{
			itsDirty = true;
		}
		
		public synchronized void attach(Page aPage)
		{
			itsAttachedPages.add(aPage);
			if (itsAttachedPages.size() > 1)
			{
				System.err.println(String.format(
					"Warning: page %s attached %d times",
					getKey(),
					itsAttachedPages.size()));
			}
		}
		
		/**
		 * Stores this page data if necessary, and
		 * detaches this page data from its pages, so that it can be reclaimed.
		 */
		public synchronized int[] detach()
		{
			if (itsDirty)
			{
				itsKey.store(itsData);
				itsDirty = false;
			}
			
			for (Page thePage : itsAttachedPages)
			{
				thePage.clearData();
			}
			itsAttachedPages = null;
			
			int[] theData = itsData;
			itsData = null;
			return theData;
		}
		
		/**
		 * Returns the first currently attached {@link Page}.
		 */
		public Page getAttachedPage()
		{
			// There should always be at least one attached page
			// when this method is called
			return itsAttachedPages.get(0);
		}
	}
	
	public static class Page extends PageBank.Page
	{
		private Entry<PageData> itsData;
		
		private Page(PageKey aKey)
		{
			super(aKey);
			assert aKey.getPageId() < aKey.getFile().itsPagesCount;
		}
		
		private Page(Entry<PageData> aData)
		{
			this(aData.getValue().getKey());
			itsData = aData;
			itsData.getValue().attach(this);
		}
		
		/**
		 * Returns a new {@link PageBitStruct} backed by this page.
		 * The advantage of having the {@link PageBitStruct} and page separate
		 * is that we can maintain several {@link PageBitStruct}s on the same page,
		 * each with a different position.
		 */
		public PageBitStruct asBitStruct()
		{
			return new PageBitStruct(this);
		}
		
		@Override
		public PageKey getKey()
		{
			return (PageKey) super.getKey();
		}
		
		public HardPagedFile getFile()
		{
			return getKey().getFile();
		}
		
		@Override
		public int getSize()
		{
			return getFile().getPageSize();
		}
		
		/**
		 * Obtains the data buffer of this page.
		 * Warning: clients modifying this buffer directly instead of using
		 * {@link #asBitStruct()} must call {@link #modified()} to notify the
		 * system that the page needs to be saved.
		 */
		public synchronized int[] getData()
		{
			if (itsData == null)
			{
				synchronized (itsPageDataManager)
				{
					itsData = itsPageDataManager.getEntry(getKey());
					itsData.getValue().attach(this);
				}
			}
			return itsData.getValue().getData();
		}
		
		synchronized void  clearData()
		{
			itsData = null;
		}
		
		public synchronized void modified()
		{
			if (itsData == null) throw new IllegalStateException("Trying to modify an absent page...");
			itsData.getValue().markDirty();
		}
		
		@Override
		public synchronized void use()
		{
			if (! DebugFlags.DISABLE_USE_PAGES && itsData != null) 
				itsPageDataManager.use(itsData);
		}
	}
	
	public static class PageBitStruct extends PageBank.PageBitStruct
	{
		public PageBitStruct(Page aPage)
		{
			// We pass null as data because we override the getData method
			super(0, aPage.getData().length, aPage);
		}
		
		@Override
		public Page getPage()
		{
			return (Page) super.getPage();
		}
		
		@Override
		protected int[] getData()
		{
			return getPage().getData();
		}
	}
	
}
