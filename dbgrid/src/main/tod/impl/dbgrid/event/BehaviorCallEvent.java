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
package tod.impl.dbgrid.event;

import java.io.Serializable;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.ILogBrowser.Query;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IThreadInfo;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.aggregator.GridEventBrowser;
import tod.utils.TODUtils;

public abstract class BehaviorCallEvent extends tod.impl.common.event.BehaviorCallEvent
{
	private CallInfo itsCallInfo = null;
	
	/**
	 * The first (direct) child event of the call
	 */
	private ILogEvent itsFirstChild;
	
	/**
	 * The last (direct) child event of the call
	 */
	private ILogEvent itsLastChild;
	
	/**
	 * The behavior exit event corresponding to the call
	 */
	private IBehaviorExitEvent itsExitEvent;
	

	public BehaviorCallEvent(GridLogBrowser aLogBrowser)
	{
		super(aLogBrowser);
	}

	/**
	 * Initialize exit event and hasRealChildren flag.
	 */
	private synchronized void initCallInfo()
	{
		TODUtils.log(1,"[initChildren] For event: "+getPointer());
		{
			long t0 = System.currentTimeMillis();
			itsCallInfo = getLogBrowser().exec(new CallInfoBuilder(getPointer()));
			long t = System.currentTimeMillis() - t0;
			TODUtils.log(1,"[initChildren] executed in " + t + "ms");
		}				
	}

	public boolean hasRealChildren()
	{
		if (itsCallInfo == null) initCallInfo();
		return itsCallInfo.hasRealChildren();
	}
	
	/**
	 * Ensure that bounds are initialized
	 */
	private void checkBounds()
	{
		if (itsCallInfo == null) initCallInfo();
		
		if (itsFirstChild == null || itsLastChild == null)
		{
			itsFirstChild = itsCallInfo.getFirstChild() != null ?
					getLogBrowser().getEvent(itsCallInfo.getFirstChild())
					: null;
					
			itsLastChild = itsCallInfo.getLastChild() != null ? 
					getLogBrowser().getEvent(itsCallInfo.getLastChild())
					: null;
		}
	}
	
	public IEventBrowser getChildrenBrowser()
	{
		checkBounds();
		GridLogBrowser theLogBrowser = (GridLogBrowser) getLogBrowser();
		
		IEventFilter theFilter = theLogBrowser.createIntersectionFilter(
				theLogBrowser.createThreadFilter(getThread()),
				theLogBrowser.createDepthFilter(getDepth()+1));
		
		// Note that this cast should never fail as the filter cannot be an id condition.
		GridEventBrowser theBrowser = (GridEventBrowser) theLogBrowser.createBrowser(theFilter);
		theBrowser.setBounds(itsFirstChild, itsLastChild);
		
		return theBrowser;
	}
	
	public IEventBrowser getCFlowBrowser()
	{
		checkBounds();
		GridLogBrowser theLogBrowser = (GridLogBrowser) getLogBrowser();
		
		IEventFilter theFilter = theLogBrowser.createThreadFilter(getThread());
		
		// Note that this cast should never fail as the filter cannot be an id condition.
		GridEventBrowser theBrowser = (GridEventBrowser) theLogBrowser.createBrowser(theFilter);
		theBrowser.setBounds(itsFirstChild, itsLastChild);
		
		return theBrowser;
	}

	public IBehaviorExitEvent getExitEvent()
	{		
		if (itsExitEvent == null)
		{
			if (itsCallInfo == null) initCallInfo();

			if (itsCallInfo.getExitEvent() == null)
			{
				itsExitEvent = null;
			}
			else if (itsCallInfo.getLastChild() != null 
					&& itsLastChild != null
					&& itsCallInfo.getLastChild().equals(itsCallInfo.getExitEvent()))
			{
				itsExitEvent = (IBehaviorExitEvent) itsLastChild;
			}
			else
			{
				itsExitEvent = (IBehaviorExitEvent) getLogBrowser().getEvent(itsCallInfo.getExitEvent());
			}
		}

		return itsExitEvent;
	}
	
	/**
	 * Retrieves exit event and if there are real children.
	 * @author gpothier
	 */
	private static class CallInfoBuilder extends Query<CallInfo>
	{
		private static final long serialVersionUID = -4193913344574735748L;
		
		private final ExternalPointer itsEventPointer;
		
		/**
		 * If the exit event is found the result of this query is immutable;
		 * otherwise it might change as the database is updated.
		 */
		private boolean itsExitEventFound = false;

		public CallInfoBuilder(ExternalPointer aEventPointer)
		{
			itsEventPointer = aEventPointer;
		}

		public CallInfo run(ILogBrowser aLogBrowser)
		{
			long t0 = System.currentTimeMillis();
			ILogEvent theCallEvent = aLogBrowser.getEvent(itsEventPointer);
			long theTimestamp = theCallEvent.getTimestamp();
			assert theTimestamp == itsEventPointer.getTimestamp();
			
			int theDepth = theCallEvent.getDepth();
			IThreadInfo theThread = theCallEvent.getThread();

			long t1 = System.currentTimeMillis();

			ILogEvent theFirstChild = null;
			ILogEvent theLastChild = null;
			IBehaviorExitEvent theExitEvent = null;
			boolean theHasRealChildren;
			
			// Find the behavior exit event.
			// First, find next event at the same depth
			IEventFilter theFilter = aLogBrowser.createIntersectionFilter(
					aLogBrowser.createThreadFilter(theThread),
					aLogBrowser.createDepthFilter(theDepth));
			
			IEventBrowser theBrowser = aLogBrowser.createBrowser(theFilter);
			boolean theFound = theBrowser.setPreviousEvent(theCallEvent);
			assert theFound : itsEventPointer+" - "+theCallEvent;
			long t2 = System.currentTimeMillis();
			
			if (theBrowser.hasNext())
			{
				// We found the next event at the same depth, so the previous
				// event (at any depth) should be the exit event.
				ILogEvent theNextEvent = theBrowser.next();
				theFilter = aLogBrowser.createThreadFilter(theThread);
				
				theBrowser = aLogBrowser.createBrowser(theFilter);
				theFound = theBrowser.setNextEvent(theNextEvent);
				assert theFound;
				
				ILogEvent theEvent = theBrowser.previous();
				assert theEvent.getDepth() == theDepth+1 : 
					String.format("Event depth issue for %d (theEvent.getDepth(): %d, theDepth: %d)",
							theTimestamp,
							theEvent.getDepth(),
							theDepth);
				assert theEvent.getTimestamp() >= theTimestamp : "Event TimeStamp issue: " +theEvent.getTimestamp() +" while parent timestamp is " +theTimestamp ;
				assert theEvent.getParentPointer().getTimestamp() == theTimestamp : "Parent TimeStamp issue: " +theEvent.getTimestamp() +" while parent timestamp should be " +theTimestamp ;
				
				theLastChild = theEvent;
				if (theEvent instanceof IBehaviorExitEvent)
				{
					theExitEvent = (IBehaviorExitEvent) theEvent;
				}
			}
			else
			{
				// This event was the last event at this depth, so we must
				// find the last event of the thread.
				theFilter = aLogBrowser.createThreadFilter(theThread);
				
				theBrowser = aLogBrowser.createBrowser(theFilter);
				theBrowser.setPreviousTimestamp(Long.MAX_VALUE);
				ILogEvent theEvent = theBrowser.previous();
				if (theEvent.getDepth() == theDepth+1)
				{
					assert theEvent.getParentPointer().getTimestamp() == theTimestamp;
					theLastChild = theEvent;
					if (theEvent instanceof IBehaviorExitEvent)
					{
						theExitEvent = (IBehaviorExitEvent) theEvent;
					}
				}
			}
			
			long t3 = System.currentTimeMillis();

			// Find out if we have real children
			theFilter = aLogBrowser.createIntersectionFilter(
					aLogBrowser.createThreadFilter(theThread),
					aLogBrowser.createDepthFilter(theDepth+1));
			
			theBrowser = aLogBrowser.createBrowser(theFilter);
			theBrowser.setPreviousEvent(theCallEvent);
			long t4 = System.currentTimeMillis();

			if (theBrowser.hasNext())
			{
				ILogEvent theEvent = theBrowser.next();
				assert theEvent.getParentPointer().getTimestamp() == theTimestamp;
				
				theFirstChild = theEvent;
				theHasRealChildren = ! theFirstChild.equals(theExitEvent);
			}
			else theHasRealChildren = false;
			
			long t5 = System.currentTimeMillis();

			TODUtils.logf(1,
					"[CallInfoBuilder] timings: %d %d %d %d %d - total %d",
					t1-t0,
					t2-t1,
					t3-t2,
					t4-t3,
					t5-t4,
					t5-t0);
			
			itsExitEventFound = theExitEvent != null;
			
			return new CallInfo(
					theFirstChild != null ? theFirstChild.getPointer() : null,
					theLastChild != null ? theLastChild.getPointer() : null,
					theExitEvent != null ? theExitEvent.getPointer() : null,
					theHasRealChildren);
		}

		@Override
		public boolean recomputeOnUpdate()
		{
			return ! itsExitEventFound;
		}

		@Override
		public int hashCode()
		{
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((itsEventPointer == null) ? 0 : itsEventPointer.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final CallInfoBuilder other = (CallInfoBuilder) obj;
			if (itsEventPointer == null)
			{
				if (other.itsEventPointer != null) return false;
			}
			else if (!itsEventPointer.equals(other.itsEventPointer)) return false;
			return true;
		}
		
	}

	private static class CallInfo implements Serializable
	{
		private static final long serialVersionUID = 642849421884431178L;

		private final ExternalPointer itsFirstChild;
		private final ExternalPointer itsLastChild;
		private final ExternalPointer itsExitEvent;
		private final boolean itsHasRealChildren;

		public CallInfo(
				ExternalPointer aFirstChild, 
				ExternalPointer aLastChild, 
				ExternalPointer aExitEvent, 
				boolean aHasRealChildren)
		{
			itsFirstChild = aFirstChild;
			itsLastChild = aLastChild;
			itsExitEvent = aExitEvent;
			itsHasRealChildren = aHasRealChildren;
		}

		public ExternalPointer getFirstChild()
		{
			return itsFirstChild;
		}

		public ExternalPointer getLastChild()
		{
			return itsLastChild;
		}

		public ExternalPointer getExitEvent()
		{
			return itsExitEvent;
		}

		public boolean hasRealChildren()
		{
			return itsHasRealChildren;
		}
	}
}
