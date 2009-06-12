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
package tod2.agent;

import java.tod.util._ArrayList;

public class AgentUtils
{
	public static String formatTimestamp(long aTimestamp)
	{
//		aTimestamp >>>= AgentConfig.TIMESTAMP_ADJUST_SHIFT;
		
		long theMicros = aTimestamp/1000;
		aTimestamp -= theMicros*1000;
		
		long theMillis = theMicros/1000;
		theMicros -= theMillis*1000;
		
		long theSeconds = theMillis/1000;
		theMillis -= theSeconds*1000;
		
		long theMinutes = theSeconds/60;
		theSeconds -= theMinutes*60;
		
		long theHours = theMinutes/60;
		theMinutes -= theHours*60;
		
		long theDays = theHours/24;
		theHours -= theDays*24;
		
		long theYears = theDays/365;
		theDays -= theYears*365;
		
		boolean theStarted = false;
		StringBuilder theBuilder = new StringBuilder();
		if (theYears > 0) 
		{
			theBuilder.append(theYears+"y ");
			theStarted = true;
		}
		
		if (theStarted || theDays > 0)
		{
			theBuilder.append(String.format("%03dd ", theDays));
			theStarted = true;
		}
		
		if (theStarted || theHours > 0)
		{
			theBuilder.append(String.format("%02dh ", theHours));
			theStarted = true;
		}
		
		if (theStarted || theMinutes > 0)
		{
			theBuilder.append(String.format("%02dm ", theMinutes));
			theStarted = true;
		}
		
		if (theStarted || theSeconds > 0)
		{
			theBuilder.append(String.format("%02ds ", theSeconds));
			theStarted = true;
		}
		
		theBuilder.append(String.format("%03d.%03d.%03d", theMillis, theMicros, aTimestamp));
		
		return theBuilder.toString();
	}
	
	/**
	 * Reads a boolean from system properties.
	 * Copied from ConfigUtils in TOD.
	 */
	public static boolean readBoolean (String aPropertyName, boolean aDefault)
	{
		String theString = System.getProperty(aPropertyName);
//		_IO.out("[TOD] "+aPropertyName+"="+theString);
		return theString != null ? Boolean.parseBoolean(theString) : aDefault;
	}
	
	/**
	 * Reads an int from system properties.
	 * Copied from ConfigUtils in TOD.
	 */
	public static int readInt (String aPropertyName, int aDefault)
	{
		String theString = System.getProperty(aPropertyName);
//		_IO.out("[TOD] "+aPropertyName+"="+theString);
		return theString != null ? Integer.parseInt(theString) : aDefault;
	}
	
	/**
	 * Reads a long from system properties.
	 * Copied from ConfigUtils in TOD.
	 */
	public static long readLong (String aPropertyName, long aDefault)
	{
		String theString = System.getProperty(aPropertyName);
//		_IO.out("[TOD] "+aPropertyName+"="+theString);
		return theString != null ? Long.parseLong(theString) : aDefault;
	}
	
	/**
	 * Reads a string from system properties.
	 * Copied from ConfigUtils in TOD.
	 */
	public static String readString (String aPropertyName, String aDefault)
	{
		String theString = System.getProperty(aPropertyName);
//		_IO.out("[TOD] "+aPropertyName+"="+theString);
		if (theString != null && theString.length() == 0) return null;
		return theString != null ? theString : aDefault;
	}
	
	/**
	 * Reads a size in bytes. Commonly used size suffixes can be used:
	 * k for kilo, m for mega, g for giga
	 * Copied from ConfigUtils in TOD.
	 */
	public static long readSize (String aPropertyName, String aDefault)
	{
		String theString = readString(aPropertyName, aDefault);
//		_IO.out("[TOD] "+aPropertyName+"="+theString);
		return readSize(theString);
	}

	/**
	 * Copied from ConfigUtils in TOD.
	 */
	public static long readSize(String aSize)
	{
		long theFactor = 1;
		if (aSize.endsWith("k")) theFactor = 1024;
		else if (aSize.endsWith("m")) theFactor = 1024*1024;
		else if (aSize.endsWith("g")) theFactor = 1024*1024*1024;
		if (theFactor != 1) aSize = aSize.substring(0, aSize.length()-1);
		
		return Long.parseLong(aSize)*theFactor;
	}
	
	public static int getJvmMinorVersion()
	{
		return getJvmMinorVersion(System.getProperty("java.version"));
	}
	
	public static int getJvmMinorVersion(String aVersionString)
	{
        String[] theVersionComponents = split(aVersionString, '.');
        int theMajor = Integer.parseInt(theVersionComponents[0]);
        int theMinor = Integer.parseInt(theVersionComponents[1]);
        
        if (theMajor != 1) throw new RuntimeException("JVM version not supported: "+aVersionString);
        
        return theMinor;
	}
	
	public static String[] split(String aString, char aChar)
	{
		_ArrayList<String> theStrings = new _ArrayList<String>();
		StringBuilder theBuilder = new StringBuilder();
		
		for(int i=0;i<aString.length();i++)
		{
			char c = aString.charAt(i);
			if (c == aChar)
			{
				if (theBuilder.length() > 0)
				{
					theStrings.add(theBuilder.toString());
					theBuilder = new StringBuilder();
				}
			}
			else
			{
				theBuilder.append(c);
			}
		}
		
		if (theBuilder.length() > 0)
		{
			theStrings.add(theBuilder.toString());
			theBuilder = new StringBuilder();
		}

		return theStrings.toArray(new String[theStrings.size()]);
	}
	
	/**
	 * Loads the given class.
	 * Only for JDK1.4
	 */
	public static void loadClass(String aName)
	{
		try
		{
			Class.forName(aName);
		}
		catch (ClassNotFoundException e)
		{
			System.err.println("[TOD] Info: could not preload "+aName+". Consider disabling class preloading in the configuration.");
		}
	}
}
