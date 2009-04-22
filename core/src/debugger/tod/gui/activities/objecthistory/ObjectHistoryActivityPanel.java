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
package tod.gui.activities.objecthistory;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JTabbedPane;

import tod.core.database.browser.ObjectIdUtils;
import tod.gui.FontConfig;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.activities.ActivityPanel;
import tod.gui.kit.html.HtmlComponent;
import tod.gui.kit.html.HtmlDoc;
import zz.utils.ui.StackLayout;

public class ObjectHistoryActivityPanel extends ActivityPanel<ObjectHistorySeed> 
{
	private ObjectEventsListPanel itsEventsListPanel;
	private ObjectMethodsPanel itsMethodsPanel;
	private HtmlComponent itsTitleComponent;
	
	public ObjectHistoryActivityPanel(IContext aContext)
	{
		super(aContext);
	}
	
	@Override
	public void init()
	{
		setLayout(new BorderLayout());

		// Title
		itsTitleComponent = new HtmlComponent();
		
		itsTitleComponent.setOpaque(false);
		add(itsTitleComponent, BorderLayout.NORTH);
		
		// Tabbed pane
		JTabbedPane theTabbedPane = new JTabbedPane();
		add(theTabbedPane, BorderLayout.CENTER);
		
		itsEventsListPanel = new ObjectEventsListPanel(this);
		theTabbedPane.add("All events", itsEventsListPanel);
		
		itsMethodsPanel = new ObjectMethodsPanel(this);
		theTabbedPane.add("By method", itsMethodsPanel);
	}
	
	@Override
	protected void connectSeed(ObjectHistorySeed aSeed)
	{
		String theTitle = ObjectIdUtils.getObjectDescription(
				getLogBrowser(), 
				aSeed.getObject(), 
				false);
		
		itsTitleComponent.setDoc(HtmlDoc.create("<b>"+theTitle+"</b>", FontConfig.BIG, Color.BLACK));

		itsEventsListPanel.connectSeed(aSeed);
		itsMethodsPanel.connectSeed(aSeed);
	}
	
	@Override
	protected void disconnectSeed(ObjectHistorySeed aSeed)
	{
		itsEventsListPanel.disconnectSeed(aSeed);
		itsMethodsPanel.disconnectSeed(aSeed);
	}
}

