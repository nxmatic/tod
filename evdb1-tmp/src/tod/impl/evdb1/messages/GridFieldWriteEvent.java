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
package tod.impl.evdb1.messages;

import static tod.impl.evdb1.ObjectCodec1.getObjectBits;
import static tod.impl.evdb1.ObjectCodec1.getObjectId;
import static tod.impl.evdb1.ObjectCodec1.readObject;
import static tod.impl.evdb1.ObjectCodec1.writeObject;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.common.event.FieldWriteEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.DebuggerGridConfig1;
import tod.impl.evdb1.SplittedConditionHandler;
import tod.impl.evdb1.db.Indexes;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.db.StdIndexSet;
import zz.utils.bit.BitStruct;

public class GridFieldWriteEvent extends BitGridEvent
{
	private static final long serialVersionUID = -6521145589404087223L;
	
	private int itsFieldId;
	private Object itsTarget;
	private Object itsValue;

	public GridFieldWriteEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridFieldWriteEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			int aFieldId, 
			Object aTarget, 
			Object aValue)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aFieldId, aTarget, aValue);
	}

	public GridFieldWriteEvent(IStructureDatabase aStructureDatabase, BitStruct aBitStruct)
	{
		super(aStructureDatabase, aBitStruct);
		itsFieldId = aBitStruct.readInt(DebuggerGridConfig1.EVENT_FIELD_BITS);
		itsTarget = readObject(aBitStruct);
		itsValue = readObject(aBitStruct);
	}

	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			int aFieldId, 
			Object aTarget, 
			Object aValue)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsFieldId = aFieldId;
		itsTarget = aTarget;
		itsValue = aValue;
	}
	
	@Override
	public void writeTo(BitStruct aBitStruct)
	{
		super.writeTo(aBitStruct);
		aBitStruct.writeInt(getFieldId(), DebuggerGridConfig1.EVENT_FIELD_BITS);
		writeObject(aBitStruct, getTarget());
		writeObject(aBitStruct, getValue());
	}

	@Override
	public int getBitCount()
	{
		int theCount = super.getBitCount();
		
		theCount += DebuggerGridConfig1.EVENT_FIELD_BITS;
		theCount += getObjectBits(getTarget());
		theCount += getObjectBits(getValue());
		
		return theCount;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		FieldWriteEvent theEvent = new FieldWriteEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setField(getFieldInfo(aBrowser, getFieldId()));
		theEvent.setTarget(getTarget());
		theEvent.setValue(getValue());
		return theEvent;
	}

	@Override
	public MessageType getEventType()
	{
		return MessageType.FIELD_WRITE;
	}

	public int getFieldId()
	{
		return itsFieldId;
	}

	public Object getTarget()
	{
		return itsTarget;
	}

	public Object getValue()
	{
		return itsValue;
	}

	private static StdIndexSet.StdTuple STD_TUPLE = new StdIndexSet.StdTuple(-1, -1);
	private static RoleIndexSet.RoleTuple ROLE_TUPLE = new RoleIndexSet.RoleTuple(-1, -1, -1);
	
	@Override
	public void index(Indexes aIndexes, long aPointer)
	{
		super.index(aIndexes, aPointer);
		STD_TUPLE.set(getTimestamp(), aPointer);
	
		aIndexes.indexField(getFieldId(), STD_TUPLE);
		
		ROLE_TUPLE.set(getTimestamp(), aPointer, RoleIndexSet.ROLE_OBJECT_TARGET);
		aIndexes.indexObject(
				getTarget(), 
				ROLE_TUPLE);
		
		ROLE_TUPLE.set(getTimestamp(), aPointer, RoleIndexSet.ROLE_OBJECT_VALUE);
		aIndexes.indexObject(
				getValue(), 
				ROLE_TUPLE);
	}
	
	@Override
	public boolean matchFieldCondition(int aFieldId)
	{
		return aFieldId == getFieldId();
	}
	
	@Override
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return ((aRole == RoleIndexSet.ROLE_OBJECT_VALUE  || aRole == RoleIndexSet.ROLE_OBJECT_ANY)
					&& SplittedConditionHandler.OBJECTS.match(
							aPart, 
							aPartialKey, 
							getObjectId(getValue(), false)))
							
			|| ((aRole == RoleIndexSet.ROLE_OBJECT_TARGET  || aRole == RoleIndexSet.ROLE_OBJECT_ANY)
					&& SplittedConditionHandler.OBJECTS.match(
							aPart, 
							aPartialKey, 
							getObjectId(getTarget(), false)));
	}

	@Override
	public String toString()
	{
		return String.format(
				"%s (f: %d, tg: %s, v: %s, %s)",
				getEventType(),
				itsFieldId,
				itsTarget,
				itsValue,
				toString0());
	}

}
