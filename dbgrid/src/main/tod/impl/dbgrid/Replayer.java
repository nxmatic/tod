/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.dbgrid;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import tod.Util;
import tod.agent.LowLevelEventType;
import tod.core.ILogCollector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IStructureDatabase;
import tod.core.transport.EventInterpreter;
import tod.core.transport.LogReceiver;
import tod.core.transport.LowLevelEventReader;
import tod.core.transport.PacketProcessor;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.utils.TODUtils;
import zz.utils.Utils;
import zz.utils.srpc.SRPCRegistry;

/**
 * Replays event stored in a file by {@link LogReceiver}
 * @author gpothier
 */
public class Replayer extends PacketProcessor
{
	private final TODConfig itsConfig;
	private final EventInterpreter itsInterpreter;
	
	public Replayer(TODConfig aConfig, IStructureDatabase aStructureDatabase, ILogCollector aCollector)
	{
		itsConfig = aConfig;
		itsInterpreter = new EventInterpreter(aStructureDatabase, aCollector);
	}
	
	public void replay()
	{
		try
		{
			File theFile = new File(itsConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");
			
			readPackets(
					new DataInputStream(new BufferedInputStream(new FileInputStream(theFile), 64*1024*1024)), 
					true);
			
			processFlush();
			
			TODUtils.log(0, "Trace imported.");

		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}

	@Override
	protected void processEvent(int aThreadId, LowLevelEventType aType, DataInput aStream)
			throws IOException
	{
		LowLevelEventReader.readEvent(aThreadId, aType, aStream, itsInterpreter);
	}

	@Override
	protected void processClear()
	{
	}

	@Override
	protected void processEnd()
	{
	}

	@Override
	protected int processFlush()
	{
		return 0;
	}
	
	@Override
	protected void processEvCaptureEnabled(boolean aEnabled)
	{
	}

	public static void main(String[] args) throws Exception
	{
		TODConfig theConfig = new TODConfig();
		
		StructureDatabase theStructureDatabase = (StructureDatabase) Utils.readObject(
				new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db.raw"));
		
		theStructureDatabase.reown();
		
		SRPCRegistry theRegistry = Util.getLocalSRPCRegistry();
		GridMaster theMaster = DBGridUtils.setupLocalMaster(theRegistry, theConfig, theStructureDatabase);

		Replayer theReplayer = new Replayer(
				theMaster.getConfig(), 
				theMaster.getStructureDatabase(),
				theMaster._getCollector());
		
		theReplayer.replay();
	}

}
