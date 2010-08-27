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
	private byte[] itsValues = new byte[4096];
	private int itsSize = 0;
	
	public void push(boolean aValue)
	{
		int theLength = itsValues.length;
		if (itsSize >= theLength)
		{
			byte[] newArray = new byte[theLength*2];
			_Arrays.arraycopy(itsValues, 0, newArray, 0, theLength);
			itsValues = newArray;
		}
		itsValues[itsSize++] = aValue ? (byte) 1 : (byte) 0;
	}
	
	public boolean pop()
	{
		return itsValues[--itsSize] != 0;
	}
	
	public boolean peek()
	{
		return itsValues[itsSize-1] != 0;
	}

	
	
//	private int[] itsValues = new int[1024];
//	private int itsMask = 1 << 31;
//	private int itsIndex = -1;
//	
//	public void push(boolean aValue)
//	{
//		itsMask <<= 1;
//		
//		if (itsMask == 0)
//		{
//			itsIndex++;
//			itsMask = 1;
//		}
//		
//		int theLength = itsValues.length;
//		if (itsIndex >= theLength)
//		{
//			_IO.out("[TOD] Expanding bitstack");
//			int[] newArray = new int[theLength*2];
//			for(int i=0;i<theLength;i++) newArray[i] = itsValues[i];
//			itsValues = newArray;
//		}
//		
//		if (aValue) itsValues[itsIndex] |= itsMask;
//		else itsValues[itsIndex] &= ~itsMask;
//		
//	}
//	
//	public boolean pop()
//	{
//		boolean value = (itsValues[itsIndex] & itsMask) != 0;
//		
//		itsMask >>>= 1;
//		if (itsMask == 0)
//		{
//			itsIndex--;
//			itsMask = 1 << 31;
//		}
//		
//		return value;
//	}
//	
//	public boolean peek()
//	{
//		return (itsValues[itsIndex] & itsMask) != 0;
//	}
}
