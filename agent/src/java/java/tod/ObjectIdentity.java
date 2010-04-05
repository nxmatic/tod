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
package java.tod;

import java.tod.util.WeakLongHashMap;

import tod2.agent.AgentConfig;
import tod2.agent.AgentDebugFlags;
import tod2.agent.util.BitUtilsLite;


/**
 * Provides a helper method that retrieves the id
 * of an object.
 * @author gpothier
 */
public class ObjectIdentity
{
	private static final boolean USE_CACHE = false;
	
	/**
	 * Used as a monitor within generated $tod$getId code.
	 */
	public static final Object MON = new Object();
	
	private static final boolean USE_JAVA = false;//_AgentConfig.JAVA14;
	private static final WeakLongHashMap MAP = USE_JAVA ? new WeakLongHashMap() : null;

	private static final int OBJID_CACHE_SIZE = BitUtilsLite.pow2i(3);
	private static final int OBJID_CACHE_MASK = OBJID_CACHE_SIZE - 1;
	private static Object[] itsObjIdCacheKey = new Object[OBJID_CACHE_SIZE];
	private static long[] itsObjIdCacheValue = new long[OBJID_CACHE_SIZE];
	private static int itsObjIdCacheIndex = 0;
	
	public static int itsObjIdCacheAccess = 0;
	public static int itsObjIdCacheHit = 0;

	
	/**
	 * Retrieves the identifier of an object.
	 * Returns a positive value if the object was already tagged.
	 * If this call causes the object to be tagged, the opposite of 
	 * the actual tag value is returned.
	 */
	public static long get (Object aObject)
	{
		if (! USE_CACHE) return USE_JAVA ? get14(aObject) : get15(aObject);
		
		if (AgentDebugFlags.COLLECT_PROFILE) itsObjIdCacheAccess++;
		
		for (int i = 0; i < OBJID_CACHE_SIZE; i++) if (itsObjIdCacheKey[i] == aObject)
		{
			if (AgentDebugFlags.COLLECT_PROFILE) itsObjIdCacheHit++;
			return itsObjIdCacheValue[i];
		}

		long theId = USE_JAVA ? get14(aObject) : get15(aObject);

		int theIndex = itsObjIdCacheIndex;
		itsObjIdCacheKey[theIndex] = aObject;
		itsObjIdCacheValue[theIndex] = theId;
		itsObjIdCacheIndex = (theIndex + 1) & OBJID_CACHE_MASK;

		return theId;
	}
	
	private static native long get15(Object aObject);

	private static long itsNextId = 1;
	
	public static synchronized long nextId()
	{
		// We create odd ids. Even ids are used for temporary ids (see TmpIdManager)
		long theId = itsNextId;
		itsNextId += 2;
		return theId;
	}
	
	private static int itsNextClassId = 1;
	
	public static synchronized int nextClassId()
	{
		return itsNextClassId++;
	}
	
	private static synchronized long get14(Object aObject)
	{
		long theId = MAP.get(aObject);
		if (theId != 0) return theId;
		else
		{
			theId = (nextId() << AgentConfig.HOST_BITS) | _AgentConfig.HOST_ID;
			MAP.put(aObject, theId);
			return -theId;
		}
	}
}
