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

import tod.core.ILogCollector;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;


/**
 * Aggregates the information a {@link ILogCollector collector}
 * receives about a thread.
 * @author gpothier
 */
public class ThreadInfo implements IThreadInfo, Serializable
{
	private IHostInfo itsHost;
	private int itsId;
	private long itsJVMId;
	private String itsName;
	
	public ThreadInfo(IHostInfo aHost, int aId, long aJVMId, String aName)
	{
		itsHost = aHost;
		itsId = aId;
		itsJVMId = aJVMId;
		itsName = aName;
	}

	public int getId()
	{
		return itsId;
	}
	
	public long getJVMId()
	{
		return itsJVMId;
	}

	public IHostInfo getHost()
	{
		return itsHost;
	}

	public String getName()
	{
		return itsName;
	}

	public void setName(String aName)
	{
		itsName = aName;
	}
	
	public String getDescription()
	{
		return getId()+" ["+getName()+"]";
	}
	
	@Override
	public String toString()
	{
		return "Thread ("+getId()+", "+getJVMId()+", "+getName()+") on "+itsHost;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((itsHost == null) ? 0 : itsHost.hashCode());
		result = PRIME * result + itsId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ThreadInfo other = (ThreadInfo) obj;
		if (itsHost == null)
		{
			if (other.itsHost != null) return false;
		}
		else if (!itsHost.equals(other.itsHost)) return false;
		if (itsId != other.itsId) return false;
		return true;
	}

	
	

}
