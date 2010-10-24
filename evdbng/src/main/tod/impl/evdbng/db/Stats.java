package tod.impl.evdbng.db;

import zz.utils.Utils;

public class Stats
{
	public static final boolean COLLECT = true;
	
	/**
	 * Number of object-field repetition during the inversion of a block
	 */
	public static int[] SUB_STATS = new int[11];
	
	public static int NO_LONGER_SINGLES = 0;
	
	public static long DELTABTREE_KEY_BITS;
	public static long DELTABTREE_VALUE_BITS;
	public static long DELTABTREE_ENTRIES_COUNT;
	public static long DELTABTREE_REFILLS;
	public static long DELTABTREE_REFILLS_AMOUNT;

	public static long RMM_BITS;
	public static long RMM_PAGES;
	
	public static void print()
	{
		if (! COLLECT)
		{
			System.out.println("No stats.");
			return;
		}
		
		for(int i=0;i<SUB_STATS.length;i++) Utils.println("Sub %d: %d", i, SUB_STATS[i]);
		Utils.println("No longer singles: %d", NO_LONGER_SINGLES);
		Utils.println(
				"DeltaBTree: %d entries, %d key bits (%.02f per entry), %d value bits (%.02f per entry)", 
				DELTABTREE_ENTRIES_COUNT,
				DELTABTREE_KEY_BITS,
				1f*DELTABTREE_KEY_BITS/DELTABTREE_ENTRIES_COUNT,
				DELTABTREE_VALUE_BITS,
				1f*DELTABTREE_VALUE_BITS/DELTABTREE_ENTRIES_COUNT);
		Utils.println("DeltaBTree: %d refills (%d entries)", DELTABTREE_REFILLS, DELTABTREE_REFILLS_AMOUNT);
		
		Utils.println("RMM: %d bits, %d pages", RMM_BITS, RMM_PAGES);
	}

}
