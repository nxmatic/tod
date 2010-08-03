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
import tod.impl.dbgrid.db.ObjectsDatabase;
import tod.impl.evdbng.db.file.Page.PageIOStream;

/**
 * A {@link StaticBTree} of object pointers for the {@link ObjectsDatabase}.
 * @author gpothier
 */
public class ObjectPointerTree extends StaticBTree<ObjectPointerTuple>
{

	public ObjectPointerTree(String aName, PagedFile aFile, PageIOStream aStream)
	{
		super(aName, aFile, aStream);
	}

	public ObjectPointerTree(String aName, PagedFile aFile)
	{
		super(aName, aFile);
	}

	@Override
	protected TupleBufferFactory<ObjectPointerTuple> getTupleBufferFactory()
	{
		return TupleBufferFactory.OBJECT_POINTER;
	}
	
	public void add(long aObjectId, int aPageId, int aOffset)
	{
		if (DebugFlags.DB_LOG_DIR != null) logLeafTuple(aObjectId, "pid: "+aPageId+", off: "+aOffset);

		PageIOStream theStream = addLeafKey(aObjectId);
		theStream.writePagePointer(aPageId);
		theStream.writePageOffset(aOffset);
	}


	
}