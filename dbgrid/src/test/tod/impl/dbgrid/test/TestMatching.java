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
package tod.impl.dbgrid.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import tod.core.ILogCollector;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.database.structure.standard.ClassInfo;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.dbgrid.Fixtures;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.GridMaster;

public class TestMatching
{
	static final GridMaster MASTER = Fixtures.setupLocalMaster(); 
	static ClassInfo CLASS;
	static IMutableBehaviorInfo BEHAVIOR1;
	static IMutableBehaviorInfo BEHAVIOR2;
	
	static
	{
		StructureDatabase theStructureDatabase = (StructureDatabase) MASTER.getStructureDatabase();
		CLASS = theStructureDatabase.addClass(1, "c");
		BEHAVIOR1 = CLASS.addBehavior(1, "b1", "()V", false);
		BEHAVIOR2 = CLASS.addBehavior(2, "b2", "()V", false);
	}
	
	/**
	 * Test what happens when an event matches a condition several times.
	 * Eg, match is on object id x and call event has arguments [x, x] 
	 */
	@Test
	public void testMultimatch()
	{
		MASTER.clear();
		
		// Fill event database
		ILogCollector theCollector = MASTER._getCollector();
		theCollector.thread(0, 0, "test");
		theCollector.methodCall(0, 0, (short) 0, 0, null, 0, false, 1, 1, null, new Object[] {new ObjectId(5), new ObjectId(5)});
		theCollector.flush();
		
		GridLogBrowser theLogBrowser = MASTER._getLocalLogBrowser();
//		IEventFilter theFilter = theLogBrowser.createObjectFilter(new ObjectId(5));
		IEventFilter theFilter = theLogBrowser.createBehaviorCallFilter(BEHAVIOR1);
		
		IEventBrowser theEventBrowser = theLogBrowser.createBrowser(theFilter);
		theEventBrowser.setNextTimestamp(0);
		
		ILogEvent theFirst = theEventBrowser.next();
		if (theEventBrowser.hasNext())
		{
			ILogEvent theSecond = theEventBrowser.next();
			Assert.fail("There should be only one event");
		}
	}
	
	/**
	 * Same as {@link #testMultimatch()}, but tests with the iterator going back and forth.
	 */
	@Test
	public void testMultimatch2()
	{
		MASTER.clear();

		// Fill event database
		ILogCollector theCollector = MASTER._getCollector();
		theCollector.thread(0, 0, "test");
		
		ObjectId o1 = new ObjectId(1);
		ObjectId o5 = new ObjectId(5);
		
		theCollector.methodCall(0, 0, (short) 0, 1, null, 0, false, 0, 0, null, new Object[] {o5, o5});
		theCollector.methodCall(0, 0, (short) 0, 2, null, 0, false, 0, 0, null, new Object[] {});
		theCollector.methodCall(0, 0, (short) 0, 3, null, 0, false, 1, 1, null, new Object[] {o1});
		theCollector.methodCall(0, 0, (short) 0, 4, null, 0, false, 0, 0, o1, new Object[] {o5, o1, o1, o1, o5, o1});
		theCollector.methodCall(0, 0, (short) 0, 5, null, 0, false, 1, 1, null, new Object[] {});
		theCollector.methodCall(0, 0, (short) 0, 6, null, 0, false, 0, 0, null, new Object[] {o5, o1, o5});
		theCollector.methodCall(0, 0, (short) 0, 7, null, 0, false, 0, 0, null, new Object[] {o5, o5, o1});
		theCollector.flush();
		
		GridLogBrowser theLogBrowser = MASTER._getLocalLogBrowser();
		IEventFilter theFilter = theLogBrowser.createUnionFilter(
				theLogBrowser.createObjectFilter(o5),
				theLogBrowser.createBehaviorCallFilter(BEHAVIOR1));
		
		IEventBrowser theEventBrowser = theLogBrowser.createBrowser(theFilter);
		theEventBrowser.setNextTimestamp(0);
		
		List<ILogEvent> theEvents = new ArrayList<ILogEvent>();
		theEvents.add(theEventBrowser.next());
		theEvents.add(theEventBrowser.next());
		theEvents.add(theEventBrowser.next());
		theEvents.add(theEventBrowser.next());
		theEvents.add(theEventBrowser.previous());
		theEvents.add(theEventBrowser.previous());
		theEvents.add(theEventBrowser.previous());
		theEvents.add(theEventBrowser.next());
		
		long[] theExpectedTimesamps = {1, 3, 4, 5, 5, 4, 3, 3};
		
		Assert.assertEquals(theEvents.size(), theExpectedTimesamps.length);
		
		for(int i=0;i<theEvents.size();i++)
		{
			Assert.assertEquals(""+i, theExpectedTimesamps[i], theEvents.get(i).getTimestamp());
		}
		
	}
}
