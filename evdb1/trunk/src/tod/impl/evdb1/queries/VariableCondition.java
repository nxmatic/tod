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
import tod.impl.evdb1.db.StdIndexSet.StdTuple;

/**
 * Represents a condition on a variable write event's variable number
 * @author gpothier
 */
public class VariableCondition extends SimpleCondition
{
	private static final long serialVersionUID = -7171025129792888283L;
	private int itsVariableId;

	public VariableCondition(int aVariableId)
	{
		itsVariableId = aVariableId;
	}

	@Override
	public IBidiIterator<StdTuple> createTupleIterator(EventList aEventList, Indexes aIndexes, long aTimestamp)
	{
		return aIndexes.getVariableIndex(itsVariableId).getTupleIterator(aTimestamp);
	}

	public boolean _match(GridEvent aEvent)
	{
		return aEvent.matchVariableCondition(itsVariableId);
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("VariableId = %d", itsVariableId);
	}
	
}
