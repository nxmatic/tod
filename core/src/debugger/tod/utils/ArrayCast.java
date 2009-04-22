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
package tod.utils;

import java.io.DataInputStream;

public class ArrayCast
{
	static
	{
		System.loadLibrary("array-cast");
	}

	public static void b2i(byte[] aSrc, int[] aDest)
	{
		b2i(aSrc, 0, aDest, 0, aSrc.length);
	}
	
	
	/**
	 * Copies the data from the source byte array to the dest int array
	 * @param aSrcOffset Offset of the first source array slot.
	 * @param aDestOffset Offset of the first dest array slot.
	 * @param aLength Number of bytes to copy.
	 */
	public static native void b2i(
			byte[] aSrc,
			int aSrcOffset,
			int[] aDest,
			int aDestOffset,
			int aLength);
		
	public static void i2b(int[] aSrc, byte[] aDest)
	{
		i2b(aSrc, 0, aDest, 0, aSrc.length*4);
	}
	
	/**
	 * Copies the data from the source int array to the dest byte array
	 * @param aSrcOffset Offset of the first source array slot.
	 * @param aDestOffset Offset of the first dest array slot.
	 * @param aLength Number of bytes to copy.
	 */
	public static native void i2b(
			int[] aSrc,
			int aSrcOffset,
			byte[] aDest,
			int aDestOffset,
			int aLength);
	
	/**
	 * Reads an int from the given byte array and returns it as an int.
	 * Byte ordering is safe (compatible with {@link DataInputStream}).
	 */
	public static native int ba2i(byte[] aSrc);
	
	/**
	 * Writes an int to the given byte array.
	 * WARNING: NO CHECK IS PERFORMED ON THE ARRAY (SIZE, NULL...)
	 * Byte ordering is safe (compatible with {@link DataInputStream}).
	 */
	public static native void i2ba(int aValue, byte[] aDest);

	

}
