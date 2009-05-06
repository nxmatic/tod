package java.tod.transport;

import java.tod.io._ByteBuffer;
import java.tod.io._IO;

import tod.agent.AgentConfig;
import tod.agent.AgentDebugFlags;

/**
 * Emulates a {@link DataOutputStream} to which event packets can be sent.
 * Uses double buffering to handle sending of buffers.
 * @author gpothier
 */
public class PacketBuffer 
{
	private final IOThread itsIOThread;
	
	/**
	 * Id of the thread that uses this buffer.
	 */
	private final int itsThreadId;
	
	/**
	 * The buffer currently written to.
	 * When this buffer is full, buffers are swapped. See {@link #swapBuffers()}
	 */
	private _ByteBuffer itsCurrentBuffer;
	
	/**
	 * The "reserve" buffer.
	 */
	private _ByteBuffer itsOtherBuffer;
	
	/**
	 * The buffer that is pending to be sent, if any.
	 */
	private _ByteBuffer itsPendingBuffer;
	
	/**
	 * True if the pending buffer starts with a new packet
	 */
	private boolean itsPendingCleanStart = true;
	
	/**
	 * True if the pending buffer ends at the end of a packet.
	 */
	private boolean itsPendingCleanEnd = true;
	
	private boolean itsCurrentCleanStart = true;
	private boolean itsCurrentCleanEnd = true;
	
	PacketBuffer(IOThread aIOThread, int aThreadId)
	{
		itsIOThread = aIOThread;
		itsThreadId = aThreadId;
		
		itsCurrentBuffer = _ByteBuffer.allocate(AgentConfig.COLLECTOR_BUFFER_SIZE);
		itsOtherBuffer = _ByteBuffer.allocate(AgentConfig.COLLECTOR_BUFFER_SIZE);
	}

	public int getThreadId()
	{
		return itsThreadId;
	}

	public _ByteBuffer getPendingBuffer()
	{
		return itsPendingBuffer;
	}

	public boolean hasCleanStart()
	{
		return itsPendingCleanStart;
	}

	public boolean hasCleanEnd()
	{
		return itsPendingCleanEnd;
	}

	/**
	 * Remaining bytes in the current buffer.
	 */
	private int remaining()
	{
		return itsCurrentBuffer.remaining();
	}
	
	/**
	 * This method is called periodically to prevent data from being held in buffers
	 * for too long when a thread is inactive.
	 * If this buffer has not been swapped for a long time, this method swaps it.
	 */
	public void pleaseSwap()
	{
		if (itsCurrentBuffer.position() == 0) return;
		synchronized (this)
		{
			if (itsPendingBuffer == null) swapBuffers();
		}
	}
	
	/**
	 * Sends the content of the current buffer and swaps the buffers.
	 * This method might have to wait for the buffer to be sent.
	 */
	public synchronized void swapBuffers()
	{
		try
		{
			while (itsPendingBuffer != null) wait();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		
		// Another thread might have called swapBuffers 
		// during the above wait.
		if (itsCurrentBuffer.position() == 0) return;
		
		itsPendingBuffer = itsCurrentBuffer;
		itsPendingCleanStart = itsCurrentCleanStart;
		itsPendingCleanEnd = itsCurrentCleanEnd;
		
		itsCurrentBuffer = itsOtherBuffer;
		itsCurrentCleanStart = true;
		itsCurrentCleanEnd = true;
		
		itsOtherBuffer = null;
		itsIOThread.pushBuffer(this);
	}
	
	public synchronized void sent()
	{
		itsOtherBuffer = itsPendingBuffer;
		itsPendingBuffer = null;
		notifyAll();
	}

	public synchronized void write(byte[] aBuffer, int aLength, boolean aCanSplit)
	{
		int theOffset = 0;
		
		// Note: we don't want to split small packets, hence the second condition
		if (! aCanSplit || aLength <= AgentConfig.COLLECTOR_BUFFER_SIZE)
		{
			if (remaining() < aLength) swapBuffers();
			IOThread._assert (remaining() >= aLength);
		}
		
		if (remaining() >= aLength)
		{
			// The packet will not be split
			itsCurrentBuffer.put(aBuffer, theOffset, aLength);
			if (remaining() == 0) swapBuffers();
		}
		else
		{
			// The packet is split
			if (AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG) 
				_IO.out("[TOD-PacketBuffer] Starting long packet for thread "+itsThreadId+" ("+aLength+" bytes)");
			
			while (aLength > 0)
			{
				int theCount = Math.min(aLength, remaining());
				if (theCount > 0)
				{
					itsCurrentBuffer.put(aBuffer, theOffset, theCount);
					theOffset += theCount;
					aLength -= theCount;
				}

				if (AgentDebugFlags.TRANSPORT_LONGPACKETS_LOG) 
					_IO.out("[TOD-PacketBuffer] Long packet for thread "+itsThreadId+": sent "+theCount+" bytes");

				if (aLength > 0) itsCurrentCleanEnd = false;
				swapBuffers(); // Swap anyway here - we want to start a fresh packet after the long one.
				if (aLength > 0) itsCurrentCleanStart = false; // This must be after the swap.
			}
		}
	}
}