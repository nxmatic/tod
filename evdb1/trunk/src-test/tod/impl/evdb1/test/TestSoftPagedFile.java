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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import tod.impl.evdb1.db.file.ExponentialPageBank;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.SoftPagedFile;
import tod.impl.evdb1.db.file.SoftPagedFile.SoftPage;
import tod.impl.evdb1.db.file.SoftPagedFile.SoftPageBitStruct;

public class TestSoftPagedFile
{
	@Test public void testRaw() throws FileNotFoundException
	{
		testRaw(4096, 128, 100);
		testRaw(16384, 16, 100);
	}
	
	private void testRaw(int aMaxSize, int aMinSize, int aPagesCount) throws FileNotFoundException
	{
		File theFile = new File("softFile.bin");
		HardPagedFile theHardFile = new HardPagedFile(theFile, aMaxSize);
		SoftPagedFile theSoftFile = new SoftPagedFile(theHardFile, aMinSize);
		
		List<Long> thePageIds = new ArrayList<Long>();
		
		while (aMinSize <= aMaxSize)
		{
			for(int i=0;i<aPagesCount;i++) 
			{
				SoftPage thePage = theSoftFile.create(aMinSize);
				long thePageId = thePage.getPageId();
				thePageIds.add(thePageId);
				
				SoftPageBitStruct theStruct = thePage.asBitStruct();
				int theTotalBits = theStruct.getTotalBits();
				assertTrue("Bad struct size", theTotalBits == aMinSize*8);
				
				theStruct.writeInt(aMinSize, 32);
				while(theStruct.getRemainingBits() >= 64)
				{
					theStruct.writeLong(thePageId, 64);
				}
			}
			
			aMinSize *= 2;
		}
		
		for (Long thePageId : thePageIds)
		{
			SoftPage thePage = theSoftFile.get(thePageId);
			
			SoftPageBitStruct theStruct = thePage.asBitStruct();
			int theTotalBits = theStruct.getTotalBits();
			
			int theSize = theStruct.readInt(32);
			assertTrue("Bad struct size", theTotalBits == theSize*8);
			
			while(theStruct.getRemainingBits() >= 64)
			{
				long theValue = theStruct.readLong(64);
				assertTrue("Bad value", theValue == thePageId.longValue());
			}
		}
	}
	
	@Test public void testExponential() throws FileNotFoundException
	{
		testExponential(4096, 128, 100000);
	}
	
	private void testExponential(int aMaxSize, int aMinSize, int aTupleCount) throws FileNotFoundException
	{
		File theFile = new File("softFile.bin");
		HardPagedFile theHardFile = new HardPagedFile(theFile, aMaxSize);
		SoftPagedFile theSoftFile = new SoftPagedFile(theHardFile, aMinSize);
		
		for(int i=0;i<aTupleCount;i++)
		{
			
		}
	}
	
}
