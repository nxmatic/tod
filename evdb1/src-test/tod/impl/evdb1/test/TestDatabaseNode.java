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

import org.junit.Before;
import org.junit.Test;

import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.dbgrid.Fixtures;
import tod.impl.dbgrid.db.EventDatabase;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.ConditionGenerator1;
import tod.impl.evdb1.EventGenerator1;
import tod.impl.evdb1.Fixtures1;
import tod.impl.evdb1.db.EventDatabase1;
import tod.impl.evdb1.queries.CompoundCondition;
import tod.impl.evdb1.queries.Disjunction;
import tod.impl.evdb1.queries.EventCondition;
import tod.impl.evdb1.queries.TypeCondition;

public class TestDatabaseNode
{
	private EventDatabase itsDatabase;
	private IMutableStructureDatabase itsStructureDatabase;

	@Before public void fill()
	{
		itsDatabase = new EventDatabase1(null, 0, new File("test.bin"));
		itsStructureDatabase = StructureDatabase.create(new TODConfig());
		EventGenerator1 theEventGenerator = createGenerator(itsStructureDatabase);
		
		System.out.println("filling...");
		Fixtures.fillDatabase(itsDatabase, theEventGenerator, 100000);
	}
	
	@Test public void check() 
	{
		System.out.println("checking...");
		
		// Check with fixed condition
		CompoundCondition theCondition = new Disjunction();
//		theCondition.addCondition(new BehaviorCondition(3, (byte) 0));
		theCondition.addCondition(new TypeCondition(MessageType.FIELD_WRITE));
		
		Fixtures1.checkCondition(
				itsDatabase, 
				theCondition,
				createGenerator(itsStructureDatabase),
				0,
				1000);

		// Check with random conditions
		ConditionGenerator1 theConditionGenerator = new ConditionGenerator1(0, createGenerator(itsStructureDatabase));
//		for (int i=0;i<591;i++) theConditionGenerator.next();
		
		for (int i=0;i<1000;i++)
		{
			System.out.println(i+1);
			EventCondition theEventCondition = theConditionGenerator.next();
			System.out.println(theEventCondition);
			
			int theCount = Fixtures1.checkCondition(
					itsDatabase, 
					theEventCondition,
					createGenerator(itsStructureDatabase),
					5000,
					10000);
			
			if (theCount > 3)
			{
				Fixtures1.checkIteration(
						itsDatabase, 
						theEventCondition, 
						createGenerator(itsStructureDatabase), 
						theCount);
			}
		}
	}
	
	private EventGenerator1 createGenerator(IMutableStructureDatabase aStructureDatabase)
	{
		return new EventGenerator1(aStructureDatabase, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100);
	}
	
}
