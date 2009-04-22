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

import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ITypeInfo;


/**
 * Information for types that are not known o the instrumenter.
 * @author gpothier
 */
public class UnknownTypeInfo extends TypeInfo
{
	public UnknownTypeInfo(IShareableStructureDatabase aDatabase, int aId, String aName)
	{
		super(aDatabase, aId, aName);
	}
	
	public String getJvmName()
	{
		return "Unknown!";
	}

	public int getSize()
	{
		return 1;
	}

	public boolean isArray()
	{
		return false;
	}

	public boolean isPrimitive()
	{
		return false;
	}

	public boolean isVoid()
	{
		return false;
	}

	public ITypeInfo createUncertainClone()
	{
		return this;
	}

	public Object getDefaultInitialValue()
	{
		return null;
	}
}
