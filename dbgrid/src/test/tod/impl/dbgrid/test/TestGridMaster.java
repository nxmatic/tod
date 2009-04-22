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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.database.structure.standard.HostInfo;
import tod.impl.database.structure.standard.ThreadInfo;
import tod.impl.dbgrid.ConditionGenerator;
import tod.impl.dbgrid.DebuggerGridConfig;
import tod.impl.dbgrid.EventGenerator;
import tod.impl.dbgrid.Fixtures;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.GridMaster;
import tod.impl.dbgrid.IGridEventFilter;
import tod.impl.dbgrid.IdGenerator;
import tod.impl.dbgrid.aggregator.GridEventBrowser;
import tod.impl.dbgrid.aggregator.RIQueryAggregator;
import tod.impl.dbgrid.messages.GridEvent;
import tod.tools.monitoring.MonitoringClient.MonitorId;
import zz.utils.Utils;

public class TestGridMaster
{
	@Test public void test() throws Throwable
	{
		try
		{
			doTest();
		}
		catch (Throwable e)
		{
			Thread.sleep(1000);
			throw e;
		}
	}
	
	private void doTest() 
	{
		GridMaster theMaster = Fixtures.setupLocalMaster();
		IMutableStructureDatabase theStructureDatabase = theMaster.getStructureDatabase();
//		theStructureDatabase.clear();
		
		for (int i=1;i<=100;i++) 
		{
			HostInfo theHostInfo = new HostInfo(i, ""+i);
			theMaster.registerHost(theHostInfo);
			
			for (int j=1;j<=100;j++)
			{
				theMaster.registerThread(new ThreadInfo(theHostInfo, j, j, ""+j));
			}
			
//			IMutableClassInfo theClass = theStructureDatabase.getNewClass("C"+i);
//			theClass.getNewBehavior("m"+i, "()V", false);
//			theClass.getNewField("f"+i, PrimitiveTypeInfo.BOOLEAN, false);
		}
		GridLogBrowser theLogBrowser = DebuggerGridConfig.createLocalLogBrowser(null, theMaster);

		EventGenerator theEventGenerator = createEventGenerator(theStructureDatabase);
		theEventGenerator.fillStructureDatabase(theStructureDatabase);
		
		System.out.println("filling...");
		Fixtures.fillDatabase(theMaster, theEventGenerator, 10000000);
		
		System.out.println("checking...");
		
//		checkBehaviorCalls(theMaster);
		
		IdGenerator theIdGenerator = new IdGenerator(100, 100, 100, 100, 100, 100, 100, 100, 100, 100);
		ConditionGenerator theConditionGenerator = createConditionGenerator(0, theIdGenerator, theLogBrowser);
		
		for (int i=0;i<2;i++) theConditionGenerator.next();
		
		for (int i=0;i<1000;i++)
		{
			IGridEventFilter theEventCondition = (IGridEventFilter) theConditionGenerator.next();
			if (i<2) continue;

			System.out.println(i+1);
			System.out.println(theEventCondition);
			
			int theCount = checkCondition(theMaster, theEventCondition, createEventGenerator(theStructureDatabase), 5000, 10000);
			
			GridEventBrowser theEventBrowser = new GridEventBrowser(theLogBrowser, theEventCondition);
			int theCount2 = checkCondition(theEventBrowser, theEventCondition, createEventGenerator(theStructureDatabase), 5000, 10000);
			
			Assert.assertTrue("Bad count", theCount == theCount2);
			
			if (theCount > 3)
			{
				checkIteration(
						theLogBrowser,
						theEventCondition, 
						createEventGenerator(theStructureDatabase), 
						theCount);
			}

		}
		
	}
	
	private void checkBehaviorCalls(GridMaster aMaster)
	{
		GridLogBrowser theLogBrowser = aMaster._getLocalLogBrowser();
		IEventFilter theFilter = theLogBrowser.createBehaviorCallFilter();
		IEventBrowser theEventBrowser = theLogBrowser.createBrowser(theFilter);
		
		int theCount = 0;
		while(theEventBrowser.hasNext() && theCount < 500)
		{
			IBehaviorCallEvent theEvent = (IBehaviorCallEvent) theEventBrowser.next();
			IEventBrowser theChildrenBrowser = theEvent.getChildrenBrowser();
			theCount++;
		}

		Utils.println("Tested %d calls", theCount);
	}
	
	private int checkCondition(
			GridMaster aMaster, 
			IGridEventFilter aCondition, 
			EventGenerator aReferenceGenerator,
			int aSkip,
			int aCount) 
	{
		GridEvent theEvent = null;
		for (int i=0;i<aSkip;i++) theEvent = aReferenceGenerator.next();
		
		long theTimestamp = theEvent != null ? theEvent.getTimestamp()+1 : 0;
		
		RIQueryAggregator theAggregator = aMaster.createAggregator(aCondition);
		theAggregator.setNextTimestamp(theTimestamp);
		
		int theMatched = 0;
		for (int i=0;i<aCount;i++)
		{
			GridEvent theRefEvent = aReferenceGenerator.next();
			if (aCondition._match(theRefEvent))
			{
				GridEvent[] theBuffer = theAggregator.next(MonitorId.get(), 1);
				GridEvent theTestedEvent = theBuffer[0]; 
				Fixtures.assertEquals(""+i, theRefEvent, theTestedEvent);
				theMatched++;
			}
		}
		
		System.out.println("Matched: "+theMatched);
		return theMatched;
	}
	
	private int checkCondition(
			GridEventBrowser aBrowser, 
			IGridEventFilter aCondition, 
			EventGenerator aReferenceGenerator,
			int aSkip,
			int aCount) 
	{
		GridEvent theEvent = null;
		for (int i=0;i<aSkip;i++) theEvent = aReferenceGenerator.next();
		
		long theTimestamp = theEvent != null ? theEvent.getTimestamp()+1 : 0;
		
		aBrowser.setNextTimestamp(theTimestamp);
		
		int theMatched = 0;
		for (int i=0;i<aCount;i++)
		{
			GridEvent theRefEvent = aReferenceGenerator.next();
			ILogEvent theLogEvent = theRefEvent.toLogEvent(aBrowser.getLogBrowser());
			
			if (aCondition._match(theRefEvent))
			{
				ILogEvent theTestedEvent = aBrowser.next();
				Fixtures.assertEquals(""+i, theLogEvent, theTestedEvent);
				theMatched++;
			}
		}
		
		System.out.println("Matched: "+theMatched);
		return theMatched;
	}
	
	public static void checkIteration(
			ILogBrowser aBrowser,
			IGridEventFilter aCondition, 
			EventGenerator aReferenceGenerator,
			int aCount)
	{
		List<ILogEvent> theEvents = new ArrayList<ILogEvent>(aCount);

		IEventBrowser theEventBrowser = aBrowser.createBrowser(aCondition);
		theEventBrowser.setNextTimestamp(0);
		while (theEvents.size() < aCount)
		{
			GridEvent theRefEvent = aReferenceGenerator.next();
			if (aCondition._match(theRefEvent)) theEvents.add(theEventBrowser.next());
		}
		
		ILogEvent theFirstEvent = theEvents.get(0);
		theEventBrowser = aBrowser.createBrowser(aCondition);
		theEventBrowser.setNextTimestamp(theFirstEvent.getTimestamp());
		Fixtures.assertEquals("first.a", theFirstEvent, theEventBrowser.next());
		Fixtures.assertEquals("first.b", theFirstEvent, theEventBrowser.previous());
		
		ILogEvent theSecondEvent = theEvents.get(1);
		theEventBrowser = aBrowser.createBrowser(aCondition);
		theEventBrowser.setNextTimestamp(theFirstEvent.getTimestamp()+1);
		Fixtures.assertEquals("sec.a", theSecondEvent, theEventBrowser.next());
		Fixtures.assertEquals("sec.b", theSecondEvent, theEventBrowser.previous());
		
		theEventBrowser = aBrowser.createBrowser(aCondition);
		theEventBrowser.setNextTimestamp(0);
		
		int theIndex = 0;
		int theDelta = aCount;
		boolean theForward = true;
		while(theDelta > 1)
		{
			for (int i=0;i<theDelta;i++)
			{
				ILogEvent theRefEvent;
				ILogEvent theTestEvent;
				if (theForward)
				{
					theRefEvent = theEvents.get(theIndex);
					theTestEvent = theEventBrowser.next();
					theIndex++;
				}
				else
				{
					theTestEvent = theEventBrowser.previous();
					theIndex--;
					theRefEvent = theEvents.get(theIndex);
				}
				
				Fixtures.assertEquals("index: "+theIndex, theRefEvent, theTestEvent);
			}
			
			theDelta /= 2;
			theForward = ! theForward;
		}
	}


	
	private EventGenerator createEventGenerator(IMutableStructureDatabase aStructureDatabase)
	{
		try
		{
			Class theClass = DebuggerGridConfig.getDbImpl().getClass("EventGenerator");
			Constructor theConstructor = theClass.getConstructor(
					IMutableStructureDatabase.class,
					long.class, int.class, int.class,
					int.class, int.class, int.class,
					int.class, int.class, int.class,
					int.class);
			
			return (EventGenerator) theConstructor.newInstance(aStructureDatabase, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private ConditionGenerator createConditionGenerator(long aSeed, IdGenerator aIdGenerator, ILogBrowser aLogBrowser)
	{
		return new ConditionGenerator(aSeed, aIdGenerator, aLogBrowser);
	}
	
}
