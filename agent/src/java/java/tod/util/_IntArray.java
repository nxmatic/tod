/*
 * Created on Nov 19, 2007
 */
package java.tod.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copied from zz.utils
 * @author gpothier
 */
public class _IntArray
{
	private int[] itsData;
	private int itsSize;

	public _IntArray()
	{
		this(16);
	}
	
	public _IntArray(int aInitialSize)
	{
		itsData = new int[aInitialSize];
	}
	
	public int get(int aIndex)
	{
		return aIndex < itsSize ? itsData[aIndex] : 0;
	}
	
	public int size()
	{
		return itsSize;
	}
	
	public boolean isEmpty()
	{
		return itsSize == 0;
	}
	
	protected void setSize(int aSize)
	{
		itsSize = aSize;
	}
	
	public void set(int aIndex, int aValue)
	{
		ensureSize(aIndex+1);
		itsData[aIndex] = aValue;
		itsSize = Math.max(itsSize, aIndex+1);
	}
	
	public void clear()
	{
		itsSize = 0;
	}
	
	private void ensureSize(int aSize)
	{
		if (itsData.length >= aSize) return;
		
		int theNewSize = Math.max(aSize, itsData.length*2);
		int[] theNewData = new int[theNewSize];
		System.arraycopy(itsData, 0, theNewData, 0, itsData.length);
		itsData = theNewData;
	}

	public int[] toArray()
	{
		int[] theResult = new int[size()];
		System.arraycopy(itsData, 0, theResult, 0, size());
		return theResult;
	}
	
	/**
	 * Transforms a collection of {@link Integer}s to an array of native ints.
	 */
	public static int[] toIntArray(Collection<Integer> aCollection)
	{
		int[] theResult = new int[aCollection.size()];
		int i=0;
		for(Integer theInt : aCollection) theResult[i++] = theInt;
		return theResult;
	}
	
	public static List<Integer> toList(int[] aArray)
	{
		if (aArray == null) return null;
		List<Integer> theList = new ArrayList<Integer>();
		for(int i : aArray) theList.add(i);
		return theList;
	}
}
