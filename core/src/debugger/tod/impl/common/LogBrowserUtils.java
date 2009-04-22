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
package tod.impl.common;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IConstructorChainingEvent;
import tod.core.database.event.IExceptionGeneratedEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.event.IConstructorChainingEvent.CallType;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IThreadInfo;
import tod.impl.common.event.Event;
import tod.impl.local.event.BrowserRootEvent;
import tod.impl.local.event.ListRootEvent;
import tod.tools.monitoring.Monitored;

/**
 * Utility methods for implementing log browsers.
 * @author gpothier
 */
public class LogBrowserUtils 
{
	/**
	 * Retrieves the event corresponding to the given pointer from a log browser.
	 */
	@Monitored
	public static ILogEvent getEvent(ILogBrowser aLogBrowser, ExternalPointer aPointer)
	{
		IEventFilter theFilter = aLogBrowser.createThreadFilter(aPointer.getThread());
		IEventBrowser theBrowser = aLogBrowser.createBrowser(theFilter);
		
		theBrowser.setNextTimestamp(aPointer.getTimestamp());
		if (theBrowser.hasNext())
		{
			ILogEvent theEvent = theBrowser.next();
			
			assert theEvent.getThread().equals(aPointer.getThread());
			if (theEvent.getTimestamp() == aPointer.getTimestamp()) return theEvent;
		}

		return null;
	}
	
	/**
	 * Indicates if the specified event is a member of the specified browser's 
	 * result set.
	 * This method might move the browser's event pointer.
	 */
	@Monitored
	public static boolean hasEvent(IEventBrowser aBrowser, ILogEvent aEvent)
	{
		aBrowser.setNextEvent(aEvent);
		if (aBrowser.hasNext())
		{
			ILogEvent theNext = aBrowser.next();
			if (aEvent.equals(theNext)) return true;
		}
		return false;
	}

	/**
	 * Implementation of {@link ILogBrowser#getCFlowRoot(IThreadInfo)} 
	 */
	@Monitored
	public static IParentEvent createCFlowRoot(ILogBrowser aBrowser, IThreadInfo aThread)
	{
		return new BrowserRootEvent(aBrowser, aThread);
	}
	
	public static CallType isSuperCall(IConstructorChainingEvent aEvent)
	{
		IBehaviorInfo theExecutedBehavior = aEvent.getExecutedBehavior();
		IBehaviorInfo theCallingBehavior = aEvent.getCallingBehavior();
		if (theCallingBehavior == null
				|| theExecutedBehavior == null) 
		{
			return CallType.UNKNOWN;
		}
		else if (theExecutedBehavior.getDeclaringType().equals(theCallingBehavior.getDeclaringType()))
		{
			return CallType.THIS;
		}
		else 
		{
			return CallType.SUPER;
		}
	}

}
