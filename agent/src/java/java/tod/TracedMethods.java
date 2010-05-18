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
import java.tod.util._ArrayList;
import java.tod.util._ByteArray;
import java.tod.util._StringBuilder;

import tod2.agent.AgentDebugFlags;
import tod2.agent.MonitoringMode;

/**
 * This class keeps a registry of the monitoring mode of methods.
 * This is used by the instrumentation of method calls:
 * the events generated for a call to a traced method are
 * not the same as those of a non-traced method.
 * The available monitoring modes are defined in {@link MonitoringMode}.
 * @author gpothier
 */
public class TracedMethods
{
	private static final _ByteArray modes = new _ByteArray(16384);
	
	public static volatile int version = 0;
	
	/**
	 * Sets the instrumentation mode for a method  
	 * @param aId The behavior id.
	 * @param aInstrumentationMode One of the INSTRUMENTATION_ constants in {@link MonitoringMode}.
	 * @param aCallMode One of the CALL_ constants in {@link MonitoringMode}.
	 */
	public static final void setMode(int aId, int aInstrumentationMode, int aCallMode)
	{
		int theMode = getMode(aId);
		
		if (aInstrumentationMode >= 0) 
			theMode = (byte) ((theMode & ~MonitoringMode.MASK_INSTRUMENTATION) | aInstrumentationMode);
		if (aCallMode >= 0) 
			theMode = (byte) ((theMode & ~MonitoringMode.MASK_CALL) | aCallMode);

		setMode(aId, (byte) theMode);
	}
	
	public static final void setMode(int aId, byte aMode)
	{
		modes.set(aId, aMode);
		if (AgentDebugFlags.EVENT_LOG && AgentReady.isStarted()) 
		{
			_StringBuilder theBuilder = new _StringBuilder();
			theBuilder.append("Set instrumentation mode: ");
			theBuilder.append(aId);
			theBuilder.append(" -> ");
			theBuilder.append(MonitoringMode.toString(aMode));
			_IO.out(theBuilder.toString());	
		}
		
		version++;
	}
	
	/**
	 * Returns the instrumentation mode for the given method
	 * @param aId A behavior id
	 * @return One of the constants in {@link MonitoringMode}.
	 */
	public static final int getMode(int aId)
	{
		return modes.get(aId);
	}
	
	/**
	 * Called by instrumented code.
	 * Whether to trace the enveloppe of an out-of-scope method. Also checks dynamic activation 
	 */
	public static final boolean traceEnveloppe(int aId)
	{
		if (! AgentReady.CAPTURE_ENABLED) return false;
		int theMode = getMode(aId);
		int theInstrumentationMode = theMode & MonitoringMode.MASK_INSTRUMENTATION;
		switch(theInstrumentationMode)
		{
		case MonitoringMode.INSTRUMENTATION_FULL:
			throw new TODError("Mode cannot be "+MonitoringMode.toString(theMode));
		
		case MonitoringMode.INSTRUMENTATION_NONE: 
			return false;

		case MonitoringMode.INSTRUMENTATION_ENVELOPPE: 
			return true;
		
		default: throw new TODError("Invalid mode");
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
		int theMode = getMode(aId);
		int theInstrumentationMode = theMode & MonitoringMode.MASK_INSTRUMENTATION;
		switch(theInstrumentationMode)
		{
		case MonitoringMode.INSTRUMENTATION_ENVELOPPE: 
			throw new TODError("Mode cannot be "+MonitoringMode.toString(theMode));
		
		case MonitoringMode.INSTRUMENTATION_NONE: 
			return false;
		
		case MonitoringMode.INSTRUMENTATION_FULL: 
			return true;
		
		default: throw new TODError("Invalid mode");
		}
	}
	
	/**
	 * Called by instrumented code.
	 * Returns true if the given behavior is unmonitored.
	 * Does not check dynamic activation (because this method is called inside a method body).
	 */
	public static final boolean traceUnmonitored(int aId)
	{
		int theMode = getMode(aId);
		int theCallMode = theMode & MonitoringMode.MASK_CALL;

		switch(theCallMode)
		{
		case MonitoringMode.CALL_MONITORED: 
			return false;
		
		case MonitoringMode.CALL_UNKNOWN:
		case MonitoringMode.CALL_UNMONITORED:
			return true;
		
		default: throw new TODError("Invalid mode");
		}
	}

	/**
	 * Simply returns the trace enabled flag.
	 */
	public static final boolean traceEnabled()
	{
		return AgentReady.CAPTURE_ENABLED;
	}
}
