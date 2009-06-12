/*
 * Created on Jan 12, 2009
 */
package tod2.agent.io;

public class _BufferOverflowException extends RuntimeException
{
	public _BufferOverflowException()
	{
	}

	public _BufferOverflowException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}

	public _BufferOverflowException(String aMessage)
	{
		super(aMessage);
	}

	public _BufferOverflowException(Throwable aCause)
	{
		super(aCause);
	}
}
