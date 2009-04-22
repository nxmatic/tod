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
package tod.core.transport;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tod.agent.LowLevelEventType;
import tod.core.ILogCollector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.database.structure.standard.HostInfo;

/**
 * A {@link LogReceiver} that uses an {@link EventInterpreter} to transform low-level events into
 * high-level events. Leaves the responsibility of processing high-level events
 * and value packets to subclasses.
 * @author gpothier
 */
public class CollectorLogReceiver extends LogReceiver
{
	private final ILogCollector itsCollector;
	private final EventInterpreter itsInterpreter;
	
	public CollectorLogReceiver(
			TODConfig aConfig,
			HostInfo aHostInfo, 
			InputStream aInStream, 
			OutputStream aOutStream, 
			boolean aStart,
			IStructureDatabase aStructureDatabase,
			ILogCollector aCollector)
	{
		super(aConfig, aStructureDatabase, aHostInfo, aInStream, aOutStream, false);
		itsCollector = aCollector;
		itsInterpreter = new EventInterpreter(aStructureDatabase, itsCollector);
		if (aStart) start();
	}
	
	public ILogCollector getCollector()
	{
		return itsCollector;
	}

	@Override
	protected void processEvent(int aThreadId, LowLevelEventType aType, DataInput aStream) throws IOException
	{
		LowLevelEventReader.readEvent(aThreadId, aType, aStream, itsInterpreter);
	}
	

	@Override
	protected void processClear()
	{
		itsCollector.clear();
	}

	@Override
	protected int processFlush()
	{
		return itsCollector.flush();
	}

	@Override
	protected void processEvCaptureEnabled(boolean aEnabled)
	{
	}
}
