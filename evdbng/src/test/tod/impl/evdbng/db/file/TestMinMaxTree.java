package tod.impl.evdbng.db.file;

import java.io.File;
import java.util.BitSet;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;


public class TestMinMaxTree
{
	@Test
	public void testGet()
	{
		RangeMinMaxTree tree = new RangeMinMaxTree(PagedFile.create(new File("test-rmmt.bin"), true));
		
		final int n = 10000000;
		BitSet sequence = gen(n, 100);
		
		for(int i=0;i<n;i++) 
		{
			if (sequence.get(i)) tree.open();
			else tree.close();
		}
		
		for(int i=0;i<n;i++) 
		{
			Assert.assertEquals("i: "+i, sequence.get(i), tree.get(i));
		}
	}
	
	private static BitSet gen(int n, int maxDepth)
	{
		BitSet set = new BitSet();
		Random random = new Random(8);
		
		int sum = 0;
		for(int i=0;i<n;i++)
		{
			float proba1 = 1f - 1f*sum/maxDepth;
			boolean bit = random.nextFloat() < proba1;
			
			if (bit) sum++;
			else sum--;
			
			set.set(i, bit);
		}
		
		return set;
	}
}
