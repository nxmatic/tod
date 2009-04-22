/*
 * Created on Jan 12, 2009
 */
package java.tod.util;

public class _SyncRingBuffer<T> extends _RingBuffer<T>
{
	public _SyncRingBuffer(int aCapacity)
	{
		super(aCapacity);
	}

	@Override
	public synchronized void add(T aObject)
	{
		try
		{
			while(isFull()) wait();
			super.add(aObject);
			notifyAll();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized T remove()
	{
		try
		{
			while(isEmpty()) wait();
			T theResult = super.remove();
			notifyAll();
			return theResult;
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public synchronized T poll()
	{
		if (isEmpty()) return null;
		else return remove();
	}

	
}
