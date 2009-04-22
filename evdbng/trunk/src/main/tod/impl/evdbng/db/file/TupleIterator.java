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

import tod.impl.database.AbstractBidiIterator;
import tod.impl.dbgrid.ITupleIterator;
import tod.impl.evdbng.db.file.PagedFile.Page;


/**
 * A tuple iterator reads {@link Tuple}s from a linked list of
 * {@link Page}s. Tuples are decoded with a user-specified
 * {@link TupleCodec}.
 * A page contains a sequence of tuples of fixed length. If S is the
 * size of the page in bits and P is the size of a page pointer in bits,
 * the space available for tuples is S-2P. The last 2P bits of the 
 * page are reserved for previous and next page pointers, in this order.
 * Page pointers are encoded so that a value of 0 means null pointer,
 * and any positive value is the actual page id plus one.
 * @author gpothier
 */
public class TupleIterator<T extends Tuple> extends AbstractBidiIterator<T>
implements ITupleIterator<T>
{
	private final BTree<T> itsTree;
	
	/**
	 * The level of the iterated pages in the {@link BTree}.
	 * For now it seems we only iterate on leaves, so the level is 0.
	 */
	private int itsLevel = 0;
	
	/**
	 * The first tuple of the current page
	 */
	private long itsFirstKey;
	
	/**
	 * The last tuple of the current page
	 */
	private long itsLastKey;
	
	private TupleBuffer<T> itsCurrentBuffer;
	
	/**
	 * The current position of this iterator within its
	 * current tuple buffer.
	 */
	private int itsPosition;
	
	/**
	 * Creates an exhausted iterator.
	 */
	public TupleIterator(BTree<T> aTree)
	{
		super (true);
		itsTree = aTree;
		itsCurrentBuffer = null;
	}

	public TupleIterator(
			BTree<T> aTree, 
			TupleBuffer<T> aTupleBuffer,
			int aPosition)
	{
		super (false);
		assert aTupleBuffer != null;
		itsTree = aTree;
		itsCurrentBuffer = aTupleBuffer;
		itsPosition = aPosition;
		readBounds();
	}
	
	/**
	 * Reads first and last tuple of the current page.
	 */
	private void readBounds()
	{
		if (itsCurrentBuffer.getSize() == 0)
		{
			itsFirstKey = itsLastKey = -1;
		}
		else
		{
			itsFirstKey = itsCurrentBuffer.getKey(0);
			itsLastKey = itsCurrentBuffer.getKey(itsCurrentBuffer.getSize()-1);
		}
	}
	
	public long getFirstKey()
	{
		return itsFirstKey;
	}

	public long getLastKey()
	{
		return itsLastKey;
	}

	public TupleIterator<T> iteratorNextKey(long aKey)
	{
		return itsTree.getTupleIterator(aKey);
	}
	
	@Override
	protected T fetchNext()
	{
		boolean theHasNext = true;
		
		if (itsPosition >= itsCurrentBuffer.getSize())
		{
			assert itsPosition == itsCurrentBuffer.getSize();
			
			// We reached the end of the page
			int thePointer = itsCurrentBuffer.getNextPageId();
			
			if (thePointer != 0)
			{
				itsCurrentBuffer = itsTree.getPageTupleBuffer(thePointer, itsLevel);
				itsPosition = 0;
				readBounds();
			}
			else
			{
				theHasNext = false;
			}
		}

		return theHasNext ? itsCurrentBuffer.getTuple(itsPosition++) : null;
	}

	@Override
	protected T fetchPrevious()
	{
		boolean theHasPrevious = true;
		
		if (itsPosition == 0)
		{
			// We reached the beginning of the page
			
			int thePointer = itsCurrentBuffer.getPreviousPageId();
			
			if (thePointer != 0)
			{
				itsCurrentBuffer = itsTree.getPageTupleBuffer(thePointer, itsLevel);
				itsPosition = itsCurrentBuffer.getSize();
				readBounds();
			}
			else
			{
				theHasPrevious = false;
			}
		}

		return theHasPrevious ? itsCurrentBuffer.getTuple(--itsPosition) : null;
	}

	/**
	 * Returns the index (or position) of the next current tuple in its level.
	 * (for now works only for level 0).
	 * If there is no next tuple, returns -1.
	 */
	public long getNextTupleIndex()
	{
		if (itsCurrentBuffer == null) return -1;
		long theTupleCount = itsCurrentBuffer.getTupleCount();
		assert theTupleCount >= 0 : "Information not available";
		
		return theTupleCount+itsPosition-getOffset();
	}
	
	/**
	 * Returns the index (or position) of the previous current tuple in its level.
	 * (for now works only for level 0)
	 */
	public long getPreviousTupleIndex()
	{
		return getNextTupleIndex()-1;
	}
	
	
}