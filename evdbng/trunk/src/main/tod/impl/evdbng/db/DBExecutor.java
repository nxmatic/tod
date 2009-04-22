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
package tod.impl.evdbng.db;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import tod.impl.evdbng.DebuggerGridConfigNG;
import zz.utils.Utils;

/**
 * Manages the executor for database tasks. The executor uses a thread pool
 * to leverage multicore machines.
 * Each task submitted to the executor belongs to a group (represented by an
 * integer, usually the hash code of the "owner" of the tasks); the executor
 * guarantees that all the tasks from a given group will always be executed by
 * the same thread. (note that this required guarantee prevents us from using 
 * {@link ThreadPoolExecutor}).
 * @author gpothier
 *
 */
public class DBExecutor 
{
	private static final DBExecutor INSTANCE = new DBExecutor();
	
	public static DBExecutor getInstance()
	{
		return INSTANCE;
	}
	
	private final Worker[] itsWorkers;
	
	private Throwable itsThrown = null;
//	private int itsCount = 0;
	
	private DBExecutor()
	{
		itsWorkers = new Worker[DebuggerGridConfigNG.DB_THREADS];
		Utils.println("DBExecutor - using %d threads", DebuggerGridConfigNG.DB_THREADS);
		for(int i=0;i<DebuggerGridConfigNG.DB_THREADS;i++) itsWorkers[i] = new Worker(i);
	}

	private void checkThrown()
	{
		Throwable theThrown = itsThrown;
		if (theThrown != null) 
		{
			itsThrown = null;
			throw new RuntimeException(theThrown);
		}
	}
	
	public void submit(DBTask aTask)
	{
		submit(aTask, false);
	}
	
	/**
	 * Submits the given task for execution and waits for it to complete.
	 */
	public void submitAndWait(DBTask aTask)
	{
		submit(aTask, true);
	}
	
	public void submit(DBTask aTask, boolean aWait)
	{
		checkThrown();
		
		int theQueue = aTask.getGroup() % DebuggerGridConfigNG.DB_THREADS;
		itsWorkers[theQueue].submit(aTask);
		
		if (aWait)
		{
			NotifyTask theNotifyTask = new NotifyTask();
			itsWorkers[theQueue].submit(theNotifyTask);
			theNotifyTask.waitRun();
		}
	}
	
	/**
	 * Waits until all the tasks that were already pending when this method
	 * was called are completed.
	 */
	public void waitPendingTasks()
	{
		NotifyTask[] theTasks = new NotifyTask[DebuggerGridConfigNG.DB_THREADS];
		for(int i=0;i<DebuggerGridConfigNG.DB_THREADS;i++) 
		{
			theTasks[i] = new NotifyTask();
			itsWorkers[i].submit(theTasks[i]);
		}
		
		for(int i=0;i<DebuggerGridConfigNG.DB_THREADS;i++) theTasks[i].waitRun(); 
	}

	
	private class Worker extends Thread
	{
		private final ArrayBlockingQueue<DBTask> itsQueue = new ArrayBlockingQueue<DBTask>(128);
		
		/**
		 * Index of this worker within the pool
		 */
		private final int itsIndex;
		
		public Worker(int aIndex)
		{
			super("DB worker "+aIndex);
			itsIndex = aIndex;
			start();
		}

		public void submit(DBTask aTask) 
		{
			try
			{
				itsQueue.put(aTask);
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				try
				{
					DBTask theTask = itsQueue.take();
					theTask.run();
				}
				catch (Throwable e)
				{
					itsThrown = e;
				}
			}
		}
	}
	
	
	public static abstract class DBTask 
	{
		/**
		 * Returns the id of the group to which the task belongs.
		 */
		public abstract int getGroup();

		/**
		 * Executes the task.
		 */
		public abstract void run();
	}
	
	/**
	 * A task that sends {@link #notifyAll()} to itself when run.
	 * @author gpothier
	 *
	 */
	private static class NotifyTask extends DBTask
	{
		private boolean itsHasRun = false;
		
		@Override
		public int getGroup()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void run()
		{
			itsHasRun = true;
			notifyAll();
		}
		
		/**
		 * Waits until the task is run.
		 */
		public synchronized void waitRun()
		{
			try
			{
				while(! itsHasRun) wait();
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
		
	}
}
