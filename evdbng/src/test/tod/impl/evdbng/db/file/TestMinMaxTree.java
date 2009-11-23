package tod.impl.evdbng.db.file;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tod.BenchBase;

/**
 * Tests for the {@link RangeMinMaxTree}.
 * Set {@link PagedFile#PAGE_SIZE} to a lower value to run these tests
 * in a reasonable time.
 * @author gpothier
 */
public class TestMinMaxTree
{
	private static final File FILE = new File("test-rmmt.bin");
	
	//@Test
	public void testGet()
	{
		RangeMinMaxTree tree = new RangeMinMaxTree(PagedFile.create(FILE, true));
		
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
	
	private static void toPage(BitSet aBitSet, Page aPage)
	{
		int j=0;
		for(int i=0;i<PagedFile.PAGE_SIZE/4;i++)
		{
			int packet = 0;
			for(int mask = 0x80000000;mask != 0;mask >>>= 1) if (aBitSet.get(j++)) packet |= mask;
			aPage.writeInt(i*4, packet);
		}
	}
	
	public static void main(String[] args)
	{
		PagedFile file = PagedFile.create(FILE, true);
		final RangeMinMaxTree tree = new RangeMinMaxTree(file);
		final Page page = new ByteArrayPage(1);
		final BitSet bits = new BitSet();
		
		final int size = PagedFile.PAGE_SIZE*8;
		
		// All 1s
		for(int i=0;i<size;i++) bits.set(i, true);
		
		toPage(bits, page);
		
		BenchBase.printBench("ref", new Runnable()
		{
			public void run()
			{
				for(int i=0;i<1/*size*/;i++) for(int v=-size-1;v<=size+1;v++)
				{
					fwdsearch(bits, size, π, i, v);
				}
			}
		});
		
		BenchBase.printBench("test", new Runnable()
		{
			public void run()
			{
				for(int i=0;i<1/*size*/;i++) for(int v=-size-1;v<=size+1;v++)
				{
					tree._test_fwdsearch_π(page, i, v);
				}
			}
		});
	}
	
//	@Test
	public void testSearch_SamePage()
	{
		PagedFile file = PagedFile.create(FILE, true);
		RangeMinMaxTree tree = new RangeMinMaxTree(file);
		Page page = new ByteArrayPage(1);
		BitSet bits = new BitSet();
		
		int size = PagedFile.PAGE_SIZE*8;
		
		// All 1s
		for(int i=0;i<size;i++) bits.set(i, true);
		testAllSearches_SamePage_π(bits, size, tree, page);
		
		// All 0s
		for(int i=0;i<size;i++) bits.set(i, true);
		testAllSearches_SamePage_π(bits, size, tree, page);
		
		// (10)*
		for(int i=0;i<size;i++) bits.set(i, i%2 == 0);
		testAllSearches_SamePage_π(bits, size, tree, page);
		
		// 1s until half, then 0s
		for(int i=0;i<size/2;i++) bits.set(i, true);
		for(int i=size/2;i<size;i++) bits.set(i, false);
		testAllSearches_SamePage_π(bits, size, tree, page);

		// random
		bits = gen(size, 20);
		testAllSearches_SamePage_π(bits, size, tree, page);
	}
	
	private void testAllSearches_SamePage_π(BitSet bits, int size, RangeMinMaxTree tree, Page page)
	{
		toPage(bits, page);
		for(int i=0;i<size;i++)
		{
//			if (i < 8) continue;
			for(int v=-size-1;v<=size+1;v++)
			{
//				if (v < 2) continue;
				try
				{
					int ref;
					int test;
					
					// Forward search
					ref = fwdsearch(bits, size, π, i, v);
					test = tree._test_fwdsearch_π(page, i, v);
					Assert.assertEquals("("+i+", "+v+")", ref, test);
					
					// Backward search
					ref = bwdsearch(bits, π, i, v);
					test = tree._test_bwdsearch_π(page, i, v);
					Assert.assertEquals("("+i+", "+v+")", ref, test);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					Assert.fail("("+i+", "+v+")");
				}
			}
			System.out.println(i);
		}
	}
	
	@Test
	public void testSearch()
	{
		BitSet bits = new BitSet();

		int nLevels = 3;
		int nInternal = (int) Math.pow(PagedFile.PAGE_SIZE/10, nLevels-1);
		int size = nInternal*PagedFile.PAGE_SIZE*8;
		
		// All 1s
		for(int i=0;i<size;i++) bits.set(i, true);
		testAllSearches_π(bits, size);
		
		// All 0s
		for(int i=0;i<size;i++) bits.set(i, true);
		testAllSearches_π(bits, size);
		
		// (10)*
		for(int i=0;i<size;i++) bits.set(i, i%2 == 0);
		testAllSearches_π(bits, size);
		
		// 1s until half, then 0s
		for(int i=0;i<size/2;i++) bits.set(i, true);
		for(int i=size/2;i<size;i++) bits.set(i, false);
		testAllSearches_π(bits, size);

		// random
		bits = gen(size, 20);
		testAllSearches_π(bits, size);

	}
	
	private void testAllSearches_π(BitSet bits, int size)
	{
		PagedFile file = PagedFile.create(FILE, true);
		RangeMinMaxTree tree = new RangeMinMaxTree(file);
		
		for(int i=0;i<size;i++) if (bits.get(i)) tree.open(); else tree.close();
		
		for(int i=0;i<size;i++)
		{
			if (i < 256) continue;
			for(int v=-size-1;v<=size+1;v++)
			{
				if (v < 2) continue;
				try
				{
					int ref;
					long test;
					
					// Forward search
					ref = fwdsearch(bits, size, π, i, v);
					test = tree._test_fwdsearch_π(i, v);
					if (ref < 0) Assert.assertTrue("("+i+", "+v+")", test == -1);
					else Assert.assertEquals("("+i+", "+v+")", ref, test);

					// Backward search
					ref = bwdsearch(bits, π, i, v);
					test = tree._test_bwdsearch_π(i, v);
					if (ref < 0) Assert.assertTrue("("+i+", "+v+")", test == -1);
					else Assert.assertEquals("("+i+", "+v+")", ref, test);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					Assert.fail("("+i+", "+v+")");
				}
			}
			System.out.println(i);
		}
	}
	

	
	private static int fwdsearch(BitSet aBitSet, int size, Func g, int i, int d)
	{
		int sum = 0;
		while(i < size)
		{
			boolean bit = aBitSet.get(i);
			sum += g.apply(bit);
			if (sum == d) return i;
			
			i++;
		}
		return 0x80000000 | sum;
	}
	
	private static int bwdsearch(BitSet aBitSet, Func g, int i, int d)
	{
		int sum = 0;
		while(i >= 0)
		{
			boolean bit = aBitSet.get(i);
			sum += g.apply(bit);
			if (sum == d) return i;
			
			i--;
		}
		return 0x80000000 | sum;
	}
	
	private static class Func
	{
		private final int val0; 
		private final int val1;
		
		public Func(int aVal0, int aVal1)
		{
			val0 = aVal0;
			val1 = aVal1;
		}

		public int apply(boolean aBit)
		{
			return aBit ? val1 : val0;
		}
	}
	
	private static final Func π = new Func(-1, 1);
	private static final Func ψ = new Func(1, 0);
	private static final Func Φ = new Func(0, 1);
	
	private static class ByteArrayPage extends Page
	{
		private ByteBuffer itsBuffer = ByteBuffer.allocate(PagedFile.PAGE_SIZE);

		public ByteArrayPage(int aPageId)
		{
			super(aPageId);
			itsBuffer.order(ByteOrder.nativeOrder());
		}

		@Override
		public void free()
		{
		}

		@Override
		public PagedFile getFile()
		{
			throw new NotImplementedException();
		}

		@Override
		public boolean readBoolean(int aPosition)
		{
			return itsBuffer.get(aPosition) != 0;
		}

		@Override
		public byte readByte(int aPosition)
		{
			return itsBuffer.get(aPosition);
		}

		@Override
		public void readBytes(int aPosition, byte[] aBuffer, int aOffset, int aCount)
		{
			throw new NotImplementedException();
		}

		@Override
		public int readInt(int aPosition)
		{
			return itsBuffer.getInt(aPosition);
		}

		@Override
		public long readLong(int aPosition)
		{
			throw new NotImplementedException();
		}

		@Override
		public short readShort(int aPosition)
		{
			return itsBuffer.getShort(aPosition);
		}

		@Override
		public void use()
		{
		}

		@Override
		public void writeBB(int aPosition, int aByte1, int aByte2)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeBI(int aPosition, int aByte, int aInt)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeBL(int aPosition, int aByte, long aLong)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeBoolean(int aPosition, boolean aValue)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeBS(int aPosition, int aByte, int aShort)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeByte(int aPosition, int aValue)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeBytes(int aPosition, byte[] aBytes, int aOffset, int aCount)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeInt(int aPosition, int aValue)
		{
			itsBuffer.putInt(aPosition, aValue);
		}

		@Override
		public void writeInternalTupleData(int aPosition, int aPageId, long aTupleCount)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeLong(int aPosition, long aValue)
		{
			throw new NotImplementedException();
		}

		@Override
		public void writeShort(int aPosition, int aValue)
		{
			itsBuffer.putShort(aPosition, (short) aValue);
		}

		@Override
		public void writeSSSI(int aPosition, short aShort1, short aShort2, short aShort3, int aInt)
		{
			itsBuffer.putShort(aPosition, aShort1);
			aPosition += 2;
			itsBuffer.putShort(aPosition, aShort2);
			aPosition += 2;
			itsBuffer.putShort(aPosition, aShort3);
			aPosition += 2;
			itsBuffer.putInt(aPosition, aInt);
		}
	}
}
