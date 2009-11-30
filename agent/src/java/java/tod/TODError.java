package java.tod;

public class TODError extends Error
{
	private static final long serialVersionUID = -5418595302649555482L;

	public TODError()
	{
		super();
	}

	public TODError(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}

	public TODError(String aMessage)
	{
		super(aMessage);
	}

	public TODError(Throwable aCause)
	{
		super(aCause);
	}
}
