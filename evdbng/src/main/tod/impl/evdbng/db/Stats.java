package tod.impl.evdbng.db;

import zz.utils.Utils;

public class Stats
{
	public static final boolean COLLECT = true;
	
	/**
	 * Number of object-field repetition during the inversion of a block
	 */
	private static int[] SUBS = new int[11];
	private static int[] SUB_ENTRIES = new int[11];
	
	public static int NO_LONGER_SINGLES = 0;
	
	public static long DELTABTREE_KEY_BITS;
	public static long DELTABTREE_VALUE_BITS;
	public static long DELTABTREE_ENTRIES_COUNT;
	public static long DELTABTREE_REFILLS;
	public static long DELTABTREE_REFILLS_AMOUNT;

	public static long RMM_BITS;
	public static long RMM_PAGES;
	
	public static long OBJECT_TREE_ENTRIES;
	
	public static Account ACC_MISC = new Account("Misc.");
	public static Account ACC_CFLOW = new Account("Control flow");
	public static Account ACC_OBJECTS = new Account("Objects");
	public static Account ACC_SNAPSHOTS = new Account("Snapshots");
	public static Account ACC_STRINGS = new Account("Strings");
	
	public static Account[] ACCOUNTS = {ACC_MISC, ACC_CFLOW, ACC_OBJECTS, ACC_SNAPSHOTS, ACC_STRINGS};
	
	public static void charge(Account aAccount)
	{
		if (COLLECT) aAccount.pages++;
	}
	
	public static void sub(int aSub)
	{
		int x = Math.min(aSub, Stats.SUBS.length-1);
		SUBS[x]++;
		SUB_ENTRIES[x] += aSub;
	}
	
	public static void print()
	{
		if (! COLLECT)
		{
			System.out.println("No stats.");
			return;
		}
		
		for(int i=0;i<SUBS.length;i++) Utils.println("Sub %d: %d (%d)", i, SUBS[i], SUB_ENTRIES[i]);
		Utils.println("No longer singles: %d", NO_LONGER_SINGLES);
		Utils.println(
				"DeltaBTree: %d entries, %d key bits (%.02f per entry), %d value bits (%.02f per entry)", 
				DELTABTREE_ENTRIES_COUNT,
				DELTABTREE_KEY_BITS,
				1f*DELTABTREE_KEY_BITS/DELTABTREE_ENTRIES_COUNT,
				DELTABTREE_VALUE_BITS,
				1f*DELTABTREE_VALUE_BITS/DELTABTREE_ENTRIES_COUNT);
		Utils.println("DeltaBTree: %d refills (%d entries)", DELTABTREE_REFILLS, DELTABTREE_REFILLS_AMOUNT);
		
		Utils.println("Object tree entries: %d", OBJECT_TREE_ENTRIES);
		
		Utils.println("RMM: %d bits, %d pages", RMM_BITS, RMM_PAGES);
		
		for(Account theAccount : ACCOUNTS)
		{
			Utils.println("Account %s: %d", theAccount.name, theAccount.pages);
		}
	}

	/**
	 * Represents accounts for page allocations
	 * @author gpothier
	 */
	public static class Account
	{
		public final String name;
		private int pages = 0;

		public Account(String aName)
		{
			name = aName;
		}
	}
}
