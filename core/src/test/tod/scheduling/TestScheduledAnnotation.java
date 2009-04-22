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
package tod.scheduling;

import junit.framework.Assert;

import org.junit.Test;

import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobSchedulerProvider;
import tod.tools.scheduling.JobScheduler;
import tod.tools.scheduling.Scheduled;


public class TestScheduledAnnotation implements IJobSchedulerProvider
{
	private JobScheduler itsScheduler = new JobScheduler();
	private Thread itsMainThread;
	private boolean itsDone = false;
	
	public IJobScheduler getJobScheduler()
	{
		return itsScheduler;
	}

	@Scheduled
	public void scheduled(Object x)
	{
		Assert.assertNotNull("Main thread not set", itsMainThread);
		Assert.assertNotSame("Same thread", itsMainThread, Thread.currentThread());
		itsDone = true;
		System.out.println("Ok");
	}
	
	@Test
	public void test() throws InterruptedException
	{
		itsMainThread = Thread.currentThread();
		scheduled("hola");
		while(itsScheduler.pQueueSize().get() > 0) Thread.sleep(100);
		Assert.assertTrue(itsDone);
	}
}
