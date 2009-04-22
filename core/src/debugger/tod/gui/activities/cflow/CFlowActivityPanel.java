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
package tod.gui.activities.cflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import tod.Util;
import tod.core.config.TODConfig;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.LocationUtils;
import tod.core.database.browser.Stepper;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.Resources;
import tod.gui.activities.ActivityPanel;
import tod.gui.components.BookmarksMural;
import tod.gui.components.eventlist.EventListPanel;
import tod.gui.components.eventlist.IntimacyLevel;
import tod.gui.components.intimacyeditor.IntimacyEditorButton;
import tod.gui.components.objectwatch.ObjectWatchSeed;
import tod.gui.components.objectwatch.ObjectWatchPanel;
import tod.gui.kit.Bus;
import tod.gui.kit.Options;
import tod.gui.kit.SavedSplitPane;
import tod.gui.kit.StdOptions;
import tod.gui.kit.messages.EventSelectedMsg;
import tod.gui.kit.messages.EventSelectedMsg.SelectionMethod;
import tod.gui.settings.IntimacySettings;
import tod.tools.scheduling.Scheduled;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.SimpleAction;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.PropertyListener;
import zz.utils.properties.SimplePropertyListener;
import zz.utils.ui.UIUtils;
import zz.utils.ui.ZLabel;

public class CFlowActivityPanel extends ActivityPanel<CFlowSeed> 
{
	public static final boolean SHOW_PARENT_FRAMES = false;
	private static final String PROPERTY_SPLITTER_POS = "cflowView.splitterPos";

	private Stepper itsStepper;
	
	private CFlowTree itsCFlowTree;
	private ObjectWatchPanel itsWatchPanel;
	
	private ZLabel itsHostLabel;
	private ZLabel itsThreadLabel;
	
	private BookmarksMural itsBookmarksMural;
	
	private BookmarkButton itsBookmarkButton;
	
	private IPropertyListener<ILogEvent> itsSelectedEventListener = new PropertyListener<ILogEvent>()
	{
		@Override
		public void propertyChanged(IProperty<ILogEvent> aProperty, ILogEvent aOldValue, ILogEvent aNewValue)
		{
			update();
		}
	};

	private IPropertyListener<ILogEvent> itsRootEventListener = new PropertyListener<ILogEvent>()
	{
		@Override
		public void propertyChanged(IProperty<ILogEvent> aProperty, ILogEvent aOldValue, ILogEvent aNewValue)
		{
			update();
		}
	};
	
//	private IBusListener<EventSelectedMsg> itsEventSelectedListener = new IBusListener<EventSelectedMsg>()
//	{
//		public boolean processMessage(EventSelectedMsg aMessage)
//		{
//			if (aMessage.getSelectionMethod() == SelectionMethod.SELECT_IN_CALL_STACK)
//			{
//				showEvent(aMessage.getEvent());
//				return true;
//			}
//			
//			return false;
//		}
//	};
//	

	private IRWProperty<Boolean> itsShowPackages;
	private IPropertyListener<Boolean> itsShowPackagesListener = new SimplePropertyListener<Boolean>()
	{
		@Override
		protected void changed(IProperty<Boolean> aProperty)
		{
			//TODO: re-create UI
		}
	};
	

	public CFlowActivityPanel(IContext aContext)
	{
		super (aContext);
	}


	@Override
	protected void connectSeed(CFlowSeed aSeed)
	{
		aSeed.pSelectedEvent().addHardListener(itsSelectedEventListener);
		connect(aSeed.pSelectedEvent(), itsCFlowTree.pSelectedEvent());
		aSeed.pRootEvent().addHardListener(itsRootEventListener);
		update();
		itsWatchPanel.showStackFrame();
		
		ObjectId theInspectedObject = aSeed.pInspectedObject().get();
		if (theInspectedObject != null) 
		{
			String theText = Util.getObjectName(getGUIManager(), theInspectedObject, null, aSeed.pSelectedEvent().get());
			itsWatchPanel.openWatch(new ObjectWatchSeed(
					getGUIManager(),
					theText,
					itsWatchPanel,
					aSeed.pSelectedEvent().get(),
					theInspectedObject));
		}
		itsCFlowTree.connectSeed(aSeed);
	}

	@Override
	protected void disconnectSeed(CFlowSeed aSeed)
	{
		aSeed.pSelectedEvent().removeListener(itsSelectedEventListener);
		disconnect(aSeed.pSelectedEvent(), itsCFlowTree.pSelectedEvent());
		aSeed.pRootEvent().removeListener(itsRootEventListener);
		itsCFlowTree.disconnectSeed(aSeed);
	}

	@Override
	public void init()
	{
		setLayout(new BorderLayout());
		
		// Create tree panel
		itsCFlowTree = new CFlowTree(this);
		
		JPanel theCFlowPanel = new JPanel(new BorderLayout());
		theCFlowPanel.add(itsCFlowTree, BorderLayout.CENTER);
		
		// Create title
		JPanel theTitlePanel = new JPanel(GUIUtils.createStackLayout());
		
		itsHostLabel = ZLabel.create("", FontConfig.SMALL_FONT, Color.BLACK);
		itsThreadLabel = GUIUtils.createLabel("");
		
		theTitlePanel.add(itsHostLabel);
		theTitlePanel.add(itsThreadLabel);
		
		JPanel theNorthPanel = new JPanel(new BorderLayout(0, 0));
		theNorthPanel.add(theTitlePanel, BorderLayout.WEST);
		theNorthPanel.add(createToolbar(), BorderLayout.CENTER);
		
		theCFlowPanel.add(theNorthPanel, BorderLayout.NORTH);
		
		// Create bookmark mural
		itsBookmarksMural = new BookmarksMural(getGUIManager(), getLogBrowser(), getGUIManager().getBookmarks());
		add(itsBookmarksMural, BorderLayout.NORTH);
		
		// Create watch panel
		itsWatchPanel = new ObjectWatchPanel(getContext(), true);

		JSplitPane theSplitPane = new SavedSplitPane(JSplitPane.HORIZONTAL_SPLIT, getGUIManager(), PROPERTY_SPLITTER_POS);
		theSplitPane.setResizeWeight(0.5);
		theSplitPane.setLeftComponent(theCFlowPanel);
		theSplitPane.setRightComponent(itsWatchPanel);
		
		add(theSplitPane, BorderLayout.CENTER);
		
		itsStepper = new Stepper(getLogBrowser());
	}
	
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	private void backwardStepOver() 
	{
		itsStepper.setCurrentEvent(getSeed().pSelectedEvent().get());
		itsStepper.backwardStepOver();
		ILogEvent theEvent = itsStepper.getCurrentEvent();
		if (theEvent != null) selectEvent(theEvent, SelectionMethod.BACKWARD_STEP_OVER);
	}
	

	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	private void backwardStepInto() 
	{
		itsStepper.setCurrentEvent(getSeed().pSelectedEvent().get());
		itsStepper.backwardStepInto();
		ILogEvent theEvent = itsStepper.getCurrentEvent();
		if (theEvent != null) selectEvent(theEvent, SelectionMethod.BACKWARD_STEP_INTO);
	}
	
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	private void stepOut() 
	{
		ILogEvent theSelectedEvent = getSeed().pSelectedEvent().get();
		if (theSelectedEvent != null)
		{
			itsStepper.setCurrentEvent(theSelectedEvent);
			itsStepper.stepOut();
			ILogEvent theEvent = itsStepper.getCurrentEvent();
			if (theEvent != null) selectEvent(theEvent, SelectionMethod.STEP_OUT);				
		}
	}

	/**
	 * Perform a step into action on the specified event.
	 */
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	public void forwardStepInto(ILogEvent aEvent)
	{
		itsStepper.setCurrentEvent(aEvent);
		itsStepper.forwardStepInto();
		ILogEvent theEvent = itsStepper.getCurrentEvent();
		
		// Step over AOP activities with which we are not intimate
		while(theEvent != null)
		{
			BytecodeRole theRole = LocationUtils.getEventRole(theEvent);
			if (! IntimacyLevel.isKnownRole(theRole)) break;
			
			IntimacySettings theIntimacySettings = getGUIManager().getSettings().getIntimacySettings();
			ProbeInfo theProbeInfo = LocationUtils.getProbeInfo(theEvent);
			IntimacyLevel theIntimacyLevel = theIntimacySettings.getIntimacyLevel(theProbeInfo.adviceSourceId);
			if (theIntimacyLevel != null && theIntimacyLevel.showRole(theRole)) break;
			
			itsStepper.forwardStepOver();
			theEvent = itsStepper.getCurrentEvent();
		}
		
		if (theEvent != null) selectEvent(theEvent, SelectionMethod.FORWARD_STEP_INTO);
	}
	
	private void forwardStepInto() 
	{
		forwardStepInto(getSeed().pSelectedEvent().get());
	}

	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	private void forwardStepOver() 
	{
		itsStepper.setCurrentEvent(getSeed().pSelectedEvent().get());
		itsStepper.forwardStepOver();
		ILogEvent theEvent = itsStepper.getCurrentEvent();
		if (theEvent != null) selectEvent(theEvent, SelectionMethod.FORWARD_STEP_OVER);
	}

	private JComponent createToolbar()
	{
		JPanel theToolbar = new JPanel();
		
		// Setup intimacy level selector
		if (getConfig().get(TODConfig.WITH_ASPECTS))
		{
			theToolbar.add(new IntimacyEditorButton(
					getGUIManager().getSettings().getIntimacySettings(), 
					getLogBrowser().getStructureDatabase()));
		}
				
		// Stepping buttons
		theToolbar.add(new JButton(new SimpleAction(
				Resources.ICON_BACKWARD_STEP_OVER.asIcon(20), 
				"Backward step over")
		{
			public void actionPerformed(ActionEvent aE)
			{
				backwardStepOver();				
			}
		}));
		
		theToolbar.add(new JButton(new SimpleAction(
				Resources.ICON_BACKWARD_STEP_INTO.asIcon(20), 
				"Backward step into")
		{
			public void actionPerformed(ActionEvent aE)
			{
				backwardStepInto();				
			}
		}));
		
		theToolbar.add(new JButton(new SimpleAction(
				Resources.ICON_STEP_OUT.asIcon(20), 
				"Step out")
		{
			public void actionPerformed(ActionEvent aE)
			{
				stepOut();
			}
		}));
		
		theToolbar.add(new JButton(new SimpleAction(
				Resources.ICON_FORWARD_STEP_INTO.asIcon(20), 
				"Forward step into")
		{
			public void actionPerformed(ActionEvent aE)
			{
				forwardStepInto();				
			}
		}));
		
		theToolbar.add(new JButton(new SimpleAction(
				Resources.ICON_FORWARD_STEP_OVER.asIcon(20), 
				"Forward step over")
		{
			public void actionPerformed(ActionEvent aE)
			{
				forwardStepOver();				
			}
		}));

		// Bookmark buttons
		theToolbar.add(new JLabel("   "));
		
		itsBookmarkButton = new BookmarkButton(getGUIManager().getBookmarks());
		theToolbar.add(itsBookmarkButton);
		
	
		// UI tweaking...
		for (int i=0;i<theToolbar.getComponentCount();i++)
		{
			Component theComponent = theToolbar.getComponent(i);
			if (theComponent instanceof JButton)
			{
				JButton theButton = (JButton) theComponent;
				theButton.setMargin(UIUtils.NULL_INSETS);
			}
		}
		
		return theToolbar;
	}
	
	private void update()
	{
		IThreadInfo theThread = getSeed().getThread();
		IHostInfo theHost = theThread.getHost();
		
		itsHostLabel.setText(String.format(
				"Host: \"%s\" [%d]",
				theHost.getName(),
				theHost.getId()));
		
		itsHostLabel.revalidate();
		itsHostLabel.repaint();
		
		itsThreadLabel.setText(String.format(
				"Thread: \"%s\" [%d]",
				theThread.getName(),
				theThread.getId()));
		
		itsThreadLabel.revalidate();
		itsThreadLabel.repaint();

		ILogEvent theSelectedEvent = getSeed().pSelectedEvent().get();
		
		if (theSelectedEvent != null)
		{
			itsWatchPanel.showStackFrame();
			showEvent(theSelectedEvent);
		}
		
		LocationUtils.gotoSource(getGUIManager(), theSelectedEvent);
		
		itsBookmarksMural.setCurrentEvent(theSelectedEvent);
		itsBookmarkButton.setCurrentEvent(theSelectedEvent);
	}
	
	private void showEvent (ILogEvent aEvent)
	{
		getSeed().pSelectedEvent().set(aEvent);
	}

	@Override
	protected void initOptions(Options aOptions)
	{
		super.initOptions(aOptions);
		aOptions.addOption(StdOptions.SHOW_PACKAGE_NAMES, true);
		EventListPanel.createDefaultOptions(aOptions, true, false);
	}
	
	@Override
	public void addNotify()
	{
//		Bus.get(this).subscribe(EventSelectedMsg.ID, itsEventSelectedListener);
		
		itsShowPackages = Options.get(this).getProperty(StdOptions.SHOW_PACKAGE_NAMES);
		itsShowPackages.addHardListener(itsShowPackagesListener);
		
		super.addNotify();
	}
	
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		
//		Bus.get(this).unsubscribe(EventSelectedMsg.ID, itsEventSelectedListener);
		
		itsShowPackages.removeListener(itsShowPackagesListener);
	}
	
	/**
	 * Whether package names should be dosplayed.
	 */
	public boolean showPackageNames()
	{
//		return itsShowPackages.get();
		return false;
	}
	
	public void selectEvent(ILogEvent aEvent, SelectionMethod aMethod)
	{
		if (aMethod.shouldCreateSeed())
		{
			getGUIManager().openSeed(new CFlowSeed(getLogBrowser(), aEvent), false);
		}
		else
		{
			getSeed().pSelectedEvent().set(aEvent);
		}
		Bus.get(CFlowActivityPanel.this).postMessage(new EventSelectedMsg(aEvent, aMethod));
	}
	
	public boolean isEventSelected(ILogEvent aEvent)
	{
		ILogEvent theSelectedEvent = getSeed().pSelectedEvent().get();
		return theSelectedEvent != null && theSelectedEvent.equals(aEvent);
	}
	
	public ILogEvent getSelectedEvent()
	{
		return getSeed().pSelectedEvent().get();
	}

	
	
}
