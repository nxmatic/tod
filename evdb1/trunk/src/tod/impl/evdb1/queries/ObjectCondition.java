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
package tod.impl.evdb1.queries;

import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdb1.SplittedConditionHandler;
import tod.impl.evdb1.db.EventList;
import tod.impl.evdb1.db.HierarchicalIndex;
import tod.impl.evdb1.db.Indexes;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.db.RoleIndexSet.RoleTuple;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;

/**
 * Represents a condition on an object, with a corresponding role.
 * @author gpothier
 */
public class ObjectCondition extends SimpleCondition
{
	private static final long serialVersionUID = 4506201457044007004L;
	
	/**
	 * Part of the key handled by this condition.
	 * @see SplittedConditionHandler 
	 */
	private int itsPart;
	private int itsObjectId;
	private byte itsRole;
	
	public ObjectCondition(int aPart, int aObjectId, byte aRole)
	{
		itsPart = aPart;
		itsObjectId = aObjectId;
		itsRole = aRole;
	}

	@Override
	public IBidiIterator<StdTuple> createTupleIterator(EventList aEventList, Indexes aIndexes, long aTimestamp)
	{
		HierarchicalIndex<RoleTuple> theIndex = 
			aIndexes.getObjectIndex(itsPart, itsObjectId);
		
		IBidiIterator<RoleIndexSet.RoleTuple> theTupleIterator = 
			theIndex.getTupleIterator(aTimestamp);
		
		if (itsRole == RoleIndexSet.ROLE_OBJECT_ANY)
		{
			theTupleIterator = new AbstractFilteredBidiIterator<RoleTuple, RoleTuple>(theTupleIterator)
			{
				@Override
				protected Object transform(RoleTuple aIn)
				{
					return aIn;
				}
			};
		}
		else if (itsRole == RoleIndexSet.ROLE_OBJECT_ANYARG)
		{
			theTupleIterator = new AbstractFilteredBidiIterator<RoleTuple, RoleTuple>(theTupleIterator)
			{
				@Override
				protected Object transform(RoleTuple aIn)
				{
					return aIn.getRole() >= 0 ? aIn : REJECT;
				}
			};
		}
		else
		{
			theTupleIterator = RoleIndexSet.createFilteredIterator(theTupleIterator, itsRole);
		}
		
		return (IBidiIterator) theTupleIterator;
	}

	public boolean _match(GridEvent aEvent)
	{
		return aEvent.matchObjectCondition(itsPart, itsObjectId, itsRole);
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("ObjectId/%d = %d (role %d)", itsPart, itsObjectId, itsRole);
	}

}
