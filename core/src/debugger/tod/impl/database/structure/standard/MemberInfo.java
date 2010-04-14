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
package tod.impl.database.structure.standard;

import tod.core.ILogCollector;
import tod.core.database.structure.Access;
import tod.core.database.structure.IMemberInfo;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ILocationInfo.ISerializableLocationInfo;
import tod.impl.bci.asm2.BCIUtils;


/**
 * Aggregates the information a {@link ILogCollector collector}
 * receives about a type member (method, constructor, field).
 * @author gpothier
 */
public abstract class MemberInfo extends LocationInfo 
implements IMemberInfo, ISerializableLocationInfo
{
	private static final long serialVersionUID = 1781954680024875732L;

	/**
	 * We keep the type id instead of actual type in order to simplify
	 * the handling of remote structure databases.
	 */
	private final int itsDeclaringTypeId;
	
	/**
	 * Access flags (public, private, static, native, etc) of the method.
	 * The values are those of ASM (eg. Opcodes.ACC_ABSTRACT).
	 */
	private int itsAccessFlags;
	
	public MemberInfo(
			IShareableStructureDatabase aDatabase, 
			int aId, 
			ITypeInfo aDeclaringType, 
			String aName, 
			int aAccessFlags)
	{
		super(aDatabase, aId, aName);
		itsDeclaringTypeId = aDeclaringType.getId();
		itsAccessFlags = aAccessFlags;
	}
	
	public ITypeInfo getDeclaringType()
	{
		return getDatabase().getType(itsDeclaringTypeId, true);
	}
	
	public boolean updateAccessFlags(int aAccessFlags)
	{
		int theOldFlags = itsAccessFlags;
		itsAccessFlags |= aAccessFlags;
		return theOldFlags != itsAccessFlags;
	}
	
	public boolean isStatic()
    {
    	return BCIUtils.isStatic(itsAccessFlags);
    }
    
    public boolean isAbstract()
    {
    	return BCIUtils.isAbstract(itsAccessFlags);
    }
	
    public boolean isNative()
    {
    	return BCIUtils.isNative(itsAccessFlags);
    }
}
