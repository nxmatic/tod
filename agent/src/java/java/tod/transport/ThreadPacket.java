/*
 * Created on May 22, 2009
 */
package java.tod.transport;

import java.tod.ThreadData;

/**
 * A data packet that was created by a thread.
 * The range of valid data is indicated by {@link #offset}
 * and {@link #length}. By default, the whole data is valid,
 * but sometimes the {@link IOThread} can request {@link ThreadData} to
 * send all available data. In that case, partial packets are sent. 
 * @author gpothier
 */
public class ThreadPacket
{
	public int threadId;
	public byte[] data;
	public boolean recyclable;
	
	public int offset;
	public int length;
	
	
	public ThreadPacket(int aThreadId, byte[] aData, boolean aRecyclable, int aOffset, int aLength)
	{
		threadId = aThreadId;
		data = aData;
		recyclable = aRecyclable;
		offset = aOffset;
		length = aLength;
	}

	public ThreadPacket(int aThreadId, byte[] aData, boolean aRecyclable)
	{
		this(aThreadId, aData, aRecyclable, 0, aData.length);
	}
}
