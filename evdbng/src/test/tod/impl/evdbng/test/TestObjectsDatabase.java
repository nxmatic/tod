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
package tod.impl.evdbng.test;

import java.io.File;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import tod.core.config.TODConfig;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.dbgrid.db.ObjectsDatabase.Decodable;
import tod.impl.evdbng.db.ObjectsDatabaseNG;
import tod.impl.evdbng.db.file.PagedFile;
import zz.utils.Utils;

public class TestObjectsDatabase
{
	private static final String S = "Hello! ";
	private static final int COUNT = 10000;
	
	@Test public void test()
	{
		File theFile = new File("objects.bin");
		theFile.delete();
		PagedFile thePagedFile = PagedFile.create(theFile, false);
		
		TODConfig theConfig = new TODConfig();
		StructureDatabase theStructureDatabase = StructureDatabase.create(theConfig);
		ObjectsDatabaseNG theDatabase = new ObjectsDatabaseNG(theStructureDatabase, thePagedFile, thePagedFile);
		
		Random theRandom = new Random(0);
		StringGen theGen = new StringGen();
		long theId = 1;
		for (int i=0;i<COUNT;i++)
		{
			theDatabase.store(theId, Utils.encode(theGen.next()), i);
			
			theId += theRandom.nextInt(100)+1;
			
			if (i % 100 == 0) System.out.println(""+i);
		}
		
		theDatabase.flush(null);
		
		theRandom = new Random(0);
		theGen = new StringGen();
		theId = 1;
		
		for (int i=0;i<COUNT;i++)
		{
			Decodable theDecodable = theDatabase.load(theId);
			String theString = (String) theDecodable.decode();
			Assert.assertEquals(theString, theGen.next());
			
			theId += theRandom.nextInt(100)+1;
			
			if (i % 100 == 0) System.out.println(""+i);
		}
	}
	
	private static class StringGen
	{
		private final StringBuilder builder = new StringBuilder();
		
		public String next()
		{
			builder.append(S);
			return builder.toString();
		}
	}
	

}
