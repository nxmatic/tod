package tod.impl.evdbng.db.file;

import java.io.File;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import tod.impl.evdbng.db.file.Page.PidSlot;
import tod.impl.evdbng.db.file.mapped.MappedPagedFile;
import zz.utils.Utils;

public class TestInsertableBTree
{
	private static final int N = 10000000;
	
//	@Test
	public void testInt()
	{
		final PagedFile file = PagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		Page theDirectory = file.create();
		final IntInsertableBTree btree = new IntInsertableBTree("test", new PidSlot(theDirectory, 0));

		Random random = new Random(8);
		System.out.println("Filling...");
		long t0 = System.currentTimeMillis();
		for(int i=0;i<N;i++)
		{
			long k = random.nextLong()*2;
			int v = random.nextInt();
			btree.add(k, v);
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		System.out.println("sync");
		file.flush();
		long t1 = System.currentTimeMillis();
		Utils.println("Took %02fs.", 0.001f*(t1-t0));

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
	
	@Test
	public void testLong()
	{
		final PagedFile file = PagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		Page theDirectory = file.create();
		final LongInsertableBTree btree = new LongInsertableBTree("test", new PidSlot(theDirectory, 0));
		
		Random random = new Random(8);
		System.out.println("Filling...");
		long t0 = System.currentTimeMillis();
		for(int i=0;i<N;i++)
		{
			long k = random.nextLong()*2;
			int v = random.nextInt();
			btree.add(k, v);
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		System.out.println("sync");
		file.flush();
		long t1 = System.currentTimeMillis();
		Utils.println("Took %02fs.", 0.001f*(t1-t0));
		
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
	
}
