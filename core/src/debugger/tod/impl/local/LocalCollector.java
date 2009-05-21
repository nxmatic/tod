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
package tod.impl.local;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import tod.core.DebugFlags;
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.transport.ObjectDecoder;
import tod.impl.common.EventCollector;
import tod.impl.common.event.ArrayWriteEvent;
import tod.impl.common.event.BehaviorExitEvent;
import tod.impl.common.event.Event;
import tod.impl.common.event.ExceptionGeneratedEvent;
import tod.impl.common.event.FieldWriteEvent;
import tod.impl.common.event.InstanceOfEvent;
import tod.impl.common.event.LocalVariableWriteEvent;
import tod.impl.database.structure.standard.ThreadInfo;
import tod.impl.local.event.BehaviorCallEvent;
import tod.impl.local.event.ConstructorChainingEvent;
import tod.impl.local.event.InstantiationEvent;
import tod.impl.local.event.MethodCallEvent;
import zz.utils.ArrayStack;
import zz.utils.Stack;


/**
 * This log collector stores all the events it receives,
 * and provides an API for accessing the recorded information
 * in a convenient way.
 * @author gpothier
 */
public class LocalCollector extends EventCollector
{
	private final LocalBrowser itsBrowser;
	
	public LocalCollector(LocalBrowser aBrowser, IHostInfo aHost)
	{
		super(aHost, aBrowser.getStructureDatabase());
		itsBrowser = aBrowser;
		itsBrowser.addHost(aHost);
	}

	private void addEvent(Event aEvent)
	{
		if (DebugFlags.LOCAL_COLLECTOR_STORE) itsBrowser.addEvent(aEvent);
	}
	
	private IBehaviorInfo getBehavior(int aId)
	{
		return itsBrowser.getStructureDatabase().getBehavior(aId, true);
	}
	
	private IFieldInfo getField(int aId)
	{
		return itsBrowser.getStructureDatabase().getField(aId, true);
	}
	
	private ITypeInfo getType(int aId)
	{
		return itsBrowser.getStructureDatabase().getType(aId, true);
	}
	
	@Override
	protected ThreadInfo createThreadInfo(IHostInfo aHost, int aId, long aJVMId, String aName)
	{
		return new LocalThreadInfo(aHost, aId, aJVMId, aName);
	}
	
	@Override
	public LocalThreadInfo getThread(int aId)
	{
		return (LocalThreadInfo) super.getThread(aId);
	}
	
	private void initEvent(
			Event aEvent, 
			LocalThreadInfo aThread, 
			long aParentTimestamp, 
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId)
	{
		BehaviorCallEvent theParentEvent = aThread.peekParent();
//		assert theParentEvent == null || theParentEvent.getTimestamp() == aParentTimestamp;
		
		aEvent.setThread(aThread);
		aEvent.setDepth(aDepth);
		
		aEvent.setParentTimestamp(theParentEvent != null ? 
				theParentEvent.getTimestamp() 
				: 0);
		
		aEvent.setTimestamp(aTimestamp);
		aEvent.setAdviceCFlow(aAdviceCFlow);
		aEvent.setProbeId(aProbeId);
		
		if (DebugFlags.LOCAL_COLLECTOR_STORE && theParentEvent != null) 
		{
			theParentEvent.addChild(aEvent);
		}
	}

	@Override
	protected void exception(
			int aThreadId, 
			long aParentTimestamp, 
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow, 
			int aBehaviorId,
			int aOperationBytecodeIndex, 
			Object aException)
	{
		ProbeInfo theProbeInfo = itsBrowser.getStructureDatabase().getNewExceptionProbe(aBehaviorId, aOperationBytecodeIndex);
		exception(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, theProbeInfo.id, aOperationBytecodeIndex, aException);
	}

	
	
	public void exception(
			int aThreadId,
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aException)
	{
		ExceptionGeneratedEvent theEvent = new ExceptionGeneratedEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);
		
		theEvent.setException(aException);
		
		addEvent(theEvent);
	}

	public void behaviorExit(
			int aThreadId,
			long aParentTimestamp, 
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow, 
			int aProbeId,
			int aBehaviorId, 
			boolean aHasThrown, Object aResult)
	{
		BehaviorExitEvent theEvent = new BehaviorExitEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);
		
		theEvent.setHasThrown(aHasThrown);
		theEvent.setResult(aResult);
		
		addEvent(theEvent);
		
		theThread.popParent();
	}

	public void fieldWrite(
			int aThreadId, 
			long aParentTimestamp, 
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			int aFieldId, 
			Object aTarget, Object aValue)
	{
		FieldWriteEvent theEvent = new FieldWriteEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setField(getField(aFieldId));
		theEvent.setTarget(aTarget);
		theEvent.setValue(aValue);
		
		addEvent(theEvent);
	}
	
	public void newArray(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId, 
			Object aTarget,
			int aBaseTypeId, int aSize)
	{
		InstantiationEvent theEvent = new InstantiationEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setTarget(aTarget);
		ITypeInfo theBaseType = getType(aBaseTypeId);
		IArrayTypeInfo theType = itsBrowser.getStructureDatabase().getArrayType(theBaseType, 1);
		theEvent.setType(theType);
		
		theEvent.setArguments(new Object[] { aSize });
		
		addEvent(theEvent);
	}

	public void arrayWrite(
			int aThreadId, 
			long aParentTimestamp, 
			short aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId, 
			Object aTarget, 
			int aIndex, Object aValue)
	{
		ArrayWriteEvent theEvent = new ArrayWriteEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setTarget(aTarget);
		theEvent.setIndex(aIndex);
		theEvent.setValue(aValue);
		
		addEvent(theEvent);
	}

	public void instantiation(
			int aThreadId, 
			long aParentTimestamp, 
			short aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow, 
			int aProbeId, 
			boolean aDirectParent, 
			int aCalledBehaviorId,
			int aExecutedBehaviorId, 
			Object aTarget, Object[] aArguments)
	{
		InstantiationEvent theEvent = new InstantiationEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setDirectParent(aDirectParent);
		theEvent.setCalledBehavior(getBehavior(aCalledBehaviorId));
		theEvent.setExecutedBehavior(getBehavior(aExecutedBehaviorId));
		theEvent.setTarget(aTarget);
		theEvent.setArguments(aArguments);
		
		addEvent(theEvent);
		
		theThread.pushParent(theEvent);
	}

	public void localWrite(
			int aThreadId, 
			long aParentTimestamp, 
			short aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId, 
			int aVariableId, Object aValue)
	{
		LocalVariableWriteEvent theEvent = new LocalVariableWriteEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		IBehaviorInfo theBehavior = theThread.peekParent().getExecutedBehavior();
		LocalVariableInfo theInfo = theBehavior.getLocalVariableInfo(theEvent.getOperationBytecodeIndex(), aVariableId); 
       	if (theInfo == null) theInfo = new LocalVariableInfo((short)-1, (short)-1, "$"+aVariableId, "", (short)-1);
        
		theEvent.setVariable(theInfo);
		
		theEvent.setValue(aValue);
		
		addEvent(theEvent);
	}
	
	

	public void instanceOf(
			int aThreadId,
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aObject,
			int aTypeId,
			boolean aResult)
	{
		InstanceOfEvent theEvent = new InstanceOfEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setObject(aObject);
		theEvent.setTestedType(getType(aTypeId));
		
		addEvent(theEvent);
	}

	public void methodCall(
			int aThreadId,
			long aParentTimestamp,
			short aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow, 
			int aProbeId, 
			boolean aDirectParent,
			int aCalledBehaviorId,
			int aExecutedBehaviorId,
			Object aTarget, Object[] aArguments)
	{
		MethodCallEvent theEvent = new MethodCallEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setDirectParent(aDirectParent);
		theEvent.setCalledBehavior(getBehavior(aCalledBehaviorId));
		theEvent.setExecutedBehavior(getBehavior(aExecutedBehaviorId));
		theEvent.setTarget(aTarget);
		theEvent.setArguments(aArguments);
		
		addEvent(theEvent);
		theThread.pushParent(theEvent);
	}

	public void superCall(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId, 
			boolean aDirectParent,
			int aCalledBehaviorId,
			int aExecutedBehaviorId, 
			Object aTarget, Object[] aArguments)
	{
		ConstructorChainingEvent theEvent = new ConstructorChainingEvent(itsBrowser);
		LocalThreadInfo theThread = getThread(aThreadId);
		initEvent(theEvent, theThread, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId);

		theEvent.setDirectParent(aDirectParent);
		theEvent.setCalledBehavior(getBehavior(aCalledBehaviorId));
		theEvent.setExecutedBehavior(getBehavior(aExecutedBehaviorId));
		theEvent.setTarget(aTarget);
		theEvent.setArguments(aArguments);
		
		addEvent(theEvent);
		theThread.pushParent(theEvent);
	}
	
	@Override
	protected void thread(ThreadInfo aThread)
	{
		itsBrowser.addThread(aThread);
	}
	
	public void register(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable)
	{
		//timestamp is not necessary in local version
		Object theObject = ObjectDecoder.decode(new DataInputStream(new ByteArrayInputStream(aData)));
		itsBrowser.register(aObjectUID, theObject);
	}
	
	public void registerClass(long aId, long aLoaderId, String aName)
	{
	}

	public void registerClassLoader(long aId, long aClassId)
	{
	}

	public void registerRefObject(long aId, long aTimestamp, long aClassId)
	{
	}

	public void clear()
	{
		itsBrowser.clear();
	}

	public int flush()
	{
		return 0;
	}



	private static class LocalThreadInfo extends ThreadInfo
	{
		private Stack<BehaviorCallEvent> itsParentsStack = new ArrayStack<BehaviorCallEvent>();

		public LocalThreadInfo(IHostInfo aHost, int aId, long aJVMId, String aName)
		{
			super(aHost, aId, aJVMId, aName);
		}
		
		public void pushParent(BehaviorCallEvent aEvent)
		{
			itsParentsStack.push(aEvent);
		}
		
		public BehaviorCallEvent popParent()
		{
			return itsParentsStack.pop();
		}
		
		public BehaviorCallEvent peekParent()
		{
			return itsParentsStack.peek();
		}
	}
}
