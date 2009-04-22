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


/**
 * Represents an in-construction {@link IClassInfo}.
 * @author gpothier
 */
public interface IMutableClassInfo extends IClassInfo, IMutableLocationInfo
{
	/**
	 * Sets up the basic information about this class.
	 * This operation can be performed only once (and throws
	 * an exception if it is called more than once).
	 */
	public void setup(
			boolean aIsInterface,
			boolean aIsInScope,
			String aChecksum, 
			IClassInfo[] aInterfaces,
			IClassInfo aSuperclass);
	
	/**
	 * Sets up the bytecode information of this class.
	 */
	public void setBytecode(byte[] aBytecode, byte[] aOriginalBytecode);
	
	/**
	 * Sets the SMAP of the class (JSR 45).
	 * @see IClassInfo#getSMAP()
	 */
	public void setSMAP(String aSMAP);
	
	/**
	 * This method either creates a new uninitialized behavior, or
	 * returns the behavior of the specified name/descriptor.
	 * If the behavior is created it is automatically assigned an id and added
	 * to the database.
	 * @param aDescriptor The descriptor (signature) of the behavior. 
	 * For now this is the ASM-provided descriptor.
	 * @param aStatic Whether the ehavior is a static one.
	 */
	public IMutableBehaviorInfo getNewBehavior(String aName, String aDescriptor, boolean aStatic);
	
	/**
	 * Adds a new behavior with a specific id.
	 * This is for platforms where ids are not assigned by the structure
	 * database (eg. python).
	 * Othwerwise, use {@link #getNewBehavior(String, String, boolean)}.
	 * @param aStatic Whether the ehavior is a static one.
	 */
	public IMutableBehaviorInfo addBehavior(int aId, String aName, String aDescriptor, boolean aStatic);
	
	/**
	 * This method either creates a new uninitialized field, or 
	 * returns the field that has the specified name.
	 * if the field is created it is automatically assigned an id and added
	 * to the database.
	 * @param aStatic Whether the field is a static one.
	 */
	public IMutableFieldInfo getNewField(String aName, ITypeInfo aType, boolean aStatic);

	/**
	 * Adds a new field with a specific id.
	 * This is for platforms where ids are not assigned by the structure
	 * database (eg. python).
	 * Othwerwise, use {@link #getNewField(String, ITypeInfo, boolean)}.
	 * @param aStatic Whether the field is a static one.
	 */
	public IMutableFieldInfo addField(int aId, String aName, ITypeInfo aType, boolean aStatic);
	
}
