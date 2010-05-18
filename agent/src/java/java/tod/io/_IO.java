/*
 * Created on Jan 18, 2009
 */
package java.tod.io;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.tod.util._StringBuilder;

public class _IO
{
	/**
	 * Prints a string to the standard output stream.
	 * We cannot use System.out in the agent because it might not be initialized.
	 */
	public static native void out(String aString);
	
	/**
	 * Same as {@link #out(String)} but on the error stream.
	 */
	public static native void err(String aString);
	
	public static native void outi(String aMessage, int... aValues);	
	public static native void outb(String aMessage, boolean... aValues);

	public static void printStackTrace(Throwable aThrowable)
	{
		StringWriter theStringWriter = new StringWriter();
		PrintWriter theWriter = new PrintWriter(theStringWriter);
		aThrowable.printStackTrace(theWriter);
		err(theStringWriter.toString());
	}
	
	public static void initNatives()
	{
		out("[TOD] _IO.initNatives");
		err("[TOD] _IO.InitNatives");
	}
	
}
