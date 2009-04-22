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

import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IThreadInfo;
import tod.tools.monitoring.Monitored;

/**
 * Provides forward and backward stepping operations.
 * @author gpothier
 */
public class Stepper
{
	private ILogBrowser itsBrowser;
	private ILogEvent itsCurrentEvent;

	public Stepper(ILogBrowser aBrowser)
	{
		itsBrowser = aBrowser;
	}

	public IThreadInfo getThread()
	{
		return getCurrentEvent().getThread();
	}

	public ILogEvent getCurrentEvent()
	{
		return itsCurrentEvent;
	}
	
	public void setCurrentEvent(ILogEvent aCurrentEvent)
	{
		itsCurrentEvent = aCurrentEvent;
	}
	
	@Monitored
	private void forward(IEventBrowser aBrowser)
	{
		aBrowser.setPreviousEvent(itsCurrentEvent);
		do
		{
			itsCurrentEvent = aBrowser.hasNext() ?
					aBrowser.next() 
					: null;
					
		} while (itsCurrentEvent instanceof IBehaviorExitEvent);
	}
	
	@Monitored
	private void backward(IEventBrowser aBrowser)
	{
		aBrowser.setNextEvent(itsCurrentEvent);
		do
		{
			itsCurrentEvent = aBrowser.hasPrevious() ? 
					aBrowser.previous() 
					: null;
					
		} while (itsCurrentEvent instanceof IBehaviorExitEvent);
	}

	public void forwardStepInto()
	{
		forward(itsBrowser.createBrowser(itsBrowser.createThreadFilter(getThread())));
	}
	
	public void backwardStepInto()
	{
		backward(itsBrowser.createBrowser(itsBrowser.createThreadFilter(getThread())));
	}
	
	public void forwardStepOver()
	{
		forward(itsBrowser.createBrowser(itsBrowser.createIntersectionFilter(
				itsBrowser.createThreadFilter(getThread()),
				itsBrowser.createDepthFilter(itsCurrentEvent.getDepth()))));
	}
	
	public void backwardStepOver()
	{
		backward(itsBrowser.createBrowser(itsBrowser.createIntersectionFilter(
				itsBrowser.createThreadFilter(getThread()),
				itsBrowser.createDepthFilter(itsCurrentEvent.getDepth()))));
	}
	
	public void stepOut()
	{
		itsCurrentEvent = itsCurrentEvent.getParent();
	}
}
