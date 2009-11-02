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
import tod.impl.common.event.InstanceOfEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.Page.PageIOStream;

public class GridInstanceOfEvent extends GridEventNG
{
	private static final long serialVersionUID = 3623816528718976105L;
	
	private Object itsObject;
	private int itsTypeId;
	private boolean itsResult;

	public GridInstanceOfEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridInstanceOfEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aObject,
			int aTypeId,
			boolean aResult)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aObject, aTypeId, aResult);
	}

	public GridInstanceOfEvent(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		super(aStructureDatabase, aStream);
		itsObject = readObject(aStream);
		itsTypeId = aStream.readTypeId();
		itsResult = aStream.readBoolean();
	}

	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			Object aObject,
			int aTypeId,
			boolean aResult)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsObject = aObject;
		itsTypeId = aTypeId;
		itsResult = aResult;
	}
	
	@Override
	public void writeTo(PageIOStream aStream)
	{
		super.writeTo(aStream);
		writeObject(aStream, getObject());
		aStream.writeTypeId(getTestedTypeId());
		aStream.writeBoolean(itsResult);
	}

	@Override
	public int getMessageSize()
	{
		int theCount = super.getMessageSize();
		
		theCount += getObjectSize(getObject());
		theCount += PageIOStream.typeIdSize();
		theCount += PageIOStream.booleanSize();
		
		return theCount;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		InstanceOfEvent theEvent = new InstanceOfEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setObject(getObject());
		theEvent.setTestedType(getTypeInfo(aBrowser, itsTypeId));
		theEvent.setResult(getResult());
		return theEvent;
	}

	@Override
	public MessageType getEventType()
	{
		return MessageType.INSTANCEOF;
	}

	public int getTestedTypeId()
	{
		return itsTypeId;
	}

	public Object getObject()
	{
		return itsObject;
	}
	
	public boolean getResult()
	{
		return itsResult;
	}

	
	@Override
	public void index(Indexes aIndexes, int aId)
	{
		super.index(aIndexes, aId);
	
		aIndexes.indexObject(getObject(), aId, RoleIndexSet.ROLE_OBJECT_TARGET);
	}
	
	@Override
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return ((aRole == RoleIndexSet.ROLE_OBJECT_TARGET  || aRole == RoleIndexSet.ROLE_OBJECT_ANY)
					&& SplittedConditionHandler.OBJECTS.match(
							aPart, 
							aPartialKey, 
							getObjectId(getObject(), false)));
	}

	@Override
	public String toString()
	{
		return String.format(
				"%s (obj: %s, t: %d, %s)",
				getEventType(),
				itsObject,
				itsTypeId,
				toString0());
	}

}
