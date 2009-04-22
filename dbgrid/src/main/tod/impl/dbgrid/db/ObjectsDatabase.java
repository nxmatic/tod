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
package tod.impl.dbgrid.db;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import tod.core.DebugFlags;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.transport.ObjectDecoder;
import tod.impl.dbgrid.db.DatabaseNode.FlushMonitor;
import zz.utils.monitoring.AggregationType;
import zz.utils.monitoring.Monitor;
import zz.utils.monitoring.Probe;

/**
 * Stores registered objects.
 * @author gpothier
 */
public abstract class ObjectsDatabase
{
	private final IMutableStructureDatabase itsStructureDatabase;
	
	private final ObjectsReorderingBuffer itsReorderingBuffer = new ObjectsReorderingBuffer();
	private final ObjectRefsReorderingBuffer itsRefsReorderingBuffer = new ObjectRefsReorderingBuffer();
	
	private final Map<Long, LoadedTypeInfo> itsClassesMap = new HashMap<Long, LoadedTypeInfo>();

	private long itsDroppedObjects = 0;
	private long itsUnorderedObjects = 0;
	private long itsProcessedObjects = 0;
	private long itsStoreSize = 0;
	
	private long itsLastAddedId;
	private long itsLastProcessedId;
	private long itsObjectsCount = 0;

	private long itsLastRefAddedId;
	private long itsLastRefProcessedId;
	private long itsRefObjectsCount = 0;
	
	public ObjectsDatabase(IMutableStructureDatabase aStructureDatabase)
	{
		itsStructureDatabase = aStructureDatabase;
		Monitor.getInstance().register(this);		
	}

	public void store(long aId, byte[] aData, long aTimestamp)
	{
		if (aId < itsLastAddedId) itsUnorderedObjects++;
		else itsLastAddedId = aId;
		
		ObjectsReorderingBuffer.Entry theEntry = new ObjectsReorderingBuffer.Entry(aId, aTimestamp, aData);
		
		if (DebugFlags.DISABLE_REORDER)
		{
			doStore(theEntry);
		}
		else
		{
			while (itsReorderingBuffer.isFull()) doStore(itsReorderingBuffer.pop());
			itsReorderingBuffer.push(theEntry);
		}
	}
	
	private void doStore(ObjectsReorderingBuffer.Entry aEntry)
	{
		itsObjectsCount++;

		long theId = aEntry.id;
		if (theId < itsLastProcessedId)
		{
			itsDroppedObjects++;
			objectDropped();
			return;
		}
		
		itsLastProcessedId = theId;
		itsProcessedObjects++;
		itsStoreSize += aEntry.data.length;
		store0(theId, aEntry.data);
	}
	
	protected abstract void store0(long aId, byte[] aData);
	
	public void registerRef(long aId, long aTimestamp, long aClassId)
	{
		if (aId < itsLastRefAddedId) itsUnorderedObjects++;
		else itsLastRefAddedId = aId;
		
		ObjectRefsReorderingBuffer.Entry theEntry = new ObjectRefsReorderingBuffer.Entry(aId, aTimestamp, aClassId);
		
		if (DebugFlags.DISABLE_REORDER)
		{
			doRegisterRef(theEntry);
		}
		else
		{
			while (itsRefsReorderingBuffer.isFull()) doRegisterRef(itsRefsReorderingBuffer.pop());
			itsRefsReorderingBuffer.push(theEntry);
		}
	}
	
	private void doRegisterRef(ObjectRefsReorderingBuffer.Entry aEntry)
	{
		itsRefObjectsCount++;

		long theId = aEntry.id;
		if (theId < itsLastRefProcessedId)
		{
			itsDroppedObjects++;
			objectDropped();
			return;
		}
		
		itsLastRefProcessedId = theId;
		itsProcessedObjects++;
		registerRef0(theId, aEntry.classId);
	}
	
	protected abstract void registerRef0(long aId, long aClassId);
	
	public LoadedTypeInfo getLoadedClassForObject(long aObjectId)
	{
		long theClassId = getObjectTypeId(aObjectId);
		return getLoadedClass(theClassId);
	}
	
	public void registerClass(long aClassId, long aLoaderId, String aName)
	{
		ITypeInfo theType = aName.charAt(0) == '[' ?
				itsStructureDatabase.getNewType(aName)
				: itsStructureDatabase.getNewClass(aName);
				
		LoadedTypeInfo theLoadedClass = new LoadedTypeInfo(aClassId, aLoaderId, theType);
		LoadedTypeInfo thePrevious = itsClassesMap.put(aClassId, theLoadedClass);
		assert thePrevious == null;
	}
	
	public LoadedTypeInfo getLoadedClass(long aClassId)
	{
		return itsClassesMap.get(aClassId);
	}

	public void registerClassLoader(long aLoaderId, long aClassId)
	{
		// TODO: implement
	}

	public synchronized int flush(FlushMonitor aFlushMonitor)
	{
		int theCount = 0;
		System.out.println("[ReorderedObjectsDatabase] Flushing...");
		while (! itsReorderingBuffer.isEmpty())
		{
			if (aFlushMonitor != null && aFlushMonitor.isCancelled()) 
			{
				System.out.println("[ObjectsDatabase] Flush cancelled.");
				break;
			}
			
			doStore(itsReorderingBuffer.pop());
			theCount++;
		}

		while (! itsRefsReorderingBuffer.isEmpty())
		{
			if (aFlushMonitor != null && aFlushMonitor.isCancelled()) 
			{
				System.out.println("[ObjectsDatabase] Flush cancelled.");
				break;
			}
			
			doRegisterRef(itsRefsReorderingBuffer.pop());
			theCount++;
		}
		
		System.out.println("[ObjectsDatabase] Flushed "+theCount+" objects.");

		return theCount;
	}
	
	public int flushOld(long aOldness, FlushMonitor aFlushMonitor)
	{
		int theCount = 0;
		
		while (isNextEventFlushable(aOldness)) 
		{
			if (aFlushMonitor != null && aFlushMonitor.isCancelled()) 
			{
				System.out.println("[ObjectsDatabase] FlushOld cancelled.");
				break;
			}

			flushOldestEvent();
			theCount++;
		}

		return theCount;
	}

	/**
	 * return 0 if the Buffer is empty else return 1
	 * @return
	 */
	public synchronized  int flushOldestEvent(){
		int theCount = 0;
		if (!itsReorderingBuffer.isEmpty())
		{
			doStore(itsReorderingBuffer.pop());
			theCount++;
		}
		return theCount;
	}

	public void dispose()
	{
		Monitor.getInstance().unregister(this);
	}
	
	public abstract Decodable load(long aObjectId);
	
	/**
	 * Returns the class id of an object previously registered
	 * with {@link #registerRef(long, long, long)}.
	 */
	protected abstract long getObjectTypeId(long aObjectId);

	/**
	 * Deserializes an object previously serialized by {@link #encode(Object)}.
	 */
	protected static Object decode(long aId, byte[] aData)
	{
		assert aData.length > 0;
		ByteArrayInputStream theStream = new ByteArrayInputStream(aData);
		return decode(aId, theStream);
	}
	
	/**
	 * Deserializes an object previously serialized by {@link #encode(Object)}.
	 */
	protected static Object decode(long aId, InputStream aStream)
	{
		return ObjectDecoder.decode(new DataInputStream(aStream));
	}
	

	/**
	 * define if the difference between the oldest event of the buffer
	 *  and the newest is more than aDelay (in nanosecond)
	 * @param aDelay
	 * @return
	 */
	private boolean isNextEventFlushable(long aDelay)
	{
		return itsReorderingBuffer.isNextEventFlushable(aDelay);
	}
	
	
	@Probe(key = "Out of order objects", aggr = AggregationType.SUM)
	public long getUnorderedEvents()
	{
		return itsUnorderedObjects;
	}

	@Probe(key = "DROPPED OBJECTS", aggr = AggregationType.SUM)
	public long getDroppedEvents()
	{
		return itsDroppedObjects;
	}

	@Probe(key = "objects count", aggr = AggregationType.SUM)
	public long getObjectsCount()
	{
		return itsObjectsCount;
	}
	
	@Probe(key = "store size", aggr = AggregationType.SUM)
	public long getStoreSize()
	{
		return itsStoreSize;
	}

	public void objectDropped()
	{
		itsDroppedObjects++;
	}

	/**
	 * Information about an actually loaded class.
	 * @author gpothier
	 */
	public static class LoadedTypeInfo
	{
		public final long id;
		public final long loaderId;
		public final ITypeInfo typeInfo;
		
		public LoadedTypeInfo(long aId, long aLoaderId, ITypeInfo aType)
		{
			id = aId;
			loaderId = aLoaderId;
			typeInfo = aType;
		}
		
		@Override
		public String toString()
		{
			return "LoadedTypeInfo "+id+", "+loaderId+" - "+typeInfo;
		}
	}
	
	/**
	 * Transport format for registered objects.
	 * @author gpothier
	 */
	public static class Decodable implements Serializable
	{
		private static final long serialVersionUID = 47812004581172391L;
		
		private final long itsId;
		private final boolean itsCompressed;
		private final byte[] itsData;
		
		public Decodable(long aId, boolean aCompressed, byte[] aData)
		{
			itsId = aId;
			itsCompressed = aCompressed;
			itsData = aData;
		}

		public Object decode()
		{
			try
			{
				InputStream theStream = new ByteArrayInputStream(itsData);
				if (itsCompressed) theStream = new GZIPInputStream(theStream);
				return ObjectsDatabase.decode(itsId, theStream);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
