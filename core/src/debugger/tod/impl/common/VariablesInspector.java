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
package tod.impl.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IVariablesInspector;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILocalVariableWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;

public class VariablesInspector implements IVariablesInspector
{
	private final ILogBrowser itsLogBrowser;
	private final IBehaviorCallEvent itsBehaviorCall;
	private final IEventBrowser itsChildrenBrowser;
	private ILogEvent itsReferenceEvent;
	private List<LocalVariableInfo> itsVariables;
	
	public VariablesInspector(ILogBrowser aLogBrowser, IBehaviorCallEvent aBehaviorCall)
	{
//		assert aBehaviorCall.isDirectParent();
		itsLogBrowser = aLogBrowser;
		itsBehaviorCall = aBehaviorCall.isDirectParent() ? aBehaviorCall : null;
		itsChildrenBrowser = itsBehaviorCall != null && itsBehaviorCall.hasRealChildren() ? 
				itsBehaviorCall.getChildrenBrowser() 
				: null;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsLogBrowser;
	}

	public IBehaviorCallEvent getBehaviorCall()
	{
		return itsBehaviorCall;
	}
	
	public IBehaviorInfo getBehavior()
	{
		return getBehaviorCall() != null ? getBehaviorCall().getExecutedBehavior() : null;
	}
	
	public List<LocalVariableInfo> getVariables()
	{
		if (itsVariables == null)
		{
			itsVariables = getBehaviorCall() != null ?
					getBehavior().getLocalVariables()
					: Collections.EMPTY_LIST;
		}
		return itsVariables;
	}
	
	public List<LocalVariableInfo> getVariables(int aBytecodeIndex)
	{
		List<LocalVariableInfo> theResult = new ArrayList<LocalVariableInfo>();
		for (LocalVariableInfo theVariable : getVariables())
		{
			if (theVariable.available(aBytecodeIndex)) theResult.add(theVariable);
		}
		
		return theResult;
	}
	
	public void setReferenceEvent(ILogEvent aEvent)
	{
		if (getBehaviorCall() == null) return;
		itsReferenceEvent = aEvent;
	}

	public ILogEvent getReferenceEvent()
	{
		return itsReferenceEvent;
	}

	/**
	 * Returns the {@link EntryValue} corresponding to the argument
	 * initially passed to the method.
	 */
	protected EntryValue getCallValue(LocalVariableInfo aVariable)
	{
		IBehaviorInfo theBehavior = itsBehaviorCall.getExecutedBehavior();
		ITypeInfo[] theArgumentTypes = theBehavior.getArgumentTypes();
		int theSlot = theBehavior.isStatic() ? 0 : 1;
		for (int i = 0; i < theArgumentTypes.length; i++)
		{
			if (aVariable.getIndex() == theSlot)
			{
				return new EntryValue(itsBehaviorCall.getArguments()[i], itsBehaviorCall);
			}
			else
			{
				ITypeInfo theType = theArgumentTypes[i];
				theSlot += theType.getSize(); // TODO: this is java-specific, generalize
			}
		}
		
		return null;		
	}
	
	protected IEventBrowser getBrowser(LocalVariableInfo aVariable)
	{
		ILogBrowser theLogBrowser = itsChildrenBrowser.getLogBrowser();
		IEventFilter theFilter = theLogBrowser.createVariableWriteFilter(aVariable);
		return itsChildrenBrowser.createIntersection(theFilter);
	}
	
	protected EntryValue getEntryValue0(LocalVariableInfo aVariable)
	{
		if (itsChildrenBrowser == null) return null;
		
		// Check if the ref event is the setter for this variable
		if (itsReferenceEvent instanceof ILocalVariableWriteEvent)
		{
			ILocalVariableWriteEvent theEvent = (ILocalVariableWriteEvent) itsReferenceEvent;
			if (theEvent.getVariable().equals(aVariable))
			{
				return new EntryValue(theEvent.getValue(), theEvent);
			}
		}
		
		IEventBrowser theBrowser = getBrowser(aVariable);
		
		theBrowser.setNextEvent(itsReferenceEvent);
		
		if (theBrowser.hasPrevious())
		{
			ILocalVariableWriteEvent theEvent = (ILocalVariableWriteEvent) theBrowser.previous();
			assert aVariable.equals(theEvent.getVariable()); 
			return new EntryValue(theEvent.getValue(), theEvent);
		}
		
		// If we did not find a variable write corresponding to the variable,
		// we consider the behavior call's initial argument values
		return getCallValue(aVariable);
	}
	
	public final EntryValue[] getEntryValue(LocalVariableInfo aVariable)
	{
		EntryValue theValue = getEntryValue0(aVariable);
		return theValue != null ? new EntryValue[] {theValue} : null;
	}
	
	protected EntryValue nextEntryValue0(LocalVariableInfo aVariable)
	{
		IEventBrowser theBrowser = getBrowser(aVariable);
		EntryValue[] theEntryValues = getEntryValue(aVariable);
		
		EntryValue theValue = theEntryValues != null ? theEntryValues[0] : null;
		
		if (theValue == null || theValue.getSetter() == itsBehaviorCall)
		{
			theBrowser.setNextTimestamp(0);
		}
		else 
		{
			theBrowser.setNextEvent(theValue.getSetter());
		}
		
		if (! theBrowser.hasNext()) return null;
		ILocalVariableWriteEvent theNext = (ILocalVariableWriteEvent) theBrowser.next();
		assert theNext.equals(theValue.getSetter());
		
		if (theBrowser.hasNext()) 
		{
			theNext = (ILocalVariableWriteEvent) theBrowser.next();
			assert aVariable.equals(theNext.getVariable()); 
			itsReferenceEvent = theNext;
			return new EntryValue(theNext.getValue(), theNext);
		}
		
		return null;
	}

	public final EntryValue[] nextEntryValue(LocalVariableInfo aVariable)
	{
		EntryValue theValue = nextEntryValue0(aVariable);
		return theValue != null ? new EntryValue[] {theValue} : null;
	}
	
	protected EntryValue previousEntryValue0(LocalVariableInfo aVariable)
	{
		IEventBrowser theBrowser = getBrowser(aVariable);
		EntryValue[] theEntryValues = getEntryValue(aVariable);
		if (theEntryValues == null) return null;
		
		EntryValue theValue = theEntryValues[0];
		
		if (theValue.getSetter() == itsBehaviorCall) return null;
		
		theBrowser.setPreviousEvent(theValue.getSetter());
		ILocalVariableWriteEvent thePrevious = (ILocalVariableWriteEvent) theBrowser.previous();
		assert thePrevious.equals(theValue.getSetter());
		
		if (theBrowser.hasPrevious()) 
		{
			thePrevious = (ILocalVariableWriteEvent) theBrowser.previous();
			assert aVariable.equals(thePrevious.getVariable()); 
			itsReferenceEvent = thePrevious;
			return new EntryValue(thePrevious.getValue(), thePrevious);
		}
		
		return getCallValue(aVariable);
	}

	public final EntryValue[] previousEntryValue(LocalVariableInfo aVariable)
	{
		EntryValue theValue = previousEntryValue0(aVariable);
		return theValue != null ? new EntryValue[] {theValue} : null;
	}
	
	public ILocalVariableWriteEvent[] getEntrySetter(LocalVariableInfo aVariable)
	{
		if (itsChildrenBrowser == null) return null;
		
		ILogBrowser theLogBrowser = itsChildrenBrowser.getLogBrowser();
		IEventFilter theFilter = theLogBrowser.createVariableWriteFilter(aVariable);
		IEventBrowser theBrowser = itsChildrenBrowser.createIntersection(theFilter);
		
		theBrowser.setNextEvent(itsReferenceEvent);
		while (theBrowser.hasPrevious())
		{
			ILocalVariableWriteEvent theEvent = (ILocalVariableWriteEvent) theBrowser.previous();
			if (aVariable.equals(theEvent.getVariable())) return new ILocalVariableWriteEvent[] {theEvent};
		}
		
		return null;
	}
}
