package tod.impl.evdbng.db.file.mapped;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.PagedFile;
import zz.utils.Utils;

public class MappedPagedFile extends PagedFile
{
	private static final int CHUNK_SIZE = 8*1024*1024; 
	private static final int PAGES_PER_CHUNK = CHUNK_SIZE/PagedFile.PAGE_SIZE;
	
	private final RandomAccessFile itsFile;
	private final FileChannel itsChannel;
	private List<MappedByteBuffer> itsBuffers = new ArrayList<MappedByteBuffer>();
	
	/**
	 * Number of pages currently in the file. 
	 */
	private int itsPagesCount;
	
	public static MappedPagedFile create(File aFile, boolean aTruncate)
	{
		if (aTruncate) aFile.delete();
		return new MappedPagedFile(aFile, aTruncate);
	}

	public MappedPagedFile(File aFile, boolean aClear)
	{
		try
		{
			itsFile = new RandomAccessFile(aFile, "rw");
			itsChannel = itsFile.getChannel();
			if (aClear) clear();
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	private MappedByteBuffer getChunkBuffer(int aChunkId)
	{
		MappedByteBuffer theBuffer = Utils.listGet(itsBuffers, aChunkId);
		if (theBuffer == null)
		{
			try
			{
				theBuffer = itsChannel.map(MapMode.READ_WRITE, aChunkId*CHUNK_SIZE, CHUNK_SIZE);
				theBuffer.order(ByteOrder.nativeOrder());
				Utils.listSet(itsBuffers, aChunkId, theBuffer);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return theBuffer;
	}

	@Override
	public long getPagesCount()
	{
		return itsPagesCount;
	}

	@Override
	public long getFileSize()
	{
		try
		{
			return itsFile.length();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Page get(int aPageId)
	{
		return new MappedPage(aPageId, getChunkBuffer(aPageId/PAGES_PER_CHUNK), (aPageId%PAGES_PER_CHUNK)*PagedFile.PAGE_SIZE);
	}

	@Override
	public synchronized Page create()
	{
		return get(++itsPagesCount);
	}

	@Override
	public void flush()
	{
		
	}

	@Override
	public void clear()
	{
		itsPagesCount = 0;
	}

	@Override
	public void free(Page aPage)
	{
	}
	
	private class MappedPage extends Page
	{
		private final MappedByteBuffer itsBuffer;
		private final int itsOffset;

		public MappedPage(int aPageId, MappedByteBuffer aBuffer, int aOffset)
		{
			super(aPageId);
			itsBuffer = aBuffer;
			itsOffset = aOffset;
		}

		@Override
		public PagedFile getFile()
		{
			return MappedPagedFile.this;
		}

		@Override
		public void use()
		{
		}

		@Override
		public void free()
		{
		}

		@Override
		public void clear(int aPosition, int aCount)
		{
			int theEnd = aPosition + aCount;
			while(aPosition+8 <= theEnd) 
			{
				itsBuffer.putLong(itsOffset+aPosition, 0);
				aPosition += 8;
			}
			while(aPosition < theEnd)
			{
				itsBuffer.put(itsOffset+aPosition, (byte) 0);
				aPosition++;
			}
		}
		
		@Override
		public boolean readBoolean(int aPosition)
		{
			assert aPosition >= 0 && aPosition < PagedFile.PAGE_SIZE : ""+aPosition;
			return itsBuffer.get(itsOffset+aPosition) != 0;
		}

		@Override
		public void writeBoolean(int aPosition, boolean aValue)
		{
			assert aPosition >= 0 && aPosition < PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.put(itsOffset+aPosition, aValue ? (byte) 1 : (byte) 0);
		}

		@Override
		public void readBytes(int aPosition, byte[] aBuffer, int aOffset, int aCount)
		{
			assert aPosition >= 0 && aPosition+aCount <= PagedFile.PAGE_SIZE : ""+aPosition;
			ByteBuffer theBuffer = itsBuffer.duplicate();
			theBuffer.position(itsOffset+aPosition);
			theBuffer.get(aBuffer, aOffset, aCount);
		}

		@Override
		public void writeBytes(int aPosition, byte[] aBytes, int aOffset, int aCount)
		{
			assert aPosition >= 0 && aPosition+aCount <= PagedFile.PAGE_SIZE : ""+aPosition;
			ByteBuffer theBuffer = itsBuffer.duplicate();
			theBuffer.position(itsOffset+aPosition);
			theBuffer.put(aBytes, aOffset, aCount);
		}

		@Override
		public byte readByte(int aPosition)
		{
			assert aPosition >= 0 && aPosition < PagedFile.PAGE_SIZE : ""+aPosition;
			return itsBuffer.get(itsOffset+aPosition);
		}

		@Override
		public void writeByte(int aPosition, int aValue)
		{
			assert aPosition >= 0 && aPosition < PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.put(itsOffset+aPosition, (byte) aValue);			
		}

		@Override
		public short readShort(int aPosition)
		{
			assert aPosition >= 0 && aPosition+2 <= PagedFile.PAGE_SIZE : ""+aPosition;
			return itsBuffer.getShort(itsOffset+aPosition);
		}

		@Override
		public void writeShort(int aPosition, int aValue)
		{
			assert aPosition >= 0 && aPosition+2 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.putShort(itsOffset+aPosition, (short) aValue);			
		}

		@Override
		public int readInt(int aPosition)
		{
			assert aPosition >= 0 && aPosition+4 <= PagedFile.PAGE_SIZE : ""+aPosition;
			return itsBuffer.getInt(itsOffset+aPosition);
		}

		@Override
		public void writeInt(int aPosition, int aValue)
		{
			assert aPosition >= 0 && aPosition+4 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.putInt(itsOffset+aPosition, aValue);						
		}

		@Override
		public long readLong(int aPosition)
		{
			assert aPosition >= 0 && aPosition+8 <= PagedFile.PAGE_SIZE : ""+aPosition;
			return itsBuffer.getLong(itsOffset+aPosition);
		}

		@Override
		public void writeLong(int aPosition, long aValue)
		{
			assert aPosition >= 0 && aPosition+8 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.putLong(itsOffset+aPosition, aValue);			
		}

		@Override
		public void writeBB(int aPosition, int aByte1, int aByte2)
		{
			assert aPosition >= 0 && aPosition+2 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.put(itsOffset+aPosition, (byte) aByte1);				
			itsBuffer.put(itsOffset+aPosition+1, (byte) aByte2);				
		}

		@Override
		public void writeBS(int aPosition, int aByte, int aShort)
		{
			assert aPosition >= 0 && aPosition+3 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.put(itsOffset+aPosition, (byte) aByte);				
			itsBuffer.putShort(itsOffset+aPosition+1, (short) aShort);				
		}

		@Override
		public void writeBI(int aPosition, int aByte, int aInt)
		{
			assert aPosition >= 0 && aPosition+5 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.put(itsOffset+aPosition, (byte) aByte);				
			itsBuffer.putInt(itsOffset+aPosition+1, aInt);							
		}

		@Override
		public void writeBL(int aPosition, int aByte, long aLong)
		{
			assert aPosition >= 0 && aPosition+9 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.put(itsOffset+aPosition, (byte) aByte);				
			itsBuffer.putLong(itsOffset+aPosition+1, aLong);				
		}

		@Override
		public void writeLI(int aPosition, long aLong, int aInt)
		{
			assert aPosition >= 0 && aPosition+12 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.putLong(itsOffset+aPosition, aLong);				
			itsBuffer.putInt(itsOffset+aPosition+8, aInt);				
		}

		@Override
		public void writeSSSI(int aPosition, short aShort1, short aShort2, short aShort3, int aInt)
		{
			assert aPosition >= 0 && aPosition+10 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.putShort(itsOffset+aPosition, aShort1);				
			itsBuffer.putShort(itsOffset+aPosition+2, aShort2);				
			itsBuffer.putShort(itsOffset+aPosition+4, aShort3);				
			itsBuffer.putInt(itsOffset+aPosition+6, aInt);				
		}

		@Override
		public void writeInternalTupleData(int aPosition, int aPageId, long aTupleCount)
		{
			assert aPosition >= 0 && aPosition+12 <= PagedFile.PAGE_SIZE : ""+aPosition;
			itsBuffer.putInt(itsOffset+aPosition, aPageId);				
			itsBuffer.putLong(itsOffset+aPosition+4, aTupleCount);							
		}

		@Override
		public void move(int aPosition, int aLength, int aOffset)
		{
			assert aPosition+aLength+aOffset <= PAGE_SIZE;
			assert aPosition+aOffset >= 0;
			
			if (aOffset > 0)
			{
				int theStart = itsOffset + aPosition + aLength;
				int theEnd = theStart - aLength;
				for (int i=theStart-1;i>=theEnd;i--) itsBuffer.put(i+aOffset, itsBuffer.get(i));
			}
			else
			{
				int theStart = itsOffset + aPosition;
				int theEnd = theStart + aLength;
				for (int i=theStart;i<theEnd;i++) itsBuffer.put(i+aOffset, itsBuffer.get(i));
			}
			
		}

		@Override
		public void copy(int aSrcPos, Page aDest, int aDstPos, int aLength)
		{
			MappedPage theDest = (MappedPage) aDest;

			assert aSrcPos+aLength <= PAGE_SIZE;
			assert aDstPos+aLength <= PAGE_SIZE;
			assert aDest.getPageId() != getPageId();

			int theSrc = itsOffset + aSrcPos;
			int theDst = theDest.itsOffset + aDstPos;
			
//			while(aLength >= 8)
//			{
//				theDest.itsBuffer.putLong(theDst, itsBuffer.getLong(theSrc));
//				theDst += 8;
//				theSrc += 8;
//				aLength -= 8;
//			}
			while(aLength > 0)
			{
				theDest.itsBuffer.put(theDst++, itsBuffer.get(theSrc++));
				aLength--;
			}
		}
	}
}
