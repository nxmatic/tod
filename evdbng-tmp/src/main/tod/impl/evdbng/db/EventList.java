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

import java.util.Iterator;

import tod.core.database.structure.IStructureDatabase;
import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.DBExecutor.DBTask;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.SequenceTree;
import tod.impl.evdbng.db.file.PagedFile.Page;
import tod.impl.evdbng.db.file.PagedFile.PageIOStream;
import tod.impl.evdbng.db.file.TupleFinder.NoMatch;
import tod.impl.evdbng.messages.GridEventNG;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;

/**
 * Contains all the events that have been recorded.
 * The events can be accessed by their position (see {@link #getEvent(int)}).
 * @author gpothier
 */
public class EventList implements IEventList
{
	private final int itsId;

	private final IStructureDatabase itsStructureDatabase;
	
	/**
	 * This tree permits to map event ids (= position) to page ids.
	 * For each page the id of the first event of the page is added to
	 * the tree.
	 */
	private final SequenceTree itsEventIdTree;
	
	/**
	 * Number of events stored
	 */
	private int itsEventsCount = 0;
	
	/**
	 * Size of storage occupied by events.
	 */
	private long itsEventsSize = 0;
	
	/**
	 * Number of used pages.
	 */
	private long itsPageCount = 0;
	
	/**
	 * Id of the node that contains this event list.
	 * Serves to construct internal pointers.
	 */
	private int itsNodeId;
	
	private final PagedFile itsEventsFile;
	
	/**
	 * Just for checking that page ids are sequential.
	 */
	private int itsLastPageId = 0;
	
	/**
	 * The current page stream of the events file.
	 */
	private PageIOStream itsEventStream;
	
	private AddTask itsCurrentTask = new AddTask();


	
	/**
	 * Creates an event list
	 * @param aNodeId The id of the database node that uses this list
	 * @param aIndexesFile The file used to store the event id tree. It can be shared with
	 * other structures.
	 * @param aEventsFile The file used to store events. Note: the file should not
	 * be shared with other structures.
	 */
	public EventList(
			int aId,
			IStructureDatabase aStructureDatabase, 
			int aNodeId, 
			PagedFile aIndexesFile, 
			PagedFile aEventsFile) 
	{
		itsId = aId;
		itsStructureDatabase = aStructureDatabase;
		itsEventIdTree = new SequenceTree("[EventList] event id tree", aIndexesFile);
		Monitor.getInstance().register(this);
		itsNodeId = aNodeId;
		itsEventsFile = aEventsFile;
	}
	
	/**
	 * Each {@link IndexSet} has a sequential id (for {@link DBExecutor}).
	 */
	protected int getId()
	{
		return itsId;
	}

	public void dispose()
	{
		Monitor.getInstance().unregister(this);
	}

	/**
	 * Adds an event to the events list and returns its id (= position).
	 */
	public int add(GridEventNG aEvent)
	{
		int theRecordLength = PageIOStream.shortSize() + aEvent.getMessageSize();
		
		// Check available space in current page (we must leave space for the next-page pointer)
		int theTailSize = PageIOStream.shortSize() + PageIOStream.pagePointerSize();
		if (itsEventStream == null || itsEventStream.remaining() - theTailSize < theRecordLength)
		{
			PageIOStream theOldStream = itsEventStream;
			itsEventStream = itsEventsFile.create().asIOStream();
			int thePageId = itsEventStream.getPage().getPageId();
			assert thePageId == ++itsLastPageId;
			itsPageCount++;
			
			if (theOldStream != null)
			{
				theOldStream.writeShort(0); // End-of-page marker 
				theOldStream.writePagePointer(thePageId);
				
				itsEventsFile.free(theOldStream.getPage());
			}
			
			itsEventIdTree.add(itsEventsCount);
			
			// Write id of the first event of the new page.
			itsEventStream.writeInt(itsEventsCount);
		}
		
		itsEventStream.getPage().use();
		
//		System.out.println(String.format(
//				"Add event %d (%d, %d)",
//				theEventPointer,
//				itsCurrentBitStruct.getPage().getPageId(),
//				itsRecordIndex));
		
		itsEventsCount++;
		itsEventsSize += theRecordLength;
		
		int p0 = itsEventStream.getPos();
		itsEventStream.writeShort(theRecordLength);
		aEvent.writeTo(itsEventStream);
		int p1 = itsEventStream.getPos();
		assert p1-p0 == theRecordLength : "theRecordLength: "+theRecordLength+", p1-p0: "+(p1-p0)+" - "+aEvent;
		
		return itsEventsCount-1;
	}
	
	public void addAsync(GridEventNG aEvent, int aExpectedId)
	{
		itsCurrentTask.addSubtask(aEvent, aExpectedId);
		if (itsCurrentTask.isFull()) flushTasks();
	}

	/**
	 * Flushes currently pending (see {@link #addAsync(long)}).
	 */
	public void flushTasks()
	{
		DBExecutor.getInstance().submit(itsCurrentTask);
		itsCurrentTask = new AddTask();
	}
	
	/**
	 * Returns the event corresponding to the specified internal pointer.
	 */
	public GridEventNG getEvent(int aId)
	{
		if (aId >= itsEventsCount) return null;
		
		// Tuple positions start at 0, but page ids start at 1.
		int thePageId = (int) itsEventIdTree.getTuplePosition(aId, NoMatch.BEFORE) + 1;
		
		PagedFile.Page thePage = itsEventsFile.get(thePageId);
		PageIOStream theStream = thePage.asIOStream();
		int theFirstEventId = theStream.readInt();
		
		int theCount = aId - theFirstEventId;
		
		do
		{
			int theRecordLength = theStream.readUShort();
			if (theRecordLength == 0) 
			{
				// End-of-page marker.
				throw new RuntimeException(String.format("Event not found: (id: %d)", aId));
			}
			
			if (theCount == 0) return GridEventNG.read(itsStructureDatabase, theStream);
			
			theStream.skip(theRecordLength-PageIOStream.shortSize());
			theCount--;
			
		} while (true);
	}
	
	/**
	 * Returns an iterator on all the events of this list
	 */
	public Iterator<GridEventNG> getEventIterator()
	{
		return new EventIterator(itsEventsFile.get(1));
	}
	
	public int getPageSize()
	{
		return itsEventsFile.getPageSize();
	}
	
	@Probe(key = "event pages", aggr = AggregationType.SUM)
	public long getPageCount()
	{
		return itsPageCount;
	}
	
	@Probe(key = "event storage", aggr = AggregationType.SUM)
	public long getStorageSpace()
	{
		return getPageCount() * getPageSize();
	}
	
	@Probe(key = "event count", aggr = AggregationType.SUM)
	public long getEventsCount()
	{
		return itsEventsCount;
	}
		
	@Probe(key = "event size", aggr = AggregationType.AVG)
	public float getAverageEventSize()
	{
		if (itsEventsCount == 0) return -1;
		return itsEventsSize / itsEventsCount / 8f;
	}
	
	private class EventIterator implements Iterator<GridEventNG>
	{
		private PageIOStream itsStream;
		
		private GridEventNG itsNext;

		public EventIterator(Page aPage)
		{
			itsStream = aPage.asIOStream();
			itsStream.readInt(); // Skip Id of first event 
			itsNext = readNext();
		}

		private GridEventNG readNext()
		{
			int theRecordLength;
			
			do
			{
				theRecordLength = itsStream.readUShort();
				
				if (theRecordLength == 0)
				{
					// We reached the end of the page, we must read the next-page pointer
					int theNextPage = itsStream.readPagePointer();
					if (theNextPage == 0) return null;
					
//					itsFile.freePage(itsPage.getPage());
					itsStream = itsEventsFile.get(theNextPage).asIOStream();
					itsStream.readInt(); // skip event id.
				}
			} while (theRecordLength == 0); // We really should not loop more than once
			
			GridEventNG theEvent = GridEventNG.read(itsStructureDatabase, itsStream);
			
			return theEvent;
		}
		
		public boolean hasNext()
		{
			return itsNext != null;
		}

		public GridEventNG next()
		{
			GridEventNG theResult = itsNext;
			itsNext = readNext();
			return theResult;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	private class AddTask extends DBTask
	{
		private final GridEventNG[] itsEvents = new GridEventNG[DebuggerGridConfigNG.DB_TASK_SIZE];
		private final int[] itsExpectedIds = new int[DebuggerGridConfigNG.DB_TASK_SIZE];
		private int itsPosition = 0;
		
		public void addSubtask(GridEventNG aEvent, int aExpectedId)
		{
			itsEvents[itsPosition] = aEvent;
			itsExpectedIds[itsPosition] = aExpectedId;
			itsPosition++;
		}
		
		public boolean isEmpty()
		{
			return itsPosition == 0;
		}
		
		public boolean isFull()
		{
			return itsPosition == itsEvents.length;
		}
		
		@Override
		public void run()
		{
			for (int i=0;i<itsPosition;i++) 
			{
				int theId = add(itsEvents[i]);
				assert theId == itsExpectedIds[i] : String.format("theId: %d, expected: %d", theId, itsExpectedIds[i]);
			}
		}

		@Override
		public int getGroup()
		{
			return getId();
		}
	}


}
