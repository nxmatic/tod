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

import java.tod.io._IO;

/**
 * This class keeps a registry of traced methods.
 * This is used by the instrumentation of method calls:
 * the events generated for a call to a traced method are
 * not the same as those of a non-traced method.
 * @author gpothier
 */
public class TracedMethods
{
	private static boolean[] traced = new boolean[10000];
	
	public static final void setTraced(int aId)
	{
		if (aId >= traced.length)
		{
			boolean[] room = new boolean[aId*2];
			System.arraycopy(traced, 0, room, 0, traced.length);
			_IO.out("Reallocated TracedMethods: "+room.length);
			traced = room;
		}
		
		//_IO.out("Marking traced: "+aId);
		traced[aId] = true;
	}
	
	public static final boolean isTraced(int aId)
	{
		//_IO.out("isTraced: "+aId);
		return aId >= traced.length ? false : traced[aId];
	}
}
