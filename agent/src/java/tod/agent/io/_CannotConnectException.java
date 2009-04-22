/*
 * Created on Jan 12, 2009
 */
package tod.agent.io;

public class _CannotConnectException extends _IOException
{
	public _CannotConnectException()
	{
	}

	public _CannotConnectException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}

	public _CannotConnectException(String aMessage)
	{
		super(aMessage);
	}

	public _CannotConnectException(Throwable aCause)
	{
		super(aCause);
	}
}
