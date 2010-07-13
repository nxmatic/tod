/*
 * Created on Dec 14, 2008
 */
package java.tod.util;

import java.tod.gnu.trove.TIntProcedure;
import java.tod.gnu.trove.TObjectProcedure;

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
		if (aIndex < 0) throw new IndexOutOfBoundsException(""+aIndex+"/"+itsSize);
		if (aIndex >= itsSize) return null;
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
//		if (aIndex > itsSize || aIndex < 0) throw new IndexOutOfBoundsException(""+aIndex+"/"+itsSize);
		ensureSize(aIndex+1);
		itsData[aIndex] = aValue;
		itsSize = Math.max(itsSize, aIndex+1);
	}
	
	public int indexOf(T aValue)
	{
		for(int i=0;i<itsSize;i++) if (itsData[i] == aValue) return i;
		return -1;
	}
	
	public boolean remove(T aValue)
	{
		int theIndex = indexOf(aValue);
		if (theIndex == -1) return false;
		remove(theIndex);
		return true;
	}
	
	public void remove(int aIndex)
	{
		_Arrays.arraycopy(itsData, aIndex+1, itsData, aIndex, itsSize-aIndex-1);
		itsSize--;
	}
	
	public void add(T aValue)
	{
		add(size(), aValue);
	}
	
	public T removeLast()
	{
		if (itsSize == 0) throw new IndexOutOfBoundsException("Empty");
		itsSize--;
		return itsData[itsSize];
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
		_Arrays.arraycopy(itsData, 0, theNewData, 0, itsData.length);
		itsData = theNewData;
	}

	public T[] toArray()
	{
		T[] theResult = (T[]) new Object[size()];
		_Arrays.arraycopy(itsData, 0, theResult, 0, size());
		return theResult;
	}
	
	public T[] toArray(T[] aDest)
	{
		_Arrays.arraycopy(itsData, 0, aDest, 0, size());
		return aDest;
	}
	
    /**
     * Applies the procedure to each value in the list in ascending
     * (front to back) order.
     *
     * @param procedure a <code>TIntProcedure</code> value
     * @return true if the procedure did not terminate prematurely.
     */
    public boolean forEach(TObjectProcedure<T> procedure) {
        for (int i = 0; i < itsSize; i++) {
            if (! procedure.execute(itsData[i])) {
                return false;
            }
        }
        return true;
    }

}
