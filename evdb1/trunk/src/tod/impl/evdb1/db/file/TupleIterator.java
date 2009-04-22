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

import tod.impl.database.AbstractBidiIterator;
import tod.impl.dbgrid.ITupleIterator;
import tod.impl.evdb1.db.HierarchicalIndex;
import tod.impl.evdb1.db.file.PageBank.Page;
import tod.impl.evdb1.db.file.PageBank.PageBitStruct;


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
public class TupleIterator<T extends IndexTuple> extends AbstractBidiIterator<T>
implements ITupleIterator<T>
{
	private final HierarchicalIndex itsIndex;
	private PageBank itsBank;
	private TupleCodec<T> itsTupleCodec;
	
	private PageBitStruct itsStruct;
	
	/**
	 * The first tuple of the current page
	 */
	private T itsFirstTuple;
	
	/**
	 * The last tuple of the current page
	 */
	private T itsLastTuple;
	
	/**
	 * Creates an exhausted iterator.
	 */
	public TupleIterator(HierarchicalIndex aIndex)
	{
		super (true);
		itsIndex = aIndex;
	}

	public TupleIterator(HierarchicalIndex aIndex, PageBank aBank, TupleCodec<T> aTupleCodec, PageBitStruct aStruct)
	{
		super (false);
		itsIndex = aIndex;
		itsBank = aBank;
		itsTupleCodec = aTupleCodec;
		itsStruct = aStruct;
		readBounds();
	}
	
	private int getPagePointerSize()
	{
		return itsBank.getPagePointerSize();
	}
	
	private int getTupleSize()
	{
		return itsTupleCodec.getTupleSize();
	}
	
	/**
	 * Reads first and last tuple of the current page.
	 */
	private void readBounds()
	{
		int thePos = itsStruct.getPos();
		
		itsStruct.setPos(0);
		itsFirstTuple = itsTupleCodec.read(itsStruct);

		int theTupleBits = itsStruct.getTotalBits()-2*getPagePointerSize();
		int theTupleCount = theTupleBits/getTupleSize();
		itsStruct.setPos((theTupleCount-1)*getTupleSize());

		itsLastTuple = itsTupleCodec.read(itsStruct);
		
		itsStruct.setPos(thePos);		
	}
	
	public T getFirstTuple()
	{
		return itsFirstTuple;
	}

	public T getLastTuple()
	{
		return itsLastTuple;
	}
	
	public long getFirstKey() 
	{
		return getFirstTuple().getKey();
	}

	public long getLastKey() 
	{
		return getLastTuple().getKey();
	}

	public TupleIterator<T> iteratorNextKey(long aKey)
	{
		return itsIndex.getTupleIterator(aKey);
	}
	
	@Override
	protected T fetchNext()
	{
		boolean itsHasNext = true;
		
		if (itsStruct.getRemainingBits() - 2*getPagePointerSize() < getTupleSize())
		{
			// We reached the end of the page, we must read the next-page
			// pointer
			int thePos = itsStruct.getPos();
			
			itsStruct.setPos(itsStruct.getTotalBits()-getPagePointerSize());
			long thePointer = itsStruct.readLong(getPagePointerSize());
			
			if (thePointer != 0)
			{
				itsStruct = itsBank.get(thePointer - 1).asBitStruct();
				readBounds();
			}
			else
			{
				itsStruct.setPos(thePos);
				itsHasNext = false;
			}
		}

		if (itsHasNext)
		{
			T theTuple = itsTupleCodec.read(itsStruct);
			if (itsTupleCodec.isNull(theTuple))
			{
				itsStruct.setPos(itsStruct.getPos()-getTupleSize());
				return null;
			}
			else
			{
				return theTuple;
			}
		}
		else return null;
	}

	@Override
	protected T fetchPrevious()
	{
		boolean theHasPrevious = true;
		
		if (itsStruct.getPos() == 0)
		{
			// We reached the beginning of the page, we must read the previous-page
			// pointer
			
			// Max. number of bits available for tuples, also address of prev. page pointer
			int theTupleBits = itsStruct.getTotalBits()-2*getPagePointerSize();
			itsStruct.setPos(theTupleBits);
			
			long thePointer = itsStruct.readLong(getPagePointerSize());
			
			if (thePointer != 0)
			{
				itsStruct = itsBank.get(thePointer - 1).asBitStruct();
				itsStruct.setPos(theTupleBits - theTupleBits % getTupleSize());
				readBounds();
			}
			else
			{
				itsStruct.setPos(0);
				theHasPrevious = false;
			}
		}

		if (theHasPrevious) 
		{
			itsStruct.setPos(itsStruct.getPos()-getTupleSize());
			T theTuple = itsTupleCodec.read(itsStruct);
			if (itsTupleCodec.isNull(theTuple))
			{
				return null;
			}
			else
			{
				itsStruct.setPos(itsStruct.getPos()-getTupleSize());
				return theTuple;
			}
		}
		else return null;
	}

}