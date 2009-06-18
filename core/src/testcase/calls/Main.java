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
package calls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Main
{
	private static int toto = 9;
	private int titi = 10;
	
	public static void staticA() throws Throwable
	{
		System.out.println("hoho");
		throw new Throwable();
	}
	
	public void compare()
	{
		List<String> theList = new ArrayList<String>();
		Object o = theList;
		theList.add("A");
		theList.add("B");
		Collections.sort(theList, new A());
		
		synchronized (this)
		{
			String s = o.toString();
		}
	}
	
	public static void main(String[] args)
	{
		for (int i=0;i<50;i++)
		{
			try
			{
				staticA();
			}
			catch (Throwable e)
			{
			}
			Main theMain = new Main();
			theMain.compare();
		}
	}
	
	public static void zap(Zoup z)
	{
		z.foo();
	}
	
	private static class A implements Comparator<String>
	{
		public int compare(String aO1, String aO2)
		{
			return aO1.compareToIgnoreCase(aO2);
		}
	}
	
	public static class Zip
	{
		public void foo()
		{
			System.out.println(getClass());
		}
	}
	
	public static class Zoup extends Zip
	{
	}
}
