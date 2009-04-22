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

import java.tod.TOD;

public class Dummy2
{
	public static void main(String[] args) throws InterruptedException
	{
		Object o = args;
		if (o instanceof Integer)
		{
			System.out.println("Dummy2.main()");
		}
		for(int i=0;i<10;i++)
		{
			System.out.println(i);
			dummy1();
			System.out.println("Clearing DB...");
			TOD.clearDatabase();
		}
		
		Thread.sleep(1000000000);
	}
	
	public static void dummy1()
	{
		for(int i=0;i<10;i++) 
		{
			foo(i);
		}
	}
	
	public static void foo(int i)
	{
		int j = i*2;
	}
	
	public static class Moo
	{
		private String s;

		public Moo(String aS)
		{
			s = aS;
		}
		
	}
}
