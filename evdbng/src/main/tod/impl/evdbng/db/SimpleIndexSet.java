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
package tod.impl.evdbng.db;

import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.DBExecutor.DBTask;
import tod.impl.evdbng.db.file.BTree;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.SimpleTree;
import tod.impl.evdbng.db.file.SimpleTuple;
import tod.impl.evdbng.db.file.Page.PageIOStream;

public class SimpleIndexSet extends IndexSet<SimpleTuple> 
{
	private AddTask itsCurrentTask = new AddTask();

	public SimpleIndexSet(
			IndexManager aIndexManager, 
			String aName, 
			PagedFile aFile)
	{
		super(aIndexManager, aName, aFile);
	}

	@Override
	public BTree<SimpleTuple> createIndex(int aIndex)
	{
		return new SimpleTree(getName()+"-"+aIndex, getFile());
	}

	@Override
	public BTree<SimpleTuple> loadIndex(int aIndex, PageIOStream aStream)
	{
		return new SimpleTree(getName()+"-"+aIndex, getFile(), aStream);
	}
	
	@Override
	public SimpleTree getIndex(int aIndex)
	{
		return (SimpleTree) super.getIndex(aIndex);
	}
	
	public void add(int aIndex, long aKey)
	{
		itsCurrentTask.addTuple(aIndex, aKey);
		if (itsCurrentTask.isFull()) flushTasks(); 
	}
	
	private void add0(int aIndex, long aKey)
	{
		getIndex(aIndex).add(aKey);
	}
	
	/**
	 * Flushes currently pending (see {@link #addAsync(long)}).
	 */
	public void flushTasks()
	{
		DBExecutor.getInstance().submit(itsCurrentTask);
		itsCurrentTask = new AddTask();
	}


	
	private class AddTask extends DBTask
	{
		private final int[] itsIndexes = new int[DebuggerGridConfigNG.DB_TASK_SIZE];
		private final long[] itsKeys = new long[DebuggerGridConfigNG.DB_TASK_SIZE];
		private int itsPosition = 0;
		
		public void addTuple(int aIndex, long aKey)
		{
			itsIndexes[itsPosition] = aIndex;
			itsKeys[itsPosition] = aKey;
			itsPosition++;
		}
		
		public boolean isEmpty()
		{
			return itsPosition == 0;
		}
		
		public boolean isFull()
		{
			return itsPosition == itsKeys.length;
		}
		
		@Override
		public void run()
		{
			for (int i=0;i<itsPosition;i++) add0(itsIndexes[i], itsKeys[i]);
		}

		@Override
		public int getGroup()
		{
			return getId();
		}
	}

}
