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
package tod.gui.activities.filteredevents;

import java.awt.Color;

import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ILogEvent;
import tod.gui.FontConfig;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import tod.gui.kit.html.HtmlDoc;
import tod.gui.kit.html.HtmlUtils;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * A seed that is based on a {@link tod.core.database.browser.IEventBrowser}.
 * Its view is simply a sequential view of filtered events.
 * @author gpothier
 */
public class FilterSeed extends ActivitySeed/*<FilterView>*/
{
	private final IEventFilter itsBaseFilter;
	private final String itsTitle;
	
	
	/**
	 * Timestamp of the first event displayed by this view.
	 */
	private long itsTimestamp;
	
	private IRWProperty<ILogEvent> pSelectedEvent = new SimpleRWProperty<ILogEvent>();
	
	private IRWProperty<IEventFilter> pAdditionalFilter = new SimpleRWProperty<IEventFilter>();

	public FilterSeed(
			ILogBrowser aLog, 
			String aTitle,
			IEventFilter aBaseFilter)
	{
		super(aLog);
		itsTitle = aTitle;
		itsBaseFilter = aBaseFilter;
	}
	
	@Override
	public Class< ? extends ActivityPanel> getComponentClass()
	{
		return FilteredEventsActivityPanel.class;
	}
	
	public long getTimestamp()
	{
		return itsTimestamp;
	}
	
	public void setTimestamp(long aTimestamp)
	{
		itsTimestamp = aTimestamp;
	}
	
	public IEventFilter getBaseFilter()
	{
		return itsBaseFilter;
	}
	
	public HtmlDoc getTitle()
	{
		return HtmlDoc.create("<b>"+HtmlUtils.escapeHTML(itsTitle)+"</b>", FontConfig.BIG, Color.BLACK);
	}
	
	/**
	 * The currently selected event in the list.
	 */
	public IRWProperty<ILogEvent> pSelectedEvent()
	{
		return pSelectedEvent;
	}

	@Override
	public String getKindDescription()
	{
		return"Event list";
	}

	@Override
	public String getShortDescription()
	{
		return itsTitle;
	}

}
