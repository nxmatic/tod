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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.event.ILogEvent;
import tod.gui.BrowserData;
import tod.gui.IGUIManager;
import tod.gui.components.eventsequences.AbstractSequenceView;
import zz.utils.list.IList;
import zz.utils.list.IListListener;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;

/**
 * A sequence view that displays a browser as background, which permits to
 * give a context to a foreground browser.
 * @author gpothier
 */
public class HighlighterSequenceView extends AbstractSequenceView
implements IPropertyListener, IListListener
{
	private HighlighterSequenceSeed itsSeed;
	
	public HighlighterSequenceView(IGUIManager aGUIManager, HighlighterSequenceSeed aSeed)
	{
		super(aGUIManager);
		itsSeed = aSeed;
	}

	
	public HighlighterSequenceSeed getSeed()
	{
		return itsSeed;
	}
	
	@Override
	public void addNotify()
	{
		super.addNotify();
		getSeed().pBackgroundBrowser.addHardListener(this);
		getSeed().pForegroundBrowsers.addHardListener(this);
	}
	
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		getSeed().pBackgroundBrowser.removeListener(this);
		getSeed().pForegroundBrowsers.removeListener(this);
	}

	@Override
	protected final List<BrowserData> getBrowsers()
	{
		List<BrowserData> theBrowsers = new ArrayList<BrowserData>();
		
		IEventBrowser theBackgroundBrowser = getSeed().pBackgroundBrowser.get();
		if(theBackgroundBrowser != null)
		{
			theBrowsers.add (new BrowserData(theBackgroundBrowser, Color.GRAY));
		}
		
		for (BrowserData theBrowserData : getSeed().pForegroundBrowsers)
		{
			theBrowsers.add (new BrowserData(
					theBackgroundBrowser.createIntersection(theBrowserData.browser.getFilter()), 
					theBrowserData.color,
					theBrowserData.markSize));
		}
		
		return theBrowsers;
	}


	public String getTitle()
	{
		return getSeed().getTitle();
	}

	@Override
	protected ILogEvent getEventAt(long aTimestamp, long aTolerance)
	{
		return getEventAt(itsSeed.pBackgroundBrowser.get().clone(), aTimestamp, aTolerance);
	}

	public void propertyChanged(IProperty aProperty, Object aOldValue, Object aNewValue)
	{
		update();
	}


	public void propertyValueChanged(IProperty aProperty)
	{
	}


	public void elementAdded(IList aList, int aIndex, Object aElement)
	{
		update();
	}


	public void elementRemoved(IList aList, int aIndex, Object aElement)
	{
		update();
	}

}
