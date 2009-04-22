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
package tod.impl.evdbng.db.file;

import java.util.Collections;


/**
 * Provides binary search of {@link Tuple}s in {@link TupleBuffer}s.
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
	
	/**
	 * Binary search of tuple.
	 * @return A Positive (or 0) number if a match is found. If no match is found,
	 * the insertion point of the requested key is given as a negative number (-r-1)
	 * ({@link Collections#binarySearch(java.util.List, Object)}).
	 */
	public static int findTupleIndex(
			TupleBuffer<?> aTupleBuffer, 
			long aKey, 
			Match aMatchBehavior,
			NoMatch aNoMatchBehavior)
	{
		int theTupleCount = aTupleBuffer.getSize();
		if (theTupleCount == 0) throw new RuntimeException("Empty buffer");
		
		return findTupleIndex(
				aTupleBuffer, 
				aKey, 
				0, 
				theTupleCount-1, 
				aMatchBehavior,
				aNoMatchBehavior);
	}
	
	/**
	 * Binary search of tuple. 
	 */
	private static int findTupleIndex(
			TupleBuffer<?> aTupleBuffer, 
			long aKey, 
			int aFirst, 
			int aLast,
			Match aMatchBehavior,
			NoMatch aNoMatchBehavior)
	{
		assert aLast-aFirst >= 0;
		
		long theFirstKey = aTupleBuffer.getKey(aFirst);
		long theLastKey = aTupleBuffer.getKey(aLast);
		
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
		long theMiddleKey = aTupleBuffer.getKey(theMiddle);
		
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
					aTupleBuffer,
					aKey, 
					theMiddle, 
					aLast, 
					aMatchBehavior,
					aNoMatchBehavior);
		}
		else 
		{
			return findTupleIndex(
					aTupleBuffer, 
					aKey, 
					aFirst, 
					theMiddle, 
					aMatchBehavior,
					aNoMatchBehavior);
		}
	}
}
