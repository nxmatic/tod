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
	
	private final boolean itsStatic;
	private Access itsAccess;
	
	public MemberInfo(
			IShareableStructureDatabase aDatabase, 
			int aId, 
			ITypeInfo aDeclaringType, 
			String aName, 
			boolean aStatic)
	{
		super(aDatabase, aId, aName);
		itsDeclaringTypeId = aDeclaringType.getId();
		itsStatic = aStatic;
	}
	
	public ITypeInfo getDeclaringType()
	{
		return getDatabase().getType(itsDeclaringTypeId, true);
	}
	
	public boolean isStatic()
	{
		return itsStatic;
	}

	public Access getAccess()
	{
		return itsAccess;
	}

	public void setAccess(Access aAccess)
	{
		itsAccess = aAccess;
	}
}
