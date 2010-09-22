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

import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;

/**
 * Utilities for implementing {@link IStructureDatabase}
 * @author gpothier
 */
public class StructureDatabaseUtils
{
	public static int getBehaviorId(
			IMutableStructureDatabase aStructureDatabase, 
			String aClassName, 
			String aMethodName, 
			String aMethodSignature)
	{
		IMutableClassInfo theClass = aStructureDatabase.getNewClass(aClassName);
		// TODO: Specifying a non-static method is not really the thing to do,
		// but for now I don't know how to do it correctly (and it does not really matter).
		IMutableBehaviorInfo theBehavior = theClass.getNewBehavior(aMethodName, aMethodSignature, 0);
		return theBehavior.getId();
	}
	
	public static int getFieldId(
			IStructureDatabase aStructureDatabase, 
			String aClassName, 
			String aFieldName,
			boolean aFailIfAbsent)
	{
		IClassInfo theClass = aStructureDatabase.getClass(aClassName, aFailIfAbsent);
		if (theClass == null)
		{
			if (aFailIfAbsent) throw new RuntimeException("Class not found: "+aClassName);
			else return -1;			
		}
		
		while(theClass != null)
		{
			IFieldInfo theField = theClass.getField(aFieldName);
			if (theField != null) return theField.getId();
			else theClass = theClass.getSupertype(); 
		}

		if (aFailIfAbsent) throw new RuntimeException("Field not found: "+aClassName+"."+aFieldName);
		else return -1;
	}
	

}
