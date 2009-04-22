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
package tod.core.database.browser;

import java.util.LinkedList;

import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;
import tod.tools.monitoring.Monitored;
import zz.utils.Utils;

/**
 * An event browser that filters an original browser by grouping adjacent events
 * that pertain to a same semantic group. A {@link IGroupDefinition} indicates the 
 * semantic group to which pertains each event. Grouped-together events are placed
 * in a "false" {@link ILogEvent}: {@link EventGroup}.
 * @param <K> The type of group keys
 * @author gpothier
 */
public class GroupingEventBrowser<K> implements IEventBrowser
{
	private final IEventBrowser itsSource;
	private final IGroupDefinition<K> itsGroupDefinition;
	
	/**
	 * Whether events that have a non-null group key should be placed in a group,
	 * even if they are alone in the group.
	 */
	private final boolean itsAllowSingletons;

	
	public GroupingEventBrowser(
			IEventBrowser aSource, 
			IGroupDefinition<K> aGroupDefinition, 
			boolean aAllowSingletons)
	{
		itsSource = aSource;
		itsGroupDefinition = aGroupDefinition;
		itsAllowSingletons = aAllowSingletons;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsSource.getLogBrowser();
	}

	public IEventFilter getFilter()
	{
		return itsSource.getFilter();
	}

	public IGroupDefinition<K> getGroupDefinition()
	{
		return itsGroupDefinition;
	}
	
	@Override
	public IEventBrowser clone() 
	{
		return new GroupingEventBrowser<K>(itsSource, itsGroupDefinition, itsAllowSingletons);
	}

	public IEventBrowser createIntersection(IEventFilter aFilter)
	{
		return new GroupingEventBrowser<K>(
				itsSource.createIntersection(aFilter), 
				itsGroupDefinition, 
				itsAllowSingletons);
	}

	public long getEventCount()
	{
		throw new UnsupportedOperationException();
	}

	public long getEventCount(long aT1, long aT2, boolean aForceMergeCounts)
	{
		throw new UnsupportedOperationException();
	}

	public long[] getEventCounts(long aT1, long aT2, int aSlotsCount, boolean aForceMergeCounts)
	{
		return itsSource.getEventCounts(aT1, aT2, aSlotsCount, aForceMergeCounts);
	}

	public long getFirstTimestamp()
	{
		return itsSource.getFirstTimestamp();
	}

	public long getLastTimestamp()
	{
		return itsSource.getLastTimestamp();
	}

	public boolean hasNext()
	{
		return itsSource.hasNext();
	}

	public boolean hasPrevious()
	{
		return itsSource.hasPrevious();
	}

	public ILogEvent next()
	{
		return more(Direction.FORWARD);
	}
	
	/**
	 * Retrieves the next event/group in the given direction.
	 */
	@Monitored
	private ILogEvent more(Direction aDirection)
	{
		ILogEvent theNext = aDirection.more(itsSource);
		K theKey = itsGroupDefinition.getGroupKey(theNext);
		if (theKey == null) return theNext;
		
		EventGroup<K> theEventGroup = null;
		
		if (itsAllowSingletons)
		{
			theEventGroup = new EventGroup(theKey);
			aDirection.add(theEventGroup, theNext);			
		}
		
		while(aDirection.hasMore(itsSource))
		{
			ILogEvent theCandidate = aDirection.more(itsSource);
			K theCandidateKey = itsGroupDefinition.getGroupKey(theCandidate);
			if (theKey.equals(theCandidateKey))
			{
				if (theEventGroup == null)
				{
					theEventGroup = new EventGroup(theKey);
					aDirection.add(theEventGroup, theNext);
				}
				aDirection.add(theEventGroup, theCandidate);
			}
			else
			{
				aDirection.back(itsSource);
				break;
			}
		}
		
		return theEventGroup != null ? theEventGroup : theNext;
	}

	public ILogEvent previous()
	{
		return more(Direction.BACKWARD);
	}

	public boolean setNextEvent(ILogEvent aEvent)
	{
		if (aEvent instanceof EventGroup)
		{
			EventGroup theGroup = (EventGroup) aEvent;
			return itsSource.setNextEvent(theGroup.getFirst());
		}
		else return itsSource.setNextEvent(aEvent);
	}

	public void setNextTimestamp(long aTimestamp)
	{
		itsSource.setNextTimestamp(aTimestamp);
		stop(Direction.FORWARD);
	}

	public boolean setPreviousEvent(ILogEvent aEvent)
	{
		if (aEvent instanceof EventGroup)
		{
			EventGroup theGroup = (EventGroup) aEvent;
			return itsSource.setPreviousEvent(theGroup.getLast());
		}
		else return itsSource.setPreviousEvent(aEvent);
	}

	public void setPreviousTimestamp(long aTimestamp)
	{
		itsSource.setPreviousTimestamp(aTimestamp);
		stop(Direction.BACKWARD);
	}

	/**
	 * Stops at the next event/group in the specified direction
	 */
	@Monitored
	private void stop(Direction aDirection)
	{
		// If we are at the beginning or end the sequence, we can stay here
		if (! aDirection.hasBack(itsSource)) return;
		if (! aDirection.hasMore(itsSource)) return;
		
		ILogEvent theNext = aDirection.more(itsSource);
		K theNextKey = itsGroupDefinition.getGroupKey(theNext);
		
		// Back to the original position
		aDirection.back(itsSource);
		
		// If the next event has no group, then we stay at the original position.
		if (theNextKey == null) return;
		
		ILogEvent thePrevious = aDirection.back(itsSource);
		K thePreviousKey = itsGroupDefinition.getGroupKey(thePrevious);

		// Back to the original position.
		aDirection.more(itsSource);
		
		// If the previous is from another group, we stay at the original position.
		if (! theNextKey.equals(thePreviousKey)) return;
		
		// Previous event belongs to the same group, jump to next group
		while(aDirection.hasMore(itsSource))
		{
			ILogEvent theCandidate = aDirection.more(itsSource);
			K theCandidateKey = itsGroupDefinition.getGroupKey(theCandidate);
			if (! theNextKey.equals(theCandidateKey))
			{
				// We reached the end of the group
				aDirection.back(itsSource);
				return;
			}
		}
	}

	/**
	 * Permits to abstract the iteration direction
	 * @author gpothier
	 */
	static abstract class Direction
	{
		public static final Direction FORWARD = new Direction()
		{
			@Override
			public void add(EventGroup aGroup, ILogEvent aEvent)
			{
				aGroup.addLast(aEvent);
			}

			@Override
			@Monitored
			public boolean hasMore(IEventBrowser aBrowser)
			{
				return aBrowser.hasNext();
			}

			@Override
			@Monitored
			public ILogEvent more(IEventBrowser aBrowser)
			{
				return aBrowser.next();
			}

			@Override
			@Monitored
			public boolean hasBack(IEventBrowser aBrowser)
			{
				return aBrowser.hasPrevious();
			}
			
			@Override
			@Monitored
			public ILogEvent back(IEventBrowser aBrowser)
			{
				return aBrowser.previous();
			}
		};
		
		public static final Direction BACKWARD = new Direction()
		{
			@Override
			public void add(EventGroup aGroup, ILogEvent aEvent)
			{
				aGroup.addFirst(aEvent);
			}
			
			@Override
			@Monitored
			public boolean hasMore(IEventBrowser aBrowser)
			{
				return aBrowser.hasPrevious();
			}
			
			@Override
			@Monitored
			public ILogEvent more(IEventBrowser aBrowser)
			{
				return aBrowser.previous();
			}
			
			@Override
			@Monitored
			public boolean hasBack(IEventBrowser aBrowser)
			{
				return aBrowser.hasNext();
			}

			@Override
			@Monitored
			public ILogEvent back(IEventBrowser aBrowser)
			{
				return aBrowser.next();
			}
		};
		
		
		public abstract boolean hasMore(IEventBrowser aBrowser);
		public abstract ILogEvent more(IEventBrowser aBrowser);
		public abstract boolean hasBack(IEventBrowser aBrowser);
		public abstract ILogEvent back(IEventBrowser aBrowser);

		public abstract void add(EventGroup aGroup, ILogEvent aEvent);
	}
	
	/**
	 * Defines how grouping of events takes place. Adjacent events that have the same
	 * group key are grouped together, unless their group key is the null. 
	 * @param <K> The type of the group keys.
	 * @author gpothier
	 */
	public interface IGroupDefinition<K>
	{
		/**
		 * Returns the group to which pertains the given event.
		 */
		public K getGroupKey(ILogEvent aEvent);
	}

	/**
	 * A "false" events that represents a group of events.
	 * @param <K> The type of the group keys.
	 * @author gpothier
	 */
	public static class EventGroup<K> implements ILogEvent
	{
		private final LinkedList<ILogEvent> itsEvents = new LinkedList<ILogEvent>();
		private K itsGroupKey;

		public EventGroup(K aGroup)
		{
			itsGroupKey = aGroup;
		}
		
		/**
		 * Returns the group key of this group.
		 */
		public K getGroupKey()
		{
			return itsGroupKey;
		}

		void addFirst(ILogEvent aEvent)
		{
			itsEvents.addFirst(aEvent);
		}
		
		void addLast(ILogEvent aEvent)
		{
			itsEvents.addLast(aEvent);
		}
		
		public ILogEvent getFirst()
		{
			return itsEvents.get(0);
		}
		
		public ILogEvent getLast()
		{
			return itsEvents.get(itsEvents.size()-1);
		}
		
		public Iterable<ILogEvent> getEvents()
		{
			return itsEvents;
		}
		
		@Override
		public int hashCode()
		{
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + (itsGroupKey.hashCode());
			for (ILogEvent theEvent : itsEvents)
			{
				result = PRIME * result + (theEvent.hashCode());
			}
			return result;

		}
		
		@Override
		public boolean equals(Object aObj)
		{
			if (aObj instanceof EventGroup)
			{
				EventGroup theOther = (EventGroup) aObj;
				if (! Utils.equalOrBothNull(theOther.itsGroupKey, itsGroupKey)) return false;
				if (theOther.itsEvents.size() != itsEvents.size()) return false;
				
				for(int i=0;i<itsEvents.size();i++)
				{
					if (! Utils.equalOrBothNull(theOther.itsEvents.get(i), itsEvents.get(i))) return false;
				}
				
				return true;
			}
			else return false;
		}
		
		public int getDepth()
		{
			throw new UnsupportedOperationException();
		}

		public IHostInfo getHost()
		{
			throw new UnsupportedOperationException();
		}

		public IBehaviorCallEvent getParent()
		{
			throw new UnsupportedOperationException();
		}

		public ExternalPointer getParentPointer()
		{
			throw new UnsupportedOperationException();
		}

		public ExternalPointer getPointer()
		{
			throw new UnsupportedOperationException();
		}

		public IThreadInfo getThread()
		{
			throw new UnsupportedOperationException();
		}

		public long getTimestamp()
		{
			throw new UnsupportedOperationException();
		}

		public int[] getAdviceCFlow()
		{
			throw new UnsupportedOperationException();
		}
	}

}
