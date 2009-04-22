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
package tod.tools.recording;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.session.ISession;
import tod.core.session.SessionTypeManager;
import zz.utils.Utils;

public class Replayer
{
	private final File itsFile;
	private final ISession itsSession;
	
	
	public Replayer(File aFile, ISession aSession)
	{
		itsFile = aFile;
		itsSession = aSession;
	}
	
	public void process() throws Exception
	{
		Map<Integer, Object> theObjects = new HashMap<Integer, Object>();
		theObjects.put(1, itsSession);
		
		List<Record> theRecords = load(itsFile);
		
		long t0 = System.currentTimeMillis();
		
		int i=0;
		int theCount = 0;
		for (Record theRecord : theRecords) 
		{
			try
			{
				theRecord.process(theObjects);
				theCount++;
			}
			catch (Throwable e)
			{
				System.err.println("Exception processing record "+i+": "+theRecord);
				e.printStackTrace();
			}
			i++;
		}
		
		long t1 = System.currentTimeMillis();

		Utils.println(
				"Replayed %d records in %d seconds (%d failures).", 
				theRecords.size(), 
				(t1-t0)/1000,
				theRecords.size()-theCount);
	}
	
	private List<Record> load(File aFile) throws Exception
	{
		List<Record> theRecords = new ArrayList<Record>();
		
		ObjectInputStream theStream = new ObjectInputStream(new FileInputStream(aFile));

		while(true)
		{
			try
			{
				Object theObject = theStream.readObject();
				Record theRecord = (Record) theObject;
				theRecords.add(theRecord);
//				System.out.println((theRecords.size()-1)+": "+theRecord);
			}
			catch (EOFException e)
			{
				break;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		Utils.println("Loaded %d records.", theRecords.size());
		
		return theRecords;
	}
	
	public static void main(String[] args) throws Exception
	{
		try
		{
			URI theUri = URI.create(args[1]);
			TODConfig theConfig = new TODConfig();
			ISession theSession = SessionTypeManager.getInstance().createSession(null, theUri, theConfig);

			Replayer theReplayer = new Replayer(new File(args[0]), theSession);
			theReplayer.process();
		}
		finally
		{
//			Thread.sleep(1000);
//			System.exit(0);
		}
	}
}
	