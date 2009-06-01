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
package java.tod;

import java.tod.io._IO;
import java.tod.io._SocketChannel;


/**
 * Contains a few flags that indicate the state of the native & java agent,
 * as well as the enabling/disabling of trace capture.
 * 
 * @author gpothier
 */
public class AgentReady
{
	/**
	 * Set to true once the {@link EventCollector} is ready to receive events.
	 */
	public static boolean COLLECTOR_READY = false;
	
	/**
	 * This flag is set to true by the native agent, if it is properly loaded.
	 */
	private static boolean NATIVE_AGENT_LOADED = false;
	
	/**
	 * Whether trace capture is currently enabled.
	 * @see TOD#enableCapture()
	 * @see TOD#disableCapture()
	 */
	public static transient boolean CAPTURE_ENABLED = false;
	
	/**
	 * Called by the native agent.
	 */
	private static void nativeAgentLoaded()
	{
		NATIVE_AGENT_LOADED = true;
	}
	
	/**
	 * Whether the native agent is enabled.
	 */
	public static boolean isNativeAgentLoaded()
	{
		return NATIVE_AGENT_LOADED;
	}
	
	/**
	 * Called by the native agent when the system is ready to start capturing
	 */
	public static void start()
	{
		// Force loading of native methods.
		_IO.initNatives();
		_SocketChannel.initNatives();
		
		EventCollector.INSTANCE.init();
		TOD.loadInitialCaptureState();
	}
}
