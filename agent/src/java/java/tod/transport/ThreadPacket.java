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
	/**
	 * Recycle queue for standard packets (all have the same length).
	 */
	public static final int RECYCLE_QUEUE_STANDARD = 0;
	
	/**
	 * Recycle queue for other packets.
	 */
	public static final int RECYCLE_QUEUE_OTHER = 1;
	public static final int RECYCLE_QUEUE_COUNT = 2;
	
	public int threadId;
	public byte[] data;
	
	/**
	 * When the packet has been sent, it will be placed on this recycle queue.
	 */
	public int recycleQueue;
	
	public int offset;
	public int length;
	
	
	public void set(int aThreadId, byte[] aData, int aRecycleQueue, int aOffset, int aLength)
	{
		threadId = aThreadId;
		data = aData;
		recycleQueue = aRecycleQueue;
		offset = aOffset;
		length = aLength;
	}

	public void set(int aThreadId, byte[] aData, int aRecycleQueue)
	{
		set(aThreadId, aData, aRecycleQueue, 0, aData.length);
	}
}
