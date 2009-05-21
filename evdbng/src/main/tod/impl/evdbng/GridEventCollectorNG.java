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
package tod.impl.evdbng;

import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.impl.dbgrid.GridEventCollector;
import tod.impl.dbgrid.RIGridMaster;
import tod.impl.dbgrid.db.DatabaseNode;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.messages.GridArrayWriteEvent;
import tod.impl.evdbng.messages.GridBehaviorCallEvent;
import tod.impl.evdbng.messages.GridBehaviorExitEvent;
import tod.impl.evdbng.messages.GridExceptionGeneratedEvent;
import tod.impl.evdbng.messages.GridFieldWriteEvent;
import tod.impl.evdbng.messages.GridInstanceOfEvent;
import tod.impl.evdbng.messages.GridNewArrayEvent;
import tod.impl.evdbng.messages.GridVariableWriteEvent;
import tod.utils.TODUtils;

/**
 * Event collector for the grid database backend. It handles events from a single
 * hosts, preprocesses them and sends them to the {@link AbstractEventDispatcher}.
 * Preprocessing is minimal and only involves packaging.
 * This class is not thread-safe.
 * @author gpothier
 */
public class GridEventCollectorNG extends GridEventCollector
{
	private final DatabaseNode itsDatabaseNode;
	
	/**
	 * Number of events received by this collector
	 */
	private long itsEventsCount;
	
	private final IMutableStructureDatabase itsStructureDatabase;
	
	
	public GridEventCollectorNG(
			RIGridMaster aMaster,
			IHostInfo aHost,
			IMutableStructureDatabase aStructureDatabase,
			DatabaseNode aDispatcher)
	{
		super(aMaster, aHost, aStructureDatabase);
		itsDatabaseNode = aDispatcher;
		itsStructureDatabase = aStructureDatabase;
	}

	private void dispatch(GridEvent aEvent)
	{
		itsDatabaseNode.pushEvent(aEvent);
		itsEventsCount++;
	}
	
	/**
	 * Returns the number of events received by this collector.
	 */
	public long getEventsCount()
	{
		return itsEventsCount;
	}
	
	/**
	 * Returns the probe info corresponding to the given probe id.
	 */
	private final ProbeInfo getProbeInfo(int aProbeId)
	{
		if (aProbeId == -1) return ProbeInfo.NULL;
		else return itsStructureDatabase.getProbeInfo(aProbeId);
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
		ProbeInfo theProbeInfo =
				itsStructureDatabase.getNewExceptionProbe(aBehaviorId, aOperationBytecodeIndex);
		assert theProbeInfo != null;

		exception(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, theProbeInfo.id, aException);
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
		TODUtils.logf(1, "GridEventCollector.exception()");

		GridExceptionGeneratedEvent theEvent = new GridExceptionGeneratedEvent(
				itsStructureDatabase,
				aThreadId,
				aDepth,
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				aException);

		dispatch(theEvent);
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
		GridBehaviorExitEvent theEvent = new GridBehaviorExitEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth, 
				aTimestamp, 
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp, 
				aHasThrown,
				aResult,
				aBehaviorId);
		
		dispatch(theEvent);
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
		GridFieldWriteEvent theEvent = new GridFieldWriteEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth,
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				aFieldId, 
				aTarget, 
				aValue);
		
		dispatch(theEvent);
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
		GridNewArrayEvent theEvent = new GridNewArrayEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth,
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				aTarget,
				aBaseTypeId,
				aSize);
		
		dispatch(theEvent);
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
		GridArrayWriteEvent theEvent = new GridArrayWriteEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth,
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				aTarget,
				aIndex,
				aValue);
		
		dispatch(theEvent);
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
		GridInstanceOfEvent theEvent = new GridInstanceOfEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth,
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				aObject,
				aTypeId,
				aResult);
		
		dispatch(theEvent);
	}

	public void instantiation(
			int aThreadId,
			long aParentTimestamp,
			short aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehavior,
			int aExecutedBehavior,
			Object aTarget, Object[] aArguments)
	{
		GridBehaviorCallEvent theEvent = new GridBehaviorCallEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				MessageType.INSTANTIATION, 
				aDirectParent, 
				aArguments,
				aCalledBehavior,
				aExecutedBehavior,
				aTarget);
		
		dispatch(theEvent);
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
		GridVariableWriteEvent theEvent = new GridVariableWriteEvent(
				itsStructureDatabase,
				aThreadId,
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				aVariableId, 
				aValue);
		
		dispatch(theEvent);
	}


	public void methodCall(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehavior,
			int aExecutedBehavior,
			Object aTarget, Object[] aArguments)
	{
		GridBehaviorCallEvent theEvent = new GridBehaviorCallEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				MessageType.METHOD_CALL, 
				aDirectParent, 
				aArguments,
				aCalledBehavior,
				aExecutedBehavior,
				aTarget);
		
		dispatch(theEvent);
	}


	public void superCall(
			int aThreadId,
			long aParentTimestamp,
			short aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent, 
			int aCalledBehavior,
			int aExecutedBehavior, 
			Object aTarget, Object[] aArguments)
	{
		GridBehaviorCallEvent theEvent = new GridBehaviorCallEvent(
				itsStructureDatabase,
				aThreadId, 
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aParentTimestamp,
				MessageType.SUPER_CALL, 
				aDirectParent, 
				aArguments,
				aCalledBehavior,
				aExecutedBehavior,
				aTarget);
		
		dispatch(theEvent);
	}


	public void register(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable)
	{
		itsDatabaseNode.register(aObjectUID, aData, aTimestamp, aIndexable);
	}
	
	public void registerRefObject(long aId, long aTimestamp, long aClassId)
	{
		itsDatabaseNode.registerRefObject(aId, aTimestamp, aClassId);
	}	
	
	public void registerClass(long aId, long aLoaderId, String aName)
	{
		itsDatabaseNode.registerClass(aId, aLoaderId, aName);
	}

	public void registerClassLoader(long aId, long aClassId)
	{
		itsDatabaseNode.registerClassLoader(aId, aClassId);
	}

	public void clear()
	{
		itsDatabaseNode.clear();
	}

	public int flush()
	{
		return itsDatabaseNode.flush();
	}

}
