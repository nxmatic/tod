/*
 * Created on Jan 13, 2009
 */
package tod.utils;

import java.nio.BufferOverflowException;
import java.tod.util._Arrays;

/**
 * A byte buffer that grows as needed
 * @author gpothier
 */
public class GrowingByteBuffer extends ByteBuffer
{
	protected GrowingByteBuffer(byte[] aBytes)
	{
		super(aBytes);
	}

	public static GrowingByteBuffer allocate(int aSize)
	{
		return new GrowingByteBuffer(new byte[aSize]);
	}

	public static GrowingByteBuffer wrap(byte[] aBytes)
	{
		return new GrowingByteBuffer(aBytes);
	}
	
	@Override
	protected void checkRemaining(int aRequested)
	{
		if (aRequested > remaining())
		{
			if (limit() != capacity()) throw new BufferOverflowException();
			byte[] theNewBuffer = new byte[Math.max(capacity()*2, remaining()+aRequested+capacity())];
			System.arraycopy(array(), 0, theNewBuffer, 0, capacity());
			_array(theNewBuffer);
			limit(theNewBuffer.length);
		}
	}
	
	
}
