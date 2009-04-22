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
package tod.impl.dbgrid;

import java.util.HashMap;
import java.util.Map;

import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.dbgrid.messages.MessageType;
import zz.utils.ArrayStack;
import zz.utils.Stack;
import zz.utils.Utils;

public abstract class EventGenerator extends IdGenerator
{
	private final IMutableStructureDatabase itsStructureDatabase;
	
	private final Map<Integer, ThreadData> itsThreadsMap = new HashMap<Integer, ThreadData>();
	
	public EventGenerator(
			IMutableStructureDatabase aStructureDatabase,
			long aSeed,
			int aThreadsRange, 
			int aDepthRange, 
			int aBytecodeRange, 
			int aBehaviorRange, 
			int aAdviceSourceIdRange, 
			int aFieldRange, 
			int aVariableRange, 
			int aObjectRange,
			int aArrayIndexRange)
	{
		super(aSeed, aThreadsRange, aDepthRange, aBytecodeRange, aBehaviorRange, aAdviceSourceIdRange, aFieldRange, aVariableRange, aObjectRange, aArrayIndexRange);
		itsStructureDatabase = aStructureDatabase;
	}

	public IMutableStructureDatabase getStructureDatabase()
	{
		return itsStructureDatabase;
	}
	
	private ThreadData getThreadData(int aThreadId)
	{
		ThreadData theThreadData = itsThreadsMap.get(aThreadId);
		if (theThreadData == null)
		{
			theThreadData = new ThreadData(genMethodCall(aThreadId, 0, 0));
			itsThreadsMap.put(aThreadId, theThreadData);
		}
		
		return theThreadData;
	}
	
	public GridEvent next()
	{
		int theThreadId = genThreadId();
		ThreadData theThreadData = getThreadData(theThreadId);
		GridEvent theCurrentParent = theThreadData.peek();

		int theDepth = theThreadData.getDepth();
		long theParentTimestamp = theCurrentParent.getTimestamp();
		
		GridEvent theEvent;
		while(true)
		{
			MessageType theType = genType();
			switch (theType)
			{
			case BEHAVIOR_EXIT:
				theEvent = genBehaviorExit(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case SUPER_CALL:
				theEvent = genSuperCall(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case EXCEPTION_GENERATED:
				theEvent = genException(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case FIELD_WRITE:
				theEvent = genFieldWrite(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case INSTANTIATION:
				theEvent = genInstantiation(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case LOCAL_VARIABLE_WRITE:
				theEvent = genVariableWrite(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case METHOD_CALL:
				theEvent = genMethodCall(theThreadId, theDepth, theParentTimestamp);
				break;
			
			case ARRAY_WRITE:
				theEvent = genArrayWrite(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case NEW_ARRAY:
				theEvent = genNewArray(theThreadId, theDepth, theParentTimestamp);
				break;
				
			case INSTANCEOF:
				theEvent = genInstanceOf(theThreadId, theDepth, theParentTimestamp);
				break;

			default: throw new RuntimeException("Not handled: "+theType); 
			}

			if (theEvent.isCall())
			{
				if (theDepth >= getDepthRange()) continue;
				theThreadData.push(theEvent);
			}
			else if (theEvent.isExit())
			{
				if (theDepth <= 1) continue;
				theThreadData.pop();
			}
			
			return theEvent;
		}
	}

	
	protected abstract GridEvent genInstanceOf(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genNewArray(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genArrayWrite(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genMethodCall(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genVariableWrite(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genInstantiation(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genFieldWrite(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genException(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genSuperCall(int aThreadId, int aDepth, long aParentTimestamp);
	protected abstract GridEvent genBehaviorExit(int aThreadId, int aDepth, long aParentTimestamp);


	private static class ThreadData
	{
		private final Stack<GridEvent> itsStack = new ArrayStack<GridEvent>();
		
		public ThreadData(GridEvent aRoot)
		{
			itsStack.push(aRoot);
		}

		public GridEvent peek()
		{
			return itsStack.peek();
		}
		
		public void push(GridEvent aEvent)
		{
			assert aEvent.isCall();
			itsStack.push(aEvent);
		}
		
		public GridEvent pop()
		{
			return itsStack.pop();
		}
		
		public int getDepth()
		{
			return itsStack.size();
		}
	}
}