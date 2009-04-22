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
package tod.impl.local.event;

import java.util.ArrayList;
import java.util.List;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.event.ILogEvent;
import tod.impl.common.event.Event;
import tod.impl.local.EventBrowser;

public abstract class BehaviorCallEvent extends tod.impl.common.event.BehaviorCallEvent 
{
	private List<ILogEvent> itsChildren;
	private IBehaviorExitEvent itsExitEvent;
	private boolean itsExitEventFound = false;

	public BehaviorCallEvent(ILogBrowser aLogBrowser)
	{
		super(aLogBrowser);
	}

	public boolean hasRealChildren()
	{
		if (itsChildren.size() == 0) return false;
		else return itsChildren.get(0) != getExitEvent();
	}

	public IEventBrowser getChildrenBrowser()
	{
		return new EventBrowser(getLogBrowser(), itsChildren, null);
	}
	
	public IEventBrowser getCFlowBrowser()
	{
		throw new UnsupportedOperationException();
	}

	public void addChild (Event aEvent)
	{
		if (itsChildren == null) itsChildren = new ArrayList<ILogEvent>();
		itsChildren.add(aEvent);
	}

	public void addChild (int aIndex, Event aEvent)
	{
		if (itsChildren == null) itsChildren = new ArrayList<ILogEvent>();
		itsChildren.add(aIndex, aEvent);
	}
	
	public IBehaviorExitEvent getExitEvent()
	{
		if (! itsExitEventFound)
		{
			if (itsChildren.size() > 0)
			{
				ILogEvent theLastEvent = itsChildren.get(itsChildren.size()-1);
				if (theLastEvent instanceof IBehaviorExitEvent)
				{
					itsExitEvent = (IBehaviorExitEvent) theLastEvent;
				}
			}
			itsExitEventFound = true;
		}
		
		return itsExitEvent;
	}
}
