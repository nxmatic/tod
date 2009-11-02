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
import tod.impl.common.event.ExceptionGeneratedEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.Page.PageIOStream;

public class GridExceptionGeneratedEvent extends GridEventNG
{
	private static final long serialVersionUID = 7070448347537157710L;
	
	private Object itsException;
	
	public GridExceptionGeneratedEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridExceptionGeneratedEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aException)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aException);
	}

	public GridExceptionGeneratedEvent(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		super(aStructureDatabase, aStream);

		itsException = readObject(aStream);
	}
	
	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aException)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsException = aException;
	}

	@Override
	public void writeTo(PageIOStream aBitStruct)
	{
		super.writeTo(aBitStruct);
		writeObject(aBitStruct, getException());
	}

	@Override
	public int getMessageSize()
	{
		int theCount = super.getMessageSize();
		
		theCount += getObjectSize(getException());

		return theCount;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		ExceptionGeneratedEvent theEvent = new ExceptionGeneratedEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setException(getException());
		return theEvent;
	}
	
	@Override
	public MessageType getEventType()
	{
		return MessageType.EXCEPTION_GENERATED;
	}

	public Object getException()
	{
		return itsException;
	}
	
	@Override
	public void index(Indexes aIndexes, int aId)
	{
		super.index(aIndexes, aId);
				
		aIndexes.indexObject(getException(), aId, RoleIndexSet.ROLE_OBJECT_EXCEPTION); 
	}
	
	@Override
	public boolean matchBehaviorCondition(int aBehaviorId, byte aRole)
	{
		return (aRole == RoleIndexSet.ROLE_BEHAVIOR_OPERATION) 
			&& (aBehaviorId == getProbeInfo().behaviorId);			
	}
	
	@Override
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return ((aRole == RoleIndexSet.ROLE_OBJECT_EXCEPTION || aRole == RoleIndexSet.ROLE_OBJECT_ANY) 
				&& SplittedConditionHandler.OBJECTS.match(
						aPart, 
						aPartialKey, 
						getObjectId(getException(), false)));
	}

	@Override
	public String toString()
	{
		return String.format(
				"%s (ex: %s, b: %d, %s)",
				getEventType(),
				itsException,
				getProbeInfo().behaviorId,
				toString0());
	}
}
