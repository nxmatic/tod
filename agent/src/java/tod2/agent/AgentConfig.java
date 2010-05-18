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

import tod2.agent.util.BitUtilsLite;


/**
 * Configuration of the agent in the target VM. 
 * @author gpothier
 */
public class AgentConfig
{
	/**
	 * Signature for connections from the native side.
	 */
	public static final int CNX_NATIVE = 0x3a71be0;
	
	/**
	 * Signature for connections from the java side.
	 */
	public static final int CNX_JAVA = 0xcafe0;
	
	public static final String PARAM_COLLECTOR_HOST = "collector-host";
	public static final String PARAM_COLLECTOR_PORT = "collector-port";
	public static final String PARAM_CAPTURE_AT_START = "capture-at-start";
	
	/**
	 * This parameter defines the name of the host the agent runs on.
	 */
	public static final String PARAM_CLIENT_NAME = "client-name";
	
	/**
	 * Number of bits used to represent the host of an event.
	 */
	public static final int HOST_BITS = AgentUtils.readInt("host-bits", 0);
	
	public static final long HOST_MASK = BitUtilsLite.pow2(HOST_BITS)-1;
	
	/**
	 * Size of {@link SocketCollector} buffer. 
	 */
	public static final int COLLECTOR_BUFFER_SIZE = 16384;
	
	/**
	 * Class ids below this value are reserved.
	 */
	public static final int FIRST_CLASS_ID = 100;
}
