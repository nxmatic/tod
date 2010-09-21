package tod.impl.evdbng.db.file;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;


public class TestSortable
{
	private static final int N = 10000000;
	
	@Test
	public void test()
	{
		new InvertBlocksTask().run();
	}
	
	private static class InvertBlocksTask extends Sorter.Sortable 
	{
		private long[] itsObjectIds;
		private long[] itsBlockIds;
		private int[] itsThreadIds;

		public void run()
		{
			itsObjectIds = new long[N+1];
			itsBlockIds = new long[N+1];
			itsThreadIds = new int[N+1];

			Random theRandom = new Random(9);
			for(int i=0;i<N;i++)
			{
				itsObjectIds[i+1] = theRandom.nextInt(N*100);
				itsBlockIds[i+1] = theRandom.nextInt(N*100);
				itsThreadIds[i+1] = theRandom.nextInt(N*100);
			}
			
			Sorter.sort(this, 0, N);

			long theLastObjectId = itsObjectIds[1];
			long theLastBlockId = itsBlockIds[1];
			int theLastThreadId = itsThreadIds[1];
			
			for(int i=1;i<N;i++)
			{
				Assert.assertTrue(compare(i-1, i) <= 0);
				if (itsObjectIds[i+1] > theLastObjectId)
				{
					theLastObjectId = itsObjectIds[i+1];
					theLastBlockId = itsBlockIds[i+1];
					theLastThreadId = itsThreadIds[i+1];
				}
				else
				{
					Assert.assertEquals(theLastObjectId, itsObjectIds[i+1]);
					if (itsBlockIds[i+1] > theLastBlockId)
					{
						theLastBlockId = itsBlockIds[i+1];
						theLastThreadId = itsThreadIds[i+1];
					}
					else
					{
						Assert.assertEquals(theLastBlockId, itsBlockIds[i+1]);
						if (itsThreadIds[i+1] > theLastThreadId)
						{
							theLastThreadId = itsThreadIds[i+1];
						}
						else
						{
							Assert.assertEquals(theLastThreadId, itsThreadIds[i+1]);
						}
					}
				}
			}
		}

		@Override
		protected void setPivot(int aIndex)
		{
			itsObjectIds[0] = itsObjectIds[aIndex+1];
			itsBlockIds[0] = itsBlockIds[aIndex+1];
			itsThreadIds[0] = itsThreadIds[aIndex+1];
		}

		@Override
		protected int compare(int a, int b)
		{
			long oa = itsObjectIds[a+1];
			long ob = itsObjectIds[b+1];
			
			if (oa > ob) return 1;
			else if (oa < ob) return -1;
			else
			{
				long ba = itsBlockIds[a+1];
				long bb = itsBlockIds[b+1];

				if (ba > bb) return 1;
				else if (ba < bb) return -1;
				else
				{
					int ta = itsThreadIds[a+1];
					int tb = itsThreadIds[b+1];
					return ta - tb;
				}
			}
		}

		@Override
		protected void swap(int a, int b)
		{
			assert a != PIVOT;
			assert b != PIVOT;
			Sorter.swap(itsObjectIds, a+1, b+1);
			Sorter.swap(itsBlockIds, a+1, b+1);
			Sorter.swap(itsThreadIds, a+1, b+1);
		}
	}

}
