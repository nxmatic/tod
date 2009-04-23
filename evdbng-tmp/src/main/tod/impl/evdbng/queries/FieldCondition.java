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
package tod.impl.evdbng.queries;


import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdbng.db.IEventList;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.file.SimpleTuple;

/**
 * Represents a condition on a Field write event's field.
 * @author gpothier
 */
public class FieldCondition extends SimpleCondition<SimpleTuple>
{
	private static final long serialVersionUID = -8850366553462947973L;
	private int itsFieldId;

	public FieldCondition(int aFieldId)
	{
		itsFieldId = aFieldId;
	}

	@Override
	public IBidiIterator<SimpleTuple> createTupleIterator(IEventList aEventList, Indexes aIndexes, long aEventId)
	{
		return aIndexes.getFieldIndex(itsFieldId).getTupleIterator(aEventId);
	}

	@Override
	public boolean _match(GridEvent aEvent)
	{
		return aEvent.matchFieldCondition(itsFieldId);
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("FieldId = %d", itsFieldId);
	}

}
