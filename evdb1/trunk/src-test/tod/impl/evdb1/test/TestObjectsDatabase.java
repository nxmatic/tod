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
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import tod.impl.dbgrid.db.ObjectsDatabase.Decodable;
import tod.impl.evdb1.db.ObjectsDatabase1;
import zz.utils.Utils;

public class TestObjectsDatabase
{
	private static final int COUNT = 1000000;
	
	@Test public void test()
	{
		File theFile = new File("objects.bin");
		theFile.delete();
		ObjectsDatabase1 theDatabase = new ObjectsDatabase1(null, theFile);
		
		Random theRandom = new Random(0);
		long theId = 1;
		for (int i=0;i<COUNT;i++)
		{
			
			theDatabase.store(theId, Utils.encode(new Long(i)), i);
			
			theId += theRandom.nextInt(100)+1;
			
			if (i % 10000 == 0) System.out.println(""+i);
		}
		
		theRandom = new Random(0);
		theId = 1;
		
		for (int i=0;i<COUNT;i++)
		{
			Decodable theDecodable = theDatabase.load(theId);
			Long theLong = (Long) theDecodable.decode();
			Assert.assertTrue(theLong.longValue() == i);
			
			theId += theRandom.nextInt(100)+1;
			
			if (i % 10000 == 0) System.out.println(""+i);
		}
	}
}
