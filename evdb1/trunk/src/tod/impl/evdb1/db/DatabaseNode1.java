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
package tod.impl.evdb1.db;

import java.io.File;

import tod.core.ILogCollector;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.database.structure.standard.ThreadInfo;
import tod.impl.dbgrid.GridMaster;
import tod.impl.dbgrid.RIGridMaster;
import tod.impl.dbgrid.db.DatabaseNode;
import tod.impl.dbgrid.db.EventDatabase;
import tod.impl.dbgrid.db.ObjectsDatabase;
import tod.impl.evdb1.GridEventCollector1;
import tod.impl.evdb1.db.file.HardPagedFile;

public class DatabaseNode1 extends DatabaseNode 
{
	@Override
	protected synchronized void initDatabase() 
	{
		HardPagedFile.clearCache(); //TODO: only clear pages of current database
		super.initDatabase();
	}
	
	@Override
	protected EventDatabase createEventDatabase(File aDirectory)
	{
		File theFile = new File(aDirectory, "events.bin");
		theFile.delete();

		return new EventDatabase1(getStructureDatabase(), getNodeId(), theFile);
	}

	@Override
	protected ObjectsDatabase createObjectsDatabase(File aDirectory, String aName)
	{
		File theFile = new File(aDirectory, "objects-"+aName+".bin");
		theFile.delete();
		
		return new ObjectsDatabase1(getStructureDatabase(), theFile);
	}
	
	@Override
	public ILogCollector createLogCollector(IHostInfo aHostInfo)
	{
		return new GridEventCollector1(getMaster(), aHostInfo, getStructureDatabase(), this);
	}
}
