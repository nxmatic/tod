/*
 * Created on Dec 5, 2008
 */
package tod.impl.evdbng;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

public class TestBitSplit
{
	private long unsplit(int i0, int i1)
	{
		long r = 0;
		
		long dstMask = 1;
		int srcMask = 1;
		for (int i=0;i<32;i++)
		{
			if ((i0 & srcMask) != 0) r |= dstMask;
			dstMask <<= 1;
			if ((i1 & srcMask) != 0) r |= dstMask;
			dstMask <<= 1;
			
			srcMask <<= 1;
		}
		
		return r;
	}
	
	@Test
	public void test()
	{
		Random r = new Random();
		for(int i=0;i<100;i++)
		{
			long v = r.nextInt(10000000);
			int[] c = SplittedConditionHandler.splitIndex2(v);
			System.out.println(Long.toBinaryString(v) + " - " + Integer.toBinaryString(c[0]) + " " + Integer.toBinaryString(c[1]));
			long u = unsplit(c[1], c[0]);
			System.out.println(Long.toBinaryString(u));
			Assert.assertEquals(v, u);
		}
		System.out.println("ok");
	}

}
