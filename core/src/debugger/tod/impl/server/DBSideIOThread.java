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
package tod.impl.server;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import tod.core.config.TODConfig;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.database.structure.standard.StructureDatabaseUtils;
import tod.impl.replay.ThreadReplayer;
import tod.impl.replay.TmpIdManager;
import tod2.agent.Message;
import tod2.agent.io._ByteBuffer;
import zz.utils.Utils;

/**
 * The class that handles the traffic from the debugged VM.
 * @author gpothier
 */
public class DBSideIOThread
{
	private final TODConfig itsConfig;
	private final IStructureDatabase itsDatabase;
	
	private final InputStream itsIn;
	private final OutputStream itsOut;
	
	private final List<ThreadReplayer> itsReplayers = new ArrayList<ThreadReplayer>();
	private final TmpIdManager itsTmpIdManager = new TmpIdManager();
	
	public DBSideIOThread(TODConfig aConfig, IStructureDatabase aDatabase, InputStream aIn, OutputStream aOut)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsIn = aIn;
		itsOut = aOut;
	}

	public void run()
	{
		try
		{
			int thePacketCount = 0;
			
			loop:
			while(true)
			{
				int thePacketType = itsIn.read();
				switch(thePacketType)
				{
				case Message.PACKET_TYPE_THREAD: processThreadPacket(); break;
				case Message.PACKET_TYPE_STRING: processStringPacket(); break;
				case -1: break loop;
				default: throw new RuntimeException("Not handled: "+thePacketType);
				}
				
				thePacketCount++;
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private ThreadReplayer getReplayer(int aThreadId)
	{
		ThreadReplayer theReplayer = Utils.listGet(itsReplayers, aThreadId);
		if (theReplayer == null)
		{
			theReplayer = new ThreadReplayer(itsConfig, itsDatabase, itsTmpIdManager);
			Utils.listSet(itsReplayers, aThreadId, theReplayer);
		}
		return theReplayer;
	}
	
	private void readFully(byte[] aBuffer) throws IOException
	{
		int l = aBuffer.length;
		int c = 0;
		while(c < l) 
		{
			int r = itsIn.read(aBuffer, c, l-c);
			if (r == -1)
			{
				throw new EOFException();
			}
			c += r;
		}
	}
	
	private void processThreadPacket() throws IOException
	{
		int theThreadId = _ByteBuffer.getIntL(itsIn);
		int theLength = _ByteBuffer.getIntL(itsIn);
		
		byte[] theBuffer = new byte[theLength];
		readFully(theBuffer);
		
		getReplayer(theThreadId).replay(_ByteBuffer.wrap(theBuffer));
	}
	
	private void processStringPacket() throws IOException
	{
		long theObjectId = _ByteBuffer.getLongL(itsIn);
		String theString = _ByteBuffer.getString(itsIn);
		
		System.out.println("Got string "+theObjectId+": "+theString);
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		try
		{
			TODConfig theConfig = new TODConfig();
			File theEventsFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");
			File theDbFile = new File(theConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db.raw");
			
			IStructureDatabase theDatabase = StructureDatabaseUtils.loadDatabase(theDbFile);
			
			DBSideIOThread theIOThread = new DBSideIOThread(theConfig, theDatabase, new FileInputStream(theEventsFile), null);
			theIOThread.run();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		Thread.sleep(1000);
		System.err.println("END");
	}
}
