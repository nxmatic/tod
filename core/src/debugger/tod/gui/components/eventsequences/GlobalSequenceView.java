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

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.ILogBrowser;
import tod.gui.IGUIManager;

public class GlobalSequenceView extends AbstractSingleBrowserSequenceView
{
	public static final Color EVENT_COLOR = Color.GRAY;

	private final GlobalSequenceSeed itsSeed;
	
	public GlobalSequenceView(IGUIManager aGUIManager, GlobalSequenceSeed aSeed)
	{
		super(aGUIManager, EVENT_COLOR);
		itsSeed = aSeed;
	}

	@Override
	public ILogBrowser getLogBrowser()
	{
		return itsSeed.getLogBrowser();
	}
	
	@Override
	protected IEventBrowser getBrowser()
	{
		return getLogBrowser().createBrowser();
	}

	public String getTitle()
	{
		return "All events";
	}
	
}
