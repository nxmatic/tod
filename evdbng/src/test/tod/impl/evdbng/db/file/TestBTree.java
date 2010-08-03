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
package tod.impl.evdbng.db.file;

import java.io.File;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import tod.impl.evdbng.db.RoleIndexSet;

public class TestBTree
{
	
	private static final int NavigationNumber = 10;
	private static final int TestNumber = 60000;
	private static final long E = 0x1fffffffffL;
	private static final int D = 0x1fffff;
	private static final int C = 0x1fff;
	private static final int B = 0x7f;
	private static final int A = 1;
	
//	private static final long E =  100000;
//	private static final int D = 10000;
//	private static final int C = 1000;
//	private static final int B = 150;
//	private static final int A = 1;
	
	Random itsRandom = new Random(12327);
	
	/**
	 * test the timestampTree implementation
	 */
	@Test
	public void testSequenceTree() 
	{
		//first key must be 0.
		int theFirstIndex=1000, theLastIndex= TestNumber;
		
		testKeyMaker(theLastIndex);
		
		File theFile = new File("test.bin");
		SequenceTree theTimestampTree = new SequenceTree("test", PagedFile.create(theFile, false));
		
		for (int i = theFirstIndex; i<=theLastIndex ; i++)
		{
			theTimestampTree.add(getKeyForIndex(i));
			if (i % 10000000 == 0) System.out.println("adding - "+i);
		}
		
		testGetEventID(theTimestampTree, theFirstIndex, theLastIndex);
		
		for (int theI = 0; theI < NavigationNumber; theI++)
		{
			System.out.println("testing - "+theI);
			long theStartTimestamp = getKeyForIndex(randomIndex(theFirstIndex, theLastIndex));
			testTupleBackward(theTimestampTree, theStartTimestamp);
			testTupleForward(theTimestampTree, theStartTimestamp);
		}
		theFile.delete();
		
	}
	
	/**
	 * test the timestampTree implementation
	 */
	@Test
	public void testSimpleTree() 
	{
		//first key must be 0.
		int theFirstIndex=1000, theLastIndex= TestNumber;
		
		testKeyMaker(theLastIndex);
		
		File theFile = new File("test.bin");
		SimpleTree theSimpleTree = new SimpleTree("test", PagedFile.create(theFile, false));
		
		for (int i = theFirstIndex; i<=theLastIndex ; i++)
		{
			theSimpleTree.add(getKeyForIndex(i));
			if (i % 10000000 == 0) System.out.println("adding - "+i);
		}	
		
		for (int theI = 0; theI < NavigationNumber; theI++)
		{
			System.out.println("testing - "+theI);
			long theStartTimestamp = getKeyForIndex(itsRandom.nextInt(theLastIndex)) ;
			testTupleBackward(theSimpleTree, theStartTimestamp);
			testTupleForward(theSimpleTree, theStartTimestamp);
		}
		theFile.delete();
		
	}
	
	@Test
	public void testRoleTree() 
	{
		//first key must be 0.
		int theFirstIndex=0, theLastIndex= TestNumber;
		
		testKeyMaker(theLastIndex);
		
		File theFile = new File("test.bin");
		RoleTree theRoleTree = new RoleTree("test", PagedFile.create(theFile, false));
		
		for (int i = theFirstIndex; i<=theLastIndex ; i++)
		{
			theRoleTree.add(getKeyForIndex(i), getRoleForIndex(i));
			if (i % 10000000 == 0) System.out.println("adding - "+i);
		}	
		
		for (int theI = 0; theI < NavigationNumber; theI++)
		{
			System.out.println("testing - "+theI);
			long theStartTimestamp = getKeyForIndex(itsRandom.nextInt(theLastIndex)) ;
			testRoleTupleBackward(theRoleTree, theStartTimestamp);
			testRoleTupleForward(theRoleTree, theStartTimestamp);
		}
		theFile.delete();
		
	}
	
	
	
	private void testKeyMaker(int aLastIndex){
		for (int theI = 0; theI < 20; theI++)
		{
			long thestartIndex = itsRandom.nextInt(aLastIndex) ;
			Assert.assertTrue(getIndexForKey(getKeyForIndex(thestartIndex))==thestartIndex);
		}
	}
	
	
	private long getIndexForKey(long aKey)
	{
		//check with different interval types
		// 1 0x7f 0x1fff   0x1fffff   0x1fffffffffL
		long theIndex = aKey/(A + B + C + D + E) *5;
		switch((int)((aKey)%(A + B + C + D + E))) 
		{
			case A: theIndex += 1; break;
			case A + B: theIndex += 2 ; break;
			case A + B + C: theIndex += 3; break;
			case A + B + C + D: theIndex += 4; break; 
		}
		return theIndex;
	}
	
	private long getKeyForIndex(long aIndex)
	{
		long theKey=( A + B + C + D + E) *  (aIndex/5);
		switch((int)(aIndex%5))
		{
			case 1: theKey += A; break;
			case 2: theKey += A + B; break;
			case 3: theKey += A + B + C; break;
			case 4: theKey += A + B + C + D; break;
		}
		return    theKey;
	}
	
	/**
	 * return a role for a certain index 
	 * all RoleIndexSet roles are checked
	 * @param aKey
	 * @return
	 */
	private byte getRoleForIndex(long aIndex)
	{
		switch((int)(aIndex%12)) 
		{
			case 0: return RoleIndexSet.ROLE_BEHAVIOR_ANY;
			case 1: return RoleIndexSet.ROLE_BEHAVIOR_ANY_ENTER;
			case 2: return RoleIndexSet.ROLE_BEHAVIOR_CALLED;
			case 3: return RoleIndexSet.ROLE_BEHAVIOR_EXECUTED;
			case 4: return RoleIndexSet.ROLE_BEHAVIOR_EXIT;
			case 5: return RoleIndexSet.ROLE_BEHAVIOR_OPERATION;
			case 6: return RoleIndexSet.ROLE_OBJECT_ANY;
			case 7: return RoleIndexSet.ROLE_OBJECT_ANYARG;
			case 8: return RoleIndexSet.ROLE_OBJECT_EXCEPTION;
			case 9: return RoleIndexSet.ROLE_OBJECT_RESULT;
			case 10: return RoleIndexSet.ROLE_OBJECT_TARGET;
			case 11: return RoleIndexSet.ROLE_OBJECT_VALUE;
		}
		return RoleIndexSet.ROLE_BEHAVIOR_ANY;
	}
	
	
	private int randomIndex(int aFirstIndex, int aLastIndex)
	{
		return itsRandom.nextInt(aLastIndex-aFirstIndex) + aFirstIndex;
	}
	
	/**
	 * keys should have been inserted sequentially
	 * @param aTimestampTree
	 * @param aFirstTimestamp
	 * @param aLastTimestamp
	 */
	private void testGetEventID(SequenceTree aTimestampTree, int aFirstIndex, int aLastIndex)
	{
		Assert.assertTrue(aTimestampTree.getTuplePosition(getKeyForIndex(aFirstIndex), null) == 0);
		Assert.assertTrue(aTimestampTree.getTuplePosition(getKeyForIndex(aFirstIndex+7), null) == 7);
		
		Assert.assertTrue(aTimestampTree.getTuplePosition(getKeyForIndex(aLastIndex), null) == aLastIndex-aFirstIndex);
		int i = 0;
		while (i++<10000){
			long theTest = randomIndex(aFirstIndex, aLastIndex);
			//if (i < 1607) continue;
			Assert.assertTrue(""+i, aTimestampTree.getTuplePosition(getKeyForIndex(theTest), null)==theTest-aFirstIndex);
		}
	}
	
	
	private void testTupleForward(StaticBTree aBTree, long aStartKey)
	{
		TupleIterator<SimpleTuple> theIterator = aBTree.getTupleIterator(aStartKey);
		long theKey = aStartKey; 
		while(theIterator.hasNext()){
			Tuple theTuple = theIterator.next();
			Assert.assertTrue(theTuple.getKey()==theKey);
			theKey= getKeyForIndex(getIndexForKey(theKey)+1);
		}
		
	}
	
	private void testTupleBackward(StaticBTree aBTree, long aStartKey)
	{
		TupleIterator<SimpleTuple> theIterator = aBTree.getTupleIterator(aStartKey);
		long theKey = aStartKey; 
		while(theIterator.hasPrevious()){
			long theIndex = getIndexForKey(theKey)-1;
			theKey= getKeyForIndex(theIndex);
			Tuple theTuple = theIterator.previous();
			Assert.assertTrue(theTuple.getKey()==theKey);
		}
		
	}
	
	private void testRoleTupleForward(RoleTree aBTree, long aStartKey)
	{
		TupleIterator<RoleTuple> theIterator = aBTree.getTupleIterator(aStartKey);
		long theKey = aStartKey; 
		while(theIterator.hasNext()){
			long theIndex = getIndexForKey(theKey);
			RoleTuple theTuple = theIterator.next();
			Assert.assertTrue(theTuple.getKey()==theKey);
			Assert.assertTrue(theTuple.getRole()==getRoleForIndex(theIndex));
			theKey= getKeyForIndex(theIndex+1);
		}
		
	}
	
	private void testRoleTupleBackward(RoleTree aBTree, long aStartKey)
	{
		TupleIterator<RoleTuple> theIterator = aBTree.getTupleIterator(aStartKey);
		long theKey = aStartKey; 
		while(theIterator.hasPrevious()){
			long theIndex = getIndexForKey(theKey)-1;
			theKey= getKeyForIndex(theIndex);
			RoleTuple theTuple = theIterator.previous();
			Assert.assertTrue(theTuple.getKey()==theKey);
			Assert.assertTrue(theTuple.getRole()==getRoleForIndex(theIndex));
		}
		
	}
	
	
	
}
