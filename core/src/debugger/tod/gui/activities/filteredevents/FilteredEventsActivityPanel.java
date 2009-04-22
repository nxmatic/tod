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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JSplitPane;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.LocationUtils;
import tod.core.database.event.ILogEvent;
import tod.gui.BrowserData;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.activities.ActivityPanel;
import tod.gui.components.eventlist.EventListPanel;
import tod.gui.components.highlighter.EventHighlighter;
import tod.gui.kit.Bus;
import tod.gui.kit.Options;
import tod.gui.kit.SavedSplitPane;
import tod.gui.kit.html.HtmlComponent;
import tod.gui.kit.html.HtmlDoc;
import tod.gui.kit.messages.ShowCFlowMsg;
import tod.gui.kit.messages.EventSelectedMsg.SelectionMethod;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.properties.PropertyListener;

/**
 * A view component that displays a list of events 
 * based on a {@link tod.core.database.browser.IEventFilter}
 * @author gpothier
 */
public class FilteredEventsActivityPanel extends ActivityPanel<FilterSeed> 
{
	private static final String PROPERTY_SPLITTER_POS = "filterView.splitterPos";
	
	private EventListPanel itsListPanel;
	private EventHighlighter itsEventHighlighter;
	
	private IPropertyListener<ILogEvent> itsSelectedEventListener = new PropertyListener<ILogEvent>()
	{
		@Override
		public void propertyChanged(IProperty<ILogEvent> aProperty, ILogEvent aOldValue, ILogEvent aNewValue)
		{
			LocationUtils.gotoSource(getGUIManager(), aNewValue);
			IEventFilter theFilter = aNewValue != null ?
					getLogBrowser().createEventFilter(aNewValue)
					: null;
					
			itsEventHighlighter.pHighlightBrowsers.clear();
			itsEventHighlighter.pHighlightBrowsers.add(new BrowserData(
					getLogBrowser().createBrowser(theFilter),
					Color.BLUE,
					BrowserData.DEFAULT_MARK_SIZE));
		}
	};

	private HtmlComponent itsTitleComponent;
	
	public FilteredEventsActivityPanel(IContext aContext)
	{
		super(aContext);
		createUI ();
	}
	
	@Override
	protected void connectSeed(FilterSeed aSeed)
	{
		connect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
		connect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
		aSeed.pSelectedEvent().addHardListener(itsSelectedEventListener);
		
		itsListPanel.setBrowser(aSeed.getBaseFilter());
		itsTitleComponent.setDoc(aSeed.getTitle());
		itsTitleComponent.getPreferredSize(); // For some reason we have to call this in order for the size to be updated...
	}

	@Override
	protected void disconnectSeed(FilterSeed aSeed)
	{
		disconnect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
		disconnect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
		aSeed.pSelectedEvent().removeListener(itsSelectedEventListener);
	}

	@Override
	protected void initOptions(Options aOptions)
	{
		super.initOptions(aOptions);
		EventListPanel.createDefaultOptions(aOptions, false, true);
	}
	
	private void createUI()
	{
		JSplitPane theSplitPane = new SavedSplitPane(JSplitPane.HORIZONTAL_SPLIT, getGUIManager(), PROPERTY_SPLITTER_POS);
		theSplitPane.setResizeWeight(0.5);
		
		itsListPanel = new EventListPanel (getGUIManager(), getBus(), getLogBrowser(), getJobScheduler()); 
		
		itsListPanel.eEventActivated().addListener(new IEventListener<ILogEvent>()
				{
					public void fired(IEvent< ? extends ILogEvent> aEvent, ILogEvent aData)
					{
						Bus.get(FilteredEventsActivityPanel.this).postMessage(new ShowCFlowMsg(aData));
					}
				});
				
		setLayout(new BorderLayout());
		add (theSplitPane, BorderLayout.CENTER);
		itsTitleComponent = new HtmlComponent();
		itsTitleComponent.setOpaque(false);
		add(itsTitleComponent, BorderLayout.NORTH);
		
		theSplitPane.setLeftComponent(itsListPanel);
		
		itsEventHighlighter = new EventHighlighter(getGUIManager(), getLogBrowser());
		theSplitPane.setRightComponent(itsEventHighlighter);
	}
	
	public IEventBrowser getEventBrowser()
	{
		IEventFilter theFilter = getSeed().getBaseFilter();
		
		return theFilter != null ?
				getLogBrowser().createBrowser(theFilter)
				: getLogBrowser().createBrowser();
	}

	public ILogEvent getSelectedEvent()
	{
		return getSeed().pSelectedEvent().get();
	}

	public void selectEvent(ILogEvent aEvent, SelectionMethod aMethod)
	{
		getSeed().pSelectedEvent().set(aEvent);
	}
	
	
}
