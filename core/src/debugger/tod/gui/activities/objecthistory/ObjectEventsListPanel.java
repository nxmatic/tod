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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.LocationUtils;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import tod.gui.BrowserData;
import tod.gui.GUIUtils;
import tod.gui.activities.ActivitySubPanel;
import tod.gui.components.eventlist.EventListPanel;
import tod.gui.components.highlighter.EventHighlighter;
import tod.gui.kit.SavedSplitPane;
import tod.gui.kit.messages.ShowCFlowMsg;
import tod.gui.kit.messages.EventSelectedMsg.SelectionMethod;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.properties.PropertyListener;
import zz.utils.ui.PropertyEditor;

/**
 * This panel displays a list of all the events related to the 
 * inspected object.
 * @author gpothier
 */
public class ObjectEventsListPanel extends ActivitySubPanel<ObjectHistorySeed>
{
	private EventListPanel itsListPanel;
	private EventHighlighter itsEventHighlighter;

	private IEventFilter itsCurrentFilter;
	
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
					Color.BLUE));
		}
	};


	private IPropertyListener<Boolean> itsFlagsListener = new PropertyListener<Boolean>()
	{
		@Override
		public void propertyChanged(IProperty<Boolean> aProperty, Boolean aOldValue, Boolean aNewValue)
		{
			updateFilter();
		}
	};

	private FlagsPanel itsFlagsPanel;
	
	public ObjectEventsListPanel(ObjectHistoryActivityPanel aView)
	{
		super(aView);
		createUI ();
	}
	
	@Override
	public void connectSeed(ObjectHistorySeed aSeed)
	{
		connect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
		
		aSeed.pSelectedEvent().addHardListener(itsSelectedEventListener);
		
		aSeed.pShowKind_ArrayWrite().addHardListener(itsFlagsListener);
		aSeed.pShowKind_BehaviorCall().addHardListener(itsFlagsListener);
		aSeed.pShowKind_Exception().addHardListener(itsFlagsListener);
		aSeed.pShowKind_FieldWrite().addHardListener(itsFlagsListener);
		aSeed.pShowKind_LocalWrite().addHardListener(itsFlagsListener);
		
		aSeed.pShowRole_Arg().addHardListener(itsFlagsListener);
		aSeed.pShowRole_Result().addHardListener(itsFlagsListener);
		aSeed.pShowRole_Target().addHardListener(itsFlagsListener);
		aSeed.pShowRole_Value().addHardListener(itsFlagsListener);
		
		itsFlagsPanel.setSeed(aSeed);
		
		updateFilter();
	}

	@Override
	public void disconnectSeed(ObjectHistorySeed aSeed)
	{
		disconnect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());

		aSeed.pSelectedEvent().removeListener(itsSelectedEventListener);

		aSeed.pShowKind_ArrayWrite().removeListener(itsFlagsListener);
		aSeed.pShowKind_BehaviorCall().removeListener(itsFlagsListener);
		aSeed.pShowKind_Exception().removeListener(itsFlagsListener);
		aSeed.pShowKind_FieldWrite().removeListener(itsFlagsListener);
		aSeed.pShowKind_LocalWrite().removeListener(itsFlagsListener);
		
		aSeed.pShowRole_Arg().removeListener(itsFlagsListener);
		aSeed.pShowRole_Result().removeListener(itsFlagsListener);
		aSeed.pShowRole_Target().removeListener(itsFlagsListener);
		aSeed.pShowRole_Value().removeListener(itsFlagsListener);

		itsFlagsPanel.setSeed(null);
	}

	private void createUI()
	{
		JSplitPane theSplitPane = new SavedSplitPane(
				JSplitPane.HORIZONTAL_SPLIT, 
				getGUIManager(), 
				"objectEventsListPanel.splitterPos");
		
		theSplitPane.setResizeWeight(0.5);
		
		itsListPanel = new EventListPanel (getGUIManager(), getBus(), getLogBrowser(), getJobScheduler());
		
		itsListPanel.eEventActivated().addListener(new IEventListener<ILogEvent>()
				{
					public void fired(IEvent< ? extends ILogEvent> aEvent, ILogEvent aData)
					{
						getBus().postMessage(new ShowCFlowMsg(aData));
					}
				});
		
//		itsListPanel.pSelectedEvent().addHardListener(new PropertyListener<ILogEvent>()
//				{
//					@Override
//					public void propertyChanged(IProperty<ILogEvent> aProperty, ILogEvent aOldValue, ILogEvent aNewValue)
//					{
//					}
//				});

		theSplitPane.setLeftComponent(itsListPanel);
		
		setLayout(new BorderLayout());
		add (theSplitPane, BorderLayout.CENTER);
		
		itsFlagsPanel = new FlagsPanel();
		
		JScrollPane theScrollPane = new JScrollPane(
				itsFlagsPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		add(theScrollPane, BorderLayout.WEST);
		
		itsEventHighlighter = new EventHighlighter(getGUIManager(), getLogBrowser());
		theSplitPane.setRightComponent(itsEventHighlighter);
	}
	
	/**
	 * Computes the event filter.
	 */
	private void updateFilter()
	{
		ObjectId theObject = getSeed().getObject();
		
		// Setup role filter
		IEventFilter theRoleFilter;
		
		Boolean r_arg = getSeed().pShowRole_Arg().get();
		Boolean r_result = getSeed().pShowRole_Result().get();
		Boolean r_target = getSeed().pShowRole_Target().get();
		Boolean r_value = getSeed().pShowRole_Value().get();
		
		if (r_arg && r_result && r_target && r_value)
		{
			theRoleFilter = getLogBrowser().createObjectFilter(theObject);
		}
		else
		{
			ICompoundFilter theCompound = getLogBrowser().createUnionFilter();
			if (r_arg) theCompound.add(getLogBrowser().createArgumentFilter(theObject));
			if (r_result) theCompound.add(getLogBrowser().createResultFilter(theObject));
			if (r_target) theCompound.add(getLogBrowser().createTargetFilter(theObject));
			if (r_value) theCompound.add(getLogBrowser().createValueFilter(theObject));
			theRoleFilter = theCompound;
		}

		// Setup kind filter
		Boolean k_arrayWrite = getSeed().pShowKind_ArrayWrite().get();
		Boolean k_behaviorCall = getSeed().pShowKind_BehaviorCall().get();
		Boolean k_exception = getSeed().pShowKind_Exception().get();
		Boolean k_fieldWrite = getSeed().pShowKind_FieldWrite().get();
		Boolean k_localWrite = getSeed().pShowKind_LocalWrite().get();

		if (k_arrayWrite && k_behaviorCall && k_exception && k_fieldWrite && k_localWrite)
		{
			// If all kinds are selected, there is no need to filter
			itsCurrentFilter = theRoleFilter;
		}
		else
		{
			ICompoundFilter theKindFilter = getLogBrowser().createUnionFilter();
			if (k_arrayWrite) theKindFilter.add(getLogBrowser().createArrayWriteFilter());
			if (k_behaviorCall) theKindFilter.add(getLogBrowser().createBehaviorCallFilter());
			if (k_exception) theKindFilter.add(getLogBrowser().createExceptionGeneratedFilter());
			if (k_fieldWrite) theKindFilter.add(getLogBrowser().createFieldWriteFilter());
			if (k_localWrite) theKindFilter.add(getLogBrowser().createVariableWriteFilter());
			
			itsCurrentFilter = getLogBrowser().createIntersectionFilter(
					theRoleFilter,
					theKindFilter);
		}
			
		itsListPanel.setBrowser(getEventBrowser());
	}
	
	public IEventBrowser getEventBrowser()
	{
		return itsCurrentFilter != null ?
				getLogBrowser().createBrowser(itsCurrentFilter)
				: getLogBrowser().createBrowser();
	}

	public ILogEvent getSelectedEvent()
	{
		return itsListPanel.pSelectedEvent().get();
	}

	public void selectEvent(ILogEvent aEvent, SelectionMethod aMethod)
	{
		itsListPanel.pSelectedEvent().set(aEvent);
	}
	

	private class FlagsPanel extends JPanel
	{
		public FlagsPanel()
		{
		}
		
		public void setSeed(ObjectHistorySeed aSeed)
		{
			removeAll();
			
			JPanel theRolePanel = new JPanel(GUIUtils.createStackLayout());
			theRolePanel.setBorder(BorderFactory.createTitledBorder("Object role"));
			
			JPanel theKindPanel = new JPanel(GUIUtils.createStackLayout());
			theKindPanel.setBorder(BorderFactory.createTitledBorder("Event kind"));
			

			
			if (aSeed != null)
			{
				theRolePanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowRole_Arg(), 
						"Argument",
						"<html>" +
						"<b>Argument role.</b> Selects events where the object <br>" +
						"appears as one of the arguments of a behavior call."));
				
				theRolePanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowRole_Result(), 
						"Result",
						"<html>" +
						"<b>Result role.</b> Selects events where the object <br>" +
						"is the result of a behavior call."));
				
				theRolePanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowRole_Target(), 
						"Target",
						"<html>" +
						"<b>Target role.</b> Selects events where the object <br>" +
						"is the target of a behavior call, field write or array write."));
				
				theRolePanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowRole_Value(), 
						"Value",
						"<html>" +
						"<b>Value role.</b> Selects events where the object <br>" +
						"is the value written into a field, local variable or array."));
				
				
				theKindPanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowKind_ArrayWrite(), 
						"Array write",
						"<html>" +
						"<b>Array write kind.</b> Selects array write events"));
				
				theKindPanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowKind_BehaviorCall(), 
						"Behavior call",
						"<html>" +
						"<b>Behavior call kind.</b> Selects behavior call events"));
				
				theKindPanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowKind_Exception(), 
						"Exception",
						"<html>" +
						"<b>Exception kind.</b> Selects exception events"));
				
				theKindPanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowKind_FieldWrite(), 
						"Field write",
						"<html>" +
						"<b>Field write kind.</b> Selects field write events"));
				
				theKindPanel.add(PropertyEditor.createCheckBox(
						aSeed.pShowKind_LocalWrite(), 
						"Local variable write",
						"<html>" +
						"<b>Local variable write kind.</b> Selects local variable write events"));
			}

			setLayout(GUIUtils.createStackLayout());
			add(theRolePanel);
			add(theKindPanel);
			
			revalidate();
			repaint();
		}
	}

}
