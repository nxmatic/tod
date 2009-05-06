/*
 * Created on Jan 12, 2009
 */
package java.tod.io;

public class _EOFException extends _IOException
{
	public _EOFException()
	{
	}

	public _EOFException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}

	public _EOFException(String aMessage)
	{
		super(aMessage);
	}

	public _EOFException(Throwable aCause)
	{
		super(aCause);
	}
}
