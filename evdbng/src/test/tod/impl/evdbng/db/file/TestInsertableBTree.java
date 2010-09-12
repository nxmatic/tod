package tod.impl.evdbng.db.file;

import java.io.File;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;

public class TestInsertableBTree
{
	private static final int N = 10000000;
	
	@Test
	public void test()
	{
		final PagedFile file = PagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		Page theDirectory = file.create();
		final IntInsertableBTree btree = new IntInsertableBTree("test", file, new PidSlot(theDirectory, 0));

		Random random = new Random(8);
		System.out.println("Filling...");
		for(int i=0;i<N;i++)
		{
			long k = random.nextLong()*2;
			int v = random.nextInt();
			btree.add(k, v);
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		System.out.println("sync");
		file.flush();

		random = new Random(8);
		System.out.println("Checking...");
		for(int i=0;i<N;i++)
		{
			long k = random.nextLong()*2;
			int v = random.nextInt();
			Assert.assertEquals(v, btree.get(k));
			Assert.assertNull(btree.getTupleAt(k+1));
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		
	}
	
	private static class IntTuple extends Tuple
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
	
	private static class IntInsertableBTree extends InsertableBTree<IntTuple>
	{
		public IntInsertableBTree(String aName, PagedFile aFile, PidSlot aRootSlot)
		{
			super(aName, aFile, aRootSlot);
		}
		
		@Override
		protected TupleBufferFactory<IntTuple> getTupleBufferFactory()
		{
			return INT_TUPLEFACTORY;
		}
		
		public void add(long aKey, int aData)
		{
			PageIOStream theStream = insertLeafKey(aKey);
			theStream.writeInt(aData);
		}
		
		public int get(long aKey)
		{
			IntTuple theTuple = getTupleAt(aKey);
			return theTuple.getData();
		}
	}
}
