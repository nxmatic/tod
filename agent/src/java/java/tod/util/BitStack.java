/*
 * Created on Apr 30, 2009
 */
package java.tod.util;

/**
 * A stack of boolean values
 * @author gpothier
 */
public class BitStack
{
	private _BitSet itsBitSet = new _BitSet();
	private int itsSize = 0;
	
	public void push(boolean aValue)
	{
		itsBitSet.set(itsSize++, aValue);
	}
	
	public boolean pop()
	{
		if (itsSize == 0) throw new RuntimeException("Stack is empty");
		return itsBitSet.get(--itsSize); 
	}
	
	public boolean peek()
	{
		return itsBitSet.get(itsSize-1);
	}
}
