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
package tod.impl.evdb1;

import static tod.impl.evdb1.DebuggerGridConfig1.DB_EVENTID_INDEX_BITS;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_EVENTID_NODE_BITS;
import static tod.impl.evdb1.DebuggerGridConfig1.DB_EVENTID_PAGE_BITS;
import zz.utils.bit.BitUtils;

/**
 * Expanded representation of an simple event pointer,
 * comprised only of the node id and the event rank.
 * @author gpothier
 */
public class SimplePointer
{
	private static final long INDEX_MASK = 
		BitUtils.pow2(DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS)-1;
	
	private static final long NODE_MASK = 
		(BitUtils.pow2(DB_EVENTID_NODE_BITS)-1) << (DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS);

	
	private long itsIndex;
	private int itsNode;
	
	public SimplePointer(long aIndex, int aNode)
	{
		set(aIndex, aNode);
	}
	
	public SimplePointer(long aPointer)
	{
		read(aPointer, this);
	}

	public long getIndex()
	{
		return itsIndex;
	}

	public int getNode()
	{
		return itsNode;
	}

	public void set(long aIndex, int aNode)
	{
		itsIndex = aIndex;
		itsNode = aNode;
	}

	public static long create(long aIndex, int aNode)
	{
		long theNode = ((long) aNode) << (DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS);
		long theIndex = aIndex;
		
		if ((theNode & ~NODE_MASK) != 0) throw new RuntimeException("Node overflow");
		if ((theIndex & ~INDEX_MASK) != 0) throw new RuntimeException("Record index overflow: "+aIndex);
		
		return theNode | theIndex;
	}
	
	/**
	 * Returns the event corresponding to the specified internal pointer.
	 */
	public static void read(long aPointer, SimplePointer aBuffer)
	{
		long theNode = (aPointer & NODE_MASK) >>> (DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS);
		long theIndex = aPointer & INDEX_MASK;

		aBuffer.set(theIndex, (int) theNode);
	}
	
}
