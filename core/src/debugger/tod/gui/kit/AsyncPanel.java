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
package tod.gui.kit;

import java.awt.Color;

import javax.swing.SwingUtilities;

import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.tools.monitoring.ITaskMonitor;
import tod.tools.monitoring.TaskMonitor.TaskCancelledException;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobSchedulerProvider;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.ui.MousePanel;

/**
 * A component whose content is retrieved asynchronously
 * @author gpothier
 */
public abstract class AsyncPanel extends MousePanel
implements IEventListener<Void>, IJobSchedulerProvider
{
	private final IJobScheduler itsJobScheduler;
	private final JobPriority itsJobPriority;
	private ITaskMonitor itsMonitor;
	private boolean itsCancelled;

	public AsyncPanel(IJobScheduler aJobScheduler, JobPriority aJobPriority)
	{
		assert aJobScheduler != null;
		itsJobScheduler = aJobScheduler;
		itsJobPriority = aJobPriority;
		setOpaque(false);
		createUI();
	}
	
	public IJobScheduler getJobScheduler()
	{
		return itsJobScheduler;
	}
	
	@Override
	public void addNotify()
	{
		super.addNotify();
		if (autoScheduleJob()) scheduleJob();
	}
	
	/**
	 * If this method returns true, the job is scheduled as soon as the panel is displayed.
	 * Otherwise, the job must be scheduled manually using {@link #scheduleJob()}.
	 */
	protected boolean autoScheduleJob()
	{
		return true;
	}
	
	protected void scheduleJob()
	{
		if (itsMonitor == null)
		{
			itsMonitor = itsJobScheduler.submit(itsJobPriority, new Runnable()
				{
					public void run()
					{
						try
						{
							runJob();
							if (! itsCancelled) postUpdate(Outcome.SUCCESS);
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
			itsMonitor.eCancelled().addListener(this);
		}
	}
	
	
	public void fired(IEvent< ? extends Void> aEvent, Void aData)
	{
		itsCancelled = true;
		postUpdate(Outcome.CANCELLED);
	}

	public void cancelJob()
	{
		if (itsMonitor != null) itsMonitor.cancel();
	}
	
	@Override
	public void removeNotify()
	{
		cancelJob();
		super.removeNotify();
	}
	
	/**
	 * Creates the initial UI of this panel.
	 * By default, displays "...".
	 */
	protected void createUI()
	{
		setLayout(GUIUtils.createSequenceLayout());
		add(GUIUtils.createLabel("..."));
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
		add(GUIUtils.createLabel("ERROR", FontConfig.STD_FONT, Color.RED));
	}
	
	

	private void postUpdate(final Outcome aOutcome)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				removeAll();
				update(aOutcome);
				revalidate();
				repaint();
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

	public enum Outcome
	{
		SUCCESS, CANCELLED, FAILURE;
	}
	
}
