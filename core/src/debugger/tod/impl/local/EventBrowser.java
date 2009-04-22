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

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.EventComparator;
import tod.core.database.event.ILogEvent;
import tod.impl.local.filter.AbstractFilter;
import zz.utils.PublicCloneable;

/**
 * @author gpothier
 */
public class EventBrowser extends PublicCloneable implements IEventBrowser
{
	private final ILogBrowser itsLogBrowser;
	private final IEventFilter itsFilter;
	
	private final List<ILogEvent> itsEvents;
	private int itsIndex;
	
	public EventBrowser(ILogBrowser aLogBrowser, List<ILogEvent> aEvents, IEventFilter aFilter)
	{
		itsLogBrowser = aLogBrowser;
		itsFilter = aFilter;
		if (aEvents == null) 
		{
			aEvents = Collections.EMPTY_LIST;
			System.err.println("Warning: EventBrowser received an empty children list.");
		}
		itsEvents = aEvents;
	}
	
	public EventBrowser(ILogBrowser aLogBrowser, EventList aEventList, AbstractFilter aFilter)
	{
		itsLogBrowser = aLogBrowser;
		itsFilter = aFilter;
		itsEvents = new ArrayList<ILogEvent>();
		for (int i=0;i<aEventList.size();i++)
		{
			ILogEvent theEvent = aEventList.get(i);
			if (aFilter.accept(theEvent)) 
			{
				theEvent = getEvent(theEvent);
				itsEvents.add (theEvent);
			}
		}
	}
	
	/**
	 * Constructor for a browser that only contains one event.
	 */
	public EventBrowser(ILogBrowser aLogBrowser, ILogEvent aEvent, IEventFilter aFilter)
	{
		itsLogBrowser = aLogBrowser;
		itsFilter = aFilter;
		itsEvents = new ArrayList<ILogEvent>();
		itsEvents.add(aEvent);
	}
	
	
	public ILogBrowser getLogBrowser()
	{
		return itsLogBrowser;
	}
	
	public IEventFilter getFilter()
	{
		return itsFilter;
	}
	
	/**
	 * Returns the event that should be included in the list
	 * given a source event.
	 */
	protected ILogEvent getEvent (ILogEvent aSourceEvent)
	{
		return aSourceEvent;
	}
	
	public boolean hasNext()
	{
		return itsIndex < itsEvents.size();
	}
	
	public boolean hasPrevious()
	{
		return itsIndex > 0;
	}
	
	public ILogEvent next()
	{
		return getEvent(itsIndex++);
	}
	
	public ILogEvent previous()
	{
		return getEvent(--itsIndex);
	}
	
	/**
	 * Returns the event at a specified index.
	 */
	public ILogEvent getEvent (int aIndex)
	{
		return itsEvents.get (aIndex);
	}
	
	public void setNextTimestamp(long aTimestamp)
	{
		itsIndex = EventComparator.indexOf(aTimestamp, itsEvents);
		while (hasPrevious() && itsEvents.get (itsIndex-1).getTimestamp() == aTimestamp) itsIndex--;
	}
	
	public void setPreviousTimestamp(long aTimestamp)
	{
		itsIndex = EventComparator.indexOf(aTimestamp, itsEvents);
		while (hasNext() && itsEvents.get(itsIndex).getTimestamp() == aTimestamp)
		{
			itsIndex++;
		} 
	}
	
	
	
	public boolean setNextEvent(ILogEvent aEvent)
	{
		itsIndex = EventComparator.indexOf(aEvent, itsEvents);
		return itsIndex>=0 
				&& itsIndex<getEventCount()
				&& getEvent(itsIndex) == aEvent;
	}

	public boolean setPreviousEvent(ILogEvent aEvent)
	{
		itsIndex = EventComparator.indexOf(aEvent, itsEvents)+1;
		return itsIndex>=0 
				&& itsIndex<getEventCount()
				&& getEvent(itsIndex) == aEvent;
	}

	public long getEventCount()
	{
		return itsEvents.size();
	}

	public long getEventCount(long aT1, long aT2, boolean aForceMergeCounts)
	{
		long theCount = 0;
		
		for (ILogEvent theEvent : itsEvents)
		{
			long theTimestamp = theEvent.getTimestamp();
			if (theTimestamp < aT1) continue;
			if (theTimestamp > aT2) break;
			theCount++;
		}
		
		return theCount;
	}

	public long[] getEventCounts(long aT1, long aT2, int aSlotsCount, boolean aForceMergeCounts)
	{
		assert aT2 >= aT1;
		long[] theCounts = new long[aSlotsCount];
		
		for (ILogEvent theEvent : itsEvents)
		{
			long theTimestamp = theEvent.getTimestamp();
			if (theTimestamp < aT1) continue;
			if (theTimestamp >= aT2) break;

			int theSlot = (int)(((theTimestamp - aT1) * aSlotsCount) / (aT2 - aT1));
			theCounts[theSlot]++;
		}
		
		return theCounts;
	}
	
	
	public List<ILogEvent> getEvents(long aT1, long aT2)
	{
		List<ILogEvent> theResult = new ArrayList<ILogEvent>();
		
		for (ILogEvent theEvent : itsEvents)
		{
			long theTimestamp = theEvent.getTimestamp();
			if (theTimestamp < aT1) continue;
			if (theTimestamp > aT2) break;

			theResult.add (theEvent);
		}
		
		return theResult;
	}

	public IEventBrowser createIntersection(IEventFilter aFilter)
	{
		AbstractFilter theFilter = (AbstractFilter) aFilter;
		List<ILogEvent> theEvents = new ArrayList<ILogEvent>();
		for (ILogEvent theEvent : itsEvents)
		{
			if (theFilter.accept(theEvent)) theEvents.add(theEvent);
		}
		
		return new EventBrowser(itsLogBrowser, theEvents, null); //TODO If necessary replace null by correct filter
	}

	public long getFirstTimestamp()
	{
		return getEventCount() > 0 ? getEvent(0).getTimestamp() : 0;
	}

	public long getLastTimestamp()
	{
		return getEvent((int) (getEventCount()-1)).getTimestamp();
	}
	
	protected List<ILogEvent> getEvents()
	{
		return itsEvents;
	}
	
	@Override
	public EventBrowser clone() 
	{
		EventBrowser theClone = (EventBrowser) super.clone();
		theClone.itsIndex = 0;
		return theClone;
	}
	
	
}
