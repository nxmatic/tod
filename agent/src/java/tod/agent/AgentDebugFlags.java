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
package tod.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.tod.io._IO;

/**
 * This class groups several flags that are used to
 * disable certain features for testing purposes.
 * 
 * @author gpothier
 */
public class AgentDebugFlags
{
	/**
	 * Whether profile data should be collected.
	 */
	public static final boolean COLLECT_PROFILE = false;
	
	/**
	 * If true, the {@link EventInterpreter} prints all the events it receives
	 */
	public static final boolean COLLECTOR_LOG = false;
	
	/**
	 * Stream to which the {@link EventInterpreter} sends debug info.
	 * Default is System.out
	 */
	public static final PrintStream EVENT_INTERPRETER_PRINT_STREAM =
		System.out;
//		createStream("eventInterpreter-" + AgentConfig.getHostName()+".log");

	/**
	 * Causes the socket collector to not send events
	 */
	public static final boolean DISABLE_EVENT_SEND = false;

	/**
	 * Causes the high level collectors to ignore all events
	 */
	public static final boolean COLLECTOR_IGNORE_ALL = false;

	/**
	 * Enables logging of long packets processing.
	 */
	public static final boolean TRANSPORT_LONGPACKETS_LOG = false;


	private static PrintStream createStream(String aName)
	{
		try
		{
			File theFile = new File(aName);
			theFile.delete();
			File theParentFile = theFile.getParentFile();
			if (theParentFile != null) theParentFile.mkdirs();
//			_IO.out(theFile.getAbsolutePath());
			return new PrintStream(new FileOutputStream(theFile));
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	

	static
	{
		if (DISABLE_EVENT_SEND == true) _IO.err("******* Warning: DISABLE_EVENT_SEND (AgentDebugFlags)");
		if (COLLECTOR_IGNORE_ALL == true) _IO.err("******* Warning: COLLECTOR_IGNORE_ALL (AgentDebugFlags)");
		if (TRANSPORT_LONGPACKETS_LOG == true) _IO.err("******* Warning: TRANSPORT_LONGPACKETS_LOG (AgentDebugFlags)");
	}
}
