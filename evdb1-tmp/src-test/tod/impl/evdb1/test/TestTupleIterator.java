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

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;
import static org.junit.Assert.*;


import tod.impl.evdb1.DebuggerGridConfig1;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;
import tod.impl.evdb1.db.StdIndexSet.StdTupleCodec;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.TupleIterator;
import tod.impl.evdb1.db.file.TupleWriter;
import tod.impl.evdb1.db.file.HardPagedFile.Page;

public class TestTupleIterator
{
	@Test public void testIteration() throws FileNotFoundException
	{
		File theFile = new File("iterator.bin");
		theFile.delete();
		HardPagedFile thePagedFile = new HardPagedFile(theFile, DebuggerGridConfig1.DB_PAGE_SIZE);
		StdTupleCodec theCodec = new StdTupleCodec();
		Page theFirstPage = thePagedFile.create();
		TupleWriter<StdTuple> theWriter = new TupleWriter<StdTuple>(thePagedFile, theCodec, theFirstPage, 0);
		
		for (int i=1;i<=10000;i++)
		{
			theWriter.add(new StdTuple(i, i));
		}
		
		TupleIterator<StdTuple> theIterator = new TupleIterator<StdTuple>(null, thePagedFile, theCodec, theFirstPage.asBitStruct());
		
		// From start, iterate until end
		assertTrue(theIterator.hasNext());
		assertFalse(theIterator.hasPrevious());
		
		for (int i=1;i<=10000;i++)
		{
			StdTuple theTuple = theIterator.next();
			assertTrue(theTuple.getKey() == i);
			assertTrue(theTuple.getEventPointer() == i);
		}
		
		// From end, iterate until beginning
		assertFalse(theIterator.hasNext());
		assertTrue(theIterator.hasPrevious());
		
		for (int i=10000;i>=1;i--)
		{
			StdTuple theTuple = theIterator.previous();
			assertTrue(theTuple.getKey() == i);
			assertTrue(theTuple.getEventPointer() == i);
		}

		// From start, iterate until end
		assertTrue(theIterator.hasNext());
		assertFalse(theIterator.hasPrevious());
		
		for (int i=1;i<=10000;i++)
		{
			StdTuple theTuple = theIterator.next();
			assertTrue(theTuple.getKey() == i);
			assertTrue(theTuple.getEventPointer() == i);
		}
		
		// From end, iterate until beginning
		assertFalse(theIterator.hasNext());
		assertTrue(theIterator.hasPrevious());
		
		for (int i=10000;i>=1;i--)
		{
			StdTuple theTuple = theIterator.previous();
			assertTrue(theTuple.getKey() == i);
			assertTrue(theTuple.getEventPointer() == i);
		}

		// From start iterate until middle
		assertTrue(theIterator.hasNext());
		assertFalse(theIterator.hasPrevious());
		
		for (int i=1;i<=5000;i++)
		{
			StdTuple theTuple = theIterator.next();
			assertTrue(theTuple.getKey() == i);
			assertTrue(theTuple.getEventPointer() == i);
		}
		
		// From middle, iterate until beginning
		assertTrue(theIterator.hasNext());
		assertTrue(theIterator.hasPrevious());
		
		for (int i=5000;i>=1;i--)
		{
			StdTuple theTuple = theIterator.previous();
			assertTrue(theTuple.getKey() == i);
			assertTrue(theTuple.getEventPointer() == i);
		}

		assertTrue(theIterator.hasNext());
		assertFalse(theIterator.hasPrevious());
	}
}
