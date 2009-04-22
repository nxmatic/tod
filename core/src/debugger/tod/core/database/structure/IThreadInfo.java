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
package tod.core.database.structure;

import java.util.Comparator;

public interface IThreadInfo
{
	/**
	 * Returns the host on which this thread is run.
	 */
	public IHostInfo getHost();

	/**
	 * Returns the internal (sequential, per host) id of the thread.
	 */
	public int getId();
	
	/**
	 * Returns the external (JVM) id of the thread.
	 */
	public long getJVMId();

	public String getName();
	
	/**
	 * Returns a description of this thread.
	 */
	public String getDescription();

	/**
	 * A comparator that compares thread ids.
	 * @author gpothier
	 */
	public static class ThreadIdComparator implements Comparator<IThreadInfo>
	{
		private static ThreadIdComparator INSTANCE = new ThreadIdComparator();

		public static ThreadIdComparator getInstance()
		{
			return INSTANCE;
		}

		private ThreadIdComparator()
		{
		}
		
		public int compare(IThreadInfo aO1, IThreadInfo aO2)
		{
			return aO1.getId() - aO2.getId();
		}
		
	}
}