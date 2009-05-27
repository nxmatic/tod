/*
 * Created on Nov 19, 2007
 */
package java.tod.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author gpothier
 */
public class _ByteArray
{
	private byte[] itsData;
	private int itsSize;

	public _ByteArray()
	{
		this(16);
	}
	
	public _ByteArray(int aInitialSize)
	{
		itsData = new byte[aInitialSize];
	}
	
	public byte get(int aIndex)
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
	
	public void set(int aIndex, byte aValue)
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
		byte[] theNewData = new byte[theNewSize];
		System.arraycopy(itsData, 0, theNewData, 0, itsData.length);
		itsData = theNewData;
	}

	public byte[] toArray()
	{
		byte[] theResult = new byte[size()];
		System.arraycopy(itsData, 0, theResult, 0, size());
		return theResult;
	}
}
