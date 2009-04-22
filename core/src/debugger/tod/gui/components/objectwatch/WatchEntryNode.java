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

import java.awt.Color;

import javax.swing.JPanel;

import tod.core.database.browser.ICompoundInspector.EntryValue;
import tod.core.database.event.ILogEvent;
import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.gui.Hyperlinks;
import tod.gui.IGUIManager;
import tod.gui.activities.cflow.CFlowSeed;
import tod.gui.components.objectwatch.AbstractWatchProvider.Entry;
import tod.gui.kit.AsyncPanel;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobScheduler.JobPriority;

/**
 * Represents a watch entry (field or variable).
 * @author gpothier
 */
public class WatchEntryNode extends JPanel
{
	private final IJobScheduler itsJobScheduler;
	
	private final IGUIManager itsGUIManager;
	private final ObjectWatchPanel itsWatchPanel;
	private final AbstractWatchProvider itsProvider;
	private final Entry itsEntry;
	
	public WatchEntryNode(
			IGUIManager aGUIManager,
			IJobScheduler aJobScheduler,
			ObjectWatchPanel aWatchPanel,
			AbstractWatchProvider aProvider, 
			Entry aEntry)
	{
		super(GUIUtils.createSequenceLayout());
		itsWatchPanel = aWatchPanel;
		setOpaque(false);
		itsJobScheduler = aJobScheduler;
		itsGUIManager = aGUIManager;
		itsProvider = aProvider;
		itsEntry = aEntry;
		createUI();
	}
	
	private void createUI()
	{
		String theName = itsEntry.getName();
		add(GUIUtils.createLabel(theName + " = "));
		add(new ValueAsyncPanel(itsJobScheduler));
	}

	/**
	 * This panel displays the value of the entry, and the why? and prev/next links
	 * @author gpothier
	 */
	private class ValueAsyncPanel extends AsyncPanel
	{
		private EntryValue[] itsValue;
		
		public ValueAsyncPanel(IJobScheduler aJobScheduler)
		{
			super(aJobScheduler, JobPriority.AUTO);
		}

		@Override
		protected void runJob()
		{
			itsValue = itsEntry.getValue();
		}

		@Override
		protected void updateSuccess()
		{
			if (itsValue != null)
			{
				boolean theFirst = true;
				for (int i=0;i<itsValue.length;i++)
				{
					if (theFirst) theFirst = false;
					else add(GUIUtils.createLabel(" / "));
					
					add(Hyperlinks.object(
							itsGUIManager, 
							Hyperlinks.SWING, 
							itsJobScheduler,
							itsProvider.getCurrentObject(),
							itsValue[i].getValue(),
							itsProvider.getRefEvent(),
							true));
					
					ILogEvent theSetter = itsValue[i].getSetter();
					add(GUIUtils.createLabel(" ("));
					if (theSetter != null)
					{
						add(Hyperlinks.event(itsGUIManager, Hyperlinks.SWING, "why?", theSetter));
						add(GUIUtils.createLabel(", "));
					}
					
					add(new PreviousValueAsyncPanel(getJobScheduler()));
					add(GUIUtils.createLabel("/"));
					add(new NextValueAsyncPanel(getJobScheduler()));
					
					add(GUIUtils.createLabel(")"));
				}
			}
		}
	}
	
	/**
	 * This panel displays the next or previous value of the entry
	 * @author gpothier
	 */
	private abstract class NeighbourValueAsyncPanel extends AsyncPanel
	{
		private EntryValue[] itsValue;
		
		public NeighbourValueAsyncPanel(IJobScheduler aJobScheduler)
		{
			super(aJobScheduler, JobPriority.LOW);
		}
		
		protected abstract String getLabel();
		
		protected abstract EntryValue[] getValue(Entry aEntry);

		@Override
		protected void createUI()
		{
			setLayout(GUIUtils.createSequenceLayout());
			add(GUIUtils.createLabel(
					getLabel(), 
					FontConfig.STD_FONT, 
					Color.DARK_GRAY));
		}
		
		@Override
		protected void runJob()
		{
			itsValue = getValue(itsEntry);
		}

		@Override
		protected void updateSuccess()
		{
			if (itsValue != null)
			{
				boolean theFirst = true;
				for (int i=0;i<itsValue.length;i++)
				{
					if (theFirst) theFirst = false;
					else add(GUIUtils.createLabel(" / "));
					
					ILogEvent theSetter = itsValue[i].getSetter();
					if (theSetter != null)
					{
						CFlowSeed theSeed = new CFlowSeed(itsGUIManager.getSession().getLogBrowser(), theSetter);
						theSeed.pInspectedObject().set(itsProvider.getInspectedObject());
						add(Hyperlinks.seed(itsGUIManager, Hyperlinks.SWING, getLabel(), theSeed));
					}
				}
			}
			else
			{
				add(GUIUtils.createLabel(
						getLabel(), 
						FontConfig.STD_FONT, 
						Color.LIGHT_GRAY));
			}
		}
	}
	
	/**
	 * This panel displays the next value of the entry
	 * @author gpothier
	 */
	private class NextValueAsyncPanel extends NeighbourValueAsyncPanel
	{
		public NextValueAsyncPanel(IJobScheduler aJobScheduler)
		{
			super(aJobScheduler);
		}
		
		@Override
		protected String getLabel()
		{
			return "next";
		}
		
		@Override
		protected EntryValue[] getValue(Entry aEntry)
		{
			return aEntry.getNextValue();
		}
	}
	
	/**
	 * This panel displays the next value of the entry
	 * @author gpothier
	 */
	private class PreviousValueAsyncPanel extends NeighbourValueAsyncPanel
	{
		public PreviousValueAsyncPanel(IJobScheduler aJobScheduler)
		{
			super(aJobScheduler);
		}
		
		@Override
		protected String getLabel()
		{
			return "prev.";
		}
		
		@Override
		protected EntryValue[] getValue(Entry aEntry)
		{
			return aEntry.getPreviousValue();
		}
	}
	
}
