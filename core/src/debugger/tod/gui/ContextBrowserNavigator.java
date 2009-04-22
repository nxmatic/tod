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
package tod.gui;

import java.lang.reflect.Constructor;

import javax.swing.JPanel;

import tod.core.database.event.ILogEvent;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import tod.gui.activities.IEventSeed;
import zz.utils.properties.PropertyUtils;
import zz.utils.properties.PropertyUtils.Connector;
import zz.utils.ui.StackLayout;

/**
 * Browser navigator for a given context.
 * @author gpothier
 */
public class ContextBrowserNavigator extends BrowserNavigator<ActivitySeed>
{
	private final IContext itsContext;
	private JPanel itsContainer;
	private ActivityPanel itsCurrentActivityPanel;
	private Connector<ILogEvent> itsSelectedEventConnector;
	
	public ContextBrowserNavigator(IContext aContext)
	{
		super(aContext.getGUIManager().getJobScheduler());
		itsContext = aContext;
		itsContainer = new JPanel(new StackLayout());
	}

	public JPanel getActivityContainer()
	{
		return itsContainer;
	}

	@Override
	protected void setSeed (ActivitySeed aSeed)
	{
		ActivityPanel thePreviousPanel = itsCurrentActivityPanel;
		
		try
		{
			if (itsCurrentActivityPanel != null 
					&& (aSeed == null 
							|| ! itsCurrentActivityPanel.getClass().equals(aSeed.getComponentClass())))
			{
				// Drop current view
				itsContainer.remove(itsCurrentActivityPanel);
				itsCurrentActivityPanel = null;
			}
			
			if (itsCurrentActivityPanel == null && aSeed != null)
			{
				Class<? extends ActivityPanel> theClass = aSeed.getComponentClass();
				Constructor<? extends ActivityPanel> theConstructor = theClass.getConstructor(IContext.class);
				itsCurrentActivityPanel = theConstructor.newInstance(itsContext);
				itsCurrentActivityPanel.init();
				itsContainer.add(itsCurrentActivityPanel);
			}

			if (itsSelectedEventConnector != null)
			{
				itsSelectedEventConnector.disconnect();
				itsSelectedEventConnector = null;
			}

			if (aSeed instanceof IEventSeed)
			{
				IEventSeed theEventSeed = (IEventSeed) aSeed;
				itsSelectedEventConnector = PropertyUtils.connect(
						theEventSeed.pEvent(), 
						itsContext.pSelectedEvent(), 
						true);
			}
			else
			{
				itsContext.pSelectedEvent().set(null);
			}

			
			if (itsCurrentActivityPanel != null) itsCurrentActivityPanel.setSeed(aSeed);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (itsCurrentActivityPanel != thePreviousPanel) viewChanged(itsCurrentActivityPanel);
		
		super.setSeed(aSeed);
		
		itsContainer.revalidate();
		itsContainer.repaint();
//		itsContainer.validate();
	}
	
	/**
	 * Called when a new view is displayed. Does
	 * nothing by default.
	 */
	protected void viewChanged(ActivityPanel theView)
	{
	}

}
