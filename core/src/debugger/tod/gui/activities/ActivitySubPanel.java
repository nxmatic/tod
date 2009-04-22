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
package tod.gui.activities;

import javax.swing.JPanel;

import tod.core.database.browser.ILogBrowser;
import tod.gui.IGUIManager;
import tod.gui.kit.Bus;
import tod.gui.kit.IBusOwner;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobSchedulerProvider;
import zz.utils.properties.IRWProperty;

/**
 * An abstract base class that eases the creation of sub panels for activity panels.
 * @author gpothier
 */
public abstract class ActivitySubPanel<T extends ActivitySeed> extends JPanel
implements IJobSchedulerProvider, IBusOwner
{
	private final ActivityPanel<T> itsActivityPanel;

	public ActivitySubPanel(ActivityPanel<T> aActivityPanel)
	{
		itsActivityPanel = aActivityPanel;
	}
	
	protected IGUIManager getGUIManager()
	{
		return itsActivityPanel.getGUIManager();
	}
	
	protected ILogBrowser getLogBrowser()
	{
		return itsActivityPanel.getLogBrowser();
	}

	public IJobScheduler getJobScheduler()
	{
		return itsActivityPanel.getJobScheduler();
	}

	protected T getSeed()
	{
		return itsActivityPanel.getSeed();
	}
	
	/**
	 * Called by the view when {@link ActivityPanel#connectSeed(ActivitySeed)}
	 * is called.
	 */
	public void connectSeed(T aSeed)
	{
	}
	
	/**
	 * Called by the view when {@link ActivityPanel#disconnectSeed(ActivitySeed)}
	 * is called.
	 */
	public void disconnectSeed(T aSeed)
	{
	}
	
	protected <V> void connect (IRWProperty<V> aSource, IRWProperty<V> aTarget)
	{
		itsActivityPanel.connect(aSource, aTarget);
	}
	
	protected <V> void disconnect (IRWProperty<V> aSource, IRWProperty<V> aTarget)
	{
		itsActivityPanel.disconnect(aSource, aTarget);
	}

	public Bus getBus() 
	{
		return itsActivityPanel.getBus();
	}

}
