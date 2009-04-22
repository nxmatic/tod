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
package tod.impl.evdbng.messages;

import tod.core.DebugFlags;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.PagedFile.PageIOStream;
import zz.utils.bit.BitStruct;

public abstract class GridEventNG extends GridEvent
{
	public GridEventNG(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridEventNG(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		this(aStructureDatabase);
		int theThread = aStream.readThreadId();
		int theDepth = aStream.readCFlowDepth();
		long theTimestamp = aStream.readTimestamp();
		int[] theAdviceCFlow = readAdviceCFlow(aStream);
		int theProbeId = aStream.readProbeId();
		
		long theParentTimestamp = aStream.readTimestamp();
		
		set(theThread, theDepth, theTimestamp, theAdviceCFlow, theProbeId, theParentTimestamp);
	}
	
	/**
	 * Writes out a representation of this event to a {@link BitStruct}.
	 * Subclasses must override this method to serialize their attributes, and
	 * must call super first.
	 */
	public void writeTo(PageIOStream aStream)
	{
		aStream.writeByte(getMessageType().ordinal());
		aStream.writeThreadId(getThread());
		aStream.writeCFlowDepth(getDepth());
		aStream.writeTimestamp(getTimestamp());
		writeAdviceCFlow(aStream, getAdviceCFlow());
		aStream.writeProbeId(getProbeId());
		
		aStream.writeTimestamp(getParentTimestamp());
	}
	
	/**
	 * Returns the number of bits necessary to serialize this event. Subclasses
	 * should override this method and sum the number of bits they need to the
	 * number of bits returned by super.
	 */
	public int getMessageSize()
	{
		return PageIOStream.byteSize() 
			+ PageIOStream.threadIdSize()
			+ PageIOStream.cflowDepthSize()
			+ PageIOStream.timestampSize()
			+ coundAdviceCFlowBits(getAdviceCFlow())
			+ PageIOStream.probeIdSize()
			+ PageIOStream.timestampSize();
	}
	
	private static void writeAdviceCFlow(PageIOStream aStream, int[] aAdviceCFlow)
	{
		assert aAdviceCFlow == null || aAdviceCFlow.length < 255;
		aStream.writeByte(aAdviceCFlow != null ? aAdviceCFlow.length : 0);
		if (aAdviceCFlow != null) 
		{
			for(int theSourceId : aAdviceCFlow) aStream.writeAdviceSourceId(theSourceId);
		}
	}
	
	private static int[] readAdviceCFlow(PageIOStream aStream)
	{
		int theCount = aStream.readByte();
		if (theCount == 0) return null;
		
		int[] theCFlow = new int[theCount];
		for(int i=0;i<theCount;i++) theCFlow[i] = aStream.readAdviceSourceId();
		return theCFlow;
	}
	
	private static int coundAdviceCFlowBits(int[] aAdviceCFlow)
	{
		int theCount = PageIOStream.byteSize();
		if (aAdviceCFlow != null) theCount += aAdviceCFlow.length * PageIOStream.adviceSourceIdSize();
		return theCount;
	}
	

	/**
	 * Instructs this event to add relevant data to the indexes. The base
	 * version handles all common data; subclasses should override this method
	 * to index specific data.
	 * 
	 * @param aId
	 *            The internal pointer to this event.
	 */
	public void index(Indexes aIndexes, int aId)
	{
		ProbeInfo theProbeInfo = getProbeInfo();
		if (theProbeInfo != null) aIndexes.eventAtProbe(theProbeInfo);
		
		aIndexes.registerTimestamp(getTimestamp());
		aIndexes.indexType(getEventType().ordinal(), aId);
		
		if (! DebugFlags.DISABLE_LOCATION_INDEX)
		{
			if (theProbeInfo != null && theProbeInfo.bytecodeIndex >= 0) aIndexes.indexLocation(theProbeInfo.bytecodeIndex, aId);
		}

		if (theProbeInfo != null && theProbeInfo.behaviorId >= 0)
		{
			aIndexes.indexBehavior(theProbeInfo.behaviorId, aId, RoleIndexSet.ROLE_BEHAVIOR_OPERATION);
		}
		
		if (theProbeInfo != null && theProbeInfo.adviceSourceId >= 0)
		{
			aIndexes.indexAdviceSourceId(theProbeInfo.adviceSourceId, aId);
		}
		
		if (getAdviceCFlow() != null)
		{
			for (int theSourceId : getAdviceCFlow()) aIndexes.indexAdviceCFlow(theSourceId, aId);
		}
		
		if (theProbeInfo != null && theProbeInfo.role != null && theProbeInfo.role != BytecodeRole.UNKNOWN)
		{
			aIndexes.indexRole(theProbeInfo.role.ordinal()+1, aId);
		}
		
		if (getThread() > 0) 
		{
			aIndexes.indexThread(getThread(), aId);
		}
		
		aIndexes.indexDepth(getDepth(), aId);
		
	}

	/**
	 * Creates an event from a serialized representation, symmetric to
	 * {@link #writeTo(PageIOStream)}.
	 */
	public static GridEventNG read(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		MessageType theType = MessageType.VALUES[aStream.readByte()];
		switch (theType)
		{
		case BEHAVIOR_EXIT:
			return new GridBehaviorExitEvent(aStructureDatabase, aStream);
			
		case SUPER_CALL:
			return new GridBehaviorCallEvent(aStructureDatabase, aStream, MessageType.SUPER_CALL);
			
		case EXCEPTION_GENERATED:
			return new GridExceptionGeneratedEvent(aStructureDatabase, aStream);
			
		case FIELD_WRITE:
			return new GridFieldWriteEvent(aStructureDatabase, aStream);
			
		case NEW_ARRAY:
			return new GridNewArrayEvent(aStructureDatabase, aStream);
			
		case ARRAY_WRITE:
			return new GridArrayWriteEvent(aStructureDatabase, aStream);
			
		case INSTANTIATION:
			return new GridBehaviorCallEvent(aStructureDatabase, aStream, MessageType.INSTANTIATION);
			
		case LOCAL_VARIABLE_WRITE:
			return new GridVariableWriteEvent(aStructureDatabase, aStream);
			
		case INSTANCEOF:
			return new GridInstanceOfEvent(aStructureDatabase, aStream);
			
		case METHOD_CALL:
			return new GridBehaviorCallEvent(aStructureDatabase, aStream, MessageType.METHOD_CALL);
			
		default: throw new RuntimeException("Not handled: "+theType); 
		}
	}
	

}
