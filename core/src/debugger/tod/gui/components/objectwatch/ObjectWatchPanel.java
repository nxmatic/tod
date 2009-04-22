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
package tod.gui.components.objectwatch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import tod.gui.BrowserNavigator;
import tod.gui.FrozenContext;
import tod.gui.GUIUtils;
import tod.gui.Hyperlinks;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.components.objectwatch.AbstractWatchProvider.Entry;
import tod.gui.kit.AsyncPanel;
import tod.gui.kit.Bus;
import tod.gui.kit.BusPanel;
import tod.gui.kit.IBusListener;
import tod.gui.kit.messages.ShowObjectMsg;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.JobGroup;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.SimpleAction;
import zz.utils.ui.ScrollablePanel;

/**
 * A panel that shows the contents of a stack frame or of an object.
 * @author gpothier
 */
public class ObjectWatchPanel extends BusPanel
{
	private final IContext itsContext;
	private final boolean itsListenBus;
	private WatchBrowserNavigator itsBrowserNavigator;
	private JobGroup itsJobGroup;
	private JScrollPane itsScrollPane;
	private AbstractWatchProvider itsProvider;
	private List<Entry> itsEntries;
	
	private IBusListener<ShowObjectMsg> itsShowObjectListener = new IBusListener<ShowObjectMsg>()
	{
		public boolean processMessage(ShowObjectMsg aMessage)
		{
			openWatch(new ObjectWatchSeed(
					getGUIManager(),
					aMessage.getTitle(), 
					ObjectWatchPanel.this,
					aMessage.getRefEvent(), 
					aMessage.getObjectId()));
			
			return true;
		}
	};
	
	/**
	 * @param aListenBus Whether this watch panel listen the bus for 
	 * {@link ShowObjectMsg} messages.
	 */
	public ObjectWatchPanel(IContext aContext, boolean aListenBus)
	{
		super(aContext.getBus());
		itsContext = aContext;
		itsListenBus = aListenBus;
		itsJobGroup = new JobGroup(getGUIManager().getJobScheduler());
		itsBrowserNavigator = new WatchBrowserNavigator(itsJobGroup);
		
		createUI();
	}
	
	private void createUI()
	{
		setLayout(new BorderLayout());
		
		JPanel theToolbar = new JPanel();
		
		Action theShowFrameAction = new SimpleAction("frame")
		{
			public void actionPerformed(ActionEvent aE)
			{
				showStackFrame();
			}
		};
		
		theToolbar.add(new JButton(theShowFrameAction));
		theToolbar.add(new JButton(itsBrowserNavigator.getBackwardAction()));
		theToolbar.add(new JButton(itsBrowserNavigator.getForwardAction()));
		
		Action thePostItAction = new SimpleAction("PostIt")
		{
			public void actionPerformed(ActionEvent aE)
			{
				showPostIt();
			}
		};
		theToolbar.add(new JButton(thePostItAction));
		
		add(theToolbar, BorderLayout.NORTH);
		
		itsScrollPane = new JScrollPane();
		itsScrollPane.getViewport().setBackground(Color.WHITE);
		add(itsScrollPane, BorderLayout.CENTER);
	}
	
	@Override
	public void addNotify()
	{
		super.addNotify();
		if (itsListenBus) Bus.get(this).subscribe(ShowObjectMsg.ID, itsShowObjectListener);
	}
	
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		itsJobGroup.cancelAll();
		if (itsListenBus) Bus.get(this).unsubscribe(ShowObjectMsg.ID, itsShowObjectListener);
	}
	
	public IGUIManager getGUIManager()
	{
		return itsContext.getGUIManager();
	}
	
	public IContext getContext()
	{
		return itsContext;
	}
	
	/**
	 * Returns a job processor that only contains jobs for this watch
	 * panel.
	 */
	public IJobScheduler getJobScheduler()
	{
		return itsJobGroup;
	}

	public void showStackFrame()
	{
		ILogEvent theRefEvent = getContext().pSelectedEvent().get();
		if (theRefEvent == null) return;
		
		itsBrowserNavigator.open(new StackFrameWatchSeed(
				getGUIManager(),
				"frame",
				ObjectWatchPanel.this,
				theRefEvent));
	}
	
	private void showPostIt()
	{
		ObjectWatchPanel thePostIt = new ObjectWatchPanel(
				FrozenContext.create(getContext()),
				false);
		
		thePostIt.openWatch(itsBrowserNavigator.getCurrentSeed());
		
		getGUIManager().showPostIt(thePostIt, getPreferredSize());
	}
	
	public void openWatch(WatchSeed aSeed)
	{
		itsBrowserNavigator.open(aSeed);
	}
	
	/**
	 * Shows the watch data obtained from the specified provider.
	 */
	public <E> void showWatch(AbstractWatchProvider aProvider)
	{
		itsProvider = aProvider;
		getJobScheduler().cancelAll();
				
		JPanel theEntriesPanel = new ScrollablePanel(GUIUtils.createStackLayout());
		theEntriesPanel.setOpaque(false);
		
		theEntriesPanel.add(aProvider.buildTitleComponent(getJobScheduler()));
		
		ObjectId theCurrentObject = aProvider.getCurrentObject();
		if (theCurrentObject != null)
		{
			theEntriesPanel.add(buildCurrentObjectLine(theCurrentObject));
		}
				
		theEntriesPanel.add(new AsyncPanel(getJobScheduler(), JobPriority.AUTO)
		{
			@Override
			protected void runJob()
			{
				itsEntries = itsProvider.getEntries(0, Integer.MAX_VALUE);
			}

			@Override
			protected void updateSuccess()
			{
				setLayout(GUIUtils.createStackLayout());
				if (itsEntries != null) for (Entry theEntry : itsEntries)
				{
					if ("this".equals(theEntry.getName())) continue;
					
					add(new WatchEntryNode(
							getGUIManager(),
							getJobScheduler(),
							ObjectWatchPanel.this,
							itsProvider,
							theEntry));
				}
			}
		});

		itsScrollPane.setViewportView(theEntriesPanel);
	}
	
	private JComponent buildCurrentObjectLine(ObjectId aCurrentObject)
	{
		JPanel theContainer = new JPanel(GUIUtils.createSequenceLayout());
		theContainer.setOpaque(false);
		
		theContainer.add(GUIUtils.createLabel("this = "));
		
		theContainer.add(Hyperlinks.object(
				getGUIManager(), 
				Hyperlinks.SWING, 
				getJobScheduler(),
				aCurrentObject,
				itsProvider.getRefEvent(),
				true));
		
		return theContainer;		
		
	}
	
	private class WatchBrowserNavigator extends BrowserNavigator<WatchSeed>
	{
		public WatchBrowserNavigator(IJobScheduler aJobScheduler)
		{
			super(aJobScheduler);
		}

		@Override
		public void open(WatchSeed aSeed)
		{
			super.open(aSeed);
		}
		
		@Override
		protected void setSeed(WatchSeed aSeed)
		{
			super.setSeed(aSeed);
			showWatch(aSeed.createProvider());
		}
		
	}

}
