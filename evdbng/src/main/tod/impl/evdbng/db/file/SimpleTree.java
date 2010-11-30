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
import tod.impl.evdbng.db.Stats.Account;
import tod.impl.evdbng.db.file.Page.PageIOStream;

/**
 * A {@link StaticBTree} of simple tuples (no extra data).
 * @author gpothier
 */
public class SimpleTree extends StaticBTree<SimpleTuple>
{
//	private AddTask itsCurrentTask = new AddTask();

	public SimpleTree(String aName, Account aAccount, PagedFile aFile)
	{
		super(aName, aAccount, aFile);
	}

	public SimpleTree(String aName, Account aAccount, PagedFile aFile, PageIOStream aStream)
	{
		super(aName, aAccount, aFile, aStream);
	}

	@Override
	protected TupleBufferFactory<SimpleTuple> getTupleBufferFactory()
	{
		return TupleBufferFactory.SIMPLE;
	}

	/**
	 * Adds a tuple to this tree. The tuple consists only in a event id (event index).
	 */
	public void add(long aEventId)
	{
		if (DebugFlags.DB_LOG_DIR != null) logLeafTuple(aEventId, null);
		addLeafKey(aEventId);
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
//	 * Same as {@link #add(long)} but uses the {@link DBExecutor}.
//	 */
//	public void addAsync(long aEventId)
//	{
//		itsCurrentTask.addTuple(aEventId);
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
//		private int itsPosition = 0;
//		
//		public void addTuple(long aEventId)
//		{
//			itsEventIds[itsPosition] = aEventId;
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
//			for (int i=0;i<itsPosition;i++) add(itsEventIds[i]);
//		}
//
//		@Override
//		public int getGroup()
//		{
//			return SimpleTree.this.hashCode();
//		}
//	}
}
