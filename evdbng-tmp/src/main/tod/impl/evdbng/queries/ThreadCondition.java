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


import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdbng.db.IEventList;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.file.SimpleTree;
import tod.impl.evdbng.db.file.SimpleTuple;

/**
 * Represents a condition on event thread.
 * @author gpothier
 */
public class ThreadCondition extends SimpleCondition<SimpleTuple>
{
	private static final long serialVersionUID = -4695584777709297984L;
	private int itsThreadId;

	public ThreadCondition(int aThreadId)
	{
		itsThreadId = aThreadId;
	}

	@Override
	public IBidiIterator<SimpleTuple> createTupleIterator(IEventList aEventList, Indexes aIndexes, long aEventId)
	{
		return aIndexes.getThreadIndex(itsThreadId).getTupleIterator(aEventId);
	}

	@Override
	public long[] getEventCounts(
			IEventList aEventList,
			Indexes aIndexes, 
			long aT1, 
			long aT2, 
			int aSlotsCount, 
			boolean aForceMergeCounts)
	{
		// TODO: check implementation
		if (aForceMergeCounts) return super.getEventCounts(aEventList, aIndexes, aT1, aT2, aSlotsCount, true);
		
		SimpleTree theIndex = aIndexes.getThreadIndex(itsThreadId);
		return aIndexes.fastCounts(theIndex, aT1, aT2, aSlotsCount);
	}

	@Override
	public boolean _match(GridEvent aEvent)
	{
		return aEvent.getThread() == itsThreadId;
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("Thread number = %d", itsThreadId);
	}

}
