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
package tod.gui.kit.html;

import javax.swing.SwingUtilities;

import tod.gui.kit.AsyncPanel.Outcome;
import tod.tools.monitoring.ITaskMonitor;
import tod.tools.monitoring.TaskMonitor.TaskCancelledException;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobScheduler.JobPriority;

/**
 * An html span element whose content is retrieved asynchronously.
 * @author gpothier
 */
public abstract class AsyncHtmlGroup extends HtmlGroup
{
	private final IJobScheduler itsJobScheduler;
	private final JobPriority itsJobPriority;
	
	private ITaskMonitor itsMonitor;
	private boolean itsCancelled;

	private HtmlText itsText;
	
	public AsyncHtmlGroup(IJobScheduler aJobScheduler, JobPriority aJobPriority)
	{
		this(aJobScheduler, aJobPriority, "...");
	}
	
	public AsyncHtmlGroup(IJobScheduler aJobScheduler, JobPriority aJobPriority, String aTempText)
	{
		itsJobScheduler = aJobScheduler;
		itsJobPriority = aJobPriority;
		itsText = HtmlText.create(aTempText);
		add(itsText);
	}
	
	@Override
	public void setDoc(HtmlDoc aDoc)
	{
		super.setDoc(aDoc);
		
		if (itsMonitor == null && ! itsCancelled)
		{
			itsMonitor = itsJobScheduler.submit(itsJobPriority, new Runnable()
			{
				public void run()
				{
					try
					{
						runJob();
						postUpdate(Outcome.SUCCESS);
					}
					catch (TaskCancelledException e)
					{
						postUpdate(Outcome.CANCELLED);
					}
					catch (Throwable e)
					{
						System.err.println("Error executing job:");
						e.printStackTrace();
						postUpdate(Outcome.FAILURE);
					}
				}
			});
		}
	}
	
	public void cancelJob()
	{
		itsCancelled = true;
		if (itsMonitor != null) itsMonitor.cancel();
	}
	
	/**
	 * Updates the UI once the long-running job of {@link #runJob()} is
	 * finished.
	 * This method is executed in the Swing thread. 
	 * It is not necessary to call {@link #revalidate()} nor {@link #repaint()}.
	 * @param aOutcome Indicates if the job run successfully or not.
	 */
	protected void update(Outcome aOutcome)
	{
		switch(aOutcome)
		{
		case SUCCESS:
			updateSuccess();
			break;
			
		case CANCELLED:
			updateCancelled();
			break;
			
		case FAILURE:
			updateFailure();
			break;
			
		default:
			throw new RuntimeException("Not handled: "+aOutcome);
		}
	}
	
	/**
	 * Called by default by {@link #update(Outcome)} if the job finished successfully.
	 * This method is executed in the Swing thread. 
	 * It is not necessary to call {@link #revalidate()} nor {@link #repaint()}.
	 */
	protected abstract void updateSuccess();
	
	protected void updateCancelled()
	{
	}
	
	protected void updateFailure()
	{
	}
	
	private void postUpdate(final Outcome aOutcome)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				remove(itsText);
				update(aOutcome);
			}
		});
	}
	
	/**
	 * This method should perform a long-running task. It will be 
	 * executed by the {@link JobProcessor}.
	 * Once this method terminates the {@link #update()} method will
	 * be scheduled for execution in the Swing thread.
	 */
	protected abstract void runJob();
}
