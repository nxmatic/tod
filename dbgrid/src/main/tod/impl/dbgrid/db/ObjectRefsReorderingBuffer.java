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
package tod.impl.dbgrid.db;

import tod.impl.dbgrid.DebuggerGridConfig;

/**
 * A buffer that permits to reoder slightly out-of-order objects.
 * 
 * @see ObjectsDatabase
 * @author gpothier
 */
public class ObjectRefsReorderingBuffer extends ReorderingBuffer<ObjectRefsReorderingBuffer.Entry>
{
	public ObjectRefsReorderingBuffer()
	{
		super(DebuggerGridConfig.DB_OBJECTS_BUFFER_SIZE);
	}
	
	public static class Entry extends ReorderingBuffer.Entry
	{
		public final long classId;

		public Entry(long aId, long aTimestamp, long aClassId)
		{
			super(aId, aTimestamp);
			classId = aClassId;
		}
	}
}
