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
package tod.impl.dbgrid;

import tod.impl.database.IBidiIterator;

/**
 * An extension of {@link IBidiIterator} for tuple iterators.
 * Supports getting the available bounds of the iterator, and quickly obtaining
 * an iterator for another point in the tuple set.
 * @author gpothier
 */
public interface ITupleIterator<T> extends IBidiIterator<T>
{
	/**
	 * Returns the first key that is available from this iterator 
	 * from the current page.
	 */
	public long getFirstKey();

	/**
	 * Returns the last key that is available from this iterator 
	 * from the current page.
	 */
	public long getLastKey();

	/**
	 * Returns an iterator that is positioned
	 * so that the key of the next returned tuple
	 * is the first that is greater or equal to the specified key
	 */
	public ITupleIterator<T> iteratorNextKey(long aKey);
}
