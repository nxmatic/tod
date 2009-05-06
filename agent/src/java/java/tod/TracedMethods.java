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

/**
 * This class keeps a registry of traced methods.
 * This is used by the instrumentation of method calls:
 * the events generated for a call to a traced method are
 * not the same as those of a non-traced method.
 * @author gpothier
 */
public class TracedMethods
{
	/**
	 * Monitoring mode constant meaning a method does not emit any event 
	 */
	public static final int NONE = 0;
	
	/**
	 * Monitoring mode constant meaning a method emits entry and exit events
	 */
	public static final int ENVELOPPE = 1;
	
	/**
	 * Monitoring mode constant meaning a method emits all events 
	 */
	public static final int FULL = 2;
	
	/**
	 * Monitoring mode constant meaning the method has a special monitoring mode
	 * specified separately (not used yet). 
	 */
	public static final int SPECIAL = 3;
	
	private static _BitSet traced = new _BitSet();
	
	/**
	 * Sets the monitoring mode for a method  
	 * @param aId The behavior id.
	 * @param aMode One of {@link #FULL}, {@link #ENVELOPPE} or {@link #NONE}
	 */
	public static final void setMode(int aId, int aMode)
	{
		traced.set(aId*2 + 0, (aMode & 0x1) != 0);
		traced.set(aId*2 + 1, (aMode & 0x2) != 0);
	}
	
	/**
	 * Returns the monitoring mode for the given method
	 * @param aId A behavior id
	 * @return One of the monitoring mode constants.
	 */
	public static final int getMode(int aId)
	{
		return (traced.get(aId*2 + 0) ? 0x1 : 0x0) | (traced.get(aId*2 + 1) ? 0x2 : 0x0);
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
		case FULL: throw new Error("Mode cannot be FULL");
		case SPECIAL: throw new Error("Mode cannot be SPCIAL");
		case NONE: return false;
		case ENVELOPPE: return true;
		default: throw new Error("Invalid mode");
		}
	}
	
	/**
	 * Called by instrumented code.
	 * Whether to trace the code of an in-of-scope method. Also checks dynamic activation 
	 */
	public static final boolean traceFull(int aId)
	{
		if (! AgentReady.CAPTURE_ENABLED) return false;
		switch(getMode(aId))
		{
		case ENVELOPPE: throw new Error("Mode cannot be ENVELOPPE");
		case SPECIAL: throw new Error("Mode cannot be SPCIAL");
		case NONE: return false;
		case FULL: return true;
		default: throw new Error("Invalid mode");
		}
	}
}
