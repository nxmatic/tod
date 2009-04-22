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

import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdb1.db.EventList;
import tod.impl.evdb1.db.Indexes;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;

/**
 * Represents a condition on behavior id and corresponding role.
 * @author gpothier
 */
public class BehaviorCondition extends SimpleCondition
{
	private static final long serialVersionUID = -9029772284148605574L;
	private int itsBehaviorId;
	private byte itsRole;
	
	public BehaviorCondition(int aBehaviorId, byte aRole)
	{
		itsBehaviorId = aBehaviorId;
		itsRole = aRole;
	}
	
	@Override
	public IBidiIterator<StdTuple> createTupleIterator(EventList aEventList, Indexes aIndexes, long aTimestamp)
	{
		IBidiIterator<RoleIndexSet.RoleTuple> theTupleIterator = 
			aIndexes.getBehaviorIndex(itsBehaviorId).getTupleIterator(aTimestamp);
		
		switch (itsRole)
		{
		case RoleIndexSet.ROLE_BEHAVIOR_ANY:
			theTupleIterator = RoleIndexSet.createFilteredIterator(theTupleIterator);
			break;
			
		case RoleIndexSet.ROLE_BEHAVIOR_ANY_ENTER:
			theTupleIterator = RoleIndexSet.createFilteredIterator(
					theTupleIterator, 
					RoleIndexSet.ROLE_BEHAVIOR_CALLED, 
					RoleIndexSet.ROLE_BEHAVIOR_EXECUTED);
			break;
			
		default:
			theTupleIterator = RoleIndexSet.createFilteredIterator(
					theTupleIterator,
					itsRole);
		}
		
		return (IBidiIterator) theTupleIterator;
	}
	
	public boolean _match(GridEvent aEvent)
	{
		return ((itsRole == RoleIndexSet.ROLE_BEHAVIOR_ANY || itsRole == RoleIndexSet.ROLE_BEHAVIOR_OPERATION) 
						&& aEvent.getProbeInfo().behaviorId == itsBehaviorId)
				|| aEvent.matchBehaviorCondition(itsBehaviorId, itsRole);
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("BehaviorId = %d (role %d)", itsBehaviorId, itsRole);
	}
}
