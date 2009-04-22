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
package tod.impl.evdbng.test;

import java.io.File;
import java.tod.AgentReady;

import org.junit.Before;
import org.junit.Test;

import tod.core.config.TODConfig;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.evdbng.ConditionGeneratorNG;
import tod.impl.evdbng.EventGeneratorNG;
import tod.impl.evdbng.FixturesNG;
import tod.impl.evdbng.db.EventDatabaseNG;
import tod.impl.evdbng.queries.EventCondition;

public class TestEventDatabase
{
	static
	{
		System.setProperty("page-buffer-size", "4m");
		System.setProperty("db-task-size", "10");
	}
	
	private EventDatabaseNG itsDatabase;
	private StructureDatabase itsStructureDatabase;

	@Before public void fill()
	{
		System.out.println("enabled: "+AgentReady.CAPTURE_ENABLED);
//		TOD.disableCapture();
		itsStructureDatabase = StructureDatabase.create(new TODConfig());
		itsDatabase = new EventDatabaseNG(
				itsStructureDatabase,
				0, 
				new File("."));
		
		EventGeneratorNG theEventGenerator = createGenerator();
		theEventGenerator.fillStructureDatabase(itsStructureDatabase);
		
		theEventGenerator = createGenerator();
		
		System.out.println("filling...");
		FixturesNG.fillDatabase(itsDatabase, theEventGenerator, 2000);
	}
	
	@Test public void check() 
	{
		System.out.println("checking...");
		
		// Check with fixed condition
//		CompoundCondition theCondition = new Disjunction();
//		theCondition.addCondition(new BehaviorCondition(3, (byte) 0));
//		
//		FixturesNG.checkCondition(
//				itsDatabase, 
//				theCondition,
//				createGenerator(),
//				0,
//				1000);

		// Check with random conditions
		ConditionGeneratorNG theConditionGenerator = new ConditionGeneratorNG(0, createGenerator());
		
		for (int i=0;i<1000;i++)
		{
			System.out.println(i+1);
			EventCondition theEventCondition = theConditionGenerator.next();
			if (i<2) continue;
			System.out.println(theEventCondition);
			
			int theCount = FixturesNG.checkCondition(
					itsDatabase, 
					theEventCondition,
					createGenerator(),
					500,
					1000);
			
			if (theCount > 3)
			{
//				TOD.enableCapture();
				FixturesNG.checkIteration(
						itsDatabase, 
						theEventCondition, 
						createGenerator(), 
						theCount);
//				TOD.disableCapture();
			}
		}
	}
	
	private EventGeneratorNG createGenerator()
	{
		return new EventGeneratorNG(itsStructureDatabase, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100);
	}
	
}
