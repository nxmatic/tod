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

import java.util.Arrays;

public class ArraysTest
{
	public static void main(String[] args)
	{
		int[] a = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		int[] b = new int[10];
		int[] c = new int[10];
		
		System.out.println("a: "+print(a));
		
		System.arraycopy(a, 1, b, 2, 5);
		System.out.println("b: "+print(b));
		
		System.arraycopy(b, 1, c, 2, 5);
		System.arraycopy(a, 8, c, 6, 2);
		System.out.println("c: "+print(c));
	}
	
	private static String print(int[] aArray)
	{
		StringBuilder theBuilder = new StringBuilder("[");
		for (int i : aArray) theBuilder.append(i+" ");
		theBuilder.append("]");
		return theBuilder.toString();
	}
}
