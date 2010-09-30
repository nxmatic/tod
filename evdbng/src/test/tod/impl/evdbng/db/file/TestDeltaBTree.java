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
	
//	@Test
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
			if (random.nextInt(1000) < 2) btree.flush();
			if ((i & 0xffff) == 0) System.out.println(i);
		}
		System.out.println("sync");
		btree.flush();
		file.flush();

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
			random.nextInt(1000);
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
	
	@Test
	public void testRefill()
	{
		final PagedFile file = PagedFile.create(new File("/home/gpothier/tmp/btreebench/mine"), true);
		Page theDirectory = file.create();
		final DeltaBTree btree = new DeltaBTree("test", file, new PidSlot(theDirectory, 0));

		final int k = 1000;
		long[] theKeys = new long[k];
		int[] theValues = new int[k];
		
		for(int i=0;i<k;i++)
		{
			theKeys[i] = i;
			theValues[i] = i*2;
		}
		
		for(int i=1;i<=k;i++)
		{
			if (i == 180)
			{
				System.out.println("TestDeltaBTree.testRefill()");
			}
			btree.insertLeafTuples(theKeys, theValues, 0, i);
		}
		
		btree.flush();
		
		for(int i=0;i<k;i++)
		{
			int[] theResult = btree.getValues(theKeys[i]);
			Assert.assertEquals(k-i, theResult.length);
			Assert.assertTrue(checkValue(theResult, i*2));
		}
	}
	
	private boolean checkValue(int[] aResult, int aValue)
	{
		for(int i=0;i<aResult.length;i++) if (aResult[i] != aValue) return false;
		return true;
	}
}
