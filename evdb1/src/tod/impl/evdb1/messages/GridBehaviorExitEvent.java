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
import tod.impl.common.event.BehaviorExitEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.DebuggerGridConfig1;
import tod.impl.evdb1.SplittedConditionHandler;
import tod.impl.evdb1.db.Indexes;
import tod.impl.evdb1.db.RoleIndexSet;
import zz.utils.bit.BitStruct;

public class GridBehaviorExitEvent extends BitGridEvent
{
	private static final long serialVersionUID = -5809462388785867681L;
	
	private boolean itsHasThrown;
	private Object itsResult;
	
	/**
	 * We need to specify the behavior id for indexing.
	 * This will be the called behavior if the executed behavior is not
	 * available.
	 */
	private int itsBehaviorId;

	
	
	public GridBehaviorExitEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}

	public GridBehaviorExitEvent(
			IStructureDatabase aStructureDatabase,
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			boolean aHasThrown, 
			Object aResult, 
			int aBehaviorId)
	{
		super(aStructureDatabase);
		set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp, aHasThrown, aResult, aBehaviorId);
	}

	public GridBehaviorExitEvent(IStructureDatabase aStructureDatabase, BitStruct aBitStruct)
	{
		super(aStructureDatabase, aBitStruct);
		itsBehaviorId = aBitStruct.readInt(DebuggerGridConfig1.EVENT_BEHAVIOR_BITS);
		itsHasThrown = aBitStruct.readBoolean();
		itsResult = readObject(aBitStruct);
	}

	public void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp,
			boolean aHasThrown, 
			Object aResult, 
			int aBehaviorId)
	{
		super.set(aThread, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aParentTimestamp);
		itsHasThrown = aHasThrown;
		itsResult = aResult;
		itsBehaviorId = aBehaviorId;
	}
	
	@Override
	public void writeTo(BitStruct aBitStruct)
	{
		super.writeTo(aBitStruct);
		aBitStruct.writeInt(getBehaviorId(), DebuggerGridConfig1.EVENT_BEHAVIOR_BITS);
		aBitStruct.writeBoolean(hasThrown());
		writeObject(aBitStruct, getResult());
	}
	
	@Override
	public int getBitCount()
	{
		int theCount = super.getBitCount();
		
		theCount += DebuggerGridConfig1.EVENT_BEHAVIOR_BITS;
		theCount += 1;
		theCount += getObjectBits(getResult());
		
		return theCount;
	}

	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		BehaviorExitEvent theEvent = new BehaviorExitEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setHasThrown(hasThrown());
		theEvent.setResult(getResult());
		return theEvent;
	}
	
	@Override
	public MessageType getEventType()
	{
		return MessageType.BEHAVIOR_EXIT;
	}

	public int getBehaviorId()
	{
		return itsBehaviorId;
	}

	public boolean hasThrown()
	{
		return itsHasThrown;
	}

	public Object getResult()
	{
		return itsResult;
	}
	
	private static RoleIndexSet.RoleTuple TUPLE = new RoleIndexSet.RoleTuple(-1, -1, -1);
	
	@Override
	public void index(Indexes aIndexes, long aPointer)
	{
		super.index(aIndexes, aPointer);
		
		TUPLE.set(getTimestamp(), aPointer, RoleIndexSet.ROLE_BEHAVIOR_EXIT);
		aIndexes.indexBehavior(
				getBehaviorId(), 
				TUPLE);
		
		TUPLE.set(getTimestamp(), aPointer, RoleIndexSet.ROLE_OBJECT_RESULT);
		aIndexes.indexObject(
				getResult(), 
				TUPLE);
	}

	@Override
	public boolean matchBehaviorCondition(int aBehaviorId, byte aRole)
	{
		return (aRole == RoleIndexSet.ROLE_BEHAVIOR_EXIT || aRole == RoleIndexSet.ROLE_BEHAVIOR_ANY)
			&& (aBehaviorId == getBehaviorId());
	}
	
	@Override
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return ((aRole == RoleIndexSet.ROLE_OBJECT_RESULT || aRole == RoleIndexSet.ROLE_OBJECT_ANY)
					&& SplittedConditionHandler.OBJECTS.match(
							aPart, 
							aPartialKey, 
							getObjectId(getResult(), false)));
	}

	@Override
	public boolean isExit()
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		return String.format(
				"%s (b: %d, ht: %b, r: %s, %s)",
				getEventType(),
				itsBehaviorId,
				itsHasThrown,
				itsResult,
				toString0());
	}

}
