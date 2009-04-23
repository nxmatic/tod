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

import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_ADVICE_SRC_ID_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_ARRAY_INDEX_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_BEHAVIOR_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_BYTECODE_LOCS_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_DEPTH_RANGE;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_FIELD_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_OBJECT_COUNT;
import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_VAR_COUNT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.StringTokenizer;

import junit.framework.Assert;

import org.junit.Test;

import tod.impl.dbgrid.DebuggerGridConfig;
import tod.impl.dbgrid.db.EventReorderingBuffer;
import tod.impl.dbgrid.db.EventReorderingBuffer.ReorderingBufferListener;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdb1.EventGenerator1;
import tod.impl.evdb1.messages.GridFieldWriteEvent;

public class TestEventReordering implements ReorderingBufferListener 
{
	private long itsLastProcessedTimestamp;
	private boolean itsDropIsFailure;
	
	@Test public void test()
	{
		itsLastProcessedTimestamp = 0;
		itsDropIsFailure = true;
		
		DebuggerGridConfig.DB_REORDER_BUFFER_SIZE = 50;
		
		EventReorderingBuffer theBuffer = new EventReorderingBuffer(this);
		EventGenerator1 theGenerator = new EventGenerator1(
				null,
				2, 
				2,
				STRUCTURE_DEPTH_RANGE,
				STRUCTURE_BYTECODE_LOCS_COUNT,
				STRUCTURE_BEHAVIOR_COUNT,
				STRUCTURE_ADVICE_SRC_ID_COUNT,
				STRUCTURE_FIELD_COUNT,
				STRUCTURE_VAR_COUNT,
				STRUCTURE_OBJECT_COUNT,
				STRUCTURE_ARRAY_INDEX_COUNT);
		
		LinkedList<GridEvent> theOoOBuffer = new LinkedList<GridEvent>();
		
		Random theRandom = new Random(0);
		
		// Fill
		for (int i=0;i<10000000;i++)
		{
			GridEvent theEvent;
			float theFloat = theRandom.nextFloat();
			if (theFloat < 0.01) 
			{
				theEvent = theGenerator.next();
				theOoOBuffer.addLast(theEvent);
				continue;
			}
			
			if (theOoOBuffer.size() > 50)
			{
				theEvent = theOoOBuffer.removeFirst();
			}
			else
			{
				theEvent = theGenerator.next();
			}
			
			while (theBuffer.isFull()) processEvent(theBuffer.pop());
			theBuffer.push(theEvent);
			
			if (i % 100000 == 0)
			{
				System.out.println("i: "+i+" - "+theOoOBuffer.size());
			}
		}
		
		// Flush
		while (! theBuffer.isEmpty()) processEvent(theBuffer.pop());
	}

	private void processEvent(GridEvent aEvent)
	{
		long theTimestamp = aEvent.getTimestamp();
		if (theTimestamp < itsLastProcessedTimestamp)
		{
			eventDropped(itsLastProcessedTimestamp, theTimestamp, "test");
			return;
		}
		
		itsLastProcessedTimestamp = theTimestamp;
	}
	
	
	public void eventDropped(long aLastRetrieved, long aNewEvent, String aReason)
	{
		if (itsDropIsFailure) Assert.fail("eventDropped");
		else System.out.println("eventDropped");
	}
	
	/**
	 * This test creates events loaded from an actual trace that displayed
	 * a bad behavior.
	 */
	@Test public void testFromTrace() throws IOException
	{
		itsLastProcessedTimestamp = 0;
		itsDropIsFailure = false;
		EventReorderingBuffer theBuffer = new EventReorderingBuffer(this);
		
		BufferedReader theReader = new BufferedReader(new FileReader("src/test/TestEventReordering-trace.txt"));
		String theLine;
		while((theLine = theReader.readLine()) != null)
		{
			StringTokenizer theTokenizer = new StringTokenizer(theLine);
			int theHost = Integer.parseInt(theTokenizer.nextToken());
			int theThread = Integer.parseInt(theTokenizer.nextToken());
			long theTimestamp = Long.parseLong(theTokenizer.nextToken());
			
			GridEvent theEvent = new GridFieldWriteEvent(null, theThread, 0, theTimestamp, null, 0, 0, 0, 0, 0);
			
			while (theBuffer.isFull()) processEvent(theBuffer.pop());
			theBuffer.push(theEvent);
		}
	}
}
