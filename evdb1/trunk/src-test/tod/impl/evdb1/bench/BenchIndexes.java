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
package tod.impl.evdb1.bench;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tod.BenchBase;
import tod.BenchBase.BenchResults;
import tod.core.database.TimestampGenerator;
import tod.impl.evdb1.Fixtures1;
import tod.impl.evdb1.db.HierarchicalIndex;
import tod.impl.evdb1.db.StdIndexSet.StdTuple;
import tod.impl.evdb1.test.TestHierarchicalIndex;

public class BenchIndexes
{
	private HierarchicalIndex<StdTuple> itsIndex;
	
	@Test
	public void hierarchicalWriteBench()
	{
		// Warm-up
		fill(10000000);

		BenchResults theResults = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				fill(100000000);
			}
		});
		
		System.out.println(theResults);
		
		long theTupleCount = itsIndex.getLeafTupleCount();
		long theStorage = 1L * itsIndex.getPageSize() * itsIndex.getTotalPageCount();
		
		float theMBs = 1.0f * (theStorage / (1024*1024)) / (theResults.totalTime / 1000);
		
		System.out.println("Tuple count: "+theTupleCount);
		System.out.println("Storage space: "+theStorage);
		System.out.println("MB/s: "+theMBs);
		
		assertTrue(theMBs > 20);
	}
	
	private void fill(long aTupleCount)
	{
		itsIndex = Fixtures1.createStdIndex();
		Fixtures1.fillStdIndex(itsIndex, new TimestampGenerator(0), aTupleCount);
	}
}
