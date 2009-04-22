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

import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_ADVICE_SRC_ID_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_ARRAY_INDEX_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_BEHAVIOR_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_BYTECODE_LOCS_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_DEPTH_RANGE;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_FIELD_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_OBJECT_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_THREADS_COUNT;
import static tod.impl.evdbng.DebuggerGridConfigNG.STRUCTURE_VAR_COUNT;

import java.util.Random;

import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.dbgrid.EventGenerator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.messages.GridArrayWriteEvent;
import tod.impl.evdbng.messages.GridBehaviorCallEvent;
import tod.impl.evdbng.messages.GridBehaviorExitEvent;
import tod.impl.evdbng.messages.GridEventNG;
import tod.impl.evdbng.messages.GridExceptionGeneratedEvent;
import tod.impl.evdbng.messages.GridFieldWriteEvent;
import tod.impl.evdbng.messages.GridInstanceOfEvent;
import tod.impl.evdbng.messages.GridNewArrayEvent;
import tod.impl.evdbng.messages.GridVariableWriteEvent;
import tod.impl.evdbng.test.TimestampGenerator;

public class EventGeneratorNG extends EventGenerator
{

	public EventGeneratorNG(
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
		super(aStructureDatabase, aSeed, aThreadsRange, aDepthRange, aBytecodeRange, aBehaviorRange, aAdviceSourceIdRange, aFieldRange, aVariableRange, aObjectRange, aArrayIndexRange);
	}

	public EventGeneratorNG(IMutableStructureDatabase aStructureDatabase, long aSeed)
	{
		this(
				aStructureDatabase,
				aSeed, 
				STRUCTURE_THREADS_COUNT,
				STRUCTURE_DEPTH_RANGE,
				STRUCTURE_BYTECODE_LOCS_COUNT,
				STRUCTURE_BEHAVIOR_COUNT,
				STRUCTURE_ADVICE_SRC_ID_COUNT,
				STRUCTURE_FIELD_COUNT,
				STRUCTURE_VAR_COUNT,
				STRUCTURE_OBJECT_COUNT,
				STRUCTURE_ARRAY_INDEX_COUNT);
	}
	
	@Override
	public GridEventNG next()
	{
		return (GridEventNG) super.next();
	}
	
	@Override
	protected GridEventNG genInstanceOf(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridInstanceOfEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				genObject(),
				genTypeId(),
				genBoolean());
	}

	@Override
	protected GridEventNG genNewArray(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridNewArrayEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				genObject(),
				genFieldId(),
				1000);
	}

	@Override
	protected GridEventNG genArrayWrite(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridArrayWriteEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				genObject(),
				genArrayIndex(),
				genObject());
	}

	@Override
	protected GridEventNG genMethodCall(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridBehaviorCallEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				MessageType.METHOD_CALL,
				genBoolean(),
				genArgs(),
				genBehaviorId(),
				genBehaviorId(),
				genObject());
	}

	@Override
	protected GridEventNG genVariableWrite(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridVariableWriteEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				genVariableId(),
				genObject());
	}

	@Override
	protected GridEventNG genInstantiation(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridBehaviorCallEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				MessageType.INSTANTIATION,
				genBoolean(),
				genArgs(),
				genBehaviorId(),
				genBehaviorId(),
				genObject());
	}

	@Override
	protected GridEventNG genFieldWrite(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridFieldWriteEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				genFieldId(),
				genObject(),
				genObject());
	}

	@Override
	protected GridEventNG genException(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridExceptionGeneratedEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				genObject());
	}

	@Override
	protected GridEventNG genSuperCall(int aThreadId, int aDepth, long aParentTimestamp)
	{
		return new GridBehaviorCallEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				genProbeId(),
				aParentTimestamp,
				MessageType.SUPER_CALL,
				genBoolean(),
				genArgs(),
				genBehaviorId(),
				genBehaviorId(),
				genObject());
	}

	@Override
	protected GridEventNG genBehaviorExit(int aThreadId, int aDepth, long aParentTimestamp)
	{
		int theProbeId = genProbeId();
		return new GridBehaviorExitEvent(
				getStructureDatabase(),
				aThreadId,
				aDepth,
				genTimestamp(),
				genAdviceCFlow(),
				theProbeId,
				aParentTimestamp,
				genBoolean(),
				genObject(),
				getProbeBehavior(theProbeId));
	}

	

}