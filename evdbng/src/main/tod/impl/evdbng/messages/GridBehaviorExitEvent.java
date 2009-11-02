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
import tod.impl.common.event.BehaviorExitEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdbng.SplittedConditionHandler;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.db.file.Page.PageIOStream;

public class GridBehaviorExitEvent extends GridEventNG
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

	public GridBehaviorExitEvent(IStructureDatabase aStructureDatabase, PageIOStream aStream)
	{
		super(aStructureDatabase, aStream);
		itsBehaviorId = aStream.readBehaviorId();
		itsHasThrown = aStream.readBoolean();
		itsResult = readObject(aStream);
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
	public void writeTo(PageIOStream aStream)
	{
		super.writeTo(aStream);
		aStream.writeBehaviorId(getBehaviorId());
		aStream.writeBoolean(hasThrown());
		writeObject(aStream, getResult());
	}
	
	@Override
	public int getMessageSize()
	{
		int theCount = super.getMessageSize();
		
		theCount += PageIOStream.behaviorIdSize();
		theCount += PageIOStream.booleanSize();
		theCount += getObjectSize(getResult());
		
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
	
	@Override
	public void index(Indexes aIndexes, int aId)
	{
		super.index(aIndexes, aId);
		
		aIndexes.indexBehavior(getBehaviorId(), aId, RoleIndexSet.ROLE_BEHAVIOR_EXIT);
		aIndexes.indexObject(getResult(), aId, RoleIndexSet.ROLE_OBJECT_RESULT); 
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
