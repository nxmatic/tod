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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.python.modules.synchronize;

import tod.core.database.structure.ObjectId;
import tod.impl.replay2.EventCollector;

public class ObjectWriteSerializeCollector extends EventCollector
{
	private static final String FILENAME = "/home/gpothier/tmp/tod/ow.tod";
	private static DataOutputStream itsStream;
	
	private static void checkFile()
	{
		if (itsStream != null) return;

		try
		{
			File theFile = new File(FILENAME);
			theFile.delete();
			itsStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(theFile)));
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static synchronized void writeWrite(int aThreadId, ObjectId aTarget, int aFieldId)
	{
		try
		{
			itsStream.writeByte(WRITE);
			itsStream.writeInt(aThreadId);
			itsStream.writeLong(aTarget != null ? aTarget.getId() : 0);
			itsStream.writeInt(aFieldId);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static synchronized void writeSync(int aThreadId, long aTimestamp)
	{
		try
		{
			itsStream.writeByte(SYNC);
			itsStream.writeInt(aThreadId);
			itsStream.writeLong(aTimestamp);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	
	private static final int WRITE = 1;
	private static final int SYNC = 2;
	
	private final int itsThreadId;
	
	public ObjectWriteSerializeCollector(int aThreadId)
	{
		checkFile();
		itsThreadId = aThreadId;
	}
	
	@Override
	public void fieldWrite(ObjectId aTarget, int aFieldId)
	{
		writeWrite(itsThreadId, aTarget, aFieldId);
	}
	
	@Override
	public void sync(long aTimestamp)
	{
		writeSync(itsThreadId, aTimestamp);
	}
	
	public static void replay(int aCount, OWSReplayer aReplayer)
	{
		try
		{
			File theFile = new File(FILENAME);
			DataInputStream theStream = new DataInputStream(new BufferedInputStream(new FileInputStream(theFile)));
			while(true)
			{
				byte theMsg = theStream.readByte();
				int theThreadId = theStream.readInt();
				switch(theMsg)
				{
				case WRITE:
					long theId = theStream.readLong();
					int theFieldId = theStream.readInt();
					aReplayer.getCollector(theThreadId).fieldWrite(theId != 0 ? new ObjectId(theId) : null, theFieldId);
					break;
				case SYNC:
					long theTimestamp = theStream.readLong();
					aReplayer.getCollector(theThreadId).sync(theTimestamp);
					break;
				default:
					throw new RuntimeException();
				}
				aCount--;
				if (aCount == 0) break;
			}
		}
		catch(EOFException e)
		{
			return;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static abstract class OWSReplayer
	{
		public abstract EventCollector getCollector(int aThreadId);
	}
}
