/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.evdb1.test;

import static org.junit.Assert.fail;


import org.junit.Test;
import org.junit.runner.JUnitCore;

import tod.core.database.TimestampGenerator;
import tod.impl.database.IBidiIterator;
import tod.impl.evdb1.Fixtures1;
import tod.impl.evdb1.db.HierarchicalIndex;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.db.StdIndexSet;
import tod.impl.evdb1.db.RoleIndexSet.RoleTuple;

public class TestHierarchicalIndex
{
	@Test 
	public void testIndex()
	{
		fillCheck(new StdIndexTester(), 10000000);
		fillCheck(new RoleIndexTester(), 10000000);
	}
	
	private <T extends StdIndexSet.StdTuple> void fillCheck(IndexTester<T> aTester, long aTupleCount)
	{
		HierarchicalIndex<T> theIndex = aTester.createIndex();
		aTester.fillIndex(theIndex, new TimestampGenerator(0), aTupleCount);
		aTester.checkIndex(theIndex, new TimestampGenerator(0), aTupleCount);
	}
	
	@Test 
	public void testSeek()
	{
		testSeek(new StdIndexTester());
		testSeek(new RoleIndexTester());
	}
	
	private <T extends StdIndexSet.StdTuple> void testSeek(IndexTester<T> aTester)
	{
		HierarchicalIndex<T> theIndex = aTester.createIndex();
		aTester.fillIndex(theIndex, new TimestampGenerator(0), 1000000);
		
		aTester.seekAndCheck(theIndex, new TimestampGenerator(0), 10000, 100);
		aTester.seekAndCheck(theIndex, new TimestampGenerator(0), 10000, 1000);
		aTester.seekAndCheck(theIndex, new TimestampGenerator(0), 10000, 10000);
		aTester.seekAndCheck(theIndex, new TimestampGenerator(0), 10000, 100000);
		aTester.seekAndCheck(theIndex, new TimestampGenerator(0), 10000, 500000);
		aTester.seekAndCheck(theIndex, new TimestampGenerator(0), 10000, 900000);
	}
	
	private abstract static class IndexTester<T extends StdIndexSet.StdTuple>
	{
		public abstract HierarchicalIndex<T> createIndex();
		
		public abstract void fillIndex(
				HierarchicalIndex<T> aIndex,
				TimestampGenerator aGenerator,
				long aTupleCount);
		
		/**
		 * Checks the values of an index filled with {@link Fixtures1#fillIndex(HierarchicalIndex, tod.impl.evdb1.test.TestHierarchicalIndex.TimestampGenerator, long)}
		 */
		public void checkIndex(
				HierarchicalIndex<T> aIndex, 
				TimestampGenerator aGenerator,
				long aTupleCount)
		{
			checkTuples(aIndex.getTupleIterator(0), aGenerator, aTupleCount, true);
		}
		
		public void seekAndCheck(
				HierarchicalIndex<T> aIndex, 
				TimestampGenerator aGenerator,
				long aTupleCount,
				long aSkippedTuples)
		{
			long theTimestamp = 0;
			for (long i=0;i<aSkippedTuples-1;i++) theTimestamp = aGenerator.next();
			
			// Check when timestamp is equal
			IBidiIterator<T> theIterator = aIndex.getTupleIterator(theTimestamp);
			T theTuple = theIterator.next();
			checkTuple(theTuple, theTimestamp);
			
			// Check when timestamp is a bit more
			theIterator = aIndex.getTupleIterator(theTimestamp+5);
			theTimestamp = aGenerator.next();
			theTuple = theIterator.next();
			checkTuple(theTuple, theTimestamp);
			
			checkTuples(theIterator, aGenerator, aTupleCount, false);
		}
		
		/**
		 * Checks that the tuple values obtained from an iterator match thoses of the given 
		 * generator.
		 * @param aExhaust If true, this method ensures that the tuple stream
		 * finishes when the specified tuple count is read.
		 * 
		 */
		private void checkTuples(
				IBidiIterator<T> aIterator, 
				TimestampGenerator aGenerator,
				long aTupleCount,
				boolean aExhaust)
		{
			for (long i=0;i<aTupleCount;i++)
			{
				long theTimestamp = aGenerator.next();
				
				if (! aIterator.hasNext()) fail("No more tuples");
				T theTuple = aIterator.next();
				checkTuple(theTuple, theTimestamp);
				
				if (i % 1000000 == 0) System.out.println("v: "+i);
			}
			
			if (aExhaust && aIterator.hasNext()) fail("Too many tuple");
		}
		
		protected void checkTuple(T aTuple, long aTimestamp)
		{
			long theData = Fixtures1.inventData(aTimestamp);
			
			if (aTuple.getKey() != aTimestamp) fail("Timestamp mismatch");
			if (aTuple.getEventPointer() != theData) fail("Data mismatch");
		}
		
	}
	
	private static class StdIndexTester extends IndexTester<StdIndexSet.StdTuple>
	{
		@Override
		public HierarchicalIndex<tod.impl.evdb1.db.StdIndexSet.StdTuple> createIndex()
		{
			return Fixtures1.createStdIndex();
		}

		@Override
		public void fillIndex(HierarchicalIndex<tod.impl.evdb1.db.StdIndexSet.StdTuple> aIndex, TimestampGenerator aGenerator, long aTupleCount)
		{
			Fixtures1.fillStdIndex(aIndex, aGenerator, aTupleCount);
		}
	}
	
	private static class RoleIndexTester extends IndexTester<RoleIndexSet.RoleTuple>
	{
		@Override
		public HierarchicalIndex<RoleTuple> createIndex()
		{
			return Fixtures1.createRoleIndex();
		}

		@Override
		public void fillIndex(HierarchicalIndex<RoleTuple> aIndex, TimestampGenerator aGenerator, long aTupleCount)
		{
			Fixtures1.fillRoleIndex(aIndex, aGenerator, aTupleCount);
		}

		@Override
		protected void checkTuple(RoleTuple aTuple, long aTimestamp)
		{
			super.checkTuple(aTuple, aTimestamp);
			if (aTuple.getRole() != Fixtures1.inventRole(aTimestamp)) fail("Role mismatch");
		}
	}
	
	
	public static void main(String[] args)
	{
		JUnitCore.runClasses(TestHierarchicalIndex.class);
	}
}
