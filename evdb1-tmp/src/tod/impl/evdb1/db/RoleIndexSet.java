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
package tod.impl.evdb1.db;

import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.IndexTupleCodec;
import tod.impl.evdb1.db.file.TupleCodec;
import zz.utils.bit.BitStruct;

/**
 * An index set where index tuples have associated roles
 * @author gpothier
 */
public class RoleIndexSet extends IndexSet<RoleIndexSet.RoleTuple>
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
	 * role value is the argument position.
	 */
	public static final byte ROLE_OBJECT_TARGET = -1;
	public static final byte ROLE_OBJECT_VALUE = -2;
	public static final byte ROLE_OBJECT_RESULT = -3;
	public static final byte ROLE_OBJECT_EXCEPTION = -4;
	public static final byte ROLE_OBJECT_ANYARG = -5;
	public static final byte ROLE_OBJECT_ANY = -6;
	
	public static final IndexTupleCodec<RoleTuple> TUPLE_CODEC = new RoleTupleCodec();
	
	public RoleIndexSet(
			String aName, 
			IndexManager aIndexManager,
			HardPagedFile aFile, 
			int aIndexCount)
	{
		super(aName, aIndexManager, aFile, aIndexCount);
	}
	
	@Override
	public TupleCodec<RoleTuple> getTupleCodec()
	{
		return TUPLE_CODEC;
	}

	/**
	 * Creates an iterator that filters out the tuples from a source iterator that
	 * don't have one of the specified roles.
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
				byte theRole = aIn.getRole();
				for (byte theAllowedRole : aRole)
				{
					if (theRole == theAllowedRole) return aIn;
				}
				return REJECT;
			}
		};
	}
	

	private static class RoleTupleCodec extends IndexTupleCodec<RoleTuple>
	{

		@Override
		public int getTupleSize()
		{
			return StdIndexSet.TUPLE_CODEC.getTupleSize() + 8;
		}

		@Override
		public RoleTuple read(BitStruct aBitStruct)
		{
			return new RoleTuple(aBitStruct);
		}
	}
	
	public static class RoleTuple extends StdIndexSet.StdTuple
	{
		private byte itsRole;

		public RoleTuple(long aTimestamp, long aEventPointer, int aRole)
		{
			super(aTimestamp, aEventPointer);
			if (aRole > Byte.MAX_VALUE) throw new RuntimeException("Role overflow");
			itsRole = (byte) aRole;
		}

		public RoleTuple(BitStruct aBitStruct)
		{
			super(aBitStruct);
			itsRole = (byte) aBitStruct.readInt(8);
		}
		
		public void set(long aTimestamp, long aEventPointer, int aRole)
		{
			super.set(aTimestamp, aEventPointer);
			itsRole = (byte) aRole;
		}
		
		@Override
		public void writeTo(BitStruct aBitStruct)
		{
			super.writeTo(aBitStruct);
			aBitStruct.writeInt(getRole(), 8);
		}
		
		@Override
		public int getBitCount()
		{
			return super.getBitCount() + 8;
		}
		
		public byte getRole()
		{
			return itsRole;
		}
		
		@Override
		public boolean equals(Object aObj)
		{
			if (aObj instanceof RoleTuple)
			{
				RoleTuple theOther = (RoleTuple) aObj;
				return theOther.getEventPointer() == getEventPointer()
						&& theOther.getRole() == getRole();
			}
			else if (aObj instanceof StdTuple)
			{
				StdTuple theOther = (StdTuple) aObj;
				return theOther.getEventPointer() == getEventPointer();
			}
			else return false;
		}

	}
}
