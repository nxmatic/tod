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

import tod.impl.evdbng.db.file.PagedFile.Page;
import tod.impl.evdbng.db.file.PagedFile.PageIOStream;

/**
 * Stores the decoded tuples of a {@link Page}.
 * @author gpothier
 */
public abstract class TupleBuffer<T extends Tuple>
{
	/**
	 * tuple count of the previous page
	 */
	private long itsTupleCount = -1;
	
	private final int itsPreviousPageId;
	private final int itsNextPageId;
	
	private final long[] itsKeyBuffer;
	private int itsPosition;
	
	public TupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
	{
		itsKeyBuffer = new long[aSize];
		itsPreviousPageId = aPreviousPageId;
		itsNextPageId = aNextPageId;
	}
	
	public void read(long aKey, PageIOStream aStream)
	{
		itsKeyBuffer[itsPosition] = aKey;
		read0(itsPosition, aStream);
		itsPosition++;
	}
	
	/**
	 * Returns the number of tuples in this buffer.
	 */
	public int getSize()
	{
		return itsPosition;
	}

	/**
	 * Returns the key at the specified position.
	 */
	public long getKey(int aPosition)
	{
		assert aPosition < itsPosition;
		return itsKeyBuffer[aPosition];
	}
	
	/**
	 * Creates a tuple object corresponding to the tuple at the 
	 * specified position.
	 */
	public abstract T getTuple(int aPosition);
	
	/**
	 * Reads a tuple data into internal buffers.
	 */
	public abstract void read0(int aPosition, PageIOStream aStream);
	
	public int getPreviousPageId()
	{
		return itsPreviousPageId;
	}

	public int getNextPageId()
	{
		return itsNextPageId;
	}

	/**
	 * Returns the number of tuples before the beginning of the page,
	 * if the info is available (otherwise returns -1).
	 */
	public long getTupleCount()
	{
		return itsTupleCount;
	}

	public void setTupleCount(long aTupleCount)
	{
		itsTupleCount = aTupleCount;
	}


	/**
	 * Tuple data reader for simple tuples
	 * @author gpothier
	 */
	public static class SimpleTupleBuffer extends TupleBuffer<SimpleTuple>
	{
		public SimpleTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
		}

		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
		}

		@Override
		public SimpleTuple getTuple(int aPosition)
		{
			return new SimpleTuple(getKey(aPosition));
		}
	}
	
	/**
	 * Tuple data reader for role tuples
	 * @author gpothier
	 */
	public static class RoleTupleBuffer extends TupleBuffer<RoleTuple>
	{
		private byte[] itsBuffer;
		
		public RoleTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsBuffer = new byte[aSize];
		}

		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsBuffer[aPosition] = (byte) aStream.readRole();
		}

		@Override
		public RoleTuple getTuple(int aPosition)
		{
			return new RoleTuple(getKey(aPosition), itsBuffer[aPosition]);
		}
	}
	
	public static class InternalTupleBuffer extends TupleBuffer<InternalTuple>
	{
		private int[] itsPageIdBuffer;
		private long[] itsTupleCountBuffer;
		
		public InternalTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsPageIdBuffer = new int[aSize];
			itsTupleCountBuffer = new long[aSize];
		}

		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsPageIdBuffer[aPosition] = aStream.readPagePointer();
			itsTupleCountBuffer[aPosition] = aStream.readTupleCount();
		}

		@Override
		public InternalTuple getTuple(int aPosition)
		{
			return new InternalTuple(
					getKey(aPosition), 
					itsPageIdBuffer[aPosition], 
					itsTupleCountBuffer[aPosition]);
		}
	}

	public static class ObjectPointerTupleBuffer extends TupleBuffer<ObjectPointerTuple>
	{
		private int[] itsPageIdBuffer;
		private short[] itsOffsetBuffer;
		
		public ObjectPointerTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsPageIdBuffer = new int[aSize];
			itsOffsetBuffer = new short[aSize];
		}
		
		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsPageIdBuffer[aPosition] = aStream.readPagePointer();
			itsOffsetBuffer[aPosition] = aStream.readPageOffset();
		}
		
		@Override
		public ObjectPointerTuple getTuple(int aPosition)
		{
			return new ObjectPointerTuple(
					getKey(aPosition), 
					itsPageIdBuffer[aPosition], 
					itsOffsetBuffer[aPosition]);
		}
	}
	
	public static class ObjectRefTupleBuffer extends TupleBuffer<ObjectRefTuple>
	{
		private long[] itsClassIdBuffer;
		
		public ObjectRefTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsClassIdBuffer = new long[aSize];
		}
		
		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsClassIdBuffer[aPosition] = aStream.readLong();
		}
		
		@Override
		public ObjectRefTuple getTuple(int aPosition)
		{
			return new ObjectRefTuple(
					getKey(aPosition), 
					itsClassIdBuffer[aPosition]);
		}
	}
	

}