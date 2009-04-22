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
package tod.impl.evdb1.messages;

import tod.agent.Output;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.common.event.OutputEvent;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.db.Indexes;

public class GridOutputEvent extends BitGridEvent
{
	private static final long serialVersionUID = 2432106275871615061L;
	
	private String itsData;
	private Output itsOutput;
	
	public GridOutputEvent(IStructureDatabase aStructureDatabase)
	{
		super(aStructureDatabase);
	}


	public GridOutputEvent(			
			IStructureDatabase aStructureDatabase,
			String aData, 
			Output aOutput)
	{
		super(aStructureDatabase);
		set(aData, aOutput);
	}

	public void set(String aData, Output aOutput)
	{
		itsData = aData;
		itsOutput = aOutput;
	}
	
	@Override
	public ILogEvent toLogEvent(GridLogBrowser aBrowser)
	{
		OutputEvent theEvent = new OutputEvent(aBrowser);
		initEvent(aBrowser, theEvent);
		theEvent.setData(getData());
		theEvent.setOutput(getOutput());
		return theEvent;
	}
	
	@Override
	public MessageType getEventType()
	{
		return MessageType.OUTPUT;
	}

	public String getData()
	{
		return itsData;
	}

	public Output getOutput()
	{
		return itsOutput;
	}
	
	@Override
	public void index(Indexes aIndexes, long aPointer)
	{
		super.index(aIndexes, aPointer);
	}
	
	@Override
	public String toString()
	{
		return String.format(
				"%s (d: %d, o: %s, %s)",
				getEventType(),
				itsData,
				itsOutput,
				toString0());
	}

}
