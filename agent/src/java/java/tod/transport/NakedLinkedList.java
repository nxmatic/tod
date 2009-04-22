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
package java.tod.transport;

import java.util.NoSuchElementException;

/**
 * Copy of {@link zz.utils.list.NakedLinkedList} (we don't want to depend on zz.utils here).
 * @author gpothier
 */
public class NakedLinkedList<E>
{
	private int itsSize = 0;
	private Entry<E> itsRoot;
	
	
	public NakedLinkedList()
	{
		itsRoot = new Entry<E>(null);
		itsRoot.setPrev(itsRoot);
		itsRoot.setNext(itsRoot);
	}
	
	/**
	 * Creates a new entry. Subclasses can override this method to create
	 * custom entries.
	 */
	public Entry<E> createEntry(E aElement)
	{
		return new Entry<E>(aElement);
	}
	
	public int size()
	{
		return itsSize;
	}
	
	public void addAfter(Entry<E> aBase, Entry<E> aEntry)
	{
		assert aEntry.getNext() == null;
		assert aEntry.getPrev() == null;
		
		aEntry.setPrev(aBase);
		aEntry.setNext(aBase.getNext());
		aBase.getNext().setPrev(aEntry);
		aBase.setNext(aEntry);
		
		itsSize++;
	}

	public E getLast()
	{
		return getLastEntry().getValue();
	}
	
	public Entry<E> getLastEntry()
	{
		Entry<E> theLast = itsRoot.getPrev();
		if (theLast == itsRoot) throw new NoSuchElementException();
		return theLast;
	}
	
	public void addLast(E aElement)
	{
		addLast(createEntry(aElement));
	}
	
	public void addLast(Entry<E> aEntry)
	{
		addAfter(itsRoot.getPrev(), aEntry);
	}
	
	public E getFirst()
	{
		return getFirstEntry().getValue();
	}
	
	public Entry<E> getFirstEntry()
	{
		Entry<E> theFirst = itsRoot.getNext();
		if (theFirst == itsRoot) throw new NoSuchElementException();
		return theFirst;
	}
	
	public void addFirst(E aElement)
	{
		addFirst(createEntry(aElement));
	}
	
	public void addFirst(Entry<E> aEntry)
	{
		addAfter(itsRoot, aEntry);
	}
	
	public void remove(Entry<E> aEntry)
	{
		aEntry.getPrev().setNext(aEntry.getNext());
		aEntry.getNext().setPrev(aEntry.getPrev());
		aEntry.setPrev(null);
		aEntry.setNext(null);
		itsSize--;
	}
	
	public static class Entry<E>
	{
		private Entry<E> itsNext;
		private Entry<E> itsPrev;
		private E itsValue;
		
		public Entry(E aValue)
		{
			itsValue = aValue;
		}

		Entry<E> getNext()
		{
			return itsNext;
		}
		
		void setNext(Entry<E> aNext)
		{
			itsNext = aNext;
		}
		
		Entry<E> getPrev()
		{
			return itsPrev;
		}
		
		void setPrev(Entry<E> aPrev)
		{
			itsPrev = aPrev;
		}
		
		public E getValue()
		{
			return itsValue;
		}
		
		public void setValue(E aValue)
		{
			itsValue = aValue;
		}
		
		public boolean isAttached()
		{
			assert (getNext() == null) == (getPrev() == null);
			return getNext() != null;
		}
	}
}
