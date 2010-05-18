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

/**
 * Commands that can be sent by the agent to the database and vice versa.
 * @author gpothier
 */
public class Command
{
	public static final byte BASE = 100;
	
	/**
	 * This command flushes all buffered events and indexes.
	 * args: none
	 * return:
	 *  number of flushed events: int
	 */
	public static final byte DBCMD_FLUSH = BASE+1;
	
	/**
	 * This command clears the database.
	 * args: none
	 * return: none
	 */
	public static final byte DBCMD_CLEAR = BASE+2;
	
	/**
	 * This command notifies the database that this VM is ending.
	 */
	public static final byte DBCMD_END = BASE+3;
	
	
	/**
	 * Informs the database about the state of trace capture.
	 * This event is sent periodically.
	 * args: isEnabled (boolean as byte).
	 */
	public static final byte DBEV_CAPTURE_ENABLED = BASE+4;
	
	/**
	 * Tells the agent to enable/disable trace capture.
	 * args: boolean(byte) aEnable
	 * return: none 
	 */
	public static final byte AGCMD_ENABLECAPTURE = BASE+5;
}
