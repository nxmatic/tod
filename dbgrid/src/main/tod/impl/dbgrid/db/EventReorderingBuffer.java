/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.dbgrid.db;

import java.util.ArrayList;
import java.util.List;

import tod.impl.dbgrid.DebuggerGridConfig;
import tod.impl.dbgrid.messages.GridEvent;
import zz.utils.RingBuffer;
import zz.utils.Utils;

/**
 * A buffer that permits to reorder incoming events.
 * 
 * @author gpothier
 */
public class EventReorderingBuffer
{
	private long itsLastInOrder;

	private long itsLastRetrieved;

	private RingBuffer<GridEvent> itsBuffer = new RingBuffer<GridEvent>(DebuggerGridConfig.DB_REORDER_BUFFER_SIZE);

	private OutOfOrderBuffer itsOutOfOrderBuffer = new OutOfOrderBuffer();

	// private RingBuffer<GridEvent> itsGlobalDebugBuffer = new
	// RingBuffer<GridEvent>(DebuggerGridConfig.DB_EVENT_BUFFER_SIZE*2);

	private ReorderingBufferListener itsListener;

	public EventReorderingBuffer(ReorderingBufferListener aListener)
	{
		itsListener = aListener;
	}

	// private static void _pushTS(GridEvent aEvent, RingBuffer<GridEvent>
	// aBuffer)
	// {
	// if (aBuffer.isFull()) aBuffer.remove();
	// aBuffer.add(aEvent);
	// }
	//	
	// private static void _printBuffer(RingBuffer<GridEvent> aBuffer)
	// {
	// while (! aBuffer.isEmpty())
	// {
	// GridEvent theEvent = aBuffer.remove();
	// System.out.println(theEvent.getHost()+"\t"+theEvent.getThread()+"\t"+theEvent.getTimestamp());
	// }
	// }

	/**
	 * Pushes an incoming event into this buffer.
	 */
	public synchronized void push(GridEvent aEvent)
	{
		// _pushTS(aEvent, itsGlobalDebugBuffer);
		long theTimestamp = aEvent.getTimestamp();
		if (theTimestamp < itsLastRetrieved)
		{
			itsListener.eventDropped(itsLastRetrieved, theTimestamp, "ERB.push() - too late");
			return;
		}

		if (theTimestamp < itsLastInOrder)
		{
			// Out of order event.
			itsOutOfOrderBuffer.add(aEvent);
		}
		else
		{
			itsLastInOrder = theTimestamp;
			itsBuffer.add(aEvent);
		}
	}

	/**
	 * Returns true if an event is available on output. if an event is available
	 * it should be immediately retrieved, before a new event is pushed.
	 */
	public synchronized boolean isFull()
	{
		return itsBuffer.isFull();
	}

	public synchronized boolean isEmpty()
	{
		return itsBuffer.isEmpty() && itsOutOfOrderBuffer.isEmpty();
	}

	/**
	 * return the timestamp of the oldest (next ordered) event in the buffer
	 * return -1 if there is no bufered events
	 * 
	 * @return
	 */
	public synchronized long getNextAvailableTimestamp()
	{
		long theNextOutOfOrder;
		long theNextInOrder;
		
		if (!itsBuffer.isEmpty()) theNextInOrder = itsBuffer.peek().getTimestamp();
		else theNextInOrder = -1;
		
		if (!itsOutOfOrderBuffer.isEmpty()) theNextOutOfOrder = itsOutOfOrderBuffer.getNextAvailable();
		else theNextOutOfOrder = -1;

		if (theNextOutOfOrder == -1) return theNextInOrder;
		if (theNextInOrder == -1) return theNextOutOfOrder;
		return Math.min(theNextOutOfOrder, theNextInOrder);
	}

	/**
	 * define if the difference between the oldest event of the buffer and the
	 * newest is more than aDelay (in nanosecond)
	 * 
	 * @param aDelay
	 * @return
	 */
	public synchronized boolean isNextEventFlushable(long aDelay)
	{
		// Don't allow flushing old events while there are pending OoO events
		if (! itsOutOfOrderBuffer.isEmpty()) return false; 
		if (itsBuffer.isEmpty()) return false;
		
		long theNextAvailableTimestamp = itsBuffer.peek().getTimestamp();
		if (theNextAvailableTimestamp == -1) return false;
		return (itsLastInOrder - theNextAvailableTimestamp) > aDelay;
	}

	/**
	 * Retrieves the next ordered event.
	 */
	public synchronized GridEvent pop()
	{
		GridEvent theResult;
		GridEvent theInOrderEvent = itsBuffer.peek();
		if (theInOrderEvent == null)
		{
			theResult = itsOutOfOrderBuffer.next();
		}
		else
		{
			long theNextOutOfOrder = itsOutOfOrderBuffer.getNextAvailable();
			if (theNextOutOfOrder < theInOrderEvent.getTimestamp())
			{
				theResult = itsOutOfOrderBuffer.next();
			}
			else
			{
				GridEvent theEvent = itsBuffer.remove();
				assert theEvent == theInOrderEvent;
				theResult = theEvent;
			}
		}

		itsLastRetrieved = theResult.getTimestamp();
		return theResult;
	}

	/**
	 * Buffer for events that arrived late
	 * 
	 * @author gpothier
	 */
	private class OutOfOrderBuffer
	{
		private long itsAdded = 0;

		private long itsRetrieved = 0;

		private List<PerThreadBuffer> itsBuffers = new ArrayList<PerThreadBuffer>();

		private GridEvent itsNextAvailable;

		private long itsLastRetrieved;

//		private RingBuffer<GridEvent> itsOoODebugBuffer = new RingBuffer<GridEvent>(1000);

		private PerThreadBuffer getBuffer(int aThreadId)
		{
			PerThreadBuffer theBuffer = null;
			if (itsBuffers.size() < aThreadId + 1)
			{
				theBuffer = new PerThreadBuffer();
				Utils.listSet(itsBuffers, aThreadId, theBuffer);
			}
			else
			{
				theBuffer = itsBuffers.get(aThreadId);
				if (theBuffer == null)
				{
					theBuffer = new PerThreadBuffer();
					itsBuffers.set(aThreadId, theBuffer);
				}
			}

			return theBuffer;
		}

		private PerThreadBuffer getBuffer(GridEvent aEvent)
		{
			return getBuffer(aEvent.getThread());
		}

		public void add(GridEvent aEvent)
		{
//			_pushTS(aEvent, itsOoODebugBuffer);
			itsAdded++;

			PerThreadBuffer theBuffer = getBuffer(aEvent);
			theBuffer.add(aEvent);
			assert aEvent.getTimestamp() >= itsLastRetrieved : aEvent.getTimestamp() + "<" + itsLastRetrieved;

			if (itsNextAvailable == null || aEvent.getTimestamp() < itsNextAvailable.getTimestamp())
			{
				itsNextAvailable = aEvent;
			}
		}

		public boolean isEmpty()
		{
			return itsNextAvailable == null;
		}

		/**
		 * Returns the timestamp of the first available event, or
		 * {@link Long#MAX_VALUE} if none is available
		 */
		public long getNextAvailable()
		{
			return itsNextAvailable != null ? itsNextAvailable.getTimestamp() : Long.MAX_VALUE;
		}

		public GridEvent next()
		{
			itsRetrieved++;

			// Advance the buffer that contained the next event
			PerThreadBuffer theNextBuffer = getBuffer(itsNextAvailable);
			GridEvent theNextEvent = theNextBuffer.remove();
			assert theNextEvent == itsNextAvailable : theNextEvent + "!=" + itsNextAvailable;
			assert theNextEvent.getTimestamp() >= itsLastRetrieved : theNextEvent.getTimestamp() + "<" +itsLastRetrieved;

			itsLastRetrieved = theNextEvent.getTimestamp();

			// Search next event
			itsNextAvailable = null;
			long theNextTimestamp = Long.MAX_VALUE;
			for (PerThreadBuffer theBuffer : itsBuffers)
			{
				if (theBuffer == null || theBuffer.isEmpty()) continue;

				GridEvent theEvent = theBuffer.peek();
				long theTimestamp = theEvent.getTimestamp();
				if (theTimestamp < theNextTimestamp)
				{
					theNextTimestamp = theTimestamp;
					itsNextAvailable = theEvent;
				}
			}

			if (itsNextAvailable != null && itsNextAvailable.getTimestamp() < itsLastRetrieved)
			{
				System.err.println(String.format("Error. last: %d, next: %d", itsLastRetrieved, itsNextAvailable
						.getTimestamp()));

				// System.err.println("Global:");
				// _printBuffer(itsGlobalDebugBuffer);
				//				
				// System.err.println("OoO:");
				// _printBuffer(itsOoODebugBuffer);
			}

			return theNextEvent;
		}
	}

	/**
	 * Events of a single thread arrive in order so we place them in a
	 * per-thread buffer.
	 * 
	 * @author gpothier
	 */
	private class PerThreadBuffer extends RingBuffer<GridEvent>
	{
		private long itsLastAdded;

		public PerThreadBuffer()
		{
			super(DebuggerGridConfig.DB_PERTHREAD_REORDER_BUFFER_SIZE);
		}

		@Override
		public void add(GridEvent aEvent)
		{
			long theTimestamp = aEvent.getTimestamp();
			if (theTimestamp < itsLastAdded)
			{
				System.err.println("[EventReorderingBuffer] Out of order events in same thread!!!");
				itsListener.eventDropped(itsLastAdded, theTimestamp, "ERB.PTB.add() - same thread");
				return;
			}

			if (isFull())
			{
				System.err.println("[EventReorderingBuffer] Per-thread buffer full");
				itsListener.eventDropped(0, 0, "ERB.PTB.add() - full");
				return;
			}

			itsLastAdded = theTimestamp;
			super.add(aEvent);
		}
	}

	public interface ReorderingBufferListener
	{
		/**
		 * Called when an event could not be reordered and had to be dropped.
		 * @param aLastRetrieved The last valid timestamp
		 * @param aNewEvent The out-of-order timestamp that was received
		 * @param aReason TODO
		 */
		public void eventDropped(long aLastRetrieved, long aNewEvent, String aReason);
	}
}
