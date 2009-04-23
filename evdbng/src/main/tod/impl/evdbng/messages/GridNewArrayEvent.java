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

import static tod.impl.evdbng.ObjectCodecNG.getObjectId;
import static tod.impl.evdbng.ObjectCodecNG.getObjectSize;
import static tod.impl.evdbng.ObjectCodecNG.readObject;
import static tod.impl.evdbng.ObjectCodecNG.writeObject;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.impl.common.event.InstanceOfEvent;
import tod.impl.common.event.NewArrayEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.event.InstantiationEvent;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.PagedFile.PageIOStream;

public class GridNewArrayEvent extends GridEventNG
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

	public GridNewArrayEvent(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		super(aStructureDatabase, aStream);

		itsTarget = readObject(aStream);
		itsBaseTypeId = aStream.readTypeId();
		itsSize = aStream.readInt();
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
	public void writeTo(PageIOStream aBitStruct)
	{
		super.writeTo(aBitStruct);
		writeObject(aBitStruct, getTarget());
		aBitStruct.writeTypeId(getBaseTypeId());
		aBitStruct.writeInt(getSize());
	}

	@Override
	public int getMessageSize()
	{
		int theCount = super.getMessageSize();
		
		theCount += getObjectSize(getTarget());
		theCount += PageIOStream.typeIdSize();
		theCount += PageIOStream.intSize();
		
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

	@Override
	public void index(Indexes aIndexes, int aId)
	{
		super.index(aIndexes, aId);
	
		aIndexes.indexObject(getTarget(), aId, RoleIndexSet.ROLE_OBJECT_TARGET); 
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
