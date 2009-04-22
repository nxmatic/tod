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
package tod.impl.dbgrid;

import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.common.EventCollector;
import tod.impl.database.structure.standard.ThreadInfo;

/**
 * Event collector for the grid database.
 * Delegates thread registration to the master.
 * @author gpothier
 */
public abstract class GridEventCollector extends EventCollector
{
	private GridMaster itsMaster;
	
	public GridEventCollector(
			RIGridMaster aMaster, 
			IHostInfo aHost, 
			IMutableStructureDatabase aStructureDatabase)
	{
		super(aHost, aStructureDatabase);

		// Only for local master (see #thread). 
		if (aMaster instanceof GridMaster)
		{
			itsMaster = (GridMaster) aMaster;
		}
	}

	@Override
	public void thread(int aThreadId, long aJVMThreadId, String aName)
	{
		if (itsMaster != null)
		{
			ThreadInfo theThread = createThreadInfo(getHost(), aThreadId, aJVMThreadId, aName);
			itsMaster.registerThread(theThread);
		}
		else throw new UnsupportedOperationException("Should have been filtered by master");		
	}

}
