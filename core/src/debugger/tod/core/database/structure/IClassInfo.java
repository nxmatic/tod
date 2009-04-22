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
package tod.core.database.structure;

import tod.tools.parsers.smap.SMAPFactory;


/**
 * Represents a Java class or interface.
 * @author gpothier
 */
public interface IClassInfo extends ITypeInfo
{
	/**
	 * Returns the bytecode of the instrumented version of the class.
	 */
	public byte[] getBytecode();
	
	/**
	 * Returns the bytecode of the original version of the class.
	 */
	public byte[] getOriginalBytecode();
	
    /**
     * Returns the value of the source debug extension attribute (JSR 45) of the class,
     * if present. This attribute permits to map different levels of source code to 
     * final generated source code, as in eg. JSPs.
     * The value of this attribute should be parsed by {@link SMAPFactory}.
     */
    public String getSMAP();
	
	/**
	 * Whether this object represents an interface, or a class.
	 */
	public boolean isInterface();
	
	/**
	 * Whether this class is in the instrumentation scope.
	 */
	public boolean isInScope();
	
	/**
	 * Returns the MD5 checksum of the class' original bytecode.
	 */
	public String getChecksum();
	
	/**
	 * Indicates the time at which this particular version
	 * of the class has been loaded into the system.
	 * This is important for cases where class redefinition is used.
	 * TODO: Consolidate this. We should have a map of all currently connected
	 * VMs with the version of the classes they use.
	 * @return A timestamp, as measured by the debugged VM.
	 */
	public long getStartTime();
	
	/**
	 * Returns the superclass of this class.
	 */
	public IClassInfo getSupertype();
	
	/**
	 * Returns all the interfaces directly implemented by this class.
	 * The returned list is immutable.
	 */
	public IClassInfo[] getInterfaces();
	
	/**
	 * Searches a field
	 * @param aName Name of the searched field.
	 * @return The field, or null if not found.
	 */
	public IFieldInfo getField(String aName);

	/**
	 * Searches a behavior according to its signature.
	 */
	public IBehaviorInfo getBehavior(String aName, ITypeInfo[] aArgumentTypes, ITypeInfo aReturnType);

	/**
	 * Returns all the fields of this class (excluding inherited ones).
	 */
	public Iterable<IFieldInfo> getFields();

	/**
	 * Returns all the behaviors of this class (excluding inherited ones).
	 */
	public Iterable<IBehaviorInfo> getBehaviors();
	
	public IClassInfo createUncertainClone();
}