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
package tod.impl.evdbng.db.file;

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

import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;
import zz.utils.primitive.IntArray;

/**
 * A file organized in pages.
 * @author gpothier
 */
public class PagedFile 
{
	private final ReentrantLock itsLock = new ReentrantLock();
	private final BufferManager itsBufferManager = BufferManager.getInstance();
	
	private final File itsFile;
	private FileChannel itsChannel;
	
	private final int itsPageSize;
	
	/**
	 * Number of pages currently in the file. 
	 */
	private int itsPagesCount;
	
	/**
	 * Map of currently loaded pages.
	 */
	private final Map<Integer, PageRef> itsPagesMap = new HashMap<Integer, PageRef>();
	
	private final ReferenceQueue<Page> itsPagesRefQueue = new ReferenceQueue<Page>();
	
	/**
	 * Mapping of logical page ids to physical page offsets.
	 * TODO: Temporary. This should be stored on the disk.
	 */
	private IntArray itsPhysicalPage;
	private int itsPid = 1;
	
	private long itsReadCount = 0;
	private long itsWriteCount = 0;
	private long itsLastAccessedPage = -1;
	private long itsPageScattering = 0;
	private long itsScatteringCount = 0;


	public static PagedFile create(File aFile, boolean aTruncate)
	{
		if (aTruncate) aFile.delete();
		return new PagedFile(aFile);
	}
	
	public PagedFile(File aFile)
	{
		itsFile = aFile;
		itsPageSize = itsBufferManager.getPageSize();
		
		clear();
		
		Monitor.getInstance().register(this);
	}
	
	public void dispose()
	{
		Monitor.getInstance().unregister(this);
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



	/**
	 * Page size, in bytes.
	 */
	public int getPageSize()
	{
		return itsPageSize;
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
		return itsFile.length();
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
		return 1f * itsPageScattering / itsScatteringCount;
	}

	private void updateScattering(long aId)
	{
		if (itsLastAccessedPage >= 0)
		{
			long theDistance = Math.abs(itsLastAccessedPage - aId);
			itsPageScattering += theDistance;
			itsScatteringCount++;
		}
		itsLastAccessedPage = aId;
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
	
	/**
	 * Returns the page with the specified id.
	 */
	public Page get(int aPageId)
	{
		try
		{
			itsLock.lock();

			assert aPageId > 0 : aPageId;
			
			PageRef theReference = itsPagesMap.get(aPageId);
			Page thePage = theReference != null ? theReference.get() : null;
			
			if (thePage == null)
			{
				thePage = new Page(aPageId);
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
			itsLock.unlock();
		}
	}
	
	/**
	 * Creates a new page.
	 */
	public Page create()
	{
		try
		{
			itsLock.lock();

			int thePageId;

			thePageId = ++itsPagesCount;
			Page thePage = itsBufferManager.create(this, thePageId);
			itsPagesMap.put(thePageId, new PageRef(thePage));
			
			return thePage;
		}
		finally
		{
			itsLock.unlock();
		}
	}
	
	/**
	 * Flushes all dirty buffers to disk
	 */
	public void flush()
	{
		itsBufferManager.flush(this);
	}
	
	/**
	 * Clears this paged file.
	 */
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
	void write(Page aPage)
	{
		try
		{
			itsLock.lock();

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
			
			itsWriteCount++;
		}
		finally
		{
			itsLock.unlock();
		}
	}
	
	/**
	 * Reads a page from the disk
	 */
	void read(Page aPage, int aBufferId)
	{
		try
		{
			itsLock.lock();

			int thePhysPageId = itsPhysicalPage.get(aPage.getPageId());
			updateScattering(thePhysPageId);
			itsBufferManager.read(aPage, thePhysPageId, aBufferId);
			itsReadCount++;
		}
		finally
		{
			itsLock.unlock();
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
	public void free(Page aPage)
	{
		itsBufferManager.free(aPage);
	}
	
	private ByteBuffer getBuffer()
	{
		return itsBufferManager.getBuffer();
	}
	
	public class Page
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
		
		/**
		 * Logical page id.
		 */
		private int itsPageId;
		
		/**
		 * Can keep a cache of decompressed tuples.
		 */
		private WeakReference<TupleBuffer<?>> itsTupleBuffer;
		
		private boolean itsDirty = false;
		
		private int itsUseCount = 0;
		
		public Page(int aPageId)
		{
			itsBufferId = -1;
			itsStartPos = Integer.MIN_VALUE;
			itsPageId = aPageId;
		}

		public Page(int aBufferId, int aPageId)
		{
			itsBufferId = aBufferId;
			itsStartPos = itsBufferId * itsPageSize;
			itsPageId = aPageId;
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

		public int getPageId()
		{
			return itsPageId;
		}
		
		int getBufferId()
		{
			return itsBufferId;
		}

		public PagedFile getFile()
		{
			return PagedFile.this;
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
				itsStartPos = itsBufferId * itsPageSize;
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
			itsTupleBuffer = null; // TODO: Maybe necessary to have some way to invalidate the tuple buffer itself
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
				itsUseCount = itsPageId % (DebuggerGridConfigNG.DB_USE_THRESHOLD/4);
				PagedFile.this.use(aBufferId);
			}
		}
		
		public void use()
		{
			use(getValidBufferId());
		}
		
		public void free()
		{
			PagedFile.this.free(this);
		}

		/**
		 * Called when the PagedFile is cleared so that this page is no longer valid.
		 */
		void invalidate()
		{
			try
			{
				lock();

				itsBufferId = -1;
				itsStartPos = Integer.MIN_VALUE;
				itsPageId = -1;
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
		
		/**
		 * Returns the cached tuple buffer, if present.
		 */
		public TupleBuffer<?> getTupleBuffer()
		{
			return itsTupleBuffer != null ? itsTupleBuffer.get() : null;
		}
		
		/**
		 * Caches a tuple buffer in this page (weakly referenced).
		 */
		public void setTupleBuffer(TupleBuffer<?> aTupleBuffer)
		{
			itsTupleBuffer = new WeakReference<TupleBuffer<?>>(aTupleBuffer);
		}
		
		public boolean readBoolean(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+1 <= itsPageSize;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().get(thePos) != 0;
			}
			finally
			{
				unlock();
			}
		}
		
		public void writeBoolean(int aPosition, boolean aValue)
		{
			try
			{
				lock();

				assert aPosition+1 <= itsPageSize;

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

		public void readBytes(int aPosition, byte[] aBuffer, int aOffset, int aCount)
		{
			try
			{
				lock();

				assert aPosition+aCount <= itsPageSize;

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
		
		public void writeBytes(int aPosition, byte[] aBytes, int aOffset, int aCount)
		{
			try
			{
				lock();

				assert aPosition+aCount <= itsPageSize;

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

		public byte readByte(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+1 <= itsPageSize;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().get(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		public void writeByte(int aPosition, int aValue)
		{
			try
			{
				lock();

				assert aPosition+1 <= itsPageSize;

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
		
		public short readShort(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+2 <= itsPageSize;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().getShort(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		public void writeShort(int aPosition, int aValue)
		{
			try
			{
				lock();

				assert aPosition+2 <= itsPageSize;

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
		
		public int readInt(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+4 <= itsPageSize;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().getInt(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		public void writeInt(int aPosition, int aValue)
		{
			try
			{
				lock();

				assert aPosition+4 <= itsPageSize;

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

		public long readLong(int aPosition)
		{
			try
			{
				lock();

				assert aPosition+8 <= itsPageSize;

				getValidBufferId();
				int thePos = itsStartPos + aPosition;
				
				return getBuffer().getLong(thePos);
			}
			finally
			{
				unlock();
			}
		}
		
		public void writeLong(int aPosition, long aValue)
		{
			try
			{
				lock();

				assert aPosition+8 <= itsPageSize;
				
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
		
		public void writeBB(int aPosition, int aByte1, int aByte2)
		{
			try
			{
				lock();

				assert aPosition+2 <= itsPageSize;
				
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

		public void writeBS(int aPosition, int aByte, int aShort)
		{
			try
			{
				lock();

				assert aPosition+3 <= itsPageSize;
				
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
		
		public void writeBI(int aPosition, int aByte, int aInt)
		{
			try
			{
				lock();

				assert aPosition+5 <= itsPageSize;
				
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
		
		public void writeBL(int aPosition, int aByte, long aLong)
		{
			try
			{
				lock();

				assert aPosition+9 <= itsPageSize;
				
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
		
		public void writeInternalTupleData(int aPosition, int aPageId, long aTupleCount)
		{
			assert aPosition+PageIOStream.internalTupleDataSize() <= itsPageSize;
			
			int theBufferId = getValidBufferId();
			int thePos = itsStartPos + aPosition;
			
			getBuffer().putInt(thePos, aPageId);
			getBuffer().putLong(thePos+4, aTupleCount);
			modified(theBufferId);			
		}

		
		public int getPageSize()
		{
			return itsPageSize;
		}
		
		public PageIOStream asIOStream()
		{
			return new PageIOStream(this);
		}
		
		@Override
		public String toString()
		{
			StringBuilder theBuilder = new StringBuilder("Page (pid: "+itsPageId+", bid: "+itsBufferId+") [\n");
			PageIOStream theBitStruct = asIOStream();
			for (int i=0;i<getPageSize()/16;i++)
			{
				theBuilder.append("  ("+i*16+") | ");
				
				for (int j=0;j<16;j++)
				{
					String theHexString = Integer.toHexString(theBitStruct.readByte() & 0xff);
					if (theHexString.length() == 1) theBuilder.append('0');
					theBuilder.append(theHexString);
					theBuilder.append(' ');
				}
				
				theBuilder.append('\n');
			}
			theBuilder.append("]");
			return theBuilder.toString();
		}
	}
	
	private static class PageRef extends WeakReference<Page>
	{
		private int itsPageId;

		public PageRef(Page aReferent)
		{
			super(aReferent);
			itsPageId = aReferent.getPageId();
		}
		
		public int getPageId()
		{
			return itsPageId;
		}
	}
	
	/**
	 * Provides a stream view of the page, with a current position and the ability to read/write
	 * basic data types.
	 * @author gpothier
	 */
	public static class PageIOStream 
	{
		private final Page itsPage;
		private int itsPosition;

		public PageIOStream(Page aPage)
		{
			assert aPage != null;
			itsPage = aPage;
			setPos(0);
		}

		public Page getPage()
		{
			return itsPage;
		}
	
		public static int booleanSize()
		{ 
			return 1;
		}
		
		public boolean readBoolean()
		{
			boolean theValue = itsPage.readBoolean(getPos());
			skip(1);
			return theValue;
		}
		
		public void writeBoolean(boolean aValue)
		{
			itsPage.writeBoolean(getPos(), aValue);
			skip(1);
		}

		public void writeBytes(byte[] aBytes)
		{
			writeBytes(aBytes, 0, aBytes.length);
		}
		
		public void readBytes(byte[] aBuffer, int aOffset, int aCount)
		{
			itsPage.readBytes(getPos(), aBuffer, aOffset, aCount);
			skip(aCount);
		}
		
		public void writeBytes(byte[] aBytes, int aOffset, int aCount)
		{
			itsPage.writeBytes(getPos(), aBytes, aOffset, aCount);
			skip(aCount);
		}

		public static int byteSize()
		{ 
			return 1;
		}
		
		public byte readByte()
		{
			byte theValue = itsPage.readByte(getPos());
			skip(1);
			return theValue;
		}
		
		public void writeByte(int aValue)
		{
			itsPage.writeByte(getPos(), aValue);
			skip(1);
		}
		
		public static int shortSize()
		{ 
			return 2;
		}
		
		public short readShort()
		{
			short theValue = itsPage.readShort(getPos());
			skip(2);
			return theValue;
		}
		
		/**
		 * Reads an unsigned short.
		 */
		public int readUShort()
		{
			return readShort() & 0xffff;
		}
		
		public void writeShort(int aValue)
		{
			itsPage.writeShort(getPos(), aValue);
			skip(2);
		}
		
		public static int intSize()
		{ 
			return 4;
		}
		
		public int readInt()
		{
			int theValue = itsPage.readInt(getPos());
			skip(4);
			return theValue;
		}
		
		public void writeInt(int aValue)
		{
			itsPage.writeInt(getPos(), aValue);
			skip(4);
		}

		public static int longSize()
		{ 
			return 8;
		}
		
		public long readLong()
		{
			long theValue = itsPage.readLong(getPos());
			skip(8);
			return theValue;
		}
		
		public void writeLong(long aValue)
		{
			itsPage.writeLong(getPos(), aValue);
			skip(8);
		}
		
		public void writeBB(int aByte1, int aByte2)
		{
			itsPage.writeBB(getPos(), aByte1, aByte2);
			skip(2);
		}

		public void writeBS(int aByte, int aShort)
		{
			itsPage.writeBS(getPos(), aByte, aShort);
			skip(3);
		}
		
		public void writeBI(int aByte, int aInt)
		{
			itsPage.writeBI(getPos(), aByte, aInt);
			skip(5);
		}
		
		public void writeBL(int aByte, long aLong)
		{
			itsPage.writeBL(getPos(), aByte, aLong);
			skip(9);
		}

		public static int internalTupleDataSize()
		{
			return PageIOStream.pagePointerSize()+PageIOStream.tupleCountSize();
		}
		
		public void writeInternalTupleData(int aPageId, long aTupleCount)
		{
			itsPage.writeInternalTupleData(getPos(), aPageId, aTupleCount);
			skip(internalTupleDataSize());
		}

		public static int behaviorIdSize()
		{
			return 4;
		}
		
		public int readBehaviorId()
		{
			return readInt();
		}
		
		public void writeBehaviorId(int aId)
		{
			writeInt(aId);
		}
		
		public static int fieldIdSize()
		{
			return 4;
		}
		
		public int readFieldId()
		{
			return readInt();
		}
		
		public void writeFieldId(int aId)
		{
			writeInt(aId);
		}
		
		public static int variableIdSize()
		{
			return 2;
		}
		
		public int readVariableId()
		{
			return readUShort();
		}
		
		public void writeVariableId(int aId)
		{
			writeShort(aId);
		}
		
		public static int typeIdSize()
		{
			return 2;
		}
		
		public int readTypeId()
		{
			return readUShort();
		}
		
		public void writeTypeId(int aId)
		{
			writeShort(aId);
		}
		
		public static int threadIdSize()
		{
			return 2;
		}
		
		public int readThreadId()
		{
			return readUShort();
		}
		
		public void writeThreadId(int aId)
		{
			writeShort(aId);
		}
		
		public static int cflowDepthSize()
		{
			return 2;
		}
		
		public int readCFlowDepth()
		{
			return readUShort();
		}
		
		public void writeCFlowDepth(int aDepth)
		{
			writeShort(aDepth);
		}
		
		public static int bytecodeIndexSize()
		{
			return 2;
		}
		
		public int readBytecodeIndex()
		{
			return readUShort();
		}
		
		public void writeBytecodeIndex(int aIndex)
		{
			writeShort(aIndex);
		}
		
		public static int adviceSourceIdSize()
		{
			return 2;
		}
		
		public int readAdviceSourceId()
		{
			return readUShort();
		}
		
		public void writeAdviceSourceId(int aId)
		{
			writeShort(aId);
		}
		
		public static int timestampSize()
		{
			return 8;
		}
		
		public long readTimestamp()
		{
			return readLong();
		}
		
		public void writeTimestamp(long aTimestamp)
		{
			writeLong(aTimestamp);
		}
		
		public static int pagePointerSize()
		{
			return 4;
		}
		
		public int readPagePointer()
		{
			return readInt();
		}
		
		public void writePagePointer(int aPointer)
		{
			writeInt(aPointer);
		}
		
		public static int tupleCountSize()
		{
			return 8;
		}
		
		public long readTupleCount()
		{
			return readLong();
		}
		
		public void writeTupleCount(long aCount)
		{
			writeLong(aCount);
		}
		
		public static int pageOffsetSize()
		{
			return 2;
		}
		
		public short readPageOffset()
		{
			return readShort();
		}
		
		public void writePageOffset(int aOffset)
		{
			writeShort(aOffset);
		}
		
		public static int eventPointerSize()
		{
			return 8;
		}
		
		public long readEventPointer()
		{
			return readLong();
		}
		
		public void writeEventPointer(long aPointer)
		{
			writeLong(aPointer);
		}
		
		public static int roleSize()
		{
			return 1;
		}
		
		public int readRole()
		{
			return readByte();
		}
		
		public void writeRole(int aRole)
		{
			writeByte(aRole);
		}
		
		public static int probeIdSize()
		{
			return 4;
		}
		
		public int readProbeId()
		{
			return readInt();
		}
		
		public void writeProbeId(int aId)
		{
			writeInt(aId);
		}
		
		/**
		 * The current position of this struct's pointer, in bytes.
		 */
		public int getPos()
		{
			return itsPosition;
		}
		
		/**
		 * Sets the current position of this struct's pointer, in bytes.
		 */
		public void setPos(int aPos)
		{
			itsPosition = aPos;
			assert itsPosition <= itsPage.getPageSize();
		}
		
		public void rewind()
		{
			setPos(0);
		}
		
		public void skip(int aCount)
		{
			setPos(getPos()+aCount);
		}
		
		/**
		 * The size of the underlying page, in bytes.
		 */
		public int size()
		{
			return itsPage.getPageSize();
		}
		
		/**
		 * Number of bytes remaining to read or write in this stream.
		 */
		public int remaining()
		{
			return size() - getPos();
		}
	}
	
	
	/**
	 * Similar to a {@link PageIOStream} but supports overflowing by chaining pages.
	 * Previous page and next page pointers are stored at the end of the page. 
	 * @author gpothier
	 */
	public static class ChainedPageIOStream
	{
		private final PagedFile itsFile;
		private PageIOStream itsCurrentStream;
		
		public ChainedPageIOStream(PagedFile aFile)
		{
			itsFile = aFile;
			itsCurrentStream = itsFile.create().asIOStream();
		}
		
		public ChainedPageIOStream(PagedFile aFile, int aPageId, int aPosition)
		{
			itsFile = aFile;
			itsCurrentStream = itsFile.get(aPageId).asIOStream();
			itsCurrentStream.setPos(aPosition);
		}
		
		/**
		 * Returns the current page.
		 */
		public Page getCurrentPage()
		{
			return itsCurrentStream.getPage();
		}
		
		public PageIOStream getCurrentStream()
		{
			return itsCurrentStream;
		}

		/**
		 * Checks that the current page has enough space to hold aSpace more bytes.
		 * If it does not, a new page is allocated and chained to this one.
		 */
		private void checkSpace(int aSpace)
		{
			assert aSpace < itsFile.getPageSize()-2*PageIOStream.pagePointerSize();
			if (itsCurrentStream.remaining()-2*PageIOStream.pagePointerSize() >= aSpace) return;
			
			Page theNewPage = itsFile.create();
			
			// Write next page id on current page
			itsCurrentStream.setPos(itsFile.getPageSize()-PageIOStream.pagePointerSize());
			itsCurrentStream.writePagePointer(theNewPage.getPageId());
			
			// Write previous page id on next page
			PageIOStream theNewStruct = theNewPage.asIOStream();
			theNewStruct.setPos(itsFile.getPageSize()-2*PageIOStream.pagePointerSize());
			int theOldPageId = itsCurrentStream.getPage().getPageId();
			theNewStruct.writePagePointer(theOldPageId);
			
			theNewStruct.rewind();
			itsCurrentStream = theNewStruct;
			
			newPageHook(theOldPageId, theNewPage.getPageId());
		}

		/**
		 * Hook that can be used to be notified when a new page is allocated
		 * @param aNewPageId Id of the newly allocated page. 
		 */
		protected void newPageHook(int aOldPageId, int aNewPageId)
		{
		}
		
		public void writeByte(int aValue, int aDataSpace)
		{
			checkSpace(1+aDataSpace);
			itsCurrentStream.writeByte(aValue);
		}
		
		public void writeShort(int aValue, int aDataSpace)
		{
			checkSpace(2+aDataSpace);
			itsCurrentStream.writeShort(aValue);
		}
		
		public void writeInt(int aValue, int aDataSpace)
		{
			checkSpace(4+aDataSpace);
			itsCurrentStream.writeInt(aValue);
		}
		
		public void writeLong(long aValue, int aDataSpace)
		{
			checkSpace(8+aDataSpace);
			itsCurrentStream.writeLong(aValue);
		}
		
		public void writeBB(int aByte1, int aByte2, int aDataSpace)
		{
			checkSpace(2+aDataSpace);
			itsCurrentStream.writeBB(aByte1, aByte2);
		}

		public void writeBS(int aByte, int aShort, int aDataSpace)
		{
			checkSpace(3+aDataSpace);
			itsCurrentStream.writeBS(aByte, aShort);
		}
		
		public void writeBI(int aByte, int aInt, int aDataSpace)
		{
			checkSpace(5+aDataSpace);
			itsCurrentStream.writeBI(aByte, aInt);
		}
		
		public void writeBL(int aByte, long aLong, int aDataSpace)
		{
			checkSpace(9+aDataSpace);
			itsCurrentStream.writeBL(aByte, aLong);
		}
		
		public void writeInternalTupleData(int aPageId, long aTupleCount)
		{
			checkSpace(PageIOStream.internalTupleDataSize());
			itsCurrentStream.writeInternalTupleData(aPageId, aTupleCount);
		}
	}
}
