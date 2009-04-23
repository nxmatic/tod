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
package tod.impl.evdb1.db.file;

import zz.utils.bit.BitStruct;

/**
 * Provides binary search of {@link IndexTuple}s in {@link Page}s.
 * @author gpothier
 */
public class TupleFinder
{
	/**
	 * Describes the possible tuple finding behavior in the case tuples
	 * with the specified key are found.
	 * @author gpothier
	 */
	public static enum Match 
	{
		/**
		 * Return the first tuple whose key matches the specified key.
		 */
		FIRST, 
		/**
		 * Returns the last tuple whose key matches the specified key
		 */
		LAST
	}
	
	/**
	 * Describes the possible tuple finding behaviors in the case no tuple
	 * with the specified key are found.
	 * @author gpothier
	 */
	public static enum NoMatch 
	{
		/**
		 * Returns the last tuple whose key is smaller than the specified key.
		 */
		BEFORE,
		/**
		 * Return the first tuple whose key is greater than the specified key.
		 */
		AFTER
	}
	
	
	public static <T> int getTuplesPerPage(
			int aPageSize, 
			int aPagePointerSize,
			TupleCodec<T> aTupleCodec)
	{
		return (aPageSize - 2*aPagePointerSize) / aTupleCodec.getTupleSize();
	}
	
	/**
	 * Finds the first tuple that verifies a condition on key.
	 * See {@link #findTupleIndex(PageBitStruct, long, tod.impl.dbgrid.dbnode.HierarchicalIndex.TupleCodec, boolean)}
	 * @return The first matching tuple, or null if no tuple matches,
	 * ie. if {@link #findTupleIndex(PageBitStruct, long, tod.impl.dbgrid.dbnode.HierarchicalIndex.TupleCodec, boolean)}
	 * returns -1.
	 */
	public static <T extends IndexTuple> T findTuple(
			BitStruct aPage, 
			int aPagePointerSize,
			long aKey, 
			TupleCodec<T> aTupleCodec,
			Match aMatchBehavior,
			NoMatch aNoMatchBehavior)
	{
		int theIndex = findTupleIndex(
				aPage, 
				aPagePointerSize, 
				aKey, 
				aTupleCodec, 
				aMatchBehavior,
				aNoMatchBehavior);
		
		if (theIndex < 0) return null;
		return readTuple(aPage, aTupleCodec, theIndex);
	}
	
	/**
	 * Binary search of tuple.
	 * @param aPagePointerSize Size in bits of page pointers for linking to next/previous pages.
	 */
	public static <T extends IndexTuple> int findTupleIndex(
			BitStruct aPage, 
			int aPagePointerSize,
			long aKey, 
			TupleCodec<T> aTupleCodec,
			Match aMatchBehavior,
			NoMatch aNoMatchBehavior)
	{
		int theTupleCount = getTuplesPerPage(aPage.getTotalBits(), aPagePointerSize, aTupleCodec);
		return findTupleIndex(
				aPage, 
				aKey, 
				aTupleCodec, 
				0, 
				theTupleCount-1, 
				aMatchBehavior,
				aNoMatchBehavior);
	}
	
	/**
	 * Binary search of tuple. 
	 * See {@link #findTupleIndex(PageBitStruct, long, tod.impl.dbgrid.dbnode.HierarchicalIndex.TupleCodec)}.
	 */
	private static <T extends IndexTuple> int findTupleIndex(
			BitStruct aPage, 
			long aKey, 
			TupleCodec<T> aTupleCodec, 
			int aFirst, 
			int aLast,
			Match aMatchBehavior,
			NoMatch aNoMatchBehavior)
	{
		assert aLast-aFirst > 0;
		
		T theFirstTuple = readTuple(aPage, aTupleCodec, aFirst);
		long theFirstKey = theFirstTuple.getKey();
		// A key value of 0 means we are on empty space at the end of the page.
		if (theFirstKey == 0) theFirstKey = Long.MAX_VALUE;
		
		T theLastTuple = readTuple(aPage, aTupleCodec, aLast);
		long theLastKey = theLastTuple.getKey();
		if (theLastKey == 0) theLastKey = Long.MAX_VALUE;
		
//		System.out.println(String.format("First  %d:%d", theFirstTimestamp, aFirst));
//		System.out.println(String.format("Last   %d:%d", theLastTimestamp, aLast));
		
		if (aKey < theFirstKey) 
		{
			switch (aNoMatchBehavior)
			{
			case BEFORE:
				assert aFirst == 0;
				return -1;
			case AFTER: return aFirst;
			default: throw new RuntimeException();
			}
		}
		
		if (aKey > theLastKey) 
		{
			switch (aNoMatchBehavior)
			{
			case BEFORE: return aLast;
			case AFTER: return -aLast-1;
			default: throw new RuntimeException();
			}
		}
		
		if (theFirstKey == theLastKey) 
		{
			switch (aMatchBehavior)
			{
			case FIRST: return aFirst;
			case LAST: return aLast;
			default: throw new RuntimeException();
			}
		}
		
		if (aLast-aFirst == 1) 
		{
			if (aKey == theFirstKey) return aFirst;
			else if (aKey == theLastKey) return aLast;
			else 
			{
				switch (aNoMatchBehavior)
				{
				case BEFORE: return aFirst;
				case AFTER: return aLast;
				default: throw new RuntimeException();
				}
			}
		}
		
		int theMiddle = (aFirst + aLast) / 2;
		T theMiddleTuple = readTuple(aPage, aTupleCodec, theMiddle);
		long theMiddleKey = theMiddleTuple.getKey();
		if (theMiddleKey == 0) theMiddleKey = Long.MAX_VALUE;
		
//		System.out.println(String.format("Middle %d:%d", theMiddleTimestamp, theMiddle));
		
		boolean theLookForward;
		
		if (aKey > theMiddleKey) theLookForward = true;
		else if (aKey < theMiddleKey) theLookForward = false;
		else
		{
			switch(aMatchBehavior)
			{
			case FIRST: 
				theLookForward = false;
				break;
			case LAST:
				theLookForward = true;
				break;
			default: throw new RuntimeException();
			}
		}
		
		if (theLookForward) 
		{
			return findTupleIndex(
					aPage, 
					aKey, 
					aTupleCodec, 
					theMiddle, 
					aLast, 
					aMatchBehavior,
					aNoMatchBehavior);
		}
		else 
		{
			return findTupleIndex(
					aPage, 
					aKey, 
					aTupleCodec, 
					aFirst, 
					theMiddle, 
					aMatchBehavior,
					aNoMatchBehavior);
		}
	}
	
	public static <T extends Tuple> T readTuple(BitStruct aPage, TupleCodec<T> aTupleCodec, int aIndex)
	{
		assert aIndex >= 0;
		aPage.setPos(aIndex * aTupleCodec.getTupleSize());
		return aTupleCodec.read(aPage);
	}


}
