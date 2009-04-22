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
package tod.test;

import java.tod.ObjectIdentity;
import java.util.ArrayList;

import javax.swing.JPanel;



public class BCITest
{
	public static void main(String[] args)
	{
		new JPanel();
		BCITest theTest = new BCITest(50, 123);
		int k = theTest.ex(0);
		long j = 90;
		
		
		theTest.foo(1, 2, 0.5f, 452.1, "ro");
		theTest.foo(10, 20, 10.5f, 4520.1, "RO");
		print ("toto");
		print ("titi");
		print (new ArrayList());
		print (theTest);
		print (theTest);
		theTest.foo(1, 2, 0.5f, 452.1, "ru");
		theTest.tcf(0);
		theTest.tcf(1);
		
		throw new RuntimeException();
	}
	
	private int itsInt;
	private long itsLong;
	
	public BCITest(int aI, long aL)
	{
		itsInt = aI;
		itsLong = aL;
	}

	public void tcf(int i)
	{
		try
		{
			if (i == 0) return;
			else System.out.println(i);
		}
		finally
		{
			System.out.println("Finally"+itsInt);
		}
		System.out.println("finish"+itsLong);
	}
	
	public static void print (Object aObject)
	{
		System.out.println("Object: "+ObjectIdentity.get(aObject));
	}
	
	private void foo (int i, long l, float f, double d, String s)
	{
		System.out.println(""+i+" "+l+" "+f+" "+d+" "+s);
	}
	
	private int ex(int n)
	{
		return 100/n;
	}
}
