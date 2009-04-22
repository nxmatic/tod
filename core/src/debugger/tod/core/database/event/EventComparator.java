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
package tod.core.database.event;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tod.core.ILogCollector;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;

/**
 * Compares events based on their timestamp.
 */
public class EventComparator implements Comparator<ILogEvent>
{
	private static EventComparator INSTANCE = new EventComparator();

	public static EventComparator getInstance()
	{
		return INSTANCE;
	}

	private EventComparator()
	{
	}
	
	public int compare(ILogEvent aEvent1, ILogEvent aEvent2)
	{
		long theDelta;
//		if (aEvent1.getThread() == aEvent2.getThread())
//		{
//			theDelta = aEvent1.getSerial() - aEvent2.getSerial(); 
//		}
//		else 
//		{
			theDelta = aEvent1.getTimestamp() - aEvent2.getTimestamp();
//		}
		
		if (theDelta > 0) return 1;
		else if (theDelta < 0) return -1;
		else return 0;
	}
	
	/**
	 * Retrieves the index (or insertion point) of the event
	 * with the specified timestamp
	 */
	public static int indexOf (long aTimestamp, List<ILogEvent> aEvents)
	{
		int theIndex = Collections.binarySearch(
				aEvents, 
				new DummyEvent(aTimestamp), 
				getInstance());
		return theIndex >= 0 ? theIndex : -theIndex-1;
	}

	/**
	 * Retrieves the index (or insertion point) of the specified event
	 */
	public static int indexOf (ILogEvent aEvent, List<ILogEvent> aEvents)
	{
		int theIndex = Collections.binarySearch(
				aEvents, 
				aEvent, 
				getInstance());
		
		theIndex = theIndex >= 0 ? theIndex : -theIndex-1;
		
		int theSize = aEvents.size();
		int theResult = theIndex;
		
		while (theResult < theSize)
		{
			ILogEvent theEvent = aEvents.get (theResult);
			if (theEvent == aEvent) break;
			else if (theEvent.getTimestamp() > aEvent.getTimestamp()) 
			{
				theResult = Math.max (theIndex, theResult-1);
				break;
			}
			
			theResult++;
		}
		return theResult;
	}


	/**
	 * This event only serves to retrieve the index for a timestamp.
	 */
	public static class DummyEvent implements ILogEvent
	{
		private final long itsTimestamp;

		public DummyEvent(long aTimestamp)
		{
			itsTimestamp = aTimestamp;
		}
		
		public IThreadInfo getThread()
		{
			return null;
		}
		
		public int getDepth()
		{
			return 0;
		}

		public IHostInfo getHost()
		{
			return null;
		}

		public long getTimestamp()
		{
			return itsTimestamp;
		}

		public IBehaviorCallEvent getParent()
		{
			return null;
		}

		public ExternalPointer getParentPointer()
		{
			return null;
		}

		public ExternalPointer getPointer()
		{
			return null;
		}

		public int[] getAdviceCFlow()
		{
			return null;
		}
	}

}