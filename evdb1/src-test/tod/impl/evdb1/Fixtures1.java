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
package tod.impl.evdb1;


import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tod.core.database.TimestampGenerator;
import tod.core.database.event.ILogEvent;
import tod.core.transport.PacketProcessor.ILogReceiverMonitor;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.DerivativeDataPrinter;
import tod.impl.dbgrid.Fixtures;
import tod.impl.dbgrid.db.EventDatabase;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdb1.db.EventList;
import tod.impl.evdb1.db.HierarchicalIndex;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.db.StdIndexSet;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.messages.BitGridEvent;
import tod.impl.evdb1.queries.EventCondition;
import zz.utils.bit.BitStruct;
import zz.utils.bit.IntBitStruct;


public class Fixtures1
{

	public static HierarchicalIndex<StdIndexSet.StdTuple> createStdIndex() 
	{
		return createStdIndexes(1)[0];
	}

	public static HierarchicalIndex<StdIndexSet.StdTuple>[] createStdIndexes(int aCount) 
	{
		try
		{
			File theFile = new File("stdIndexTest.bin");
			theFile.delete();
			HardPagedFile thePagedFile = new HardPagedFile(theFile, DebuggerGridConfig1.DB_PAGE_SIZE);
			
			HierarchicalIndex<StdIndexSet.StdTuple>[] theIndexes = new HierarchicalIndex[aCount];
			for (int i = 0; i < theIndexes.length; i++)
			{
				theIndexes[i] = new HierarchicalIndex<StdIndexSet.StdTuple>("test", StdIndexSet.TUPLE_CODEC, thePagedFile);
			}
			
			return theIndexes;
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static HierarchicalIndex<RoleIndexSet.RoleTuple> createRoleIndex() 
	{
		return createRoleIndexes(1)[0];
	}
	
	public static HierarchicalIndex<RoleIndexSet.RoleTuple>[] createRoleIndexes(int aCount) 
	{
		try
		{
			File theFile = new File("roleIndexTest.bin");
			theFile.delete();
			HardPagedFile thePagedFile = new HardPagedFile(theFile, DebuggerGridConfig1.DB_PAGE_SIZE);
			
			HierarchicalIndex<RoleIndexSet.RoleTuple>[] theIndexes = new HierarchicalIndex[aCount];
			for (int i = 0; i < theIndexes.length; i++)
			{
				theIndexes[i] = new HierarchicalIndex<RoleIndexSet.RoleTuple>("test", RoleIndexSet.TUPLE_CODEC, thePagedFile);
			}
			
			return theIndexes;
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static EventList createEventList() 
	{
		try
		{
			File theFile = new File("eventTest.bin");
			theFile.delete();
			HardPagedFile thePagedFile = new HardPagedFile(theFile, DebuggerGridConfig1.DB_PAGE_SIZE);
			return new EventList(null, 0, thePagedFile);
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	
	public static long inventData(long aTimestamp)
	{
		return aTimestamp*7;
	}

	public static byte inventRole(long aTimestamp)
	{
		return (byte) aTimestamp;
	}
	
	/**
	 * Fills an std index with values.
	 */
	public static void fillStdIndex(
			HierarchicalIndex<StdIndexSet.StdTuple> aIndex, 
			TimestampGenerator aGenerator,
			long aTupleCount)
	{
		for (long i=0;i<aTupleCount;i++)
		{
			long theTimestamp = aGenerator.next();
			aIndex.add(new StdIndexSet.StdTuple(theTimestamp, inventData(theTimestamp)));
			
			if (i % 1000000 == 0) System.out.println("w: "+i);
		}
	}

	/**
	 * Fills an std index with values.
	 */
	public static void fillRoleIndex(
			HierarchicalIndex<RoleIndexSet.RoleTuple> aIndex, 
			TimestampGenerator aGenerator,
			long aTupleCount)
	{
		for (long i=0;i<aTupleCount;i++)
		{
			long theTimestamp = aGenerator.next();
			aIndex.add(new RoleIndexSet.RoleTuple(
					theTimestamp, 
					inventData(theTimestamp), 
					inventRole(theTimestamp)));
			
			if (i % 1000000 == 0) System.out.println("w: "+i);
		}
	}
	
	/**
	 * Fills an event list
	 */
	public static void fillEventList(
			EventList aEventList, 
			EventGenerator1 aGenerator,
			long aCount)
	{
		for (long i=0;i<aCount;i++)
		{
			GridEvent theEvent = aGenerator.next();
			aEventList.add(theEvent);
			
			if (i % 1000000 == 0) System.out.println("w: "+i);
		}
	}
	
	/**
	 * Fills an event list and returns the ids of the events
	 */
	public static long[] fillEventListReport(
			EventList aEventList, 
			EventGenerator1 aGenerator,
			int aCount)
	{
		long[] theIds = new long[aCount];
		for (int i=0;i<aCount;i++)
		{
			GridEvent theEvent = aGenerator.next();
			theIds[i] = aEventList.add(theEvent);
			
			if (i % 1000000 == 0) System.out.println("w: "+i);
		}
		
		return theIds;
	}
	

	/**
	 * @deprecated See {@link Fixtures}
	 */
	public static void assertEquals(String aMessage, ILogEvent aRefEvent, ILogEvent aEvent)
	{
		if (! aRefEvent.equals(aEvent)) fail(aMessage);
	}
	
	/**
	 * Checks that two events are equal.
	 * @deprecated See {@link Fixtures#assertEquals(String, GridEvent, GridEvent)}
	 */
	public static void assertEquals(String aMessage, BitGridEvent aRefEvent, BitGridEvent aEvent)
	{
		BitStruct theRefStruct = new IntBitStruct(1000);
		BitStruct theStruct = new IntBitStruct(1000);
		
		aRefEvent.writeTo(theRefStruct);
		aEvent.writeTo(theStruct);
		
		if (theRefStruct.getPos() != theStruct.getPos())
		{
			System.out.println("ref:  "+aRefEvent);
			System.out.println("test: "+aEvent);
			fail("Size mismatch - "+aMessage);
		}
		
		int theSize = (theRefStruct.getPos()+31)/32;
		
		theRefStruct.setPos(0);
		theStruct.setPos(0);
		
		for (int i=0;i<theSize;i++)
		{
			if (theRefStruct.readInt(32) != theStruct.readInt(32))
			{
				System.out.println("ref:  "+aRefEvent);
				System.out.println("test: "+aEvent);
				fail("Data mismatch - "+aMessage);				
			}
		}
	}
	
	public static int checkCondition(
			EventDatabase aDatabase, 
			EventCondition aCondition, 
			EventGenerator1 aReferenceGenerator,
			int aSkip,
			int aCount)
	{
		GridEvent theEvent = null;
		for (int i=0;i<aSkip;i++) theEvent = aReferenceGenerator.next();
		
		long theTimestamp = theEvent != null ? theEvent.getTimestamp()+1 : 0;
		
		IBidiIterator<GridEvent> theIterator = aDatabase.evaluate(aCondition, theTimestamp);
		return checkCondition(theIterator, aCondition, aReferenceGenerator, aCount);
	}
	
	public static int checkCondition(
			IBidiIterator<GridEvent> aIterator, 
			EventCondition aCondition,
			EventGenerator1 aReferenceGenerator,
			int aCount)
	{
		int theMatched = 0;
		for (int i=0;i<aCount;i++)
		{
			BitGridEvent theRefEvent = aReferenceGenerator.next();
			if (aCondition._match(theRefEvent))
			{
				BitGridEvent theTestedEvent = (BitGridEvent) aIterator.next(); 
				Fixtures1.assertEquals(""+i, theRefEvent, theTestedEvent);
				theMatched++;
//				System.out.println(i+"m");
			}
//			else System.out.println(i);
		}
		
		System.out.println("Matched: "+theMatched);
		return theMatched;
	}
	
	public static void checkIteration(
			EventDatabase aDatabase, 
			EventCondition aCondition,
			EventGenerator1 aReferenceGenerator,
			int aCount)
	{
		List<BitGridEvent> theEvents = new ArrayList<BitGridEvent>(aCount);

		IBidiIterator<GridEvent> theIterator = aDatabase.evaluate(aCondition, 0);
		while (theEvents.size() < aCount)
		{
			GridEvent theRefEvent = aReferenceGenerator.next();
			if (aCondition._match(theRefEvent)) theEvents.add((BitGridEvent) theIterator.next());
		}
		
		BitGridEvent theFirstEvent = theEvents.get(0);
		theIterator = aDatabase.evaluate(aCondition, theFirstEvent.getTimestamp());
		assertEquals("first.a", theFirstEvent, (BitGridEvent)theIterator.next());
		assertEquals("first.b", theFirstEvent, (BitGridEvent)theIterator.previous());
		
		BitGridEvent theSecondEvent = theEvents.get(1);
		theIterator = aDatabase.evaluate(aCondition, theFirstEvent.getTimestamp()+1);
		assertEquals("sec.a", theSecondEvent, (BitGridEvent)theIterator.next());
		assertEquals("sec.b", theSecondEvent, (BitGridEvent)theIterator.previous());
		
		theIterator = aDatabase.evaluate(aCondition, 0);
		
		int theIndex = 0;
		int theDelta = aCount;
		boolean theForward = true;
		while(theDelta > 1)
		{
			for (int i=0;i<theDelta;i++)
			{
				BitGridEvent theRefEvent;
				BitGridEvent theTestEvent;
				if (theForward)
				{
					theRefEvent = theEvents.get(theIndex);
					theTestEvent = (BitGridEvent)theIterator.next();
					theIndex++;
				}
				else
				{
					theTestEvent = (BitGridEvent)theIterator.previous();
					theIndex--;
					theRefEvent = theEvents.get(theIndex);
				}
				
				assertEquals("index: "+theIndex, theRefEvent, theTestEvent);
			}
			
			theDelta /= 2;
			theForward = ! theForward;
		}
	}

	private static class MyLogReceiverMonitor implements ILogReceiverMonitor
	{
		private long t0;
		private DerivativeDataPrinter itsPrinter;
		
		private MyLogReceiverMonitor()
		{
			try
			{
				itsPrinter = new DerivativeDataPrinter(
									new File("replay-times.txt"),
									"time (ms)",
									"events");
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		public void started()
		{
			t0 = System.currentTimeMillis();
		}

		public void processedMessages(long aCount)
		{
			System.out.println(aCount);

			long t = System.currentTimeMillis()-t0;
			itsPrinter.addPoint(t/1000f, aCount);
		}

		
	}
	

}
