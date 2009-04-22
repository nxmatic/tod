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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tod.Util;
import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ITypeInfo;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySubPanel;
import tod.gui.components.eventlist.EventListPanel;
import tod.gui.kit.SavedSplitPane;
import tod.gui.kit.messages.ShowCFlowMsg;
import tod.tools.scheduling.Scheduled;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import tod.utils.LocationComparator;
import zz.utils.SimpleListModel;
import zz.utils.Utils;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UniversalRenderer;

/**
 * This panel displays the history of individual executions of the object.
 * @author gpothier
 */
public class ObjectMethodsPanel extends ActivitySubPanel<ObjectHistorySeed>
{
	private SimpleListModel itsMethodsListModel;
	private EventListPanel itsListPanel;
	private IObjectInspector itsInspector;
	private JList itsMethodsList;
	
	public ObjectMethodsPanel(ActivityPanel<ObjectHistorySeed> aView)
	{
		super(aView);
		createUI();
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
		
		theSplitPane.setRightComponent(itsListPanel);
		
		itsMethodsListModel = new SimpleListModel();
		itsMethodsList = new JList(itsMethodsListModel);
		itsMethodsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		itsMethodsList.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent aE)
			{
				if (aE.getValueIsAdjusting()) return;
				update();
			}
		});
		theSplitPane.setLeftComponent(new JScrollPane(itsMethodsList));
		
		setLayout(new StackLayout());
		add (theSplitPane);
	}

	@Override
	public void connectSeed(ObjectHistorySeed aSeed)
	{
		itsInspector = getLogBrowser().createObjectInspector(getSeed().getObject());
		setupMethodsList();
		connect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
	}
	
	@Override
	public void disconnectSeed(ObjectHistorySeed aSeed)
	{
		itsInspector = null;
		disconnect(aSeed.pSelectedEvent(), itsListPanel.pSelectedEvent());
	}
	
	private void setupMethodsList()
	{
		ITypeInfo theType = itsInspector.getType();
		if (theType instanceof IClassInfo)
		{
			IClassInfo theClass = (IClassInfo) theType;
			itsMethodsList.setCellRenderer(new BehaviorRenderer(theClass));
			
			List<IBehaviorInfo> theBehaviors = new ArrayList<IBehaviorInfo>();
			
			List<IClassInfo> theRemainingClasses = new ArrayList<IClassInfo>();
			Set<IClassInfo> theProcessedClasses = new HashSet<IClassInfo>();
			theRemainingClasses.add(theClass);
			
			while(! theRemainingClasses.isEmpty())
			{
				IClassInfo theCurrentClass = theRemainingClasses.remove(theRemainingClasses.size()-1);
				if (! theProcessedClasses.add(theCurrentClass)) continue;
				
				Utils.fillCollection(theBehaviors, theCurrentClass.getBehaviors());
				IClassInfo theSupertype = theCurrentClass.getSupertype();
				if (theSupertype != null) theRemainingClasses.add(theSupertype);
				if (theCurrentClass.getInterfaces() != null) Utils.fillCollection(theRemainingClasses, theCurrentClass.getInterfaces());
			}
			
			Collections.sort(theBehaviors, LocationComparator.getInstance());
			itsMethodsListModel.setList(theBehaviors);
		}
		else 
		{
			itsMethodsListModel.setList(null);
		}
	}
	
	private void update()
	{
		Object[] theValues = itsMethodsList.getSelectedValues();
		IBehaviorInfo[] theMethods = new IBehaviorInfo[theValues.length];
		for(int i=0;i<theValues.length;i++) theMethods[i] = (IBehaviorInfo) theValues[i];
		behaviorSelected(theMethods);
	}
	
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	private void behaviorSelected(IBehaviorInfo[] aBehaviors)
	{
		ICompoundFilter theMethodsFilter = getLogBrowser().createUnionFilter();
		for (IBehaviorInfo theBehavior : aBehaviors)
		{
			theMethodsFilter.add(getLogBrowser().createBehaviorCallFilter(theBehavior));
		}
		
		ICompoundFilter theFilter = getLogBrowser().createIntersectionFilter(
				getLogBrowser().createTargetFilter(getSeed().getObject()),
				theMethodsFilter);
		
		itsListPanel.setBrowser(theFilter);
	}
	
	/**
	 * List cell renderer for behaviors.
	 * @author gpothier
	 */
	private static class BehaviorRenderer extends UniversalRenderer<IBehaviorInfo>
	{
		/**
		 * The class of the object. For methods of this class, only the method name 
		 * is displayed, otherwise the class name is also displayed.
		 */
		private IClassInfo itsClass;
		
		private BehaviorRenderer(IClassInfo aClass)
		{
			itsClass = aClass;
		}

		@Override
		protected String getName(IBehaviorInfo aBehavior)
		{
			StringBuilder theBuilder = new StringBuilder();
			theBuilder.append(Util.getFullName(aBehavior));
			
			IClassInfo theClass = aBehavior.getDeclaringType();
			if (! theClass.equals(itsClass))
			{
				theBuilder.append(" [");
				theBuilder.append(theClass.getName());
				theBuilder.append("]");
			}
			
			return theBuilder.toString();
		}
		
	}
}
