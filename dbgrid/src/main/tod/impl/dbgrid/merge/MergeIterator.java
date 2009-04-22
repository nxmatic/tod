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
package tod.impl.dbgrid.merge;

import tod.impl.database.AbstractBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.ITupleIterator;
import zz.utils.ITask;
import zz.utils.Utils;

/**
 * Base class for merge iterators. Merge iterators merge the elements
 * provided by a number of source iterators, in ascending key order, where
 * the key is a long. The source iterators must also provide their elements
 * in ascending key order.
 * This abstract class maintains an array of head items,
 * one for each source iterator.
 * @author gpothier
 */
public abstract class MergeIterator<T> extends AbstractBidiIterator<T>
{
	private final IBidiIterator<T>[] itsIterators;
	
	private final ITask<Integer, T> PREV_HEAD = new ITask<Integer, T>()
					{
						public T run(Integer aIndex)
						{
							return peekPreviousHead(aIndex);
						}
					};
					
	private final ITask<Integer, T> NEXT_HEAD = new ITask<Integer, T>()
					{
						public T run(Integer aIndex)
						{
							return peekNextHead(aIndex);
						}
					};
					
	private final Integer[] INDEXES;

	public MergeIterator(IBidiIterator<T>[] aIterators)
	{
		itsIterators = aIterators;
		INDEXES = new Integer[getHeadCount()];
		for(int i=0;i<getHeadCount();i++) INDEXES[i] = i;
	}
	
	/**
	 * Returns the number of heads (base iterators) of this merge
	 * iterator.
	 */
	protected int getHeadCount()
	{
		return itsIterators.length;
	}

	/**
	 * Moves the specified head to the next element.
	 * @return True if it was possible to move, false otherwise.
	 */
	protected boolean moveNext(int aHeadIndex)
	{
		IBidiIterator<T> theIterator = itsIterators[aHeadIndex];
		if (theIterator.hasNext())
		{
			theIterator.next();
			return true;
		}
		else return false;
	}
	
	/**
	 * Moves the specified head to the next element whose timestamp is at least
	 * the specified minimum timestamp.
	 */
	protected void moveForward(int aHeadIndex, long aMinKey)
	{
		IBidiIterator<T> theIterator = itsIterators[aHeadIndex];
		boolean theMustAdvance = true;
		
		if (theIterator instanceof ITupleIterator)
		{
			ITupleIterator theTupleIterator = (ITupleIterator) theIterator;
			long theLastKey = theTupleIterator.getLastKey();
			
			if (aMinKey > theLastKey)
			{
				theIterator = theTupleIterator.iteratorNextKey(aMinKey);
				itsIterators[aHeadIndex] = theIterator;
				theMustAdvance = false;
			}
		}
		
		if (theMustAdvance && theIterator.hasNext()) theIterator.next();
		while (theIterator.hasNext())
		{
			T theTuple = theIterator.peekNext();
			if (getKey(theTuple) >= aMinKey) break;
			theIterator.next();
		}
	}
	
	/**
	 * Moves the specified head to the previous element whose timestamp is at most
	 * the specified maximum timestamp.
	 */
	protected void moveBackward(int aHeadIndex, long aMaxKey)
	{
		IBidiIterator<T> theIterator = itsIterators[aHeadIndex];
		boolean theMustAdvance = true;

		if (theIterator instanceof ITupleIterator)
		{
			ITupleIterator theTupleIterator = (ITupleIterator) theIterator;
			long theFirstKey = theTupleIterator.getFirstKey();
			
			if (aMaxKey < theFirstKey)
			{
				theIterator = theTupleIterator.iteratorNextKey(aMaxKey);
				itsIterators[aHeadIndex] = theIterator;
				theMustAdvance = false;
				
				while (theIterator.hasNext())
				{
					T theTuple = theIterator.next();
					if (getKey(theTuple) > aMaxKey) break;
				}
			}
		}
		
		if (theMustAdvance && theIterator.hasPrevious()) theIterator.previous();
		while (theIterator.hasPrevious())
		{
			T theTuple = theIterator.peekPrevious();
			if (getKey(theTuple) <= aMaxKey) break;
			theIterator.previous();
		}
	}
	
	/**
	 * Moves the specified head to the previous element.
	 * @return True if it was possible to move, false otherwise.
	 */
	protected boolean movePrevious(int aHeadIndex)
	{
		IBidiIterator<T> theIterator = itsIterators[aHeadIndex];
		if (theIterator.hasPrevious())
		{
			theIterator.previous();
			return true;
		}
		else return false;
	}
	
	/**
	 * Returns the key of the specified tuple.
	 */
	protected abstract long getKey(T aItem);
	
	/**
	 * Indicates if the specified items represent the same event.
	 */
	protected abstract boolean sameEvent(T aItem1, T aItem2);
	
	protected boolean sameKey(T aItem1, T aItem2)
	{
		return getKey(aItem1) == getKey(aItem2);
	}
	
	/**
	 * Indicates if the specified items are the same.
	 * This is potentially more restrictive than {@link #sameEvent(Object, Object)},
	 * for example for role tuples the role equality is checked.
	 */
	protected boolean sameItem(T aItem1, T aItem2)
	{
		return aItem1.equals(aItem2);
	}

	protected T peekNextHead(int aHead)
	{
		return itsIterators[aHead].peekNext();
	}
	
	protected T[] peekNextHeads(T[] aBuffer)
	{
		return fork(aBuffer, NEXT_HEAD);
	}

	protected T peekPreviousHead(int aHead)
	{
		return itsIterators[aHead].peekPrevious();
	}
	
	protected T[] peekPreviousHeads(T[] aBuffer)
	{
		return fork(aBuffer, PREV_HEAD);
	}

	/**
	 * Forks a given task to all heads. The tasks are executed in parallel
	 * iff {@link #parallelFetch()} returns true.
	 * @param aTask The task to fork, which receives a head index as input.
	 * @param aBuffer A buffer to use to store the results, used if possible.
	 * @return The result of each task, in the same order as the heads.
	 */
	protected T[] fork(T[] aBuffer, ITask<Integer, T> aTask)
	{
		if (parallelFetch())
		{
			return Utils.fork(INDEXES, aTask);
		}
		else
		{
			for(int i=0;i<getHeadCount();i++) aBuffer[i] = aTask.run(INDEXES[i]);
			return aBuffer;
		}
	}
	
	/**
	 * Whether heads should be fetched in parallel.
	 */
	protected boolean parallelFetch()
	{
		return false;
	}
	
	/**
	 * Abstracts the direction of navigation primitives
	 * @author gpothier
	 */
	protected abstract class Nav
	{
		private Nav()
		{
		}
		
		public abstract T[] peekHeads(T[] aBuffer);
		public abstract T peekHead(int aHeadIndex);
		public abstract boolean move(int aHeadIndex);
		public abstract void move(int aHeadIndex, long aKey);
	}
	
	protected final Nav FORWARD = new Nav()
	{
		@Override
		public void move(int aHeadIndex, long aKey)
		{
			moveForward(aHeadIndex, aKey);
		}

		@Override
		public boolean move(int aHeadIndex)
		{
			return moveNext(aHeadIndex);
		}

		@Override
		public T peekHead(int aHeadIndex)
		{
			return peekNextHead(aHeadIndex);
		}

		@Override
		public T[] peekHeads(T[] aBuffer)
		{
			return peekNextHeads(aBuffer);
		}
	};
	
	protected final Nav BACKWARD = new Nav()
	{			
		@Override
		public void move(int aHeadIndex, long aKey)
		{
			moveBackward(aHeadIndex, aKey);
		}

		@Override
		public boolean move(int aHeadIndex)
		{
			return movePrevious(aHeadIndex);
		}

		@Override
		public T peekHead(int aHeadIndex)
		{
			return peekPreviousHead(aHeadIndex);
		}

		@Override
		public T[] peekHeads(T[] aBuffer)
		{
			return peekPreviousHeads(aBuffer);
		}
	};
	

}
