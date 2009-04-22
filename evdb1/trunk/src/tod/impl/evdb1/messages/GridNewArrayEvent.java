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
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.impl.common.event.NewArrayEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.DebuggerGridConfig1;
import tod.impl.evdb1.SplittedConditionHandler;
import tod.impl.evdb1.db.Indexes;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.db.StdIndexSet;
import zz.utils.bit.BitStruct;

public class GridNewArrayEvent extends BitGridEvent
{
	private static final long serialVersionUID = 6021435584407687823L;

	private Object itsTarget;
	private int itsBaseTypeId;
	private int itsSize;

	public GridNewArrayEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridNewArrayEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aTarget,
			int aBaseTypeId,
			int aSize)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aTarget, aBaseTypeId, aSize);
	}

	public GridNewArrayEvent(IStructureDatabase aStructureDatabase, BitStruct aBitStruct)
	{
		super(aStructureDatabase, aBitStruct);

		itsTarget = readObject(aBitStruct);
		itsBaseTypeId = aBitStruct.readInt(DebuggerGridConfig1.EVENT_TYPE_BITS);
		itsSize = aBitStruct.readInt(32);
	}

	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aTarget,
			int aBaseTypeId,
			int aSize)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsTarget = aTarget;
		itsBaseTypeId = aBaseTypeId;
		itsSize = aSize;
	}
	
	@Override
	public void writeTo(BitStruct aBitStruct)
	{
		super.writeTo(aBitStruct);
		writeObject(aBitStruct, getTarget());
		aBitStruct.writeInt(getBaseTypeId(), DebuggerGridConfig1.EVENT_TYPE_BITS);
		aBitStruct.writeInt(getSize(), 32);
	}

	@Override
	public int getBitCount()
	{
		int theCount = super.getBitCount();
		
		theCount += getObjectBits(getTarget());
		theCount += DebuggerGridConfig1.EVENT_TYPE_BITS;
		theCount += 32;
		
		return theCount;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		NewArrayEvent theEvent = new NewArrayEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setInstance(getTarget());
		
		ITypeInfo theBaseType = getTypeInfo(aBrowser, getBaseTypeId());
		IArrayTypeInfo theType = getArrayTypeInfo(aBrowser, theBaseType, 1);

		theEvent.setType(theType);
		theEvent.setArraySize(getSize());
		
		return theEvent;
	}

	@Override
	public MessageType getEventType()
	{
		return MessageType.NEW_ARRAY;
	}

	public int getBaseTypeId()
	{
		return itsBaseTypeId;
	}

	public Object getTarget()
	{
		return itsTarget;
	}

	public int getSize()
	{
		return itsSize;
	}

	private static StdIndexSet.StdTuple STD_TUPLE = new StdIndexSet.StdTuple(-1, -1);
	private static RoleIndexSet.RoleTuple ROLE_TUPLE = new RoleIndexSet.RoleTuple(-1, -1, -1);
	
	@Override
	public void index(Indexes aIndexes, long aPointer)
	{
		super.index(aIndexes, aPointer);
		STD_TUPLE.set(getTimestamp(), aPointer);
	
		ROLE_TUPLE.set(getTimestamp(), aPointer, RoleIndexSet.ROLE_OBJECT_TARGET);
		aIndexes.indexObject(
				getTarget(), 
				ROLE_TUPLE);
	}
	
	@Override
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return ((aRole == RoleIndexSet.ROLE_OBJECT_TARGET  || aRole == RoleIndexSet.ROLE_OBJECT_ANY)
					&& SplittedConditionHandler.OBJECTS.match(
							aPart, 
							aPartialKey, 
							getObjectId(getTarget(), false)));
	}

	@Override
	public String toString()
	{
		return String.format(
				"%s (tg: %s, bt: %d, d: %d, %s)",
				getEventType(),
				itsTarget,
				itsBaseTypeId,
				itsSize,
				toString0());
	}

}
