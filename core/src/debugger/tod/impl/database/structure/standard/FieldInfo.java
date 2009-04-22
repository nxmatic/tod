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
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableFieldInfo;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ITypeInfo;

/**
 * Aggregates the information a {@link ILogCollector collector}
 * receives about a field.
 * @author gpothier
 */
public class FieldInfo extends MemberInfo implements IMutableFieldInfo
{
	private static final long serialVersionUID = 1642825455287392890L;

	/**
	 * We keep the type id instead of actual type in order to simplify
	 * the handling of remote structure databases.
	 */
	private final int itsTypeId;

	public FieldInfo(
			IShareableStructureDatabase aDatabase, 
			int aId, 
			ITypeInfo aDeclaringType, 
			String aName,
			ITypeInfo aType,
			boolean aStatic)
	{
		super(aDatabase, aId, aDeclaringType, aName, aStatic);
		itsTypeId = aType.getId();
	}

	@Override
	public IClassInfo getDeclaringType()
	{
		return (IClassInfo) super.getDeclaringType();
	}
	
	public ITypeInfo getType()
	{
		return getDatabase().getType(itsTypeId, true);
	}

	@Override
	public String toString()
	{
		return "Field ("+getId()+", "+getName()+")";
	}

}
