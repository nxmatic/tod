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
package tod.tools.monitoring;

import tod.core.DebugFlags;
import zz.utils.notification.IEvent;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;
import zz.utils.properties.IProperty;

public class TaskMonitor implements ITaskMonitor
{
	private static ThreadLocal<TaskMonitor> current = new ThreadLocal<TaskMonitor>();
	
	public static TaskMonitor current()
	{
		return current.get();
	}
	
	public static void setCurrent(TaskMonitor aMonitor)
	{
		current.set(aMonitor);
		if (DebugFlags.TRACE_MONITORING) System.out.println("TaskMonitor.setCurrent("+aMonitor+")");
	}
	
	private TaskMonitor itsParent;
	
	/**
	 * The current child monitor, if any.
	 * TODO: should be a list so that we support parallel tasks.
	 */
	private TaskMonitor itsCurrentChild;
	
	/**
	 * Set to true when the monitor has been cancelled.
	 */
	private boolean itsCancelled = false;
	
	private final IFireableEvent<Void> eCancelled = new SimpleEvent<Void>();
	
	/**
	 * Set to true when the monitor should no longer be used.
	 */
	private boolean itsInvalid = false;
	
	private boolean itsIgnoreNextPop = false;
	
	TaskMonitor(TaskMonitor aParent)
	{
		super();
		itsParent = aParent;
	}

	public TaskMonitor getParent()
	{
		return itsParent;
	}
	
	/**
	 * Cancels this task.
	 * Once the task is cancelled, any attempt to mutate the monitor will
	 * throw a {@link TaskCancelledException}.
	 */
	public void cancel()
	{
		if (itsCancelled) return;
		itsCancelled = true;
		TaskMonitor theCurrentChild = itsCurrentChild; // Local var. to avoid concurrency issues.
		if (theCurrentChild != null) theCurrentChild.cancel();
		eCancelled.fire(null);
	}
	
	public void invalidate()
	{
		itsInvalid = true;
	}
	
	/**
	 * Throws an exception if this monitor is either cancelled or invalid.
	 */
	private void checkState()
	{
		if (itsCancelled) 
		{
			System.out.println("Cancelled!");
			throw new TaskCancelledException();
		}
		if (itsInvalid) throw new IllegalStateException();
	}
	
	/**
	 * Whether this task has been cancelled.
	 */
	public boolean isCancelled()
	{
		return itsCancelled;
	}

	/**
	 * An event that is triggered when the task is cancelled.
	 */
	public IEvent<Void> eCancelled()
	{
		return eCancelled;
	}
	
	/**
	 * A property that holds the current progress of the task, range is [0..1]. 
	 */
	public IProperty<Float> pProgress()
	{
		return null;
	}
	
	/**
	 * Sets the name of the monitor, if it doesn't already have one.
	 */
	public void setName(String aName)
	{
		checkState();
	}
	
	/**
	 * Tells this monitor that it is going to have a number of work items,
	 * which can be subtasks ({@link #sub()}) or direct work items ({@link #work()}).
	 */
	public void expectItems(int aCount)
	{
		checkState();
	}
	
	/**
	 * Creates a monitor for a subtask.
	 * It is actually possible to create more subtasks that were requested through
	 * {@link #expectSubtasks(int)}.
	 */
	public TaskMonitor sub(String aName)
	{
		checkState();
		TaskMonitor theMonitor = new TaskMonitor(this);
		theMonitor.setName(aName);
		return theMonitor;
	}
	
	public void work()
	{
		checkState();

	}
	
	/**
	 * Pushes a submonitor with the given name.
	 */
	void push(String aName)
	{
		assert itsIgnoreNextPop == false;
		try
		{
			if (DebugFlags.TRACE_MONITORING) System.out.println("TaskMonitor.push()");
			assert current() == this;
			TaskMonitor theSub = sub(aName);
			setCurrent(theSub);
			itsCurrentChild = theSub;
		}
		catch (RuntimeException e)
		{
			itsIgnoreNextPop = true;
			throw e;
		}
	}

	/**
	 * Invalidates this monitor, mark a work item done in the parent
	 * and make the parent the current monitor. 
	 */
	void pop()
	{
		if (itsIgnoreNextPop)
		{
			if (DebugFlags.TRACE_MONITORING) System.out.println("TaskMonitor.pop() - ignored");
			itsIgnoreNextPop = false;
			return;
		}
		
		if (DebugFlags.TRACE_MONITORING) System.out.println("TaskMonitor.pop()");
		invalidate();
		TaskMonitor theParent = getParent();
		if (theParent != null) 
		{
			theParent.work();
			theParent.itsCurrentChild = null;
		}
		setCurrent(theParent);
	}
	
	public static class TaskCancelledException extends RuntimeException
	{
	}
}
