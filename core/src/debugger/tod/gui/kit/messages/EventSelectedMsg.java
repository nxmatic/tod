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
package tod.gui.kit.messages;

import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.ObjectId;

/**
 * This message is sent when an event is selected in an event list.
 * @author gpothier
 */
public class EventSelectedMsg extends Message
{
	public static final String ID = "tod.eventSelected";

	/**
	 * The selected event.
	 */
	private final ILogEvent itsEvent;
	
	/**
	 * The way the event was selected (selection, stepping...)
	 */
	private final SelectionMethod itsSelectionMethod;
	
	public EventSelectedMsg(ILogEvent aEvent, SelectionMethod aMethod)
	{
		super(ID);
		itsEvent = aEvent;
		itsSelectionMethod = aMethod;
	}

	public ILogEvent getEvent()
	{
		return itsEvent;
	}

	public SelectionMethod getSelectionMethod()
	{
		return itsSelectionMethod;
	}

	public static abstract class SelectionMethod
	{
		public static final SelectionMethod FORWARD_STEP_INTO = new SM_ForwardStepInto();
		public static final SelectionMethod FORWARD_STEP_OVER = new SM_ForwardStepOver();
		public static final SelectionMethod BACKWARD_STEP_INTO = new SM_BackwardStepInto();
		public static final SelectionMethod BACKWARD_STEP_OVER = new SM_BackwardStepOver();
		public static final SelectionMethod STEP_OUT = new SM_StepOut();
		public static final SelectionMethod SELECT_IN_LIST = new SM_SelectInList();
		public static final SelectionMethod SELECT_IN_CALL_STACK = new SM_SelectInCallStack();
		
		/**
		 * Whether a new seed should be created when an event is selected with this method.
		 * This permits to organize navigation.
		 */
		public abstract boolean shouldCreateSeed();
	}
	
	public static class SM_ForwardStepInto extends SelectionMethod
	{
		private SM_ForwardStepInto()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static class SM_ForwardStepOver extends SelectionMethod
	{
		private SM_ForwardStepOver()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return false;
		}
	}
	
	public static class SM_BackwardStepInto extends SelectionMethod
	{
		private SM_BackwardStepInto()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static class SM_BackwardStepOver extends SelectionMethod
	{
		private SM_BackwardStepOver()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return false;
		}
	}

	public static class SM_StepOut extends SelectionMethod
	{
		private SM_StepOut()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static class SM_SelectInList extends SelectionMethod
	{
		private SM_SelectInList()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return false;
		}
	}
	
	public static class SM_SelectInCallStack extends SelectionMethod
	{
		private SM_SelectInCallStack()
		{
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static class SM_JumpFromWhyLink extends SelectionMethod
	{
		private ObjectId itsObject;
		private IFieldInfo itsField;
		
		public SM_JumpFromWhyLink(ObjectId aObject, IFieldInfo aField)
		{
			itsObject = aObject;
			itsField = aField;
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}

	public static class SM_SelectInBookmarks extends SelectionMethod
	{
		private String itsName;

		public SM_SelectInBookmarks(String aName)
		{
			itsName = aName;
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static class SM_SelectInHistory extends SelectionMethod
	{
		private EventSelectedMsg itsSelectedMessage;

		public SM_SelectInHistory(EventSelectedMsg aSelectedMessage)
		{
			itsSelectedMessage = aSelectedMessage;
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static abstract class SM_ShowForLine extends SelectionMethod
	{
		private IBehaviorInfo itsBehavior;
		private int itsLineNumber;
		
		public SM_ShowForLine(IBehaviorInfo aBehavior, int aLineNumber)
		{
			itsBehavior = aBehavior;
			itsLineNumber = aLineNumber;
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}

	public static class SM_ShowNextForLine extends SM_ShowForLine
	{
		public SM_ShowNextForLine(IBehaviorInfo aBehavior, int aLineNumber)
		{
			super(aBehavior, aLineNumber);
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return true;
		}
	}
	
	public static class SM_ShowPreviousForLine extends SM_ShowForLine
	{
		public SM_ShowPreviousForLine(IBehaviorInfo aBehavior, int aLineNumber)
		{
			super(aBehavior, aLineNumber);
		}

		@Override
		public boolean shouldCreateSeed()
		{
			return false;
		}
	}

}
