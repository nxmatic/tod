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
package tod.impl.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tod.core.database.event.EventComparator;
import tod.impl.common.event.Event;

/**
 * A list of events, ordered by timestamps.
 * @author gpothier
 */
public class EventList
{
	private List<Event> itsEvents = new ArrayList<Event>();
	private long itsFirstTimestamp = Long.MAX_VALUE;
	private long itsLastTimestamp = 0;
	
	public void clear()
	{
		itsEvents.clear();
	}
	
	/**
	 * Adds the specified event to this list.
	 * @return The index at which the event is inserted
	 */
	public int add (Event aEvent)
	{
		int theIndex;
		
		long theTimestamp = aEvent.getTimestamp();
		
		if (size() == 0
			|| getLast().getTimestamp() <= theTimestamp)
		{
			theIndex = size();
		}
		else
		{
			theIndex = Collections.binarySearch(
					itsEvents, 
					aEvent, 
					EventComparator.getInstance());
			
			if (theIndex < 0) theIndex = -theIndex-1;
		}
		
		itsEvents.add (theIndex, aEvent);
		
		if (theTimestamp > 0)
		{
			itsFirstTimestamp = Math.min(itsFirstTimestamp, theTimestamp);
			itsLastTimestamp = Math.max(itsLastTimestamp, theTimestamp);
		}
		
		return theIndex;
	}
	
	public Event getLast()
	{
		return itsEvents.get (itsEvents.size()-1);
	}
	
	public int size()
	{
		return itsEvents.size();
	}

	public Event get (int aIndex)
	{
		return itsEvents.get(aIndex);
	}

	public long getFirstTimestamp()
	{
		return itsFirstTimestamp;
	}

	public long getLastTimestamp()
	{
		return itsLastTimestamp;
	}
	
}
