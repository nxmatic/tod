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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.gui.activities.ActivityPanel;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.SwingJob;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.Utils;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.properties.PropertyListener;
import zz.utils.ui.GridStackLayout;
import zz.utils.ui.ScrollablePanel;
import zz.utils.ui.StackLayout;

/**
 * A panel that displays a call stack, ie. a list of the ancestors of a given
 * event.
 * 
 * @author gpothier
 */
public class CallStackPanel extends JPanel
{
	private CFlowSeed itsSeed;
	private final IJobScheduler itsJobScheduler;

	/**
	 * Root of the control flow tree for the current leaf event
	 */
	private IParentEvent itsRootEvent;
	
	private List<AbstractStackNode> itsStackNodes = new ArrayList<AbstractStackNode>();
	
	private AbstractStackNode itsCurrentStackNode;

	private JScrollPane itsScrollPane;
	
	private final IPropertyListener<ILogEvent> itsSelectedEventListener = new PropertyListener<ILogEvent>()
	{
		@Override
		public void propertyChanged(
				IProperty<ILogEvent> aProperty,
				ILogEvent aOldValue,
				ILogEvent aNewValue)
		{
			selectedEventChanged(aNewValue);
		}
	};
	
	private final IPropertyListener<ILogEvent> itsLeafEventListener = new PropertyListener<ILogEvent>()
	{
		@Override
		public void propertyChanged(
				IProperty<ILogEvent> aProperty,
				ILogEvent aOldValue,
				ILogEvent aNewValue)
		{
			leafEventChanged();
		}
	};
	
	public CallStackPanel(IJobScheduler aJobScheduler)
	{
		itsJobScheduler = aJobScheduler;
		createUI();
	}
	
	/**
	 * Called when the seed is connected to the CFlowView.
	 * @see ActivityPanel#connectSeed
	 */
	protected void connectSeed(CFlowSeed aSeed)
	{
		itsSeed = aSeed;
		itsSeed.pSelectedEvent().addHardListener(itsSelectedEventListener);
		itsSeed.pLeafEvent().addHardListener(itsLeafEventListener);
		rebuildStack();
	}

	/**
	 * Called when the seed is disconnected from the CFlowView.
	 * @see ActivityPanel#disconnectSeed
	 */
	protected void disconnectSeed(CFlowSeed aSeed)
	{
		itsSeed.pSelectedEvent().removeListener(itsSelectedEventListener);
		itsSeed.pLeafEvent().removeListener(itsLeafEventListener);
		itsSeed = null;
	}



	public IJobScheduler getJobScheduler()
	{
		return itsJobScheduler;
	}

	public ILogBrowser getLogBrowser()
	{
		return itsSeed.getLogBrowser();
	}
	
	private void selectedEventChanged(ILogEvent aEvent)
	{
		// Search if parent is already in the call stack
		for (AbstractStackNode theNode : itsStackNodes)
		{
			if (theNode.getFrameEvent().equals(aEvent.getParent()))
			{
				setCurrentStackNode(theNode);
				return;
			}
		}
		
		// if not found, rebuild the stack.
		itsSeed.pLeafEvent().set(aEvent);
	}

	private void leafEventChanged()
	{
		rebuildStack();
	}
	
	private void setCurrentStackNode(AbstractStackNode aNode)
	{
		if (itsCurrentStackNode != null) itsCurrentStackNode.setCurrentStackFrame(false);
		itsCurrentStackNode = aNode;
		if (itsCurrentStackNode != null) itsCurrentStackNode.setCurrentStackFrame(true);
	}
	
	public IParentEvent getRootEvent()
	{
		if (itsRootEvent == null)
		{
			itsRootEvent = getLogBrowser().getCFlowRoot(itsSeed.getThread());
		}
		return itsRootEvent;
	}

	/**
	 * show the child event of the IParentEvent given in parameter in the
	 * current event list
	 */
	public void selectEvent(ILogEvent aEvent)
	{
		itsSeed.pSelectedEvent().set(aEvent);
//		Bus.get(this).postMessage(new EventSelectedMsg(aEvent, SelectionMethod.SELECT_IN_CALL_STACK));
	}

	private void createUI()
	{
		itsScrollPane = new JScrollPane();
		setLayout(new StackLayout());
		add(itsScrollPane);
	}

	private void rebuildStack()
	{
		getJobScheduler().submit(JobPriority.AUTO, new StackBuilderJob());
	}
	
	private class StackBuilderJob extends SwingJob
	{
		@Override
		protected void work()
		{
			itsRootEvent = null;
			itsCurrentStackNode = null;

			ILogEvent theCurrentEvent = itsSeed.pLeafEvent().get();
			itsStackNodes.clear();
			
			ILogEvent theSelected = itsSeed.pSelectedEvent().get();
			IBehaviorCallEvent theSelectedFrame = theSelected != null ? theSelected.getParent() : null;
			
			while (theCurrentEvent != null)
			{
				AbstractStackNode theStackNode = buildStackNode(theCurrentEvent);
				itsStackNodes.add(theStackNode);
				
				theCurrentEvent = theCurrentEvent.getParent();
				if (theCurrentEvent != null && theCurrentEvent.equals(theSelectedFrame)) setCurrentStackNode(theStackNode);
			}
		}
		
		private AbstractStackNode buildStackNode(ILogEvent aEvent)
		{
			if (aEvent.getParent() == null || Utils.equalOrBothNull(aEvent.getParent(), getRootEvent()))
			{
				return new RootStackNode(getJobScheduler(), getRootEvent(), CallStackPanel.this);
			}
			else 
			{
				return new NormalStackNode(getJobScheduler(), aEvent, CallStackPanel.this);
			}
		}

		
		@Override
		protected void update()
		{
			JPanel theStack = new ScrollablePanel(new GridStackLayout(1, 0, 2, true, false));
			theStack.setOpaque(false);
			
			for (JComponent theNode : itsStackNodes) theStack.add(theNode);
			
			itsScrollPane.setViewportView(theStack);
			itsScrollPane.getViewport().setBackground(Color.WHITE);

			revalidate();
			repaint();
		}
	}
}
