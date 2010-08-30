/*
 * Created on May 23, 2009
 */
package tod2.access;

import java.tod.ThreadData;

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
	
	public static ThreadData getThreadData(Thread aThread)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
	
	public static void setThreadData(Thread aThread, ThreadData aThreadData)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
	
	public static char[] getThreadName(Thread aThread)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
	
	public static long getThreadId(Thread aThread)
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

	/**
	 * Sets the bootstrap flag in Object.
	 * See ClassInstrumenter#BOOTSTRAP_FLAG
	 */
	public static void setBootstrapFlag(boolean aValue)
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
	
	public static boolean getBootstrapFlag()
	{
		throw new Error("This code is supposed to be replaced by the instrumenter.");
	}
	
//	public static Object Object_clone(Object aTarget)
//	{
//		throw new Error("This code is supposed to be replaced by the instrumenter.");
//	}
	
	
}
