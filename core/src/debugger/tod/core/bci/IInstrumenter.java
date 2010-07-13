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


import tod.core.config.TODConfig;

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
	 * Sets a new config for this instrumenter.
	 * In particular this alters the scope.
	 */
	public void setConfig(TODConfig aConfig);
	
	/**
	 * Aggregates the results of class instrumentation
	 * @author gpothier
	 */
	public static class InstrumentedClass
	{
		public final int id;
		
		/**
		 * Instrumented bytecode
		 */
		public final byte[] bytecode;
		
		/**
		 * Information about the structure of the class
		 * @see MethodGroupManager
		 */
		public final byte[] info;

		public InstrumentedClass(int aId, byte[] aBytecode, byte[] aInfo)
		{
			id = aId;
			bytecode = aBytecode;
			info = aInfo;
		}
	}
}
