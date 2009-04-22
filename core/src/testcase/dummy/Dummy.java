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
package dummy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

public class Dummy
{
	public static void main(String[] args) throws InterruptedException
	{
		System.out.println("Dummy");
//		try
//		{
//			if (true) throw new RuntimeException("Plop");
//		}
//		catch (RuntimeException e)
//		{
//			Thread.sleep(100);
//			System.exit(0);
//		}
		int j;
		
		long t0 = System.currentTimeMillis();
		
		Object[] theObjects = new Object[100];
		for(int i=0;i<theObjects.length;i++) theObjects[i] = new Object();
		
		Random theRandom = new Random(0);
		for(int i=0;i<2000000;i++)
		{
			j = i*2;
			foo(theObjects[theRandom.nextInt(theObjects.length)], j);
			if (i % 1000000 == 0) System.out.println(i);
		}
		
		long t1 = System.currentTimeMillis();
		System.out.println("Time: "+(t1-t0));
		
		Thread.sleep(1000000000);
	}
	
	public static int foo(Object o, long b)
	{
		long c = o.hashCode()+b;
		return (int)(c/2);
	}
}
