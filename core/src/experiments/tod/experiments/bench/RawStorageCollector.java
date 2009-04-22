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
package tod.experiments.bench;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RawStorageCollector extends ISimpleLogCollector
{
	private DataOutputStream itsOutputStream;
	private File itsFile; 

	public RawStorageCollector()
	{
		try
		{
			itsFile = new File("/home/gpothier/tmp/tod-raw.bin");
			if (itsFile.exists()) itsFile.delete();
			itsOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(itsFile), 100000));
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public long getStoredSize()
	{
		try
		{
			itsOutputStream.flush();
			return itsFile.length();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void logBehaviorEnter(long aTid, long aSeq, int aBehaviorId, long aTarget, long[] args)
	{
		try
		{
			itsOutputStream.writeByte(EventType.BEHAVIOR_ENTER.ordinal());
			itsOutputStream.writeLong(aTid);
			itsOutputStream.writeLong(aSeq);
			itsOutputStream.writeLong(time());
			itsOutputStream.writeInt(aBehaviorId);
			itsOutputStream.writeLong(aTarget);
			for (int i = 0; i < args.length; i++)
			{
				long arg = args[i];
				itsOutputStream.writeLong(arg);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized void logBehaviorExit(long aTid, long aSeq, long aRetValue)
	{
		try
		{
			itsOutputStream.writeByte(EventType.BEHAVIOR_EXIT.ordinal());
			itsOutputStream.writeLong(aTid);
			itsOutputStream.writeLong(aSeq);
			itsOutputStream.writeLong(time());
			itsOutputStream.writeLong(aRetValue);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized void logFieldWrite(long aTid, long aSeq, int aFieldId, long aTarget, long aValue)
	{
		try
		{
			itsOutputStream.writeByte(EventType.FIELD_WRITE.ordinal());
			itsOutputStream.writeLong(aTid);
			itsOutputStream.writeLong(aSeq);
			itsOutputStream.writeLong(time());
			itsOutputStream.writeInt(aFieldId);
			itsOutputStream.writeLong(aTarget);
			itsOutputStream.writeLong(aValue);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public synchronized void logVarWrite(long aTid, long aSeq, int aVarId, long aValue)
	{
		try
		{
			itsOutputStream.writeByte(EventType.VAR_WRITE.ordinal());
			itsOutputStream.writeLong(aTid);
			itsOutputStream.writeLong(aSeq);
			itsOutputStream.writeLong(time());
			itsOutputStream.writeInt(aVarId);
			itsOutputStream.writeLong(aValue);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
