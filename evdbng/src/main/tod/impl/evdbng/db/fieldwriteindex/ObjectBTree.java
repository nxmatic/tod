package tod.impl.evdbng.db.fieldwriteindex;

import tod.impl.evdbng.db.fieldwriteindex.OnDiskIndex.ObjectPageSlot;
import tod.impl.evdbng.db.file.InsertableBTree;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.Tuple;
import tod.impl.evdbng.db.file.TupleBuffer;
import tod.impl.evdbng.db.file.TupleBufferFactory;

public class ObjectBTree extends InsertableBTree<ObjectBTree.IntTuple>
{
	
	public ObjectBTree(String aName, PidSlot aRootSlot)
	{
		super(aName, aRootSlot);
	}
	
	@Override
	protected TupleBufferFactory<IntTuple> getTupleBufferFactory()
	{
		return INT_TUPLEFACTORY;
	}

	/**
	 * Returns a slot that can store a pointer to an object page.
	 * This method insert an entry for the given key if there is none.
	 */
	public ObjectPageSlot getSlot(long aKey)
	{
		PageIOStream theStream = insertLeafKey(aKey, true);
		return new ObjectPageSlot(theStream);
	}
	
	public int get(long aKey)
	{
		IntTuple theTuple = getTupleAt(aKey);
		return theTuple != null ? theTuple.getData() : 0;
	}

	public static class IntTuple extends Tuple
	{
		private final int itsData;
		
		public IntTuple(long aKey, int aData)
		{
			super(aKey);
			itsData = aData;
		}

		public int getData()
		{
			return itsData;
		}
	}
	
	public static final TupleBufferFactory<IntTuple> INT_TUPLEFACTORY = new TupleBufferFactory<IntTuple>()
	{
		@Override
		public IntTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new IntTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}
		
		@Override
		public int getDataSize()
		{
			return PageIOStream.intSize();
		}

		@Override
		public IntTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new IntTuple(aKey, aStream.readInt());
		}
	};
	
	private static class IntTupleBuffer extends TupleBuffer<IntTuple>
	{
		private int[] itsDataBuffer;
		
		public IntTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsDataBuffer = new int[aSize];
		}
		
		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsDataBuffer[aPosition] = aStream.readInt();
		}
		
		@Override
		public void write0(int aPosition, PageIOStream aStream)
		{
			aStream.writeInt(itsDataBuffer[aPosition]);
		}
		
		@Override
		public IntTuple getTuple(int aPosition)
		{
			return new IntTuple(
					getKey(aPosition), 
					itsDataBuffer[aPosition]);
		}
		
		@Override
		protected void swap(int a, int b)
		{
			swap(itsDataBuffer, a, b);
		}
	}
	

}
