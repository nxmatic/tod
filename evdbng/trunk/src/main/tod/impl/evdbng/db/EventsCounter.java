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
package tod.impl.evdbng.db;

import tod.core.database.browser.IEventBrowser;
import tod.impl.database.IBidiIterator;
import tod.impl.evdbng.db.file.SimpleTuple;
import tod.impl.evdbng.queries.EventCondition;

/**
 * A helper class that computes event counts.
 * @see IEventBrowser#getEventCounts(long, long, int)
 * @author gpothier
 */
public class EventsCounter
{
	public static long[] mergeCountEvents(
			EventCondition aCondition, 
			IEventList aEventList,
			Indexes aIndexes, 
			long aT1, 
			long aT2, 
			int aSlotsCount) 
	{
//		System.out.println("Computing counts...");
//		long t0 = System.currentTimeMillis();
		long[] theCounts = new long[aSlotsCount];
		
		long theTotal = 0;
		
		IBidiIterator<SimpleTuple> theIterator = aCondition.createTupleIterator(aEventList, aIndexes, aT1);
		while (theIterator.hasNext())
		{
			SimpleTuple theTuple = theIterator.next();
			long theTimestamp = theTuple.getKey();
			if (theTimestamp < aT1) continue;
			if (theTimestamp >= aT2) break;

			int theSlot = (int)(((theTimestamp - aT1) * aSlotsCount) / (aT2 - aT1));
			theCounts[theSlot]++;
			theTotal++;
		}
		
//		long t1 = System.currentTimeMillis();
//		System.out.println("Counts computed in "+(t1-t0)+"ms (found "+theTotal+" events)");
		return theCounts;
	}
}
