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
package tod.core.database.event;

import java.io.Serializable;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IThreadInfo;


/**
 * External event pointer, comprised of host id, thread id
 * and timestamp, which is enough information to uniquely
 * identify an event. 
 * @see ILogBrowser#getEvent(ExternalPointer)
 * @author gpothier
 */
public class ExternalPointer implements Serializable
{
	private static final long serialVersionUID = -3084204556891153420L;
	
	private final IThreadInfo thread;
	private final long timestamp;

	public ExternalPointer(IThreadInfo aThread, long aTimestamp)
	{
		thread = aThread;
		timestamp = aTimestamp;
	}

	public IThreadInfo getThread()
	{
		return thread;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((thread == null) ? 0 : thread.hashCode());
		result = PRIME * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ExternalPointer other = (ExternalPointer) obj;
		if (thread == null)
		{
			if (other.thread != null) return false;
		}
		else if (!thread.equals(other.thread)) return false;
		if (timestamp != other.timestamp) return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		return String.format(
				"ExternalPointer [host: %d, thread: %d, ts: %d]", 
				thread.getHost().getId(),
				thread.getId(),
				timestamp);
	}

}

