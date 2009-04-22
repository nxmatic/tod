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
package tod.impl.dbgrid.aggregator;

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IThreadInfo;
import tod.impl.database.BufferedBidiIterator;
import tod.impl.dbgrid.DebuggerGridConfig;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.IGridEventFilter;
import tod.impl.dbgrid.IScheduled;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.dbgrid.queries.EventIdCondition;
import tod.tools.monitoring.MonitoringClient.MonitorId;

/**
 * Implementation of {@link IEventBrowser} that serves as the client-side
 * of a {@link QueryAggregator}.
 * @author gpothier
 */
public class GridEventBrowser extends BufferedBidiIterator<ILogEvent[], ILogEvent>
implements IEventBrowser, IScheduled
{
	private final GridLogBrowser itsLogBrowser;
	private final IGridEventFilter itsFilter;
	
	private final RIQueryAggregator itsAggregator;
	
	private ILogEvent itsFirstEvent;
	private ILogEvent itsLastEvent;
	
	public GridEventBrowser(GridLogBrowser aBrowser, IGridEventFilter aFilter)
	{
		itsLogBrowser = aBrowser;
		itsFilter = aFilter;
		itsAggregator = itsLogBrowser.getMaster().createAggregator(aFilter);
		assert itsAggregator != null;
		reset();
	}
	
	public GridLogBrowser getLogBrowser()
	{
		return itsLogBrowser;
	}
	
	public IEventFilter getFilter()
	{
		return itsFilter;
	}
	
	public ILogBrowser getKey()
	{
		return itsLogBrowser;
	}
	
	public void setBounds(ILogEvent aFirstEvent, ILogEvent aLastEvent)
	{
		itsFirstEvent = aFirstEvent;
		itsLastEvent = aLastEvent;
	}
	
	@Override
	protected ILogEvent[] fetchNextBuffer()
	{
		GridEvent[] theGridEvents = itsAggregator.next(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
		if (theGridEvents == null) return null;
		else return convert(theGridEvents);
	}
	
	@Override
	protected ILogEvent[] fetchPreviousBuffer()
	{
		GridEvent[] theGridEvents = itsAggregator.previous(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
		if (theGridEvents == null) return null;
		else return convert(theGridEvents);
	}


	@Override
	protected ILogEvent get(ILogEvent[] aBuffer, int aIndex)
	{
		return aBuffer[aIndex];
	}

	@Override
	protected int getSize(ILogEvent[] aBuffer)
	{
		return aBuffer.length;
	}
	
	@Override
	public boolean hasNext()
	{
		if (super.hasNext())
		{
			if (itsLastEvent == null) return true;
			
			long theFirstT = itsFirstEvent != null ? itsFirstEvent.getTimestamp() : 0;
			long theLastT = itsLastEvent.getTimestamp();
			
			ILogEvent theNext = peekNext();
			long theNextT = theNext.getTimestamp();
			
			if (theNextT < theFirstT) return false;
			else if (theNextT > theLastT) return false;
			else if (theNextT < theLastT) return true;
			else if (theNext.equals(itsLastEvent)) return true;
			else 
			{
				// TODO: There is a border case we don't handle 
				// (all available events have the same timestamp)
				ILogEvent thePrevious = peekPrevious();
				return ! itsLastEvent.equals(thePrevious);
			}
		}
		else return false;
	}
	
	@Override
	public boolean hasPrevious()
	{
		if (super.hasPrevious())
		{
			if (itsFirstEvent == null) return true;
			
			long theFirstT = itsFirstEvent.getTimestamp();
			long theLastT = itsLastEvent != null ? itsLastEvent.getTimestamp() : Long.MAX_VALUE;
			
			ILogEvent thePrevious = peekPrevious();
			long thePreviousT = thePrevious.getTimestamp();
			
			if (thePreviousT > theLastT) return false;
			else if (thePreviousT < theFirstT) return false;
			else if (thePreviousT > theFirstT) return true;
			else if (thePrevious.equals(itsFirstEvent)) return true;
			else 
			{
				// TODO: There is a border case we don't handle 
				// (all available events have the same timestamp)
				ILogEvent theNext = peekNext();
				return ! itsFirstEvent.equals(theNext);
			}
		}
		else return false;
	}
	
	/**
	 * Converts a {@link GridEvent} into an {@link ILogEvent}.
	 */
	private ILogEvent convert(GridEvent aEvent)
	{
		aEvent._setStructureDatabase(itsLogBrowser.getStructureDatabase());
		if (! itsFilter._match(aEvent))
		{
			assert false;
		}
		return aEvent.toLogEvent(itsLogBrowser);
	}
	
	private ILogEvent[] convert(GridEvent[] aEvents)
	{
		ILogEvent[] theLogEvents = new ILogEvent[aEvents.length];
		for(int i=0;i<aEvents.length;i++) theLogEvents[i] = convert(aEvents[i]);
		return theLogEvents;
	}
	
	public long getEventCount()
	{
		return getEventCount(0, Long.MAX_VALUE, false);
	}

	public long getEventCount(long aT1, long aT2, boolean aForceMergeCounts)
	{
		return getEventCounts(aT1, aT2, 1, aForceMergeCounts)[0];
	}

	public long[] getEventCounts(long aT1, long aT2, int aSlotsCount, boolean aForceMergeCounts)
	{
		long[] theCounts = itsAggregator.getEventCounts(
							aT1, 
							aT2, 
							aSlotsCount, 
							aForceMergeCounts);
		
		// TODO: take into account first & last events.
		return theCounts;
	}

	public boolean setNextEvent(ILogEvent aEvent)
	{
		boolean theResult = itsAggregator.setNextEvent(
				checkTimestamp(aEvent.getTimestamp()),
				aEvent.getThread().getId());
		reset();
		
		return theResult;
	}

	public boolean setPreviousEvent(ILogEvent aEvent)
	{
		long theTimestamp = checkTimestamp(aEvent.getTimestamp());
		IThreadInfo theThread = aEvent.getThread();
		int theThreadId = theThread.getId();
		boolean theResult = itsAggregator.setPreviousEvent(theTimestamp, theThreadId);
		reset();
		
		return theResult;
	}

	/**
	 * Corrects the given timestamp if necessary so that
	 * it fits between first and last events.
	 */
	private long checkTimestamp(long aTimestamp)
	{
		if (itsFirstEvent != null)
		{
			long theTimestamp = itsFirstEvent.getTimestamp();
			aTimestamp = Math.max(aTimestamp, theTimestamp);
		}
		if (itsLastEvent != null)
		{
			long theTimestamp = itsLastEvent.getTimestamp();
			aTimestamp = Math.min(aTimestamp, theTimestamp);
		}
		return aTimestamp;
	}
	
	public void setNextTimestamp(long aTimestamp)
	{
		itsAggregator.setNextTimestamp(checkTimestamp(aTimestamp));
		reset();
	}

	public void setPreviousTimestamp(long aTimestamp)
	{
		itsAggregator.setPreviousTimestamp(checkTimestamp(aTimestamp));
		reset();
	}
	
	@Override
	public IEventBrowser clone()
	{
		return itsLogBrowser.createBrowser(itsFilter);
	}

	public IEventBrowser createIntersection(IEventFilter aFilter)
	{
		assert aFilter != null;
		if (aFilter instanceof EventIdCondition)
		{
			// That method explicitly handles EventIdConditions
			ICompoundFilter theFilter = itsLogBrowser.createIntersectionFilter(itsFilter, aFilter);
			return itsLogBrowser.createBrowser(theFilter);
		}
		else
		{
			ICompoundFilter theFilter = itsLogBrowser.createIntersectionFilter(itsFilter, aFilter);
			GridEventBrowser theBrowser = (GridEventBrowser) itsLogBrowser.createBrowser(theFilter);
			
			theBrowser.setBounds(itsFirstEvent, itsLastEvent);
			
			return theBrowser;
		}
	}

	public long getFirstTimestamp()
	{
		return itsFirstEvent != null ?
				itsFirstEvent.getTimestamp()
				: getFirstTimestamp(clone());
	}

	public long getLastTimestamp()
	{
		return itsLastEvent != null ?
				itsLastEvent.getTimestamp()
				: getLastTimestamp(clone());
	}
	
	/**
	 * Returns the timestamp of the first event available to the 
	 * given browser, or 0 if there is no event. 
	 */
	private static long getFirstTimestamp(IEventBrowser aBrowser)
	{
		aBrowser.setNextTimestamp(0);
		if (aBrowser.hasNext())
		{
			return aBrowser.next().getTimestamp();
		}
		else return 0;
	}
	
	/**
	 * Returns the timestamp of the last event available to the 
	 * given browser, or 0 if there is no event. 
	 */
	private static long getLastTimestamp(IEventBrowser aBrowser)
	{
		aBrowser.setPreviousTimestamp(Long.MAX_VALUE);
		if (aBrowser.hasPrevious())
		{
			return aBrowser.previous().getTimestamp();
		}
		else return 0;
	}

	/**
	 * Overridden only as a hack for Recorder.aj (otherwise execution is not picked)
	 */
	@Override
	public ILogEvent next()
	{
		return super.next();
	}

	/**
	 * Overridden only as a hack for Recorder.aj (otherwise execution is not picked)
	 */
	@Override
	public ILogEvent peekNext()
	{
		return super.peekNext();
	}

	/**
	 * Overridden only as a hack for Recorder.aj (otherwise execution is not picked)
	 */
	@Override
	public ILogEvent peekPrevious()
	{
		return super.peekPrevious();
	}

	/**
	 * Overridden only as a hack for Recorder.aj (otherwise execution is not picked)
	 */
	@Override
	public ILogEvent previous()
	{
		return super.previous();
	}


	
	
}
