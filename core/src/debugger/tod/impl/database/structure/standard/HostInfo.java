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
package tod.impl.database.structure.standard;

import java.io.Serializable;
import java.util.ArrayList;

import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;

/**
 * Holds information about a debugged host.
 * @author gpothier
 */
public class HostInfo implements IHostInfo, Serializable
{
	private int itsId;
	private String itsName;
	
	private ArrayList<IThreadInfo> itsThreads = new ArrayList<IThreadInfo>();
	
	public HostInfo(int aId)
	{
		itsId = aId;
	}

	public HostInfo(int aId, String aName)
	{
		itsId = aId;
		itsName = aName;
	}
	
	public int getId()
	{
		return itsId;
	}
	
	public String getName()
	{
		return itsName;
	}

	public void setName(String aName)
	{
		itsName = aName;
	}
	
	public void addThread(IThreadInfo aThread)
	{
		itsThreads.add(aThread);
	}
	
	public Iterable<IThreadInfo> getThreads()
	{
		return itsThreads;
	}

	@Override
	public String toString()
	{
		return "Host ("+getId()+", "+getName()+")";
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + itsId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final HostInfo other = (HostInfo) obj;
		if (itsId != other.itsId) return false;
		return true;
	}
}
