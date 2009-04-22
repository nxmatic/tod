/*
 * Created on Dec 14, 2008
 */
package tod.agent.util;

/**
 * A simple version of {@link ArrayList}.
 * We don't use {@link ArrayList} because it might be instrumented
 * @author gpothier
 */
public class _ArrayList<T>
{
	private T[] itsData;
	private int itsSize;

	public _ArrayList()
	{
		this(16);
	}
	
	public _ArrayList(int aInitialSize)
	{
		itsData = (T[]) new Object[aInitialSize];
	}
	
	public T get(int aIndex)
	{
		if (aIndex >= itsSize || aIndex < 0) throw new IndexOutOfBoundsException(""+aIndex+"/"+itsSize);
		return itsData[aIndex];
	}
	
	public int size()
	{
		return itsSize;
	}
	
	public boolean isEmpty()
	{
		return itsSize == 0;
	}
	
	public void add(int aIndex, T aValue)
	{
		if (aIndex > itsSize || aIndex < 0) throw new IndexOutOfBoundsException(""+aIndex+"/"+itsSize);
		ensureSize(aIndex+1);
		itsData[aIndex] = aValue;
		itsSize = Math.max(itsSize, aIndex+1);
	}
	
	public void add(T aValue)
	{
		add(size(), aValue);
	}
	
	public void clear()
	{
		itsSize = 0;
	}
	
	private void ensureSize(int aSize)
	{
		if (itsData.length >= aSize) return;
		
		int theNewSize = Math.max(aSize, itsData.length*2);
		T[] theNewData = (T[]) new Object[theNewSize];
		System.arraycopy(itsData, 0, theNewData, 0, itsData.length);
		itsData = theNewData;
	}

	public T[] toArray()
	{
		T[] theResult = (T[]) new Object[size()];
		System.arraycopy(itsData, 0, theResult, 0, size());
		return theResult;
	}
	
	public T[] toArray(T[] aDest)
	{
		System.arraycopy(itsData, 0, aDest, 0, size());
		return aDest;
	}
}
