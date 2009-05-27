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

import java.tod.util._BitSet;
import java.tod.util._ByteArray;
import java.tod.util._IntArray;

import tod.agent.MonitoringMode;

/**
 * This class keeps a registry of traced methods.
 * This is used by the instrumentation of method calls:
 * the events generated for a call to a traced method are
 * not the same as those of a non-traced method.
 * The available monitoring modes are defined in {@link MonitoringMode}.
 * @author gpothier
 */
public class TracedMethods
{
	private static final boolean USE_BITSET = false;
	private static _BitSet tracedB = null;
	private static _ByteArray tracedA = null;
	
	/**
	 * Sets the monitoring mode for a method  
	 * @param aId The behavior id.
	 * @param aMode One of the constants in {@link MonitoringMode}.
	 */
	public static final void setMode(int aId, int aMode)
	{
		if (USE_BITSET)
		{
			if (tracedB == null) tracedB = new _BitSet();
			tracedB.set(aId*2 + 0, (aMode & 0x1) != 0);
			tracedB.set(aId*2 + 1, (aMode & 0x2) != 0);
		}
		else
		{
			if (tracedA == null) tracedA = new _ByteArray(16384);
			tracedA.set(aId, (byte) aMode);
		}
	}
	
	/**
	 * Returns the monitoring mode for the given method
	 * @param aId A behavior id
	 * @return One of the constants in {@link MonitoringMode}.
	 */
	public static final int getMode(int aId)
	{
		if (USE_BITSET)
		{
			if (tracedB == null) return MonitoringMode.NONE;
			return (tracedB.get(aId*2 + 0) ? 0x1 : 0x0) | (tracedB.get(aId*2 + 1) ? 0x2 : 0x0);
		}
		else
		{
			if (tracedA == null) return MonitoringMode.NONE;
			return tracedA.get(aId);
		}
	}
	
	/**
	 * Called by instrumented code.
	 * Whether to trace the enveloppe of an out-of-scope method. Also checks dynamic activation 
	 */
	public static final boolean traceEnveloppe(int aId)
	{
		if (! AgentReady.CAPTURE_ENABLED) return false;
		switch(getMode(aId))
		{
		case MonitoringMode.FULL: throw new Error("Mode cannot be FULL");
		case MonitoringMode.SPECIAL: throw new Error("Mode cannot be SPCIAL");
		case MonitoringMode.NONE: return false;
		case MonitoringMode.ENVELOPPE: return true;
		default: throw new Error("Invalid mode");
		}
	}
	
	/**
	 * Called by instrumented code.
	 * Whether to trace the code of an in-of-scope method. 
	 * Also checks dynamic activation 
	 */
	public static final boolean traceFull(int aId)
	{
		if (! AgentReady.CAPTURE_ENABLED) return false;
		switch(getMode(aId))
		{
		case MonitoringMode.ENVELOPPE: throw new Error("Mode cannot be ENVELOPPE");
		case MonitoringMode.SPECIAL: throw new Error("Mode cannot be SPCIAL");
		case MonitoringMode.NONE: return false;
		case MonitoringMode.FULL: return true;
		default: throw new Error("Invalid mode");
		}
	}
	
	/**
	 * Called by instrumented code.
	 * Returns true if the given behavior is unmonitored.
	 * Does not check dynamic activation (because this method is called inside a method body).
	 */
	public static final boolean traceUnmonitored(int aId)
	{
		switch(getMode(aId))
		{
		case MonitoringMode.FULL: 
		case MonitoringMode.ENVELOPPE: 
		case MonitoringMode.SPECIAL: return false;
		case MonitoringMode.NONE: return true;
		default: throw new Error("Invalid mode");
		}
	}
}
