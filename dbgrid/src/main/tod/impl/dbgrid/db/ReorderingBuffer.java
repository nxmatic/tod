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

import java.util.Comparator;
import java.util.PriorityQueue;

import zz.utils.RingBuffer;

/**
 * A buffer that permits to reoder slightly out-of-order objects.
 * 
 * @see ObjectsDatabase
 * @author gpothier
 */
public class ReorderingBuffer<E extends ReorderingBuffer.Entry>
{
	private long itsLastPushed;

	private final RingBuffer<E> itsBuffer;
	private final PriorityQueue<E> itsOutOfOrderBuffer = new PriorityQueue<E>(100, EntryComparator.getInstance());

	public ReorderingBuffer(int aSize)
	{
		itsBuffer = new RingBuffer<E>(aSize);
	}

	/**
	 * Pushes an incoming event into this buffer.
	 */
	public void push(E aEntry)
	{
		long theId = aEntry.id;
		if (theId < itsLastPushed)
		{
			// Out of order event.
			itsOutOfOrderBuffer.offer(aEntry);
		}
		else
		{
			itsLastPushed = theId;
			itsBuffer.add(aEntry);
		}
	}

	/**
	 * define if the difference between the oldest event of the buffer and the
	 * newest is more than aDelay (in nanosecond)
	 * 
	 * @param aDelay
	 * @return
	 */
	public boolean isNextEventFlushable(long aDelay)
	{
		long theNextAvailableTimestamp = getNextAvailableTimestamp();
		if (theNextAvailableTimestamp == -1) return false;
		return (itsLastPushed - theNextAvailableTimestamp) > aDelay;
	}

	/**
	 * return the timestamp of the oldest (next ordered) event in the buffer
	 * return -1 if no more event are available
	 */
	public long getNextAvailableTimestamp()
	{
		long theInOrderEvent;
		long theNextOutOfOrder;

		if (!itsBuffer.isEmpty()) theInOrderEvent = itsBuffer.peek().timestamp;
		else theInOrderEvent = -1;
		if (!itsOutOfOrderBuffer.isEmpty()) theNextOutOfOrder = itsOutOfOrderBuffer.peek().timestamp;
		else theNextOutOfOrder = -1;

		if (theNextOutOfOrder == -1) return theInOrderEvent;
		if (theInOrderEvent == -1) return theNextOutOfOrder;
		return Math.min(theNextOutOfOrder, theInOrderEvent);
	}

	/**
	 * Returns true if an event is available on output. if an event is available
	 * it should be immediately retrieved, before a new event is pushed.
	 */
	public boolean isFull()
	{
		return itsBuffer.isFull();
	}

	public boolean isEmpty()
	{
		return itsBuffer.isEmpty() && itsOutOfOrderBuffer.isEmpty();
	}

	/**
	 * Retrieves the next ordered event.
	 */
	public E pop()
	{
		if (itsBuffer.isEmpty())
		{
			return itsOutOfOrderBuffer.poll();
		}
		else
		{
			E theInOrder = itsBuffer.peek();
			E theNextOutOfOrder = itsOutOfOrderBuffer.peek();
			if (theNextOutOfOrder != null && theNextOutOfOrder.id < theInOrder.id)
			{
				return itsOutOfOrderBuffer.poll();
			}
			else
			{
				E theEntry = itsBuffer.remove();
				assert theEntry == theInOrder;
				return theEntry;
			}
		}
	}

	public static abstract class Entry
	{
		public final long id;
		public final long timestamp;

		public Entry(long aId, long aTimestamp)
		{
			id = aId;
			timestamp = aTimestamp;
		}
	}

	private static class EntryComparator implements Comparator<Entry>
	{
		private static EntryComparator INSTANCE = new EntryComparator();

		public static EntryComparator getInstance()
		{
			return INSTANCE;
		}

		private EntryComparator()
		{
		}

		public int compare(Entry e1, Entry e2)
		{
			long id1 = e1.id;
			long id2 = e2.id;

			if (id1 < id2) return -1;
			else if (id1 == id2) return 0;
			else return 1;
		}
	}
}
