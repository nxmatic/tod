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
package tod.gui.activities.cflow;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import tod.gui.activities.IEventListSeed;
import tod.gui.formatter.EventFormatter;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * This seed permits to display the cflow view of a particualr thread.
 * @author gpothier
 */
public class CFlowSeed extends ActivitySeed
implements IEventListSeed
{
	private IThreadInfo itsThread;
	
	private IRWProperty<ILogEvent> pSelectedEvent = new SimpleRWProperty<ILogEvent>(this)
	{
		@Override
		protected void changed(ILogEvent aOldValue, ILogEvent aNewValue)
		{
			if (aNewValue instanceof IBehaviorExitEvent) aNewValue = aNewValue.getParent();
			itsThread = aNewValue.getThread();
		}
	};
	
	private IRWProperty<IParentEvent> pRootEvent = new SimpleRWProperty<IParentEvent>(this);
	private IRWProperty<ILogEvent> pLeafEvent = new SimpleRWProperty<ILogEvent>(this);
	
	private IRWProperty<ObjectId> pInspectedObject = new SimpleRWProperty<ObjectId>(this);
	
	public CFlowSeed(ILogBrowser aLog, ILogEvent aSelectedEvent)
	{
		super(aLog);
		itsThread = aSelectedEvent.getThread();
		pSelectedEvent().set(aSelectedEvent);
		pLeafEvent().set(aSelectedEvent);
		IParentEvent theRoot = aLog.getCFlowRoot(itsThread);
		pRootEvent().set(theRoot);
	}
	
	public static CFlowSeed forThread(ILogBrowser aLog, IThreadInfo aThread)
	{
		IParentEvent theRoot = aLog.getCFlowRoot(aThread);
		ILogEvent theSelectedEvent = null;
		IEventBrowser theChildrenBrowser = theRoot.getChildrenBrowser();
		if (theChildrenBrowser.hasNext())
		{
			theSelectedEvent = theChildrenBrowser.next();
		}
		
		CFlowSeed theSeed = new CFlowSeed(aLog, theSelectedEvent);
		theSeed.pRootEvent().set(theRoot);
//		theSeed.pParentEvent().set(theRoot);
		return theSeed;
	}

	@Override
	public Class< ? extends ActivityPanel> getComponentClass()
	{
		return CFlowActivityPanel.class;
	}
	
	public IThreadInfo getThread()
	{
		return itsThread;
	}

	/**
	 * The currently selected event in the tree.
	 */
	public IRWProperty<ILogEvent> pSelectedEvent()
	{
		return pSelectedEvent;
	}

	/**
	 * The event at the root of the CFlow tree. Ancestors of the root event
	 * are displayed in the call stack.  
	 */
	public IRWProperty<IParentEvent> pRootEvent()
	{
		return pRootEvent;
	}

	/**
	 * The bottommost event in the control flow (for call stack display).
	 */
	public IRWProperty<ILogEvent> pLeafEvent()
	{
		return pLeafEvent;
	}
	
	/**
	 * The currently inspected object, if any.
	 */
	public IRWProperty<ObjectId> pInspectedObject()
	{
		return pInspectedObject;
	}

	public IRWProperty<ILogEvent> pEvent()
	{
		return pSelectedEvent;
	}

	@Override
	public String getKindDescription()
	{
		return "Control flow view";
	}

	@Override
	public String getShortDescription()
	{
		return EventFormatter.formatEvent(getLogBrowser(), pSelectedEvent.get());
	}
	
	public IEventBrowser getEventBrowser()
	{
		ILogEvent theSelectedEvent = pSelectedEvent().get();
		IParentEvent theParent = theSelectedEvent.getParent();
		return theParent.getChildrenBrowser();
	}
}
