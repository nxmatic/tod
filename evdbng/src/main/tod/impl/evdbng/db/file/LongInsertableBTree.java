package tod.impl.evdbng.db.file;

import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;

/**
 * An insertable BTree with long values (and long keys).
 * @author gpothier
 */
public class LongInsertableBTree extends InsertableBTree<LongInsertableBTree.LongTuple>
{
	public LongInsertableBTree(String aName, PidSlot aRootSlot)
	{
		super(aName, aRootSlot);
	}
	
	@Override
	protected TupleBufferFactory<LongTuple> getTupleBufferFactory()
	{
		return LONG_TUPLEFACTORY;
	}
	
	public void add(long aKey, long aData)
	{
		PageIOStream theStream = insertLeafKey(aKey, false);
		theStream.writeLong(aData);
	}
	
	public long get(long aKey)
	{
		LongTuple theTuple = getTupleAt(aKey);
		return theTuple.getData();
	}
	
	public static final TupleBufferFactory<LongTuple> LONG_TUPLEFACTORY = new TupleBufferFactory<LongTuple>()
	{
		@Override
		public LongInsertableBTree.LongTupleBuffer create(int aSize, int aPreviousPageId, int aNextPageId)
		{
			return new LongTupleBuffer(aSize, aPreviousPageId, aNextPageId);
		}
		
		@Override
		public int getDataSize()
		{
			return PageIOStream.longSize();
		}

		@Override
		public LongTuple readTuple(long aKey, PageIOStream aStream)
		{
			return new LongTuple(aKey, aStream.readLong());
		}

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writeLong(0);
		}
	};
	
	public static class LongTupleBuffer extends TupleBuffer<LongTuple>
	{
		private long[] itsDataBuffer;
		
		public LongTupleBuffer(int aSize, int aPreviousPageId, int aNextPageId)
		{
			super(aSize, aPreviousPageId, aNextPageId);
			itsDataBuffer = new long[aSize];
		}
		
		@Override
		public void read0(int aPosition, PageIOStream aStream)
		{
			itsDataBuffer[aPosition] = aStream.readLong();
		}
		
		@Override
		public void write0(int aPosition, PageIOStream aStream)
		{
			aStream.writeLong(itsDataBuffer[aPosition]);
		}
		
		@Override
		public LongTuple getTuple(int aPosition)
		{
			return new LongTuple(
					getKey(aPosition), 
					itsDataBuffer[aPosition]);
		}
		
		@Override
		protected void swap(int a, int b)
		{
			swap(itsDataBuffer, a, b);
		}
	}
	
	public static class LongTuple extends Tuple
	{
		private final long itsData;
		
		public LongTuple(long aKey, long aData)
		{
			super(aKey);
			itsData = aData;
		}

		public long getData()
		{
			return itsData;
		}
	}
}