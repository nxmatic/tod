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
package tod.core.bci;

import java.tod.TracedMethods;
import java.util.List;

import tod.core.config.TODConfig;
import tod.impl.bci.asm.SpecialCases;

public interface IInstrumenter
{
    /**
     * Instruments the given class.
     * @param aClassName JVM internal class name (eg. "java/lang/Object")
     * @param aBytecode Original bytecode of the class
     * @param aUseJava14 If true, only Java 1.4 bytecode/APIs can be used.
     * Retroweaver APIs can also be used.
     * @return New bytecode, or null if no instrumentation is performed.
     */
	public InstrumentedClass instrumentClass (String aClassName, byte[] aBytecode, boolean aUseJava14);
	
	/**
	 * Returns all the classes that should be treated as special case.
	 * The set of classes is sent to the agent so that they are not excluded from instrumentation.
	 */
	public Iterable<String> getSpecialCaseClasses();
	
	/**
	 * Changes the current trace working set.
	 * @see TODConfig#SCOPE_TRACE_FILTER
	 */
	public void setTraceWorkingSet(String aWorkingSet);

	/**
	 * Sets the current global working set.
	 * @see TODConfig#SCOPE_GLOBAL_FILTER
	 */
	public void setGlobalWorkingSet(String aWorkingSet);
	
	/**
	 * Aggregates the results of class instrumentation
	 * @author gpothier
	 */
	public static class InstrumentedClass
	{
		/**
		 * Instrumented bytecode
		 */
		public final byte[] bytecode;
		
		/**
		 * List of behaviors whose monitoring mode changes as a result of processing the class.
		 * @see TracedMethods
		 */
		public final List<BehaviorMonitoringMode> modeChanges;

		public InstrumentedClass(byte[] aBytecode, List<BehaviorMonitoringMode> aModeChanges)
		{
			bytecode = aBytecode;
			modeChanges = aModeChanges;
		}
	}
	
	public static class BehaviorMonitoringMode
	{
		public final int behaviorId;
		public final int mode;
		
		public BehaviorMonitoringMode(int aBehaviorId, int aMode)
		{
			behaviorId = aBehaviorId;
			mode = aMode;
		}
	}
}
