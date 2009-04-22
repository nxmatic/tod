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

import org.junit.Test;

import tod.core.config.TODConfig;
import tod.impl.dbgrid.DebuggerGridConfig;
import tod.impl.dbgrid.db.DatabaseNode;
import tod.impl.dbgrid.db.RIBufferIterator;
import tod.impl.dbgrid.dispatch.RINodeConnector.StringSearchHit;
import tod.tools.monitoring.MonitoringClient.MonitorId;

public class TestStringSearch
{
	private static final String[] STRINGS = {
		"Hello", "World", "Hello World", "Hello123", "123", "Hello123World", "HelloWorld"
	};
	
	@Test public void testSearch()
	{
		TODConfig theConfig = new TODConfig();
		theConfig.set(TODConfig.INDEX_STRINGS, true);
		DatabaseNode theNode = DebuggerGridConfig.createDatabaseNode();

		for (int i=0;i<STRINGS.length;i++)
		{
			throw new UnsupportedOperationException("Reimplement");
//			theNode.register(i, STRINGS[i], i);
		}
		
		search(theNode, "Hello");
		search(theNode, "123");
		search(theNode, "Hello World");
		search(theNode, "H*lo");
	}
	
	private void search(DatabaseNode aNode, String aText)
	{
		System.out.println("Search: "+aText);
		printIterator(aNode.searchStrings(aText));
		System.out.println("Done");
	}
	
	private void printIterator(RIBufferIterator<StringSearchHit[]> aIterator)
	{
		while(true)
		{
			StringSearchHit[] theHits = aIterator.next(MonitorId.get(), 1);
			if (theHits == null) break;
			
			StringSearchHit theHit = theHits[0];
			
			System.out.println(theHit.getObjectId()
					+": "+STRINGS[(int) theHit.getObjectId()]
					+" ("+theHit.getScore()+")");
		}
	}
}
