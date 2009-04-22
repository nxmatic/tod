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
import tod.tools.monitoring.TaskMonitoring;
import zz.utils.list.NakedLinkedList;
import zz.utils.list.NakedLinkedList.Entry;

/**
 * 
 * @author gpothier
 */
public class JobGroup implements IJobScheduler
{
	private final IJobScheduler itsScheduler;
	private final NakedLinkedList<JobWrapper> itsJobs = new NakedLinkedList<JobWrapper>();

	public JobGroup(IJobScheduler aScheduler)
	{
		itsScheduler = aScheduler;
	};
	
	public synchronized void cancelAll()
	{
		while(itsJobs.size() > 0)
		{
			Entry<JobWrapper> theEntry = itsJobs.getFirstEntry();
			theEntry.getValue().cancel();
			itsJobs.remove(theEntry);
		}
	}

	public synchronized ITaskMonitor submit(JobPriority aPriority, Runnable aRunnable)
	{
		TaskMonitor theMonitor = TaskMonitoring.createMonitor();
		submit(aPriority, aRunnable, theMonitor);
		return theMonitor;
	}
	
	public void submit(JobPriority aPriority, Runnable aRunnable, TaskMonitor aMonitor)
	{
		JobWrapper theWrapper = new JobWrapper(aMonitor, aRunnable);
		Entry<JobWrapper> theEntry = itsJobs.createEntry(theWrapper);
		theWrapper.setEntry(theEntry);
		itsJobs.addLast(theEntry);
		
		itsScheduler.submit(aPriority, theWrapper, aMonitor);
	}

	private synchronized void remove(Entry<JobWrapper> aEntry)
	{
		if (aEntry.isAttached()) itsJobs.remove(aEntry);
	}

	/**
	 * Wraps the real job so as to be able to remove it from the list
	 * when completed.
	 * @author gpothier
	 */
	private class JobWrapper implements Runnable
	{
		private Entry<JobWrapper> itsEntry;
		private final Runnable itsRealJob;
		private final TaskMonitor itsMonitor;

		public JobWrapper(TaskMonitor aMonitor, Runnable aRealJob)
		{
			itsMonitor = aMonitor;
			itsRealJob = aRealJob;
		}
		
		public void setEntry(Entry<JobWrapper> aEntry)
		{
			itsEntry = aEntry;
		}
		
		public void cancel()
		{
			itsMonitor.cancel();
		}

		public void run()
		{
			try
			{
				itsRealJob.run();
			}
			finally
			{
				remove(itsEntry);
			}
		}
	}
}
