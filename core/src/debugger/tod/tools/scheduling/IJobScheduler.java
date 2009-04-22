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
package tod.tools.scheduling;

import tod.tools.monitoring.ITaskMonitor;
import tod.tools.monitoring.TaskMonitor;

/**
 * An entity that permits to schedule jobs.
 * @author gpothier
 */
public interface IJobScheduler
{
	/**
	 * Submits a job for asynchronous execution.
	 * @param aPriority The priority of the job. Jobs with higher priority 
	 * are always executed before jobs with lower priority, even if they
	 * are submitted later.
	 * See {@link JobPriority}.
	 * 
	 * @return a {@link ITaskMonitor} that can be used to track the job's
	 * progress and to cancel the job.
	 */
	public ITaskMonitor submit(JobPriority aPriority, Runnable aRunnable);
	
	/**
	 * Same as {@link #submit(JobPriority, Runnable)}, but specifying an existing monitor.
	 */
	public void submit(JobPriority aPriority, Runnable aRunnable, TaskMonitor aMonitor);
	
	/**
	 * Cancel all the jobs of this scheduler.
	 */
	public void cancelAll();
	
	
	public enum JobPriority
	{
		/**
		 * The default priority. Use if you don't know what priority to use.
		 */
		DEFAULT(0),
		
		/**
		 * Priority for jobs that have been explicitly requested by the user.
		 */
		EXPLICIT(10),
		
		/**
		 * Priority for auxilliary jobs that have not been explicitly requested by the user.
		 */
		AUTO(-10),
		
		LOW(-20);
		
		private final int itsValue;

		private JobPriority(int aValue)
		{
			itsValue = aValue;
		}
		
		int getValue()
		{
			return itsValue;
		}
	}
}
