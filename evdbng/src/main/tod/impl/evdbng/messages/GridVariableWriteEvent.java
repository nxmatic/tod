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
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.impl.common.event.LocalVariableWriteEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.Page.PageIOStream;

public class GridVariableWriteEvent extends GridEventNG
{
	private static final long serialVersionUID = 5600466618091824186L;
	
	private int itsVariableId;
	private Object itsValue;
	
	public GridVariableWriteEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridVariableWriteEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			int aVariableId, 
			Object aValue)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aVariableId, aValue);
	}


	public GridVariableWriteEvent(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		super(aStructureDatabase, aStream);

		itsVariableId = aStream.readVariableId();
		
		// TODO: this is a hack. We should not allow negative values.
		if (itsVariableId == 0xffff) itsVariableId = -1;
		
		itsValue = readObject(aStream);
	}

	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			int aVariableId, 
			Object aValue)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsVariableId = aVariableId;
		itsValue = aValue;
	}
	
	@Override
	public void writeTo(PageIOStream aBitStruct)
	{
		super.writeTo(aBitStruct);
		aBitStruct.writeVariableId(getVariableId());
		writeObject(aBitStruct, getValue());
	}

	@Override
	public int getMessageSize()
	{
		int theCount = super.getMessageSize();
		
		theCount += PageIOStream.variableIdSize();
		theCount += getObjectSize(getValue());
		
		return theCount;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		LocalVariableWriteEvent theEvent = new LocalVariableWriteEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setValue(getValue());
		
		IBehaviorInfo theBehavior = theEvent.getOperationBehavior();
		
		LocalVariableInfo theInfo = theBehavior.getLocalVariableInfo(
				getProbeInfo().bytecodeIndex, 
				getVariableId());
		
       	if (theInfo == null) theInfo = new LocalVariableInfo(
       			(short) -1, 
       			(short) -1, 
       			"$"+getVariableId(), 
       			"", 
       			(short) getVariableId());

		theEvent.setVariable(theInfo); 
		
		return theEvent;
	}

	@Override
	public MessageType getEventType()
	{
		return MessageType.LOCAL_VARIABLE_WRITE;
	}


	public int getVariableId()
	{
		return itsVariableId;
	}


	public Object getValue()
	{
		return itsValue;
	}
	
	@Override
	public void index(Indexes aIndexes, int aId)
	{
		super.index(aIndexes, aId);
		
		// TODO: this should not be necessary, we should not have negative values.
		if (getVariableId() >= 0)
		{
			aIndexes.indexVariable(getVariableId(), aId);
		}

		aIndexes.indexObject(getValue(), aId, RoleIndexSet.ROLE_OBJECT_VALUE); 
	}
	
	@Override
	public boolean matchVariableCondition(int aVariableId)
	{
		return aVariableId == getVariableId();
	}
	
	@Override
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return ((aRole == RoleIndexSet.ROLE_OBJECT_VALUE || aRole == RoleIndexSet.ROLE_OBJECT_ANY)
					&& SplittedConditionHandler.OBJECTS.match(
							aPart, 
							aPartialKey, 
							getObjectId(getValue(), false)));
	}

	@Override
	public String toString()
	{
		return String.format(
				"%s (val: %s, var: %d, %s)",
				getEventType(),
				itsValue,
				itsVariableId,
				toString0());
	}

}
