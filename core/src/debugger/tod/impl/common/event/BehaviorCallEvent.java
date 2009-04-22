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
package tod.impl.common.event;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.structure.IBehaviorInfo;

public abstract class BehaviorCallEvent extends Event implements IBehaviorCallEvent
{
	private boolean itsDirectParent;
	private Object[] itsArguments;
	private IBehaviorInfo itsCalledBehavior;
	private IBehaviorInfo itsExecutedBehavior;
	private Object itsTarget;
	
	public BehaviorCallEvent(ILogBrowser aLogBrowser)
	{
		super(aLogBrowser);
	}

	
	public IBehaviorInfo getExecutedBehavior()
	{
		return itsExecutedBehavior;
	}

	public void setExecutedBehavior(IBehaviorInfo aExecutedBehavior)
	{
		itsExecutedBehavior = aExecutedBehavior;
	}
	
	public IBehaviorInfo getCalledBehavior()
	{
		return itsCalledBehavior;
	}

	public void setCalledBehavior(IBehaviorInfo aCalledBehavior)
	{
		itsCalledBehavior = aCalledBehavior;
	}

	public boolean isDirectParent()
	{
		return itsDirectParent;
	}

	public void setDirectParent(boolean aDirectParent)
	{
		itsDirectParent = aDirectParent;
	}

	public Object[] getArguments()
	{
		return itsArguments;
	}

	public void setArguments(Object[] aArguments)
	{
		itsArguments = aArguments;
	}

	public IBehaviorInfo getCallingBehavior()
	{
		if (getParent() == null) return null;
		else return getParent().isDirectParent() ? 
				getParent().getExecutedBehavior()
				: null;
	}

	public Object getTarget()
	{
		return itsTarget;
	}

	public void setTarget(Object aCurrentObject)
	{
		itsTarget = aCurrentObject;
	}
	
	public long getFirstTimestamp()
	{
		return getTimestamp();
	}

	public long getLastTimestamp()
	{
		IBehaviorExitEvent theExitEvent = getExitEvent();
		if (theExitEvent != null) return theExitEvent.getTimestamp();
		else return getChildrenBrowser().getLastTimestamp();
	}
}
