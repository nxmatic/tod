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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import tod.core.database.browser.ICompoundInspector;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import zz.utils.Utils;

public class RecorderHelper
{
	private static RecorderHelper INSTANCE = new RecorderHelper();

	public static RecorderHelper getInstance()
	{
		return INSTANCE;
	}

	private ObjectOutputStream out;

	private RecorderHelper()
	{
		try
		{
			out = new ObjectOutputStream(new FileOutputStream("rec.bin"));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private Map<Object, Integer> itsObjectIdsMap = new IdentityHashMap<Object, Integer>();
	private Map<Thread, Integer> itsThreadIdsMap = new IdentityHashMap<Thread, Integer>();
	private int itsLastObjectId = 1;
	private int itsLastThreadId = 1;

	private synchronized int nextObjectId()
	{
		return itsLastObjectId++;
	}
	
	private synchronized int nextThreadId()
	{
		return itsLastThreadId++;
	}
	
	private int getObjectId(Object aObject, boolean aAllowNewId)
	{
		Integer theId = itsObjectIdsMap.get(aObject);
		if (theId == null)
		{
			theId = nextObjectId();
			if (! aAllowNewId && theId != 1) 
			{
				throw new RuntimeException("Object has no id: "+aObject);
			}
			itsObjectIdsMap.put(aObject, theId);
		}
		
		return theId;
	}
	
	/**
	 * Checks that the given object has an id.
	 * Only for debugging.
	 */
	public void checkId(Object aObject)
	{
		if (! itsObjectIdsMap.containsKey(aObject)) throw new RuntimeException("Not id: "+aObject);
	}
	
	private int getThreadId(Thread aThread)
	{
		Integer theId = itsThreadIdsMap.get(aThread);
		if (theId == null)
		{
			theId = nextThreadId();
			itsThreadIdsMap.put(aThread, theId);
		}
		
		return theId;
	}
	
	/**
	 * Indicates if the given object should be recorded by id.
	 */
	private boolean isRecorded(Object aObject)
	{
		return (aObject instanceof ILogBrowser)
			|| (aObject instanceof IEventBrowser)
			|| (aObject instanceof IStructureDatabase)
			|| (aObject instanceof ILocationInfo)
			|| (aObject instanceof IEventFilter)
			|| (aObject instanceof IThreadInfo)
			|| (aObject instanceof ILogEvent)
			|| (aObject instanceof ProbeInfo)
			|| (aObject instanceof ICompoundInspector)
			|| (aObject instanceof ICompoundInspector.EntryValue)
			|| (aObject instanceof ExternalPointer)
//			|| ("tod.impl.dbgrid.event.BehaviorCallEvent$CallInfoBuilder".equals(aObject.getClass().getName()))
//			|| ("tod.impl.dbgrid.event.BehaviorCallEvent$CallInfo".equals(aObject.getClass().getName()))
			;
	}
	
	private boolean isIgnored(Object aObject)
	{
		return (aObject instanceof ISession);
	}
	
	private Object[] transformArray(Object[] aArray, boolean aAllowNewId)
	{
		Object[] theResult = new Object[aArray.length];
		for (int i=0;i<theResult.length;i++)
		{
			theResult[i] = transform(aArray[i], aAllowNewId);
		}
		
		return theResult;
	}
	
	private Object transform(Object aObject, boolean aAllowNewId)
	{
		if (aObject == null) return null;
		else if (isIgnored(aObject)) return null;
		else if (aObject.getClass().isArray()) 
		{
			if (aObject.getClass().getComponentType().isPrimitive()) return aObject;
			else return transformArray((Object[]) aObject, aAllowNewId);
		}
		else if (aObject instanceof Iterable)
		{
			Iterable theIterable = (Iterable) aObject;
			List theList = new ArrayList();
			Utils.fillCollection(theList, theIterable);
			return transformArray(theList.toArray(), aAllowNewId);
		}
		else if (isRecorded(aObject)) return new Record.ProxyObject(getObjectId(aObject, aAllowNewId));
		else return aObject;
	}
	
	private synchronized void write(Record aRecord)
	{
		try
		{
			out.writeObject(aRecord);
			out.flush();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void recordCall(
			Object aTarget, 
			String aMethod,
			Class[] aFormalsType,
			Object[] aArgs, 
			Object aReturn,
			String aLocation)
	{
		Record theRecord = new Record.Call(
				getThreadId(Thread.currentThread()),
				new Record.ProxyObject(getObjectId(aTarget, false)),
				new Record.MethodSignature(aMethod, aFormalsType),
				transformArray(aArgs, false),
				transform(aReturn, true),
				aLocation);
		
		write(theRecord);
	}
	
	public void recordNew(
			String aMethod,
			Class[] aFormalsType,
			Object[] aArgs, 
			Object aReturn,
			String aLocation)
	{
		Record theRecord = new Record.New(
				getThreadId(Thread.currentThread()),
				new Record.MethodSignature(aMethod, aFormalsType),
				transformArray(aArgs, false),
				transform(aReturn, true),
				aLocation);
		
		write(theRecord);
	}
}
