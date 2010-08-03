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
package tod.impl.evdbng.db.file;

import tod.core.DebugFlags;
import tod.impl.evdbng.db.file.Page.PageIOStream;

/**
 * A {@link StaticBTree} of role tuples.
 * @author gpothier
 */
public class RoleTree extends StaticBTree<RoleTuple>
{
//	private AddTask itsCurrentTask = new AddTask();

	public RoleTree(String aName, PagedFile aFile)
	{
		super(aName, aFile);
	}
	
	public RoleTree(String aName, PagedFile aFile, PageIOStream aStream)
	{
		super(aName, aFile, aStream);
	}

	@Override
	protected TupleBufferFactory<RoleTuple> getTupleBufferFactory()
	{
		return TupleBufferFactory.ROLE;
	}

	/**
	 * Adds a tuple to this tree. The tuple consists in an event id (event index) and a role.
	 */
	public void add(long aEventId, byte aRole)
	{
		if (DebugFlags.DB_LOG_DIR != null) logLeafTuple(aEventId, "("+aRole+")");
		
		PageIOStream theStream = addLeafKey(aEventId);
		theStream.writeByte(aRole);
	}
	
//	@Override
//	public void writeTo(PageIOStream aStream)
//	{
//		// Flush buffered tuples before writing out this tree
//		if (! itsCurrentTask.isEmpty()) DBExecutor.getInstance().submitAndWait(itsCurrentTask);
//		
//		super.writeTo(aStream);
//	}
//	
//	/**
//	 * Same as {@link #add(long, byte)} but uses the {@link DBExecutor}.
//	 */
//	public void addAsync(long aEventId, byte aRole)
//	{
//		itsCurrentTask.addTuple(aEventId, aRole);
//		if (itsCurrentTask.isFull()) 
//		{
//			DBExecutor.getInstance().submit(itsCurrentTask);
//			itsCurrentTask = new AddTask();
//		}
//	}
//
//	private class AddTask extends DBTask
//	{
//		private final long[] itsEventIds = new long[DebuggerGridConfigNG.DB_TASK_SIZE];
//		private final byte[] itsRoles = new byte[DebuggerGridConfigNG.DB_TASK_SIZE];
//		private int itsPosition = 0;
//		
//		public void addTuple(long aEventId, byte aRole)
//		{
//			itsEventIds[itsPosition] = aEventId;
//			itsRoles[itsPosition] = aRole;
//			itsPosition++;
//		}
//		
//		public boolean isEmpty()
//		{
//			return itsPosition == 0;
//		}
//		
//		public boolean isFull()
//		{
//			return itsPosition == itsEventIds.length;
//		}
//		
//		@Override
//		public void run()
//		{
//			for (int i=0;i<itsPosition;i++) add(itsEventIds[i], itsRoles[i]);
//		}
//
//		@Override
//		public int getGroup()
//		{
//			return RoleTree.this.hashCode();
//		}
//	}
}
