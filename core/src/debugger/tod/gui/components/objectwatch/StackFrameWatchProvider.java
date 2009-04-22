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

import static tod.gui.FontConfig.STD_HEADER_FONT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import tod.Util;
import tod.core.database.browser.IVariablesInspector;
import tod.core.database.browser.ICompoundInspector.EntryValue;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.gui.GUIUtils;
import tod.gui.Hyperlinks;
import tod.gui.IGUIManager;
import tod.tools.scheduling.IJobScheduler;
import zz.utils.ui.ZLabel;

/**
 * Watch provider for stack frame reconstitution
 * @author gpothier
 */
public class StackFrameWatchProvider extends AbstractWatchProvider
{
	private final ILogEvent itsRefEvent;
	
	private IBehaviorCallEvent itsParentEvent;
	private boolean itsInvalid = false;
	private boolean itsIndirectParent = false;

	private IVariablesInspector itsInspector;
	private List<Entry> itsEntries;
	
	public StackFrameWatchProvider(
			IGUIManager aGUIManager, 
			String aTitle,
			ILogEvent aRefEvent)
	{
		super(aGUIManager, aTitle);
		itsRefEvent = aRefEvent;
	}

	private IBehaviorCallEvent getParentEvent()
	{
		if (itsParentEvent == null && ! itsInvalid)
		{
			itsParentEvent = itsRefEvent.getParent();
			if (itsParentEvent == null)
			{
				itsInvalid = true;
				return null;
			}
			else if (! itsParentEvent.isDirectParent())
			{
				itsInvalid = true;
				itsIndirectParent = true;
				return null;
			}
		}
		return itsParentEvent;
	}
	
	private IVariablesInspector getInspector()
	{
		if (itsInspector == null && ! itsInvalid)
		{
			IBehaviorCallEvent theParentEvent = getParentEvent();
			if (theParentEvent != null)
			{
				itsInspector = getLogBrowser().createVariablesInspector(theParentEvent);
			}
		}
		
		return itsInspector;
	}

	@Override
	public JComponent buildTitleComponent(IJobScheduler aJobScheduler)
	{
		IBehaviorCallEvent theParentEvent = getParentEvent();

		if (itsIndirectParent)
		{
			return GUIUtils.createMessage(
					"Variable information not available", 
					Color.DARK_GRAY,
					"Cause: missing control flow information, check working set.",
					Color.DARK_GRAY);
		}
		else if (itsInvalid)
		{
			return GUIUtils.createMessage(
					"Variable information not available", 
					Color.DARK_GRAY,
					"Cause: the currently selected event is a control flow root.",
					Color.DARK_GRAY);
		}
		else
		{
			IBehaviorInfo theBehavior = theParentEvent.getExecutedBehavior();
			
			JPanel theContainer = new JPanel(GUIUtils.createSequenceLayout());
			theContainer.setOpaque(false);

			if (theBehavior != null)
			{
				theContainer.add(ZLabel.create("Behavior: ", STD_HEADER_FONT, Color.BLACK));
				theContainer.add(Hyperlinks.behavior(getGUIManager(), Hyperlinks.SWING, theBehavior));
				theContainer.add(ZLabel.create(" ("+Util.getPrettyName(theBehavior.getDeclaringType().getName())+")", STD_HEADER_FONT, Color.BLACK));
			}
			
			return theContainer;
		}			
	}

	@Override
	public ObjectId getCurrentObject()
	{
		if (itsInvalid) return null;
		IBehaviorCallEvent theParentEvent = getParentEvent();
		return theParentEvent != null ?
				(ObjectId) theParentEvent.getTarget()
				: null;
	}

	@Override
	public ObjectId getInspectedObject()
	{
		return null;
	}

	@Override
	public ILogEvent getRefEvent()
	{
		return itsRefEvent;
	}
	
	private void checkEntries()
	{
		IVariablesInspector theInspector = getInspector(); // Obtain inspector first to update validity
		if (itsInvalid) return;
		if (itsEntries == null)
		{
			List<LocalVariableInfo> theVariables = theInspector.getVariables();
			itsEntries = new ArrayList<Entry>();
			for (LocalVariableInfo theLocalVariable : theVariables)
			{
				itsEntries.add(new LocalVariableEntry(theLocalVariable));
			}
		}
	}
	
	@Override
	public int getEntryCount()
	{
		checkEntries();
		return itsEntries != null ? itsEntries.size() : 0;
	}

	@Override
	public List<Entry> getEntries(int aRangeStart, int aRangeSize)
	{
		checkEntries();
		List<Entry> theResult = new ArrayList<Entry>();
		for(int i=aRangeStart;i<Math.min(aRangeStart+aRangeSize, getEntryCount());i++) 
			theResult.add(itsEntries.get(i));
		
		return theResult;
	}

	private class LocalVariableEntry extends Entry
	{
		private LocalVariableInfo itsLocalVariable;

		public LocalVariableEntry(LocalVariableInfo aLocalVariable)
		{
			itsLocalVariable = aLocalVariable;
		}
		
		@Override
		public String getName()
		{
			if (itsInvalid) return null;
			return itsLocalVariable.getVariableName();
		}

		@Override
		public EntryValue[] getValue()
		{
			if (itsInvalid) return null;
			IVariablesInspector theInspector = getInspector();
			theInspector.setReferenceEvent(itsRefEvent);
			return theInspector.getEntryValue(itsLocalVariable);
		}

		@Override
		public EntryValue[] getNextValue()
		{
			if (itsInvalid) return null;
			IVariablesInspector theInspector = getInspector();
			theInspector.setReferenceEvent(itsRefEvent);
			return theInspector.nextEntryValue(itsLocalVariable);
		}

		@Override
		public EntryValue[] getPreviousValue()
		{
			if (itsInvalid) return null;
			IVariablesInspector theInspector = getInspector();
			theInspector.setReferenceEvent(itsRefEvent);
			return theInspector.previousEntryValue(itsLocalVariable);
		}
		
		
	}
}
