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

public class ConfigUtils
{
	/**
	 * Reads a boolean from system properties.
	 */
	public static boolean readBoolean (String aPropertyName, boolean aDefault)
	{
		String theString = System.getProperty(aPropertyName);
		TODUtils.log(1,"[TOD] "+aPropertyName+"="+theString);
		return theString != null ? Boolean.parseBoolean(theString) : aDefault;
	}
	
	/**
	 * Reads an int from system properties.
	 */
	public static int readInt (String aPropertyName, int aDefault)
	{
		String theString = System.getProperty(aPropertyName);
		TODUtils.log(1,"[TOD] "+aPropertyName+"="+theString);
		return theString != null ? Integer.parseInt(theString) : aDefault;
	}
	
	/**
	 * Reads a long from system properties.
	 */
	public static long readLong (String aPropertyName, long aDefault)
	{
		String theString = System.getProperty(aPropertyName);
		TODUtils.log(1,"[TOD] "+aPropertyName+"="+theString);
		return theString != null ? Long.parseLong(theString) : aDefault;
	}
	
	/**
	 * Reads a string from system properties.
	 */
	public static String readString (String aPropertyName, String aDefault)
	{
		String theString = System.getProperty(aPropertyName);
		TODUtils.log(1,"[TOD] "+aPropertyName+"="+theString);
		if (theString != null && theString.length() == 0) return null;
		return theString != null ? theString : aDefault;
	}
	
	/**
	 * Reads a size in bytes. Commonly used size suffixes can be used:
	 * k for kilo, m for mega, g for giga
	 */
	public static long readSize (String aPropertyName, String aDefault)
	{
		String theString = readString(aPropertyName, aDefault);
		TODUtils.log(1,"[TOD] "+aPropertyName+"="+theString);
		return readSize(theString);
	}
	
	public static long readSize(String aSize)
	{
		long theFactor = 1;
		if (aSize.endsWith("k")) theFactor = 1024;
		else if (aSize.endsWith("m")) theFactor = 1024*1024;
		else if (aSize.endsWith("g")) theFactor = 1024*1024*1024;
		if (theFactor != 1) aSize = aSize.substring(0, aSize.length()-1);
		
		return Long.parseLong(aSize)*theFactor;
	}
	

}
