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
package tod.impl.evdb1.db.file;

import zz.utils.bit.BitStruct;


/**
 * Base class for all index tuples. Only contains the 
 * indexed field (timestamp in the case of events).
 */
public class IndexTuple extends Tuple
{
	private long itsKey;

	public IndexTuple(long aKey)
	{
		itsKey = aKey;
	}
	
	public IndexTuple(BitStruct aBitStruct)
	{
		this(aBitStruct.readLong(64));
	}

	@Override
	public void writeTo(BitStruct aBitStruct)
	{
		aBitStruct.writeLong(getKey(), 64);
	}
	
	@Override
	public int getBitCount()
	{
		return super.getBitCount() + 64;
	}
	
	protected void set(long aKey)
	{
		itsKey = aKey;
	}

	public long getKey()
	{
		return itsKey;
	}

	@Override
	public String toString()
	{
		return String.format("%s: k=%d",
				getClass().getSimpleName(),
				getKey());
	}
}