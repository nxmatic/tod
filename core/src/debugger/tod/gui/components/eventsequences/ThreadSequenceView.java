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
package tod.gui.components.eventsequences;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import tod.core.DebugFlags;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;
import tod.gui.IGUIManager;
import tod.gui.activities.cflow.CFlowSeed;
import tod.gui.activities.filteredevents.FilterSeed;
import tod.tools.scheduling.Scheduled;
import zz.utils.ItemAction;
import zz.utils.Utils;

public class ThreadSequenceView extends AbstractSingleBrowserSequenceView
{
	public static final Color EVENT_COLOR = Color.GRAY;

	private final ThreadSequenceSeed itsSeed;
	
	public ThreadSequenceView(IGUIManager aGUIManager, ThreadSequenceSeed aSeed)
	{
		super(aGUIManager, EVENT_COLOR);
		itsSeed = aSeed;
		
		if (DebugFlags.SHOW_DEBUG_GUI)
		{
			addBaseAction(new ShowCFlowAction());
			addBaseAction(new ShowEventsAction());
		}
	}

	@Override
	public ILogBrowser getLogBrowser()
	{
		return itsSeed.getLogBrowser();
	}
	
	public IThreadInfo getThread()
	{
		return itsSeed.getThread();
	}
	
	@Override
	protected void muralClicked()
	{
		showCFlow();
	}
	
	@Override
	protected IEventBrowser getBrowser()
	{
		IEventFilter theFilter = getLogBrowser().createThreadFilter(getThread());
		IEventBrowser theBrowser = getLogBrowser().createBrowser(theFilter);
		return theBrowser;
	}

	public String getTitle()
	{
		List<IHostInfo> theHosts = new ArrayList<IHostInfo>();
		Utils.fillCollection(theHosts, getLogBrowser().getHosts());

		if (theHosts.size() > 1)
		{
			return String.format(
					"Thread \"%s\" [id %d] on host \"%s\" [id %d]",
					getThread().getName(),
					getThread().getId(),
					getThread().getHost().getName(),
					getThread().getHost().getId());
		}
		else
		{
			return String.format(
					"Thread \"%s\" [id %d]",
					getThread().getName(),
					getThread().getId());
		}
	}
	
	@Scheduled
	protected void showCFlow()
	{
		CFlowSeed theSeed = CFlowSeed.forThread(getLogBrowser(), getThread());
		getGUIManager().openSeed(theSeed, false);
	}
	
	private class ShowCFlowAction extends ItemAction
	{
		public ShowCFlowAction()
		{
			setTitle("view control flow");
			setDescription(
					"<html>" +
					"<b>Show control flow.</b> Shows the control flow of <br>" +
					"this thread.");
		}
		
		@Override
		public void actionPerformed(ActionEvent aE)
		{
			showCFlow();
		}
	}
	
	private class ShowEventsAction extends ItemAction
	{
		public ShowEventsAction()
		{
			setTitle("(all events)");
			setDescription(
					"<html>" +
					"<b>Show all events.</b> Show all the events of this <br>" +
					"thread in a list. This is used to debug TOD itself.");
		}

		@Override
		public void actionPerformed(ActionEvent aE)
		{
			IEventFilter theFilter = getLogBrowser().createThreadFilter(getThread());
			FilterSeed theSeed = new FilterSeed(
					getLogBrowser(), 
					"All events of thread "+getThread().getName(),
					theFilter);
			getGUIManager().openSeed(theSeed, false);
		}
	}
}
