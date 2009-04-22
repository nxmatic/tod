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


import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import tod.impl.database.BufferedBidiIterator;
import tod.impl.database.IBidiIterator;

public class TestBufferedIterator
{
	@Test public void testEndToEnd()
	{
		BaseIterator theBaseIterator = new BaseIterator(10, 20, 13);
		BufferedIterator theIterator = new BufferedIterator(theBaseIterator, 5);
		
		int theCurrent = 13;
		while(theIterator.hasNext())
		{
			Integer theNext = theIterator.next();
			Assert.assertEquals(theNext.intValue(), theCurrent);
			theCurrent++;
		}
		
		while(theIterator.hasPrevious())
		{
			Integer thePrevious = theIterator.previous();
			theCurrent--;
			Assert.assertEquals(thePrevious.intValue(), theCurrent);
		}
		
		while(theIterator.hasNext())
		{
			Integer theNext = theIterator.next();
			Assert.assertEquals(theNext.intValue(), theCurrent);
			theCurrent++;
		}
	}
	
	@Test public void testEarlyBack()
	{
		BaseIterator theBaseIterator = new BaseIterator(5, 20, 13);
		BufferedIterator theIterator = new BufferedIterator(theBaseIterator, 5);
		
		int theCurrent = 13;
		while(theCurrent < 19)
		{
			Integer theNext = theIterator.next();
			Assert.assertEquals(theNext.intValue(), theCurrent);
			theCurrent++;
		}
		
		while(theCurrent > 5)
		{
			Integer thePrevious = theIterator.previous();
			theCurrent--;
			Assert.assertEquals(thePrevious.intValue(), theCurrent);
		}
		
		while(theIterator.hasNext())
		{
			Integer theNext = theIterator.next();
			Assert.assertEquals(theNext.intValue(), theCurrent);
			theCurrent++;
		}
	}
	
	private static class BaseIterator implements IBidiIterator<Integer>
	{
		private int itsMin;
		private int itsMax;
		private int itsCurrent;
		
		public BaseIterator(int aMin, int aMax, int aCurrent)
		{
			itsMin = aMin;
			itsMax = aMax;
			itsCurrent = aCurrent;
		}

		public boolean hasNext()
		{
			return itsCurrent <= itsMax;
		}

		public boolean hasPrevious()
		{
			return itsCurrent > itsMin;
		}

		public Integer next()
		{
			assert hasNext();
			return itsCurrent++;
		}

		public Integer peekNext()
		{
			return hasNext() ? itsCurrent : null;
		}

		public Integer peekPrevious()
		{
			return hasPrevious() ? itsCurrent-1 : null;
		}

		public Integer previous()
		{
			assert hasPrevious();
			return --itsCurrent;
		}
	}
	
	private static class BufferedIterator extends BufferedBidiIterator<Integer[], Integer>
	{
		private IBidiIterator<Integer> itsBaseIterator;
		
		private int itsBufferSize;
		
		public BufferedIterator(IBidiIterator<Integer> aBaseIterator, int aBufferSize)
		{
			itsBaseIterator = aBaseIterator;
			itsBufferSize = aBufferSize;
		}

		@Override
		protected Integer[] fetchNextBuffer()
		{
			List<Integer> theList = new ArrayList<Integer>(itsBufferSize);
			for (int i=0;i<itsBufferSize;i++)
			{
				if (itsBaseIterator.hasNext()) theList.add(itsBaseIterator.next());
				else break;
			}

			return theList.size() > 0 ?
					theList.toArray(new Integer[theList.size()])
					: null;
		}

		@Override
		protected Integer[] fetchPreviousBuffer()
		{
			List<Integer> theList = new ArrayList<Integer>(itsBufferSize);
			for (int i=0;i<itsBufferSize;i++)
			{
				if (itsBaseIterator.hasPrevious()) theList.add(itsBaseIterator.previous());
				else break;
			}

			int theSize = theList.size();
			if (theSize == 0) return null;
			
			Integer[] theResult = new Integer[theSize];
			for (int i=0;i<theSize;i++) theResult[i] = theList.get(theSize-i-1);
			
			return theResult;
		}

		@Override
		protected Integer get(Integer[] aBuffer, int aIndex)
		{
			return aBuffer[aIndex];
		}

		@Override
		protected int getSize(Integer[] aBuffer)
		{
			return aBuffer.length;
		}
	}
	
}
