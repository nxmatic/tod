/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.evdb1;

import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_ADVICE_SRC_ID_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_ARRAY_INDEX_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_BEHAVIOR_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_BYTECODE_LOCS_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_DEPTH_RANGE;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_FIELD_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_OBJECT_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_THREADS_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_VAR_COUNT;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.dbgrid.EventGenerator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.messages.BitGridEvent;
import tod.impl.evdb1.messages.GridArrayWriteEvent;
import tod.impl.evdb1.messages.GridBehaviorCallEvent;
import tod.impl.evdb1.messages.GridBehaviorExitEvent;
import tod.impl.evdb1.messages.GridExceptionGeneratedEvent;
import tod.impl.evdb1.messages.GridFieldWriteEvent;
import tod.impl.evdb1.messages.GridInstanceOfEvent;
import tod.impl.evdb1.messages.GridNewArrayEvent;
import tod.impl.evdb1.messages.GridVariableWriteEvent;

public class EventGenerator1 extends EventGenerator
{
	public EventGenerator1(IMutableStructureDatabase aStructureDatabase, long aSeed)
	{
		super(
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
	
	public EventGenerator1(IMutableStructureDatabase aStructureDatabase, long aSeed, int aThreadsRange, int aDepthRange,
			int aBytecodeRange, int aBehaviorRange, int aAdviceSourceIdRange,
			int aFieldRange, int aVariableRange, int aObjectRange,
			int aArrayIndexRange)
	{
		super(aStructureDatabase, aSeed, aThreadsRange, aDepthRange, aBytecodeRange, aBehaviorRange,
				aAdviceSourceIdRange, aFieldRange, aVariableRange, aObjectRange,
				aArrayIndexRange);
	}

	@Override
	public BitGridEvent next()
	{
		return (BitGridEvent) super.next();
	}
	
	@Override
	protected BitGridEvent genInstanceOf(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genNewArray(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genArrayWrite(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genMethodCall(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genVariableWrite(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genInstantiation(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genFieldWrite(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genException(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genSuperCall(int aThreadId, int aDepth, long aParentTimestamp)
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
	protected BitGridEvent genBehaviorExit(int aThreadId, int aDepth, long aParentTimestamp)
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