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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.PagedFile;
import zz.utils.primitive.IntArray;

/**
 * A file organized in pages.
 * @author gpothier
 */
public class ClassicPagedFile extends PagedFile
{
	private final ReentrantLock itsLock = new ReentrantLock();
	private final BufferManager itsBufferManager = BufferManager.getInstance();
	
	private final File itsFile;
	private FileChannel itsChannel;
	
	/**
	 * Number of pages currently in the file. 
	 */
	private int itsPagesCount;
	
	/**
	 * Map of currently loaded pages.
	 */
	private final Map<Integer, PageRef> itsPagesMap = new HashMap<Integer, PageRef>();
	
	private final ReferenceQueue<FilePage> itsPagesRefQueue = new ReferenceQueue<FilePage>();
	
	/**
	 * Mapping of logical page ids to physical page offsets.
	 * TODO: Temporary. This should be stored on the disk.
	 */
	private IntArray itsPhysicalPage;
	private int itsPid = 1;
	
	public static ClassicPagedFile create(File aFile, boolean aTruncate)
	{
		if (aTruncate) aFile.delete();
		return new ClassicPagedFile(aFile);
	}
	
	public ClassicPagedFile(File aFile)
	{
		itsFile = aFile;
		clear();
	}
	
	void lock()
	{
		itsLock.lock();
	}
	
	void unlock()
	{
		itsLock.unlock();
	}
	
	boolean tryLock()
	{
		return itsLock.tryLock();
	}

	@Override
	public long getPagesCount()
	{
		return itsPagesCount;
	}

	@Override
	public long getFileSize()
	{
		return itsFile.length();
	}
	
	public FileChannel getChannel()
	{
		return itsChannel;
	}
	
	/**
	 * Returns the name of the underlying file.
	 */
	public String getName()
	{
		return itsFile.getName();
	}
	
	@Override
	public Page get(int aPageId)
	{
		try
		{
			lock();

			assert aPageId > 0 : aPageId;
			
			PageRef theReference = itsPagesMap.get(aPageId);
			FilePage thePage = theReference != null ? theReference.get() : null;
			
			if (thePage == null)
			{
				thePage = new FilePage(aPageId);
				itsPagesMap.put(aPageId, new PageRef(thePage));
			}

			// Clean up garbage-collected references.
			do
			{
				PageRef theRef = (PageRef) itsPagesRefQueue.poll();
				if (theRef == null) break;
				else itsPagesMap.remove(theRef.getPageId());
			} while (true);
			
			return thePage;
		}
		finally
		{
			unlock();
		}
	}
	
	@Override
	public FilePage create()
	{
		try
		{
			lock();

			int thePageId;

			thePageId = ++itsPagesCount;
			FilePage thePage = itsBufferManager.create(this, thePageId);
			itsPagesMap.put(thePageId, new PageRef(thePage));
			
			return thePage;
		}
		finally
		{
			unlock();
		}
	}
	
	@Override
	public void flush()
	{
		itsBufferManager.flush(this);
	}
	
	@Override
	public void clear()
	{
		itsBufferManager.invalidatePages(this);
		
		try
		{
			if (itsChannel != null) itsChannel.close();
			itsFile.delete();
			itsChannel = new RandomAccessFile(itsFile, "rw").getChannel();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		itsPagesCount = 0;
		itsPagesMap.clear();
		
		itsPhysicalPage = new IntArray();
		itsPid = 0;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [" + itsFile.getName() + "]";
	}
	
	/**
	 * Writes a particular page to the disk
	 */
	void write(FilePage aPage)
	{
		try
		{
			lock();

			int thePageId = aPage.getPageId();
			
			// Map logical id to physical id
			int thePid = itsPhysicalPage.get(thePageId);
			if (thePid == 0)
			{ 
				thePid = itsPid++;
				itsPhysicalPage.set(thePageId, thePid);
			}
			thePageId = thePid;

			updateScattering(thePageId);
			
			itsBufferManager.write(aPage, thePageId);
			
			incWrite();
		}
		finally
		{
			unlock();
		}
	}
	
	/**
	 * Reads a page from the disk
	 */
	void read(FilePage aPage, int aBufferId)
	{
		try
		{
			lock();

			int thePhysPageId = itsPhysicalPage.get(aPage.getPageId());
			updateScattering(thePhysPageId);
			itsBufferManager.read(aPage, thePhysPageId, aBufferId);
			incRead();
		}
		finally
		{
			unlock();
		}
	}

	
	
	/**
	 * Registers an access of the given buffer.
	 */
	public void use(int aBufferId)
	{
		itsBufferManager.use(aBufferId);
	}
	
	/**
	 * Indicates to the page manager that this page is not going to be used anymore.
	 * This is optional, not calling it has no adverse effects, and the effect of calling
	 * it is a potiential increase in efficiency.
	 */
	@Override
	public void free(Page aPage)
	{
		itsBufferManager.free((FilePage) aPage);
	}
	
	private ByteBuffer getBuffer()
	{
		return itsBufferManager.getBuffer();
	}
	
	public class FilePage extends Page
	{
		private final ReentrantLock itsLock = new ReentrantLock();
		
		/**
		 * Id of the buffer page that holds the data for this page, or -1
		 * if the page is not in memory.
		 */
		private int itsBufferId;
		
		/**
		 * We cache the position of the page's buffer to avoid a multiplication.
		 */
		private int itsStartPos;
		
		private boolean itsDirty = false;
		
		private int itsUseCount = 0;
		
		public FilePage(int aPageId)
		{
			super(aPageId);
			itsBufferId = -1;
			itsStartPos = Integer.MIN_VALUE;
		}

		public FilePage(int aBufferId, int aPageId)
		{
			super(aPageId);
			itsBufferId = aBufferId;
			itsStartPos = itsBufferId * PAGE_SIZE;
		}
		
		void lock()
		{
			itsLock.lock();
		}
		
		void unlock()
		{
			itsLock.unlock();
		}
		
		boolean tryLock()
		{
			return itsLock.tryLock();
		}

		int getBufferId()
		{
			return itsBufferId;
		}

		@Override
		public ClassicPagedFile getFile()
		{
			return ClassicPagedFile.this;
		}
		
		/**
		 * Called when the buffer that holds the data of this page is paged out.
		 */
		void pagedOut()
		{
			try
			{
				lock();

				assert itsBufferId != -1;
				itsBufferId = -1;
				itsStartPos = Integer.MIN_VALUE;
			}
			finally
			{
				unlock();
			}
		}
		
		void pagedIn(int aBufferId)
		{
			try
			{
				lock();

				assert itsBufferId == -1;
				itsBufferId = aBufferId;
				itsStartPos = itsBufferId * PAGE_SIZE;
				itsDirty = false;
			}
			finally
			{
				unlock();
			}
		}
		
		/**
		 * Marks this page as dirty.
		 */
		private void modified(int aBufferId)
		{
			itsDirty = true;
			use(aBufferId);
			setDecodedPage(null); // TODO: Maybe necessary to have some way to invalidate the tuple buffer itself
		}
		
		public boolean isDirty()
		{
			return itsDirty;
		}

		private void use(int aBufferId)
		{
			if (itsUseCount++ > DebuggerGridConfigNG.DB_USE_THRESHOLD)
			{
				// We somewhat randomize the use count to avoid all pages trying to
				// call use at the same time.
				itsUseCount = getPageId() % (DebuggerGridConfigNG.DB_USE_THRESHOLD/4);
				ClassicPagedFile.this.use(aBufferId);
			}
		}
		
		@Override
		public void use()
		{
			use(getValidBufferId());
		}
		
		@Override
		public void free()
		{
			ClassicPagedFile.this.free(this);
		}

		/**
		 * Called when the PagedFile is cleared so that this page is no longer valid.
		 */
		@Override
		protected void invalidate()
		{
			try
			{
				lock();

				itsBufferId = -1;
				itsStartPos = Integer.MIN_VALUE;
				super.invalidate();
			}
			finally
			{
				unlock();
			}
		}

		/**
		 * Returns the id of the buffer that holds the data of this page,
		 * reloading it from the file if necessary.
		 */
		private int getValidBufferId()
		{
			try
			{
				lock();

				if (itsBufferId == -1) itsBufferManager.loadPage(this);
				return itsBufferId;
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public boolean readBoolean(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+1 <= PAGE_SIZE;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().get(thePos) != 0;
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeBoolean(int aPosition, boolean aValue)
		{
			try
			{
				lock();

				assert aPosition+1 <= PAGE_SIZE;

				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().put(thePos, aValue ? (byte) 1 : (byte) 0);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}

		@Override
		public void readBytes(int aPosition, byte[] aBuffer, int aOffset, int aCount)
		{
			try
			{
				lock();

				assert aPosition+aCount <= PAGE_SIZE;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				ByteBuffer theBuffer = getBuffer().duplicate();
				theBuffer.position(thePos);
				
				theBuffer.get(aBuffer, aOffset, aCount);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeBytes(int aPosition, byte[] aBytes, int aOffset, int aCount)
		{
			try
			{
				lock();

				assert aPosition+aCount <= PAGE_SIZE;

				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				ByteBuffer theBuffer = getBuffer().duplicate();
				theBuffer.position(thePos);

				theBuffer.put(aBytes, aOffset, aCount);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}

		@Override
		public byte readByte(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+1 <= PAGE_SIZE;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().get(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeByte(int aPosition, int aValue)
		{
			try
			{
				lock();

				assert aPosition+1 <= PAGE_SIZE;

				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().put(thePos, (byte)aValue);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public short readShort(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+2 <= PAGE_SIZE;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().getShort(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeShort(int aPosition, int aValue)
		{
			try
			{
				lock();

				assert aPosition+2 <= PAGE_SIZE;

				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().putShort(thePos, (short)aValue);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public int readInt(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+4 <= PAGE_SIZE;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().getInt(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeInt(int aPosition, int aValue)
		{
			try
			{
				lock();

				assert aPosition+4 <= PAGE_SIZE;

				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().putInt(thePos, aValue);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}

		@Override
		public long readLong(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+8 <= PAGE_SIZE;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().getLong(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeLong(int aPosition, long aValue)
		{
			try
			{
				lock();

				assert aPosition+8 <= PAGE_SIZE;
				
				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().putLong(thePos, aValue);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeBB(int aPosition, int aByte1, int aByte2)
		{
			try
			{
				lock();

				assert aPosition+2 <= PAGE_SIZE;
				
				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().put(thePos, (byte) aByte1);
				getBuffer().put(thePos+1, (byte) aByte2);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}

		@Override
		public void writeBS(int aPosition, int aByte, int aShort)
		{
			try
			{
				lock();

				assert aPosition+3 <= PAGE_SIZE;
				
				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().put(thePos, (byte) aByte);
				getBuffer().putShort(thePos+1, (short) aShort);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeBI(int aPosition, int aByte, int aInt)
		{
			try
			{
				lock();

				assert aPosition+5 <= PAGE_SIZE;
				
				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().put(thePos, (byte) aByte);
				getBuffer().putInt(thePos+1, aInt);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}
		
		@Override
		public void writeBL(int aPosition, int aByte, long aLong)
		{
			try
			{
				lock();

				assert aPosition+9 <= PAGE_SIZE;
				
				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().put(thePos, (byte) aByte);
				getBuffer().putLong(thePos+1, aLong);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}
		
		
		@Override
		public void writeSSSI(int aPosition, short aShort1, short aShort2, short aShort3, int aInt)
		{
			try
			{
				lock();

				assert aPosition+10 <= PAGE_SIZE;
				
				int theBufferId = getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				getBuffer().putShort(thePos, aShort1);
				getBuffer().putShort(thePos+2, aShort2);
				getBuffer().putShort(thePos+4, aShort3);
				getBuffer().putInt(thePos+6, aInt);
				modified(theBufferId);
			}
			finally
			{
				unlock();
			}
		}

		@Override
		public void writeInternalTupleData(int aPosition, int aPageId, long aTupleCount)
		{
			assert aPosition+PageIOStream.internalTupleDataSize() <= PAGE_SIZE;
			
			int theBufferId = getValidBufferId();
			int thePos = itsStartPos + aPosition;
			
			getBuffer().putInt(thePos, aPageId);
			getBuffer().putLong(thePos+4, aTupleCount);
			modified(theBufferId);			
		}

		
		@Override
		public String toString()
		{
			return "Page (pid: "+getPageId()+", bid: "+itsBufferId+")" + super.toString();
		}
	}
	
	private static class PageRef extends WeakReference<FilePage>
	{
		private int itsPageId;

		public PageRef(FilePage aReferent)
		{
			super(aReferent);
			itsPageId = aReferent.getPageId();
		}
		
		public int getPageId()
		{
			return itsPageId;
		}
	}
	
	
	
}
