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

import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.evdbng.DebuggerGridConfigNG;
import tod.impl.evdbng.db.DBExecutor.DBTask;
import tod.impl.evdbng.db.file.BTree;
import tod.impl.evdbng.db.file.PagedFile;
import tod.impl.evdbng.db.file.RoleTree;
import tod.impl.evdbng.db.file.RoleTuple;
import tod.impl.evdbng.db.file.Page.PageIOStream;

/**
 * An index set where index tuples have associated roles
 * @author gpothier
 */
public class RoleIndexSet extends IndexSet<RoleTuple>
{
	/**
	 * Represents any of the behavior roles.
	 */
	public static final byte ROLE_BEHAVIOR_ANY = 0;
	
	/**
	 * Represents either {@link #ROLE_BEHAVIOR_CALLED} or {@link #ROLE_BEHAVIOR_EXECUTED}.
	 */
	public static final byte ROLE_BEHAVIOR_ANY_ENTER = 1;
	
	public static final byte ROLE_BEHAVIOR_CALLED = 2;
	public static final byte ROLE_BEHAVIOR_EXECUTED = 3;
	public static final byte ROLE_BEHAVIOR_EXIT = 4;
	public static final byte ROLE_BEHAVIOR_OPERATION = 5;
	
	
	/**
	 * Roles are negative unless when it deals with arguments. In this case, 
	 * role value is the argument position (starting a 1).
	 */
	public static final byte ROLE_OBJECT_TARGET = -1;
	public static final byte ROLE_OBJECT_VALUE = -2;
	public static final byte ROLE_OBJECT_RESULT = -3;
	public static final byte ROLE_OBJECT_EXCEPTION = -4;
	public static final byte ROLE_OBJECT_ANYARG = -5;
	public static final byte ROLE_OBJECT_ANY = -6;
	
	private AddTask itsCurrentTask = new AddTask();

	public RoleIndexSet(
			IndexManager aIndexManager, 
			String aName, 
			PagedFile aFile)
	{
		super(aIndexManager, aName, aFile);
	}
	
	@Override
	public BTree<RoleTuple> createIndex(int aIndex)
	{
		return new RoleTree(getName()+"-"+aIndex, getFile());
	}

	@Override
	public BTree<RoleTuple> loadIndex(int aIndex, PageIOStream aStream)
	{
		return new RoleTree(getName()+"-"+aIndex, getFile(), aStream);
	}
	
	@Override
	public RoleTree getIndex(int aIndex)
	{
		return (RoleTree) super.getIndex(aIndex);
	}

	public void add(int aIndex, long aKey, byte aRole)
	{
		itsCurrentTask.addTuple(aIndex, aKey, aRole);
		if (itsCurrentTask.isFull()) flushTasks(); 
	}
	
	private void add0(int aIndex, long aKey, byte aRole)
	{
		getIndex(aIndex).add(aKey, aRole);
	}

	/**
	 * Flushes currently pending (see {@link #addAsync(long)}).
	 */
	public void flushTasks()
	{
		DBExecutor.getInstance().submit(itsCurrentTask);
		itsCurrentTask = new AddTask();
	}


	/**
	 * Creates an iterator that filters out the tuples from a source iterator that
	 * don't have one of the specified roles.
	 * See also: {@link #createFilteredIterator(IBidiIterator)}
	 */
	public static IBidiIterator<RoleTuple> createFilteredIterator(
			IBidiIterator<RoleTuple> aIterator,
			final byte... aRole)
	{
		return new AbstractFilteredBidiIterator<RoleTuple, RoleTuple>(aIterator)
		{
			@Override
			protected Object transform(RoleTuple aIn)
			{
				int theRole = aIn.getRole();
				for (byte theAllowedRole : aRole)
				{
					if (theRole == theAllowedRole) return aIn;
				}
				return REJECT;
			}
		};
	}
	
	private class AddTask extends DBTask
	{
		private final int[] itsIndexes = new int[DebuggerGridConfigNG.DB_TASK_SIZE];
		private final long[] itsKeys = new long[DebuggerGridConfigNG.DB_TASK_SIZE];
		private final byte[] itsRoles = new byte[DebuggerGridConfigNG.DB_TASK_SIZE];
		private int itsPosition = 0;
		
		public void addTuple(int aIndex, long aKey, byte aRole)
		{
			itsIndexes[itsPosition] = aIndex;
			itsKeys[itsPosition] = aKey;
			itsRoles[itsPosition] = aRole;
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
			for (int i=0;i<itsPosition;i++) add0(itsIndexes[i], itsKeys[i], itsRoles[i]);
		}

		@Override
		public int getGroup()
		{
			return getId();
		}
	}

}
