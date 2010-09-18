package tod.impl.evdbng.db.file;

import gnu.trove.TIntArrayList;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import tod.impl.evdbng.db.file.Page.PidSlot;

public class TestDeltaBTree
{
	private static final int N = 10000000;
	
	@Test
	public void test()
	{
		final PagedFile file = PagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		Page theDirectory = file.create();
		final DeltaBTree btree = new DeltaBTree("test", file, new PidSlot(theDirectory, 0));

		Random random = new Random(8);
		System.out.println("Filling...");
		long k = random.nextInt(50000);
		int v = random.nextInt(100);
		
		for(int i=0;i<N;i++)
		{
			int dk = (int) Math.log(random.nextInt());
			k += dk;
			int dv = (int) (Math.log(random.nextInt(1000000))*(random.nextInt(11)-5));
			v += dv;
//			System.out.println("("+k+", "+v+")");
			btree.insertLeafTuple(k, v);
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		System.out.println("sync");
		file.flush();
		btree.flush();

		random = new Random(8);
		System.out.println("Checking...");
		k = random.nextInt(50000);
		v = random.nextInt(100);
		long lastKey = k;
		TIntArrayList vs = new TIntArrayList();
		for(int i=0;i<N;i++)
		{
			int dk = (int) Math.log(random.nextInt());
			k += dk;
			int dv = (int) (Math.log(random.nextInt(1000000))*(random.nextInt(11)-5));
			v += dv;
			if (k != lastKey)
			{
				int[] theValues = btree.getValues(lastKey);
				if (vs.size() == 0) Assert.assertNull(theValues);
				else Assert.assertTrue(Arrays.equals(vs.toNativeArray(), theValues));
				lastKey = k;
				vs.clear();
			}
			vs.add(v);
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		
	}
}
