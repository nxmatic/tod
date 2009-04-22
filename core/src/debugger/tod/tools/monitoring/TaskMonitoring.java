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

/**
 * This is the class that should be used by methods that need to explicitly
 * signal progress.
 * @author gpothier
 */
public class TaskMonitoring
{
	/**
	 * Indicates that the monitor should expect at least the specified number
	 * of work items.
	 */
	public static void expectItems(int aCount)
	{
		TaskMonitor theMonitor = TaskMonitor.current();
		if (theMonitor != null) theMonitor.expectItems(aCount);
	}
	
	/**
	 * Indicates that a work item has been completed.
	 */
	public static void work()
	{
		TaskMonitor theMonitor = TaskMonitor.current();
		if (theMonitor != null) theMonitor.work();
	}
	
	public static TaskMonitor createMonitor()
	{
		return new TaskMonitor(null);
	}
	
	public static void start(TaskMonitor aMonitor)
	{
		if (TaskMonitor.current() != null) throw new IllegalStateException("There is already a monitor");
		TaskMonitor.setCurrent(aMonitor);
	}
	
	public static TaskMonitor start()
	{
		TaskMonitor theMonitor = createMonitor();
		start(theMonitor);
		return theMonitor;
	}
	
	public static void stop()
	{
		TaskMonitor.current().pop();
		assert TaskMonitor.current() == null;
	}
}
