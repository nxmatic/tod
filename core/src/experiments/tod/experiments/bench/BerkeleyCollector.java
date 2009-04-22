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

import java.io.File;

import zz.utils.ArrayStack;
import zz.utils.Stack;

import com.sleepycat.bind.tuple.TupleBase;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.bind.tuple.TupleTupleKeyCreator;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

public class BerkeleyCollector extends ISimpleLogCollector
{
	private static final String DIR_NAME = "/home/gpothier/tmp/dbbench";
	
	private Database itsEvents;
	private SecondaryDatabase itsTimestampsIndex;
	private SecondaryDatabase itsFieldIndex;
	private SecondaryDatabase itsVarIndex;
	private SecondaryDatabase itsBehaviorIndex;
	private SecondaryDatabase itsParentIndex;
	
	private DatabaseEntry itsValueEntry = new DatabaseEntry();
	private DatabaseEntry itsKeyEntry = new DatabaseEntry();
	
	private TupleOutput itsValueOutput = new TupleOutput();
	private TupleOutput itsKeyOutput = new TupleOutput();
	private Environment itsEnvironment;
	
	private Stack<EventKey> itsStack = new ArrayStack<EventKey>();

	public BerkeleyCollector()
	{
		try
		{
			Process theProcess = Runtime.getRuntime().exec("rm -rf "+DIR_NAME);
			theProcess.waitFor();

			File theBaseDir = new File(DIR_NAME);
			theBaseDir.mkdirs();
			
			EnvironmentConfig theConfig = new EnvironmentConfig();
			theConfig.setAllowCreate(true);
			theConfig.setReadOnly(false);
			theConfig.setTransactional(false);
			itsEnvironment = new Environment(theBaseDir, theConfig);
			DatabaseConfig theDBConfig = new DatabaseConfig();
			theDBConfig.setAllowCreate(true);
			theDBConfig.setReadOnly(false);
			theDBConfig.setTransactional(false);
			
			itsEvents = itsEnvironment.openDatabase(null, "events", theDBConfig);
			itsTimestampsIndex = createIndex("timestamps", new TimestampKeyCreator(), null);
			itsFieldIndex = createIndex("field", new IdKeyCreator(EventType.FIELD_WRITE), null);
			itsVarIndex = createIndex("var", new IdKeyCreator(EventType.VAR_WRITE), null);
			itsBehaviorIndex = createIndex("behavior", new IdKeyCreator(EventType.BEHAVIOR_ENTER), null);
			itsParentIndex = createIndex("parent", new ParentKeyCreator(), null);
			
			itsStack.push(new EventKey(-1, -1));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private SecondaryDatabase createIndex(
			String aName, 
			SecondaryKeyCreator aKeyCreator, 
			Class aValueComparatorClass) throws DatabaseException
	{
		SecondaryConfig theConfig = new SecondaryConfig();
		theConfig.setAllowCreate(true);
		theConfig.setImmutableSecondaryKey(true);
		theConfig.setSortedDuplicates(true);
		theConfig.setKeyCreator(aKeyCreator);
		if (aValueComparatorClass != null) theConfig.setDuplicateComparator(aValueComparatorClass);
		
		return itsEnvironment.openSecondaryDatabase(null, aName, itsEvents, theConfig);
	}

	public long getStoredSize()
	{
		return InsertBench.getDirSize(DIR_NAME);
	}

	private void insert(Database aDatabase) 
	{
		try
		{
			TupleBase.outputToEntry(itsKeyOutput, itsKeyEntry);
			TupleBase.outputToEntry(itsValueOutput, itsValueEntry);
			aDatabase.put(null, itsKeyEntry, itsValueEntry);
			
			itsKeyOutput.reset();
			itsValueOutput.reset();
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void writeCurrentParent()
	{
		EventKey theCurrentParent = itsStack.peek();
		itsValueOutput.writeLong(theCurrentParent.getTid());
		itsValueOutput.writeLong(theCurrentParent.getSeq());
	}
	
	public synchronized void logBehaviorEnter(long aTid, long aSeq, int aBehaviorId, long aTarget, long[] args)
	{
		itsKeyOutput.writeLong(aTid);
		itsKeyOutput.writeLong(aSeq);
		
		itsValueOutput.writeLong(time());
		itsValueOutput.writeByte(EventType.BEHAVIOR_ENTER.ordinal());
		writeCurrentParent();
		itsValueOutput.writeInt(aBehaviorId);
		itsValueOutput.writeLong(aTarget);
		
		itsValueOutput.writeInt(args.length);
		for (long arg : args)
		{
			itsValueOutput.writeLong(arg);
		}
		
		insert(itsEvents);
		
		itsStack.push(new EventKey(aTid, aSeq));
	}

	public synchronized void logBehaviorExit(long aTid, long aSeq, long aRetValue)
	{
		itsKeyOutput.writeLong(aTid);
		itsKeyOutput.writeLong(aSeq);

		itsValueOutput.writeLong(time());
		itsValueOutput.writeByte(EventType.BEHAVIOR_EXIT.ordinal());
		writeCurrentParent();
		itsValueOutput.writeLong(aRetValue);
		
		insert(itsEvents);
		
		itsStack.pop();
	}

	public synchronized void logFieldWrite(long aTid, long aSeq, int aFieldId, long aTarget, long aValue)
	{
		itsKeyOutput.writeLong(aTid);
		itsKeyOutput.writeLong(aSeq);
		
		itsValueOutput.writeLong(time());
		itsValueOutput.writeByte(EventType.FIELD_WRITE.ordinal());
		writeCurrentParent();
		itsValueOutput.writeInt(aFieldId);
		itsValueOutput.writeLong(aTarget);
		itsValueOutput.writeLong(aValue);
		
		insert(itsEvents);
	}

	public synchronized void logVarWrite(long aTid, long aSeq, int aVarId, long aValue)
	{
		itsKeyOutput.writeLong(aTid);
		itsKeyOutput.writeLong(aSeq);
		
		itsValueOutput.writeLong(time());
		itsValueOutput.writeByte(EventType.VAR_WRITE.ordinal());
		writeCurrentParent();
		itsValueOutput.writeInt(aVarId);
		itsValueOutput.writeLong(aValue);
		
		insert(itsEvents);
	}
	
	private static class TimestampKeyCreator extends TupleTupleKeyCreator
	{
		@Override
		public boolean createSecondaryKey(
				TupleInput aPrimaryKeyInput, 
				TupleInput aDataInput, 
				TupleOutput aIndexKeyOutput)
		{
			long t = aDataInput.readLong();
			aIndexKeyOutput.writeLong(t);
			return true;
		}
	}
	
	private static class IdKeyCreator extends TupleTupleKeyCreator
	{
		private EventType itsType;
		
		public IdKeyCreator(EventType aType)
		{
			itsType = aType;
		}

		@Override
		public boolean createSecondaryKey(
				TupleInput aPrimaryKeyInput, 
				TupleInput aDataInput, 
				TupleOutput aIndexKeyOutput)
		{
			aDataInput.readLong();
			EventType type = EventType.values()[aDataInput.readByte()];
			if (type == itsType)
			{
				int id = aDataInput.readInt();
				aIndexKeyOutput.writeInt(id);
				return true;
			}
			else return false;
		}
	}

	private static class ParentKeyCreator extends TupleTupleKeyCreator
	{
		@Override
		public boolean createSecondaryKey(
				TupleInput aPrimaryKeyInput, 
				TupleInput aDataInput, 
				TupleOutput aIndexKeyOutput)
		{
			aDataInput.readLong(); // timestamp
			aDataInput.readByte(); // type
			
			long tid = aDataInput.readLong();
			long seq = aDataInput.readLong();
			
			aIndexKeyOutput.writeLong(tid);
			aIndexKeyOutput.writeLong(seq);
			
			return true;
		}
	}
	
	private static class EventKey
	{
		private long itsTid;
		private long itsSeq;
		
		public EventKey(long aTid, long aSeq)
		{
			itsTid = aTid;
			itsSeq = aSeq;
		}

		public long getSeq()
		{
			return itsSeq;
		}

		public long getTid()
		{
			return itsTid;
		}
	}
//	private static class ThreadSeqComparator implements Comparator
//	{
//
//		public int compare(Object aO1, Object aO2)
//		{
//			byte[] b1 = (byte[]) aO1;
//			byte[] b2 = (byte[]) aO2;
//			
//			
//		}
//		
//	}
}
