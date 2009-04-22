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
package tod.impl.evdbng.db;

import static tod.impl.evdbng.DebuggerGridConfigNG.DB_THREADS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import tod.core.DebugFlags;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.IGridEventFilter;
import tod.impl.dbgrid.db.EventDatabase;
import tod.impl.dbgrid.db.DatabaseNode.FlushMonitor;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.messages.GridEventNG;
import tod.impl.evdbng.queries.EventCondition;

/**
 * This class manages an event database for a debugging session.
 * An event database consists in an event list and a number
 * of indexes.
 * @author gpothier
 */
public class EventDatabaseNG extends EventDatabase
{
	private final PagedFile itsIndexesFile;
	
	/**
	 * Multiple event lists in order to leverage multicore.
	 * The event ids stored in the indexes are calculated this way:
	 * evId = l[i].evId*K+i 
	 * where K is the number of event lists and i is the index of an event list.
	 */
	private final EventList[] itsEventLists;
	
	private final SyntheticEventList itsSyntheticEventList = new SyntheticEventList();
	
	private int itsNextEventId = 0;
	
	private final Indexes itsIndexes;
	
	private PrintWriter itsLogWriter;
	
	/**
	 * Creates a new database using the specified files.
	 */
	public EventDatabaseNG(
			IStructureDatabase aStructureDatabase,
			int aNodeId, 
			File aDirectory) 
	{
		super(aStructureDatabase, aNodeId);
		System.out.println("Using evdbng");
		
		itsIndexesFile = PagedFile.create(new File(aDirectory, "indexes.bin"), true);
		
		itsEventLists = new EventList[DB_THREADS];
		for(int i=0;i<DB_THREADS;i++)
		{
			PagedFile theEventsFile = PagedFile.create(new File(aDirectory, "events-"+i+".bin"), true);
			
			itsEventLists[i] = new EventList(
					i,
					getStructureDatabase(), 
					aNodeId, 
					itsIndexesFile, 
					theEventsFile);	
		}
		
		itsIndexes = new Indexes(itsIndexesFile);
		
		if (DebugFlags.DB_LOG_DIR != null) 
		{
			try
			{
				itsLogWriter = new PrintWriter(new File(DebugFlags.DB_LOG_DIR+"/evdb.log"));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void dispose()
	{
		super.dispose();
		for(int i=0;i<DB_THREADS;i++) itsEventLists[i].dispose();
		itsIndexes.dispose();
		itsIndexesFile.dispose();
	}

	public Indexes getIndexes()
	{
		return itsIndexes;
	}
	
	/**
	 * Creates an iterator over matching events of this node, starting at the specified timestamp.
	 */
	@Override
	protected IBidiIterator<GridEvent> evaluate0(IGridEventFilter aCondition, long aTimestamp)
	{
		long theEventId = itsIndexes.getEventId(aTimestamp);
		return ((EventCondition<?>) aCondition).createIterator(itsSyntheticEventList, getIndexes(), theEventId);
	}

	@Override
	public long[] getEventCounts(
			IGridEventFilter aCondition,
			long aT1, 
			long aT2,
			int aSlotsCount, 
			boolean aForceMergeCounts)
	{
		return ((EventCondition<?>) aCondition).getEventCounts(
				itsSyntheticEventList, 
				getIndexes(), 
				aT1, 
				aT2, 
				aSlotsCount, 
				aForceMergeCounts);
	}
	
	@Override
	protected synchronized void processEvent0(GridEvent aEvent)
	{
		GridEventNG theEvent = (GridEventNG) aEvent;

		int theId = itsNextEventId++;
		int theEventList = theId % DB_THREADS;
		itsEventLists[theEventList].addAsync(theEvent, theId / DB_THREADS);
		
		if (! DebugFlags.DISABLE_INDEXES) theEvent.index(itsIndexes, theId);	
		if (DebugFlags.DB_LOG_DIR != null) 
		{
			itsLogWriter.println(theId+" - "+aEvent);
			itsLogWriter.flush();
		}
	}
	
	@Override
	public synchronized int flush(FlushMonitor aFlushMonitor)
	{
		int theResult = super.flush(aFlushMonitor);
		
		itsIndexes.flushTasks();
		for(int i=0;i<DB_THREADS;i++) itsEventLists[i].flushTasks();
		
		DBExecutor.getInstance().waitPendingTasks();
		
		return theResult;
	}
	
	/**
	 * Returns the amount of disk storage used by this node.
	 */
	@Override
	public long getStorageSpace()
	{
		return itsIndexesFile.getStorageSpace();
	}
	
	@Override
	public long getEventsCount()
	{
		long theCount = 0;
		for(int i=0;i<DB_THREADS;i++) theCount += itsEventLists[i].getEventsCount();
		return theCount;
	}

	@Override
	public long[] getEventCountAtBehaviors(int[] aBehaviorIds)
	{
		long[] theCounts = new long[aBehaviorIds.length];
		for(int i=0;i<theCounts.length;i++) theCounts[i] = itsIndexes.getEventsAtBehavior(aBehaviorIds[i]);
		return theCounts;
	}
	
	/**
	 * An event list that takes external event ids and trasnform them
	 * to a (i, evId) pair (l[i].evId === evId)
	 * @author gpothier
	 *
	 */
	private class SyntheticEventList implements IEventList
	{
		public GridEventNG getEvent(int aId)
		{
			int i = aId % DB_THREADS;
			int id = aId / DB_THREADS;
			
			return itsEventLists[i].getEvent(id);
		}
	}
}
