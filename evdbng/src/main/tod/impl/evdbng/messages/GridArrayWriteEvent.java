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
import tod.core.database.structure.IStructureDatabase;
import tod.impl.common.event.ArrayWriteEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.Page.PageIOStream;

public class GridArrayWriteEvent extends GridEventNG
{
	private static final long serialVersionUID = 3605816555618929935L;
	
	private Object itsTarget;
	private int itsIndex;
	private Object itsValue;

	public GridArrayWriteEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridArrayWriteEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aTarget,
			int aIndex,
			Object aValue)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aTarget, aIndex, aValue);
	}

	public GridArrayWriteEvent(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		super(aStructureDatabase, aStream);

		itsTarget = readObject(aStream);
		itsIndex = aStream.readInt();
		itsValue = readObject(aStream);
	}

	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aTarget,
			int aIndex,
			Object aValue)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsTarget = aTarget;
		itsIndex = aIndex;
		itsValue = aValue;
	}
	
	@Override
	public void writeTo(PageIOStream aBitStruct)
	{
		super.writeTo(aBitStruct);
		writeObject(aBitStruct, getTarget());
		aBitStruct.writeInt(getIndex());
		writeObject(aBitStruct, getValue());
	}

	@Override
	public int getMessageSize()
	{
		int theCount = super.getMessageSize();
		
		theCount += getObjectSize(getTarget());
		theCount += PageIOStream.intSize();
		theCount += getObjectSize(getValue());
		
		return theCount;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		ArrayWriteEvent theEvent = new ArrayWriteEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setTarget(getTarget());
		theEvent.setIndex(itsIndex);
		theEvent.setValue(getValue());
		return theEvent;
	}

	@Override
	public MessageType getEventType()
	{
		return MessageType.ARRAY_WRITE;
	}

	public int getIndex()
	{
		return itsIndex;
	}

	public Object getTarget()
	{
		return itsTarget;
	}

	public Object getValue()
	{
		return itsValue;
	}

	@Override
	public void index(Indexes aIndexes, int aId)
	{
		super.index(aIndexes, aId);
		
		aIndexes.indexObject(getTarget(), aId, RoleIndexSet.ROLE_OBJECT_TARGET);
		aIndexes.indexArrayIndex(getIndex(), aId);
		
		aIndexes.indexObject(getValue(), aId, RoleIndexSet.ROLE_OBJECT_VALUE);
	}
	
	@Override
	public boolean matchIndexCondition(int aPart, int aPartialKey)
	{
		return SplittedConditionHandler.INDEXES.match(aPart, aPartialKey, itsIndex);
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
				"%s (tg: %s, i: %d, v: %s, %s)",
				getEventType(),
				itsTarget,
				itsIndex,
				itsValue,
				toString0());
	}

}
