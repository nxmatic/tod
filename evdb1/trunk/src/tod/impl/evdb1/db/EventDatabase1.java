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
package tod.impl.evdb1.db;

import static tod.impl.evdb1.DebuggerGridConfig1.DB_PAGE_SIZE;

import java.io.File;
import java.io.IOException;

import tod.core.DebugFlags;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.IGridEventFilter;
import tod.impl.dbgrid.db.EventDatabase;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.messages.BitGridEvent;
import tod.impl.evdb1.queries.EventCondition;

/**
 * This class manages an event database for a debugging session.
 * An event database consists in an event list and a number
 * of indexes.
 * @author gpothier
 */
public class EventDatabase1 extends EventDatabase 
{
	private final HardPagedFile itsFile;
	
	private final EventList itsEventList;
	private final Indexes itsIndexes;
	
	/**
	 * Creates a new database using the specified file.
	 */
	public EventDatabase1(IStructureDatabase aStructureDatabase, int aNodeId, File aFile) 
	{
		super(aStructureDatabase, aNodeId);
		System.out.println("Using evdb1");
		
		try
		{
			itsFile = new HardPagedFile(aFile, DB_PAGE_SIZE);
			itsEventList = new EventList(getStructureDatabase(), aNodeId, itsFile);
			itsIndexes = new Indexes(itsFile);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void dispose()
	{
		super.dispose();
		itsEventList.dispose();
		itsIndexes.dispose();
		itsFile.dispose();
	}

	private Indexes getIndexes()
	{
		return itsIndexes;
	}
	
	/**
	 * Creates an iterator over matching events of this node, starting at the specified timestamp.
	 */
	@Override
	protected IBidiIterator<GridEvent> evaluate0(IGridEventFilter aCondition, long aTimestamp)
	{
		return ((EventCondition) aCondition).createIterator(itsEventList, getIndexes(), aTimestamp);
	}

	public long[] getEventCounts(
			IGridEventFilter aCondition,
			long aT1, 
			long aT2,
			int aSlotsCount, 
			boolean aForceMergeCounts)
	{
		return ((EventCondition) aCondition).getEventCounts(itsEventList, getIndexes(), aT1, aT2, aSlotsCount, aForceMergeCounts);
	}
	
	@Override
	protected void processEvent0(GridEvent aEvent)
	{
		BitGridEvent theEvent = (BitGridEvent) aEvent;
		
		long theId = itsEventList.add(theEvent);
		if (! DebugFlags.DISABLE_INDEXES) theEvent.index(itsIndexes, theId);		
	}
	
	@Override
	public long getStorageSpace()
	{
		return itsFile.getStorageSpace();
	}
	
	@Override
	public long getEventsCount()
	{
		return itsEventList.getEventsCount();
	}
	
	@Override
	public long[] getEventCountAtBehaviors(int[] aBehaviorIds)
	{
		throw new UnsupportedOperationException("Implement that");
	}
}
