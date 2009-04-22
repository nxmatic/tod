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

import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ILocationInfo.ISerializableLocationInfo;

/**
 * Note: This class is not a {@link ISerializableLocationInfo}
 * because it must be recreated at the destination rather than passed
 * through the wire.
 * @author gpothier
 */
public class ArrayTypeInfo extends TypeInfo implements IArrayTypeInfo
{
	private static final long serialVersionUID = 1415897267440123250L;
	private final ITypeInfo itsElementType;
	private final int itsDimensions;
	
	public ArrayTypeInfo(IShareableStructureDatabase aDatabase, ITypeInfo aElementType, int aDimensions)
	{
		super(aDatabase, -1, aElementType.getName()+getBrackets(aDimensions));
		itsElementType = aElementType;
		itsDimensions = aDimensions;
	}
	
	public String getJvmName()
	{
		throw new UnsupportedOperationException();
	}
	
	private static String getBrackets(int aDimensions)
	{
		StringBuilder theBuilder = new StringBuilder();
		for(int i=0;i<aDimensions;i++) theBuilder.append("[]");
		return theBuilder.toString();
	}

	public int getDimensions()
	{
		return itsDimensions;
	}

	public ITypeInfo getElementType()
	{
		return itsElementType;
	}

	public int getSize()
	{
		return 1;
	}

	public boolean isArray()
	{
		return true;
	}

	public boolean isPrimitive()
	{
		return false;
	}

	public boolean isVoid()
	{
		return false;
	}
	
	public ArrayTypeInfo createUncertainClone()
	{
		return new ArrayTypeInfo(getDatabase(), getElementType().createUncertainClone(), getDimensions());
	}

	public Object getDefaultInitialValue()
	{
		return null;
	}
}
