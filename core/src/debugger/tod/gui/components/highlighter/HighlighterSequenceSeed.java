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
package tod.gui.components.highlighter;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.gui.BrowserData;
import tod.gui.IGUIManager;
import tod.gui.components.eventsequences.IEventSequenceSeed;
import tod.gui.components.eventsequences.IEventSequenceView;
import zz.utils.list.IList;
import zz.utils.list.ZArrayList;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

public class HighlighterSequenceSeed implements IEventSequenceSeed
{
	private String itsTitle;
	
	public final IRWProperty<IEventBrowser> pBackgroundBrowser =
		new SimpleRWProperty<IEventBrowser>();
	
	public final IList<BrowserData> pForegroundBrowsers =
		new ZArrayList<BrowserData>();

	public HighlighterSequenceSeed(
			String aTitle,
			IEventBrowser aBackgroundBrowser,
			BrowserData... aForegroundBrowsers)
	{
		itsTitle = aTitle;
		pBackgroundBrowser.set(aBackgroundBrowser);
		
		if (aForegroundBrowsers != null) for (BrowserData theBrowserData : aForegroundBrowsers) 
			pForegroundBrowsers.add(theBrowserData);
	}

	public IEventSequenceView createView(IGUIManager aGUIManager)
	{
		return new HighlighterSequenceView(aGUIManager, this);
	}

	public String getTitle()
	{
		return itsTitle;
	}
}
