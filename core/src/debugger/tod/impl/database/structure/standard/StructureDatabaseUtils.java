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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import zz.utils.Utils;

/**
 * Utilities for implementing {@link IStructureDatabase}
 * @author gpothier
 */
public class StructureDatabaseUtils
{
	public static ThreadLocal<Boolean> SAVING = new ThreadLocal<Boolean>()
	{
		@Override
		protected Boolean initialValue()
		{
			return false;
		}
	};
	
	public static int getBehaviorId(
			IMutableStructureDatabase aStructureDatabase, 
			String aClassName, 
			String aMethodName, 
			String aMethodSignature)
	{
		IMutableClassInfo theClass = aStructureDatabase.getNewClass(aClassName);
		// TODO: Specifying a non-static method is not really the thing to do,
		// but for now I don't know how to do it correctly (and it does not really matter).
		IMutableBehaviorInfo theBehavior = theClass.getNewBehavior(aMethodName, aMethodSignature, false);
		return theBehavior.getId();
	}
	
	/**
	 * Saves the given database to file.
	 * This method ensures that some of the transient fields (in eg {@link BehaviorInfo}) are saved. 
	 */
	public static void saveDatabase(IStructureDatabase aStructureDatabase, File aFile) throws IOException
	{
		try
		{
			SAVING.set(true);
			Utils.writeObject(aStructureDatabase, aFile);
		}
		finally
		{
			SAVING.set(true);
		}
	}
	
	static boolean isSaving()
	{
		return SAVING.get();
	}

}
