/*
 * Created on May 23, 2009
 */
package tod2.access;

/**
 * This class is modified by the instrumenter so that it calls various generated 
 * $tod$Xxxx methods.
 * @author gpothier
 */
public class TODAccessor
{
	public static long getObjectId(Object aObject)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
	
	/**
	 * Returns the backing character array of the string.
	 * @see String#value
	 */
	public static char[] getStringChars(String aString)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");		
	}
	
	/**
	 * Returns the offset of the given string in its backing array.
	 * @see String#offset
	 */
	public static int getStringOffset(String aString)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");		
	}
	
	/**
	 * Returns the number of characters in the string.
	 * @see String#count
	 */
	public static int getStringCount(String aString)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");		
	}
	
	public static int getClassId(Class aClass)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
}
