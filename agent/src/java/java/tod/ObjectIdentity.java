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

import tod.agent.AgentConfig;


/**
 * Provides a helper method that retrieves the id
 * of an object.
 * @author gpothier
 */
public class ObjectIdentity
{
	/**
	 * Retrieves the identifier of an object.
	 * Returns a positive value if the object was already tagged.
	 * If this call causes the object to be tagged, the opposite of 
	 * the actual tag value is returned.
	 */
	public static long get (Object aObject)
	{
		return _AgentConfig.JAVA14 ? get14(aObject) : get15(aObject); 
	}
	
	private static native long get15(Object aObject);

	private static final WeakLongHashMap MAP = _AgentConfig.JAVA14 ? new WeakLongHashMap() : null;
	
	private static long itsNextId = 1;
	
	private static synchronized long nextId()
	{
		return itsNextId++;
	}
	
	private static long get14(Object aObject)
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
