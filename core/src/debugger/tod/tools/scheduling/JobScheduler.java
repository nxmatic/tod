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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import tod.tools.monitoring.ITaskMonitor;
import tod.tools.monitoring.TaskMonitor;
import tod.tools.monitoring.TaskMonitoring;
import tod.tools.monitoring.TaskMonitor.TaskCancelledException;
import zz.utils.properties.IProperty;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

public class JobScheduler extends Thread
implements IJobScheduler
{
	private BlockingQueue<Job> itsQueuedJobs = new PriorityBlockingQueue<Job>();
	private Job itsCurrentJob;
	
	private IRWProperty<Integer> pQueueSize = new SimpleRWProperty<Integer>(0);
	
	public JobScheduler()
	{
		super("JobScheduler");
		start();
	}

	private void updateQueueSize()
	{
		pQueueSize.set(itsQueuedJobs.size() + (itsCurrentJob != null ? 1 : 0));
	}
	
	public void cancelAll()
	{
		List<Job> theJobs = new ArrayList<Job>();
		itsQueuedJobs.drainTo(theJobs);
		for (Job theJob : theJobs) theJob.monitor.cancel();
		
		Job theCurrentJob = itsCurrentJob; // local var. for concurrency.
		if (theCurrentJob != null) 
		{
			// Cancel the current job only if cancel is not called from
			// that job.
			if (! theCurrentJob.isSameThread()) theCurrentJob.monitor.cancel();
		}
		
		updateQueueSize();
	}

	public ITaskMonitor submit(JobPriority aPriority, Runnable aRunnable)
	{
		TaskMonitor theMonitor = TaskMonitoring.createMonitor();
		submit(aPriority, aRunnable, theMonitor);
		return theMonitor;
	}
	
	public void submit(JobPriority aPriority, Runnable aRunnable, TaskMonitor aMonitor)
	{
		itsQueuedJobs.offer(new Job(aPriority, aMonitor, aRunnable));
		updateQueueSize();
	}
	
	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				itsCurrentJob = itsQueuedJobs.take();

				updateQueueSize();

				if (itsCurrentJob.monitor.isCancelled()) continue;
				try
				{
					itsCurrentJob.setThread(Thread.currentThread());
					TaskMonitoring.start(itsCurrentJob.monitor);
					itsCurrentJob.runnable.run();
					TaskMonitoring.stop();
				}
				catch (TaskCancelledException e)
				{
					System.err.println("Task cancelled: "+itsCurrentJob);
				}
				catch (Throwable e)
				{
					System.err.println("Error while executing job: "+itsCurrentJob);
					e.printStackTrace();
				}
				finally
				{
					TaskMonitor.setCurrent(null);
					itsCurrentJob = null;
					updateQueueSize();
				}
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * A property that holds the number of pending jobs.
	 */
	public IProperty<Integer> pQueueSize()
	{
		return pQueueSize;
	}
	
	private static class Job implements Comparable<Job>
	{
		private final JobPriority itsPriority;
		public final Runnable runnable;
		public final TaskMonitor monitor;
		
		/**
		 * The thread that executes the job.
		 * Only valid once the job is executing.
		 */
		private Thread itsThread;

		public Job(JobPriority aPriority, TaskMonitor aMonitor, Runnable aRunnable)
		{
			itsPriority = aPriority;
			monitor = aMonitor;
			runnable = aRunnable;
		}
		
		public void setThread(Thread aThread)
		{
			itsThread = aThread;
		}
		
		/**
		 * Whether the current thread is the thread that executes this job.
		 */
		public boolean isSameThread()
		{
			return itsThread == Thread.currentThread();
		}

		public int compareTo(Job j)
		{
			return j.itsPriority.getValue() - itsPriority.getValue();
		}
	}
}
