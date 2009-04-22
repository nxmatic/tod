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
package tod.core.database.browser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import tod.core.database.EventGenerator;
import tod.core.database.browser.GroupingEventBrowser.EventGroup;
import tod.core.database.browser.GroupingEventBrowser.IGroupDefinition;
import tod.core.database.event.ILogEvent;
import tod.impl.local.EventBrowser;
import zz.utils.ITask;


public class TestGroupingEventBrowser
{
	private static final int COUNT = 1000;
	
	private GroupingEventBrowser<Integer> createBrowser()
	{
		// Create event list
		List<ILogEvent> theEvents = new ArrayList<ILogEvent>();
		final Map<ILogEvent, Integer> theGroups = new HashMap<ILogEvent, Integer>();
		
		EventGenerator theGenerator = new EventGenerator(12);
		
		Random theRandom = new Random(12);
		for(int i=0;i<COUNT;i++)
		{
			ILogEvent theEvent = theGenerator.next();
			theEvents.add(theEvent);
			
			int theGroup = theRandom.nextInt(10);
			
			theGroups.put(theEvent, theGroup > 4 ? theGroup : null);
		}
		
		IEventBrowser theSourceBrowser = new EventBrowser(null, theEvents, null);
		
		IGroupDefinition<Integer> theGroupDefinition = new IGroupDefinition<Integer>()
		{
			public Integer getGroupKey(ILogEvent aEvent)
			{
				return theGroups.get(aEvent);
			}
		};
		
		return new GroupingEventBrowser<Integer>(theSourceBrowser, theGroupDefinition, false);
	}
	
	@Test public void testForward()
	{
		System.out.println("TestGroupingEventBrowser.testForward()");
		GroupingEventBrowser<Integer> theBrowser = createBrowser();
		testSequential(theBrowser, GroupingEventBrowser.Direction.FORWARD);
	}
	
	@Test public void testBackward()
	{
		System.out.println("TestGroupingEventBrowser.testBackward()");
		GroupingEventBrowser<Integer> theBrowser = createBrowser();
		theBrowser.setPreviousTimestamp(Long.MAX_VALUE);
		testSequential(theBrowser, GroupingEventBrowser.Direction.BACKWARD);
	}
	
	private void testSequential(
			GroupingEventBrowser<Integer> aBrowser, 
			GroupingEventBrowser.Direction aDirection)
	{
		IGroupDefinition<Integer> theGroupDefinition = aBrowser.getGroupDefinition();
		
		Integer theCurrentKey = null;
		int theTotalSize = 0;
		while(aDirection.hasMore(aBrowser))
		{
			ILogEvent theNext = aDirection.more(aBrowser);
			Integer theNextKey = theGroupDefinition.getGroupKey(theNext);
			
			if (theNext instanceof EventGroup)
			{
				EventGroup<Integer> theEventGroup = (EventGroup) theNext;
				
				Integer theInnerKey = null;
				int theSize = 0;
				for (ILogEvent theGroupEvent : theEventGroup.getEvents())
				{
					Integer theCurrentKey2 = theGroupDefinition.getGroupKey(theGroupEvent);
					Assert.assertTrue(theCurrentKey2 != null);
					
					if (theInnerKey == null) theInnerKey = theCurrentKey2;
					else Assert.assertTrue(theInnerKey.equals(theCurrentKey2));
					
					theSize++;
				}
				theNextKey = theInnerKey;
				
				theTotalSize += theSize;
				System.out.println("Found group of "+theSize+" events: "+theInnerKey);
			}
			else
			{
				Assert.assertTrue(theNextKey == null || ! theNextKey.equals(theCurrentKey));
				theTotalSize++;
			}
			
			theCurrentKey = theNextKey;
		}
		
		Assert.assertEquals(COUNT, theTotalSize);
	}
}
