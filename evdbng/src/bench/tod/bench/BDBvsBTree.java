package tod.bench;

import java.io.File;

import tod.BenchBase;
import tod.BenchBase.BenchResults;
import tod.impl.evdbng.db.file.InsertableBTree;
import tod.impl.evdbng.db.file.Page;
import tod.impl.evdbng.db.file.Page.PageIOStream;
import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.StaticBTree;
import tod.impl.evdbng.db.file.Tuple;
import tod.impl.evdbng.db.file.TupleBuffer;
import tod.impl.evdbng.db.file.TupleBufferFactory;
import tod.impl.evdbng.db.file.mapped.MappedPagedFile;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class BDBvsBTree
{
	private static final int N = 10000000;
	private static final int[] KEY_INC = {2, 4, 10, 3, 4877, 78, 1, 1, 1, 1, 300, 766, 2, 4 , 78};
	private static final int[] VALUES = {5, 6, 7, 8, 9, 3, 4, 5, 6, 7, 8, 9, 1, 0, 4, 3, 5};
	
	public static void main(String[] args)
	{
//		mine_static();
		mine_insertable();
//		bdb();
	}
	
	private static void bdb()
	{
		// Open the environment. Allow it to be created if it does not 
		// already exist.
	    EnvironmentConfig envConfig = new EnvironmentConfig();
	    envConfig.setAllowCreate(true);
	    Environment env = new Environment(new File("/home/gpothier/tmp/btreebench/bdb"), envConfig);
	    
	    // Open the database. Create it if it does not already exist.
	    DatabaseConfig dbConfig = new DatabaseConfig();
	    dbConfig.setAllowCreate(true);
	    dbConfig.setDeferredWrite(true);
		final Database db = env.openDatabase(null, "test", dbConfig);
		
		final BDBLongTupleBinding kbind = new BDBLongTupleBinding();
		final BDBIntTupleBinding vbind = new BDBIntTupleBinding();
		final BDBLongTuple ktuple = new BDBLongTuple();
		final BDBIntTuple vtuple = new BDBIntTuple();
		final DatabaseEntry keyEntry = new DatabaseEntry();
		final DatabaseEntry valueEntry = new DatabaseEntry();
		
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int ki = 0;
				int vi = 0;
				long key = 0;
				for(int i=0;i<N;i++)
				{
					ktuple.data = key;
					kbind.objectToEntry(ktuple, keyEntry);
					vtuple.data = VALUES[vi];
					vbind.objectToEntry(vtuple, valueEntry);
					db.put(null, keyEntry, valueEntry);
					
					key += KEY_INC[ki];
					
					ki++;
					if (ki >= KEY_INC.length) ki = 0;
					
					vi++;
					if (vi >= VALUES.length) vi = 0;
					
					if ((i & 0xffff) == 0) System.out.println(i);
				}
				
				System.out.println("sync");
				db.sync();
			}
		});
		
		System.out.println("BDB: "+b);
	}
	
	private static class BDBLongTuple 
	{
		public long data;
	}
	
	private static class BDBIntTuple 
	{
		public int data;
	}
	
	private static class BDBLongTupleBinding extends TupleBinding<BDBLongTuple>
	{
		@Override
		public BDBLongTuple entryToObject(TupleInput ti)
		{
			throw new RuntimeException();
		}

		@Override
		public void objectToEntry(BDBLongTuple obj, TupleOutput to)
		{
			to.writeLong(obj.data);
		}
	}
	
	private static class BDBIntTupleBinding extends TupleBinding<BDBIntTuple>
	{
		@Override
		public BDBIntTuple entryToObject(TupleInput ti)
		{
			throw new RuntimeException();
		}
		
		@Override
		public void objectToEntry(BDBIntTuple obj, TupleOutput to)
		{
			to.writeInt(obj.data);
		}
	}
	
	private static void mine_static()
	{
		final PagedFile file = PagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		final IntStaticBTree btree = new IntStaticBTree("test", file);
		
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int ki = 0;
				int vi = 0;
				long key = 0;
				for(int i=0;i<N;i++)
				{
					btree.add(key, VALUES[vi]);
					
					key += KEY_INC[ki];
					
					ki++;
					if (ki >= KEY_INC.length) ki = 0;
					
					vi++;
					if (vi >= VALUES.length) vi = 0;
					
					if ((i & 0xffff) == 0) System.out.println(i);
				}
				
				System.out.println("sync");
				file.flush();
			}
		});
		
		System.out.println("Mine (static): "+b);

	}
	
	private static void mine_insertable()
	{
		final PagedFile file = MappedPagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		Page theDirectory = file.create();
		final IntInsertableBTree btree = new IntInsertableBTree("test", file, new PidSlot(theDirectory, 0));
		
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int ki = 0;
				int vi = 0;
				long key = 0;
				for(int i=0;i<N;i++)
				{
					btree.add(key, VALUES[vi]);
					
					key += KEY_INC[ki];
					
					ki++;
					if (ki >= KEY_INC.length) ki = 0;
					
					vi++;
					if (vi >= VALUES.length) vi = 0;
					
					if ((i & 0xffff) == 0) System.out.println(i);
				}
				
				System.out.println("sync");
				file.flush();
			}
		});
		
		System.out.println("Mine (insertable): "+b);
		
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

		@Override
		public void clearTuple(PageIOStream aStream)
		{
			aStream.writeInt(0);
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
	
	private static class IntStaticBTree extends StaticBTree<IntTuple>
	{
		public IntStaticBTree(String aName, PagedFile aFile)
		{
			super(aName, aFile);
		}

		@Override
		protected TupleBufferFactory<IntTuple> getTupleBufferFactory()
		{
			return INT_TUPLEFACTORY;
		}
		
		public void add(long aKey, int aData)
		{
			PageIOStream theStream = addLeafKey(aKey);
			theStream.writeInt(aData);
		}
	}
	
	private static class IntInsertableBTree extends InsertableBTree<IntTuple>
	{
		public IntInsertableBTree(String aName, PagedFile aFile, PidSlot aRootSlot)
		{
			super(aName, aRootSlot);
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
	}
	
	


}
