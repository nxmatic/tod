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
 * Expanded representation of an internal event pointer
 * @author gpothier
 */
public class InternalPointer
{
	private static final long INDEX_MASK = 
		BitUtils.pow2(DB_EVENTID_INDEX_BITS)-1;
	
	private static final long PAGE_MASK = 
		(BitUtils.pow2(DB_EVENTID_PAGE_BITS)-1) << DB_EVENTID_INDEX_BITS;
	
	private static final long NODE_MASK = 
		(BitUtils.pow2(DB_EVENTID_NODE_BITS)-1) << (DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS);

	
	private int itsIndex;
	private long itsPage;
	private int itsNode;
	
	public InternalPointer(int aIndex, long aPage, int aNode)
	{
		set(aIndex, aPage, aNode);
	}
	
	public InternalPointer(long aPointer)
	{
		read(aPointer, this);
	}

	public int getIndex()
	{
		return itsIndex;
	}

	public int getNode()
	{
		return itsNode;
	}

	public long getPage()
	{
		return itsPage;
	}
	
	public void set(int aIndex, long aPage, int aNode)
	{
		itsIndex = aIndex;
		itsPage = aPage;
		itsNode = aNode;
	}

	public static long create(int aIndex, long aPage, int aNode)
	{
		long theNode = ((long) aNode) << (DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS);
		long thePage = aPage << DB_EVENTID_INDEX_BITS;
		long theIndex = aIndex;
		
		if ((theNode & ~NODE_MASK) != 0) throw new RuntimeException("Node overflow");
		if ((thePage & ~PAGE_MASK) != 0) throw new RuntimeException("Page Id overflow");
		if ((theIndex & ~INDEX_MASK) != 0) throw new RuntimeException("Record index overflow: "+aIndex);
		
		return theNode | thePage | theIndex;
	}
	
	/**
	 * Returns the event corresponding to the specified internal pointer.
	 */
	public static void read(long aPointer, InternalPointer aBuffer)
	{
		long theNode = (aPointer & NODE_MASK) >>> (DB_EVENTID_INDEX_BITS + DB_EVENTID_PAGE_BITS);
		long thePage = (aPointer & PAGE_MASK) >>> DB_EVENTID_INDEX_BITS;
		long theIndex = aPointer & INDEX_MASK;

		aBuffer.set((int) theIndex, thePage, (int) theNode);
	}
	
}
