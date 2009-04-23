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
package tod.impl.evdbng.queries;

import java.io.Serializable;

import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.IGridEventFilter;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdbng.db.EventList;
import tod.impl.evdbng.db.EventsCounter;
import tod.impl.evdbng.db.IEventList;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.file.Tuple;

/**
 * Represents a boolean filtering condition on event attributes.
 * @author gpothier
 */
public abstract class EventCondition<T extends Tuple>
implements IGridEventFilter, Serializable
{
	public abstract boolean _match(GridEvent aEvent);
	
	/**
	 * Creates an iterator over matching events, taking them from the specified
	 * {@link EventList} and {@link Indexes}.
	 */
	public final IBidiIterator<GridEvent> createIterator(
			final IEventList aEventList,
			Indexes aIndexes,
			long aEventId)
	{
		IBidiIterator<T> theIterator = createTupleIterator(aEventList, aIndexes, aEventId);
		return new AbstractFilteredBidiIterator<T, GridEvent>(theIterator)
		{
			@Override
			protected Object transform(T aTuple)
			{
				return aEventList.getEvent((int) aTuple.getKey());
			}
		};
	}
	
	/**
	 * Returns the number of clauses (terminal nodes) of this condition;
	 */
	public abstract int getClausesCount();

	/**
	 * Creates an iterator over matching events, taking them from the specified
	 * {@link EventList} and {@link Indexes}.
	 */
	public abstract IBidiIterator<T> createTupleIterator(
			IEventList aEventList,
			Indexes aIndexes, 
			long aEventId);
	
	/**
	 * Returns the number of events that matches this condition.
	 * By default performs a merge count. Subclasses can override this method
	 * to provide a more efficient implementation.
	 */
	public long[] getEventCounts(
			IEventList aEventList,
			Indexes aIndexes, 
			long aT1, 
			long aT2, 
			int aSlotsCount, 
			boolean aForceMergeCounts)
	{
		return EventsCounter.mergeCountEvents(this, aEventList, aIndexes, aT1, aT2, aSlotsCount); 
	}

	
	protected abstract String toString(int aIndent);
	
	@Override
	public String toString()
	{
		return toString(0);
	}
	
}
