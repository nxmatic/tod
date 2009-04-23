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
package tod.impl.evdb1.db;

import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_BYTEOFFSET_BITS;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_POINTER_BITS;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_SIZE;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.dbgrid.db.ObjectsDatabase;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.IndexTuple;
import tod.impl.evdb1.db.file.IndexTupleCodec;
import tod.impl.evdb1.db.file.HardPagedFile.Page;
import zz.utils.bit.BitStruct;

/**
 * A database for storing registered objects.
 * Each object has an associated id. It is assumed that
 * objects are sent in in their id order.
 * @author gpothier
 */
public class ObjectsDatabase1 extends ObjectsDatabase
{
	private HardPagedFile itsFile;
	private HierarchicalIndex<ObjectPointerTuple> itsindex;
	private ObjectPointerTuple itsTuple = new ObjectPointerTuple(0, 0, 0);
	
	private ByteBuffer itsByteBuffer = ByteBuffer.allocate(0);
	private IntBuffer itsIntBuffer = itsByteBuffer.asIntBuffer();
	
	/**
	 * Current data page
	 */
	private Page itsCurrentPage;
	
	/**
	 * Offset in the current page, in bytes.
	 */
	private int itsCurrentOffset;
	
	
	public ObjectsDatabase1(IMutableStructureDatabase aStructureDatabase, File aFile)
	{
		super(aStructureDatabase);
		try
		{
			itsFile = new HardPagedFile(aFile, DB_PAGE_SIZE);
			itsindex = new HierarchicalIndex<ObjectPointerTuple>(
					"[ObjectsDatabase] index",
					ObjectTupleCodec.getInstance(),
					itsFile);
			
			itsCurrentPage = itsFile.create();
			itsCurrentOffset = 0;
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		itsFile.dispose();
	}
	
	
	private void checkBufferSize(int aSize)
	{
		if (itsIntBuffer.capacity() < aSize)
		{
			itsByteBuffer = ByteBuffer.allocate(4*Math.max(itsIntBuffer.capacity()*2, aSize));
			itsByteBuffer.order(ByteOrder.nativeOrder());
			itsIntBuffer = itsByteBuffer.asIntBuffer();
		}
	}
	
	/**
	 * Stores an already-serialized object into the database.
	 */
	@Override
	protected void store0(long aId, byte[] aData)
	{
		// Add index tuple
		itsTuple.set(aId, itsCurrentPage.getPageId(), itsCurrentOffset);
		itsindex.add(itsTuple);
		
		// Store object data
		int theDataSize = aData.length;
		int theStorageSize = 1 + (theDataSize+3)/4;
		
		checkBufferSize(theStorageSize);
		
		itsIntBuffer.position(0);
		itsIntBuffer.put(theDataSize);
		itsByteBuffer.position(4);
		itsByteBuffer.put(aData);

		itsIntBuffer.position(0);
		int theRemaining = theStorageSize*4;
		while (theRemaining > 0)
		{			
			// Determine available space in current page, keeping 64 bits
			// for next-page pointer
			int theSpaceInPage = itsCurrentPage.getSize()-8-itsCurrentOffset;
			int[] thePageData = itsCurrentPage.getData();
			
			int theAmountToCopy = Math.min(theRemaining, theSpaceInPage);
			assert theAmountToCopy % 4 == 0;
			assert itsCurrentOffset % 4 == 0;
			
			itsIntBuffer.get(thePageData, itsCurrentOffset/4, theAmountToCopy/4);
			
			itsCurrentPage.modified();
			
			theRemaining -= theAmountToCopy;
			itsCurrentOffset += theAmountToCopy;
			
			if (theAmountToCopy == theSpaceInPage)
			{
				// Allocate next page
				Page theNextPage = itsFile.create();
				long thePageId = theNextPage.getPageId();
				assert itsCurrentPage.getSize()-8 == itsCurrentOffset;
				
				thePageData[itsCurrentOffset/4] = (int) (thePageId & 0xffffffff);
				thePageData[(itsCurrentOffset/4)+1] = (int) (thePageId >>> 32);
				
				itsCurrentPage = theNextPage;
				itsCurrentOffset = 0;
			}
			
			itsCurrentPage.use();
		}
	}
	
	/**
	 * Loads an object from the database.
	 * @param aId Id of the object to load.
	 */
	@Override
	public Decodable load(long aId)
	{
		ObjectPointerTuple theTuple = itsindex.getTupleAt(aId, true);
		if (theTuple == null) return null;
		
		int theOffset = theTuple.getOffset();
		Page thePage = itsFile.get(theTuple.getPageId());
		
		assert theOffset % 4 == 0;
		int theDataSize = thePage.getData()[theOffset/4];

		int theStorageSize = (theDataSize+3)/4;
		
		checkBufferSize(theStorageSize);

		theOffset += 4;
		
		itsIntBuffer.position(0);
		int theRemaining = theStorageSize*4;
		while (theRemaining > 0)
		{
			// Determine available space in current page, keeping 64 bits
			// for next-page pointer
			int theSpaceInPage = thePage.getSize()-8-theOffset;
			int[] thePageData = thePage.getData();
			
			int theAmountToCopy = Math.min(theRemaining, theSpaceInPage);
			assert theAmountToCopy % 4 == 0;
			assert theOffset % 4 == 0;
			
			if (theAmountToCopy > 0) itsIntBuffer.put(thePageData, theOffset/4, theAmountToCopy/4); 
			
			theRemaining -= theAmountToCopy;
			theOffset += theAmountToCopy;
			
			if (theRemaining > 0)
			{
				// Get next page
				assert thePage.getSize()-8 == theOffset;

				long theI1 = thePageData[theOffset/4];
				long theI2 = thePageData[(theOffset/4)+1];
				
				long thePageId = theI1 + (theI2 << 32); 
				thePage = itsFile.get(thePageId);
				theOffset = 0;
			}
		}
		
		return new Decodable(aId, false, itsByteBuffer.array());
	}
	
	@Override
	protected void registerRef0(long aId, long aClassId)
	{
		// evdb1 does not handle that
	}
	
	@Override
	protected long getObjectTypeId(long aObjectId)
	{
		// evdb1 does not handle that
		return 0;
	}

	/**
	 * Codec for {@link InternalTuple}.
	 * @author gpothier
	 */
	private static class ObjectTupleCodec extends IndexTupleCodec<ObjectPointerTuple>
	{
		private static ObjectTupleCodec INSTANCE = new ObjectTupleCodec();

		public static ObjectTupleCodec getInstance()
		{
			return INSTANCE;
		}

		private ObjectTupleCodec()
		{
		}
		
		@Override
		public int getTupleSize()
		{
			return super.getTupleSize() 
					+ DB_PAGE_POINTER_BITS
					+ DB_PAGE_BYTEOFFSET_BITS;
		}

		@Override
		public ObjectPointerTuple read(BitStruct aBitStruct)
		{
			return new ObjectPointerTuple(aBitStruct);
		}
	}
	
	
	
	private static class ObjectPointerTuple extends IndexTuple
	{
		private long itsPageId;
		private int itsOffset;
		
		public ObjectPointerTuple(long aKey, long aPageId, int aOffset)
		{
			super(aKey);
			itsPageId = aPageId;
			itsOffset = aOffset;
		}
		
		public ObjectPointerTuple(BitStruct aBitStruct)
		{
			super(aBitStruct);
			itsPageId = aBitStruct.readLong(DB_PAGE_POINTER_BITS);
			itsOffset = aBitStruct.readInt(DB_PAGE_BYTEOFFSET_BITS);
		}

		@Override
		public void writeTo(BitStruct aBitStruct)
		{
			super.writeTo(aBitStruct);
			aBitStruct.writeLong(itsPageId, DB_PAGE_POINTER_BITS);
			aBitStruct.writeInt(itsOffset, DB_PAGE_BYTEOFFSET_BITS);
		}
		
		@Override
		public int getBitCount()
		{
			return super.getBitCount() 
					+ DB_PAGE_POINTER_BITS
					+ DB_PAGE_BYTEOFFSET_BITS;
		}
		
		public void set(long aKey, long aPageId, int aOffset)
		{
			super.set(aKey);
			itsPageId = aPageId;
			itsOffset = aOffset;
		}

		public int getOffset()
		{
			return itsOffset;
		}

		public long getPageId()
		{
			return itsPageId;
		}

	}
}
