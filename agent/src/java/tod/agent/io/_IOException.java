/*
 * Created on Jan 12, 2009
 */
package tod.agent.io;

public class _IOException extends Exception
{
	public _IOException()
	{
	}

	public _IOException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}

	public _IOException(String aMessage)
	{
		super(aMessage);
	}

	public _IOException(Throwable aCause)
	{
		super(aCause);
	}
}
