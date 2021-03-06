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


import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdb1.db.EventList;
import tod.impl.evdb1.db.Indexes;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;

/**
 * Represents a condition on the role of a caller-side event
 * @author gpothier
 */
public class RoleCondition extends SimpleCondition
{
	private static final long serialVersionUID = 2727420447011218824L;
	
	private BytecodeRole itsRole;


	public RoleCondition(BytecodeRole aRole)
	{
		itsRole = aRole;
	}

	@Override
	public IBidiIterator<StdTuple> createTupleIterator(EventList aEventList, Indexes aIndexes, long aTimestamp)
	{
		return aIndexes.getRoleIndex(itsRole.ordinal()+1).getTupleIterator(aTimestamp);
	}

	public boolean _match(GridEvent aEvent)
	{
		ProbeInfo theProbeInfo = aEvent.getProbeInfo();
		return theProbeInfo != null && theProbeInfo.role == itsRole;
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("Role = %s", itsRole);
	}

}
