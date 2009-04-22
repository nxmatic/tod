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

import tod.impl.database.IBidiIterator;
import tod.tools.monitoring.Monitored;
import zz.utils.ITask;

/**
 * A disjunction (boolean OR) merge iterator.
 * @author gpothier
 */
public abstract class DisjunctionIterator<T> extends MergeIterator<T>
{
	public DisjunctionIterator(IBidiIterator<T>[] aIterators)
	{
		super(aIterators);
	}
	
	
	@Override
	@Monitored
	protected T fetchNext()
	{
		T[] theBuffer = (T[]) new Object[getHeadCount()];
		T theMinTimestampItem = null;
		long theMinTimestamp = Long.MAX_VALUE;

		T[] theHeads = peekNextHeads(theBuffer);
		
		// Find the item with the minimum timestamp
		for (int i = 0; i < getHeadCount(); i++)
		{
			T theItem = theHeads[i];

			if (theItem != null)
			{
				long theTimestamp = getKey(theItem);
				if (theTimestamp < theMinTimestamp)
				{
					theMinTimestamp = theTimestamp;
					theMinTimestampItem = theItem;
				}
			}
		}
		
		if (theMinTimestampItem == null) return null;

		// Move all heads that point to the same event
		final T theMinTimestampItem0 = theMinTimestampItem;
		fork(theBuffer, new ITask<Integer, T>()
				{
					public T run(Integer aIndex)
					{
						T theItem = peekNextHead(aIndex);

						if (theItem != null && sameEvent(theMinTimestampItem0, theItem))
						{
							moveNext(aIndex);
						}
						return null;
					}
				});

		return theMinTimestampItem;
	}

	@Override
	protected T fetchPrevious()
	{
		T[] theBuffer = (T[]) new Object[getHeadCount()];
		T theMaxTimestampItem = null;
		long theMaxTimestamp = -1;

		T[] theHeads = peekPreviousHeads(theBuffer);
		
		// Find the item with the maximum timestamp
		for (int i = 0; i < getHeadCount(); i++)
		{
			T theItem = theHeads[i];

			if (theItem != null)
			{
				long theTimestamp = getKey(theItem);
				if (theTimestamp > theMaxTimestamp)
				{
					theMaxTimestamp = theTimestamp;
					theMaxTimestampItem = theItem;
				}
			}
		}
		
		if (theMaxTimestampItem == null) return null;

		// Move all heads that point to the same event
		final T theMaxTimestampItem0 = theMaxTimestampItem;
		fork(theBuffer, new ITask<Integer, T>()
				{
					public T run(Integer aIndex)
					{
						T theItem = peekPreviousHead(aIndex);

						if (theItem != null && sameEvent(theMaxTimestampItem0, theItem))
						{
							movePrevious(aIndex);
						}
						return null;
					}
				});

		return theMaxTimestampItem;
	}
}
