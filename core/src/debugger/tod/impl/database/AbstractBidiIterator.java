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
package tod.impl.database;

import java.util.NoSuchElementException;



public abstract class AbstractBidiIterator<T> implements IBidiIterator<T>
{
	private boolean itsNextReady;
	private T itsNext;
	
	private boolean itsPreviousReady;
	private T itsPrevious;
	
	private int itsOffset;
	
	public AbstractBidiIterator()
	{
		this(false);
	}
	
	
	/**
	 * Creates an new iterator.
	 * @param aExhausted if true, the iterator is created exhausted and cannot 
	 * return any element
	 */
	public AbstractBidiIterator(boolean aExhausted)
	{
		itsNext = itsPrevious = null;
		itsNextReady = itsPreviousReady = aExhausted;
		itsOffset = 0;
	}
	
	protected void reset()
	{
		itsNext = itsPrevious = null;
		itsNextReady = itsPreviousReady = false;
		itsOffset = 0;		
	}
	
	/**
	 * Fetches the next element and moves the low-level internal pointer
	 * of the iterator. The offset between the low-level internal pointer
	 * and the logical pointer of the iterator is stored in {@link #itsOffset}.
	 * @return Null if no next element is available
	 */
	protected abstract T fetchNext();
	
	
	/**
	 * Fetches the previous element. Symmetric of {@link #fetchNext()}.
	 */
	protected abstract T fetchPrevious();
	
	/**
	 * Reads the next tuple
	 */
	private void readNext()
	{
		if (itsNextReady)
		{
			itsPrevious = itsNext;
			itsPreviousReady = true;
		}
		else itsPreviousReady = false;
		
		itsNext = fetchNext();
		
		itsNextReady = true;
	}

	/**
	 * Reads the previous tuple
	 */
	private void readPrevious()
	{
		if (itsPreviousReady)
		{
			itsNext = itsPrevious;
			itsNextReady = true;
		}
		else itsNextReady = false;
		
		itsPrevious = fetchPrevious();
		
		itsPreviousReady = true;
	}
	
	public boolean hasNext()
	{
		return peekNext() != null;
	}

	public int getOffset()
	{
		return itsOffset;
	}
	
	public T peekNext()
	{
		if (itsOffset == -1) 
		{
			readNext();
			itsNextReady = false;
			itsOffset = 0;
		}
		
		if (! itsNextReady) 
		{
			readNext();
			if (itsNext != null) itsOffset = 1;
		}
		
		return itsNext;
	}

	public T next()
	{
		if (itsOffset == -1) 
		{
			readNext();
			itsNextReady = false;
		}
		itsOffset = 0;
		
		if (! itsNextReady) readNext();
		if (itsNext == null) 
		{
			throw new NoSuchElementException();
		}
		
		T theResult = itsNext;
		itsNextReady = false;
		
		return theResult;
	}

	public boolean hasPrevious()
	{
		return peekPrevious() != null;
	}

	public T peekPrevious()
	{
		if (itsOffset == 1) 
		{
			readPrevious();
			itsPreviousReady = false;
			itsOffset = 0;
		}
		
		if (! itsPreviousReady) 
		{
			readPrevious();
			if (itsPrevious != null) itsOffset = -1;
		}
		
		return itsPrevious;
	}
	
	public T previous()
	{
		if (itsOffset == 1) 
		{
			readPrevious();
			itsPreviousReady = false;
		}
		itsOffset = 0;
		
		if (! itsPreviousReady) readPrevious();
		if (itsPrevious == null) throw new NoSuchElementException();
		
		T theResult = itsPrevious;
		itsPreviousReady = false;
		
		return theResult;
	}
}
