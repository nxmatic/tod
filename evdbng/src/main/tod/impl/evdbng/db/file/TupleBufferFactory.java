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

import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.TupleBuffer.ObjectPointerTupleBuffer;
import tod.impl.evdbng.db.file.TupleBuffer.ObjectRefTupleBuffer;
import tod.impl.evdbng.db.file.TupleBuffer.RoleTupleBuffer;
import tod.impl.evdbng.db.file.TupleBuffer.SimpleTupleBuffer;

/**
 * A factory of {@link TupleBuffer}s
 * @author gpothier
 */
public abstract class TupleBufferFactory<T extends Tuple>
{
	public static final TupleBufferFactory<SimpleTuple> SIMPLE = new TupleBufferFactory<SimpleTuple>()
	{
		@Override
		public SimpleTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new SimpleTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}
		
		@Override
		public int getDataSize()
		{
			return 0;
		}

		@Override
		public SimpleTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new SimpleTuple(aKey);
		}

		@Override
		public void clearTuple(PageIOStream aStream)
		{
		}
	};
	
	public static final TupleBufferFactory<RoleTuple> ROLE = new TupleBufferFactory<RoleTuple>()
	{
		@Override
		public RoleTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new RoleTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}
		
		@Override
		public int getDataSize()
		{
			return PageIOStream.roleSize();
		}

		@Override
		public RoleTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new RoleTuple(aKey, (byte) aStream.readRole());
		}

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writeRole(0);
		}
	};
	
	public static final TupleBufferFactory<ObjectPointerTuple> OBJECT_POINTER = new TupleBufferFactory<ObjectPointerTuple>()
	{
		@Override
		public ObjectPointerTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new ObjectPointerTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}

		@Override
		public int getDataSize()
		{
			return PageIOStream.pagePointerSize()+PageIOStream.pageOffsetSize();
		}

		@Override
		public ObjectPointerTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new ObjectPointerTuple(aKey,
					aStream.readPagePointer(),
					aStream.readPageOffset());
		}

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writePagePointer(0);
			aStream.writePageOffset(0);
		}
	};
	
	public static final TupleBufferFactory<ObjectRefTuple> OBJECT_REF = new TupleBufferFactory<ObjectRefTuple>()
	{
		@Override
		public ObjectRefTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new ObjectRefTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}
		
		@Override
		public int getDataSize()
		{
			return PageIOStream.longSize();
		}

		@Override
		public ObjectRefTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new ObjectRefTuple(aKey, aStream.readLong());
		}

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writeLong(0);
		}
	};
	
	/**
	 * Creates a new buffer.
	 */
	public abstract TupleBuffer<T> create(int aSize, int aPreviousPageId, int aNextPageId);

	/**
	 * Returns the size of extra tuple data.
	 */
	public abstract int getDataSize();
	
	public abstract T readTuple(long aKey, PageIOStream aStream);
	
	public abstract void clearTuple(PageIOStream aStream);
}