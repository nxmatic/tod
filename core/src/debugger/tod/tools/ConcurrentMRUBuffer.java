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
package tod.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import zz.utils.ArrayStack;
import zz.utils.cache.MRUBuffer;
import zz.utils.cache.SyncMRUBuffer;
import zz.utils.list.NakedLinkedList.Entry;

/**
 * A concurrent variation of {@link MRUBuffer} that provides a high throughput
 * in presence of concurrent accesses.
 * This is unlike {@link SyncMRUBuffer}, that preserves the exact semantics of {@link MRUBuffer}
 * but can be a bottleneck if several threads do access the buffer at the same time.
 * 
 * TODO: This is a work in progress, only a few methods are supported.
 * @author gpothier
 *
 * @param <K>
 * @param <V>
 */
public abstract class ConcurrentMRUBuffer<K, V> extends MRUBuffer<K, V>
{
	private final int itsQueueSize;
	
	private final ReentrantReadWriteLock itsLock = new ReentrantReadWriteLock();
	private final List<Stacks<V>> itsStacks = new ArrayList<Stacks<V>>();
	private final ThreadLocal<Stacks<V>> itsLocalStacks = new ThreadLocal<Stacks<V>>()
	{
		@Override
		protected Stacks<V> initialValue()
		{
			Stacks<V> theStacks = new Stacks<V>(itsQueueSize);
			synchronized(itsStacks)
			{
				itsStacks.add(theStacks);
			}
			
			return theStacks;
		}
	};
	
	public ConcurrentMRUBuffer(int aCacheSize, boolean aUseMap, int aQueueSize)
	{
		super(aCacheSize, aUseMap);
		assert aCacheSize > aQueueSize*8 : String.format("cache: %d, queue: %d", aCacheSize, aQueueSize);
		itsQueueSize = aQueueSize;
	}

	public ConcurrentMRUBuffer(int aCacheSize, int aQueueSize)
	{
		super(aCacheSize);
		assert aCacheSize > aQueueSize*8;
		itsQueueSize = aQueueSize;
	}
	
	@Override
	public final Entry<V> add(V aValue)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public final void drop(K aKey)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void dropAll()
	{
		try
		{
			itsLock.writeLock().lock();
			commit();
			
			super.dropAll();
		}
		finally
		{
			itsLock.writeLock().unlock();
		}
	}

	@Override
	public Entry<V> getEntry(K aKey, boolean aFetch)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void use(Entry<V> aEntry)
	{
		Stacks<V> theStacks = itsLocalStacks.get();
		
		try
		{
			itsLock.readLock().lock();
			if (theStacks.useStack.isEmpty() || theStacks.useStack.peek() != aEntry)
				theStacks.useStack.push(aEntry);
		}
		finally
		{
			itsLock.readLock().unlock();
		}
		
		if (theStacks.useStack.isFull()) commit(theStacks);
	}

	private void commit(Stacks<V> aStacks)
	{
		try
		{
			itsLock.writeLock().lock();
			while(! aStacks.useStack.isEmpty()) 
			{
				Entry<V> theEntry = aStacks.useStack.pop();
				if (isStillValid(theEntry)) use0(theEntry);
			}
		}
		finally
		{
			itsLock.writeLock().unlock();
		}
	}
	
	/**
	 * Indicates if the given entry still pertains to the buffer.
	 * This is needed to handle cases where a client calls {@link #use(Entry)} and
	 * the entry gets removed before the use stack is committed.
	 */
	protected abstract boolean isStillValid(Entry<V> aEntry);
	
	protected void use0(Entry<V> aEntry)
	{
		super.use(aEntry);
	}
	
	private void commit()
	{
		for (Stacks<V> theStacks : itsStacks) commit(theStacks);
	}
	
	private static class Stacks<V>
	{
		public final ArrayStack<Entry<V>> useStack;
		
		private Stacks(int aQueueSize)
		{
			useStack = new ArrayStack<Entry<V>>(aQueueSize);
		}
	}

}
