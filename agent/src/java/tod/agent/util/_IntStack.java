/*
 * Created on Mar 20, 2008
 */
package tod.agent.util;

/**
 * Copied from zz.utils
 * @author gpothier
 */
public class _IntStack extends _IntArray
{
	public _IntStack()
	{
	}

	public _IntStack(int aInitialSize)
	{
		super(aInitialSize);
	}

	public void push(int aValue)
	{
		set(size(), aValue);
	}
	
	public int pop()
	{
		int theValue = get(size()-1);
		setSize(size()-1);
		return theValue;
	}
	
	public int peek()
	{
		return get(size()-1);
	}
}
