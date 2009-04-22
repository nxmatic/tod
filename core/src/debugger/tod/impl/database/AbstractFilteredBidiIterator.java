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

import zz.utils.AbstractFilteredIterator;

/**
 * Reimplementation of {@link AbstractFilteredIterator} for {@link IBidiIterator}s
 * @author gpothier
 */
public abstract class AbstractFilteredBidiIterator<I, O> extends AbstractBidiIterator<O>
{
	protected static final Object REJECT = new Object();
	
	private IBidiIterator<I> itsIterator;
	
	public AbstractFilteredBidiIterator(IBidiIterator<I> aIterator)
	{
		itsIterator = aIterator;
	}
	
	protected abstract Object transform(I aInput);

	@Override
	protected O fetchNext()
	{
		while (itsIterator.hasNext())
		{
			I theInput = itsIterator.next();
			Object theOutput = transform(theInput);
			if (theOutput != REJECT) return (O) theOutput;
		}
		
		return null;
	}

	@Override
	protected O fetchPrevious()
	{
		while (itsIterator.hasPrevious())
		{
			I theInput = itsIterator.previous();
			Object theOutput = transform(theInput);
			if (theOutput != REJECT) return (O) theOutput;
		}
		
		return null;
	}
	
	
}
