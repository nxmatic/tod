package tod.impl.evdbng.db.file;

import tod.impl.evdbng.db.Stats.Account;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;

/**
 * An insertable BTree with int values (and long keys).
 * @author gpothier
 */
public class IntInsertableBTree extends InsertableBTree<IntInsertableBTree.IntTuple>
{
	public IntInsertableBTree(String aName, Account aAccount, PidSlot aRootSlot)
	{
		super(aName, aAccount, aRootSlot);
	}
	
	@Override
	protected TupleBufferFactory<IntTuple> getTupleBufferFactory()
	{
		return INT_TUPLEFACTORY;
	}
	
	public void add(long aKey, int aData)
	{
		PageIOStream theStream = insertLeafKey(aKey, false);
		theStream.writeInt(aData);
	}
	
	public int get(long aKey)
	{
		IntTuple theTuple = getTupleAt(aKey, true);
		return theTuple.getData();
	}
	
	public static final TupleBufferFactory<IntTuple> INT_TUPLEFACTORY = new TupleBufferFactory<IntTuple>()
	{
		@Override
		public IntInsertableBTree.IntTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
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

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writeInt(0);
		}
	};
	
	public static class IntTupleBuffer extends TupleBuffer<IntTuple>
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
}