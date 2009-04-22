/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.evdb1;

import tod.impl.evdb1.db.IndexSet;
import tod.impl.evdb1.db.RoleIndexSet.RoleTuple;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;
import tod.impl.evdb1.db.file.IndexTuple;
import tod.impl.evdb1.queries.ArrayIndexCondition;
import tod.impl.evdb1.queries.Conjunction;
import tod.impl.evdb1.queries.ObjectCondition;
import tod.impl.evdb1.queries.SimpleCondition;
import zz.utils.bit.BitUtils;

/**
 * Abstract factory for creating splitted conditions. Splitted conditions are
 * conditions on fields that have a domain too large to fit in a single index
 * set (eg. object id, or array index). When such a field is indexes, the key is
 * split into a number of components, usually by partitioning the bits of the
 * key value, and each key part is indexed separately. For queries, the key
 * value is split in the same way and a conjunctive condition is created for
 * each key part.
 * 
 * @author gpothier
 */
public abstract class SplittedConditionHandler<T extends IndexTuple>
{
	/**
	 * Splits the given key into various key parts.
	 * For a given condition factory, this method should always return
	 * the same number of parts, for all key values. 
	 */
	protected abstract int[] splitKey(long aKey);
	
	protected abstract SimpleCondition createPartialCondition(
			int aPart,
			int aPartialKey,
			byte aRole);

	/**
	 * Creates a compound condition matching all key parts.
	 * @param aRole Optional tuple role.
	 */
	public Conjunction createCondition(long aKey, byte aRole)
	{
		Conjunction theResult = new Conjunction(true, true);
		
		int[] theParts = splitKey(aKey);
		
		for(int i=0;i<theParts.length;i++)
		{
			theResult.add(createPartialCondition(i, theParts[i], aRole));
		}
		
		return theResult;
	}
	
	/**
	 * Adds the specified tuple to all the indexes that correspond
	 * to the partition of the key.
	 */
	public void index(
			long aKey, 
			T aTuple,
			IndexSet<T>[] aIndexSets)
	{
		int[] theParts = splitKey(aKey);
		for(int i=0;i<theParts.length;i++)
		{
			aIndexSets[i].addTuple(theParts[i], aTuple);
		}
	}

	/**
	 * Whether the given part/partial key are a partial match for
	 * the given value.
	 */
	public boolean match(int aPart, int aPartialKey, long aValue)
	{
		int[] theParts = splitKey(aValue);
		return theParts[aPart] == aPartialKey;
	}
	/**
	 * A standard key partitioning method.
	 * @param aKey The key to partition.
	 * @param aPartition The number of bits to go into each
	 * partition. The sum must not exceed 64, and individual values
	 * cannot exceed 32.
	 */
	protected static int[] splitKey(long aKey, int[] aPartition)
	{
		int[] theParts = new int[aPartition.length];
		
		long theKey = aKey;
		for (int i=0;i<aPartition.length;i++)
		{
			int theBits = aPartition[i];
			
			long theMask = BitUtils.pow2(theBits)-1;
			long thePart = theKey & theMask;
			assert thePart <= Integer.MAX_VALUE;
			
			theParts[i] = (int) thePart;
			
			theKey >>>= theBits;
		}
		
		if (theKey != 0)
		{
			throw new RuntimeException("Key overflow: "+aKey);
		}
		
		return theParts;
	}
	
	public static Objects OBJECTS = new Objects();
	public static ArrayIndexes INDEXES = new ArrayIndexes();

	public static class Objects extends SplittedConditionHandler<RoleTuple>
	{
		@Override
		protected SimpleCondition createPartialCondition(int aPart, int aPartialKey, byte aRole)
		{
			return new ObjectCondition(aPart, aPartialKey, aRole);
		}

		@Override
		protected int[] splitKey(long aKey)
		{
			return splitKey(aKey, DebuggerGridConfig1.INDEX_OBJECT_PARTS);
		}
	}
	
	public static class ArrayIndexes extends SplittedConditionHandler<StdTuple>
	{

		@Override
		protected SimpleCondition createPartialCondition(int aPart, int aPartialKey, byte aRole)
		{
			return new ArrayIndexCondition(aPart, aPartialKey);
		}

		@Override
		protected int[] splitKey(long aKey)
		{
			return splitKey(aKey, DebuggerGridConfig1.INDEX_ARRAY_INDEX_PARTS);
		}
	}
}