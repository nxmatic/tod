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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import zz.utils.Utils;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.ui.UIUtils;

/**
 * A components that displays the queue size of a 
 * {@link JobScheduler}.
 * @author gpothier
 */
public class JobSchedulerMonitor extends JPanel
implements IPropertyListener<Integer>
{
	private final JobScheduler itsJobScheduler;
	
	private final MaxUpdaterThread itsMaxUpdaterThread;
	private int itsCurrentMax;
	private int itsCurrentVal;
	
	private JProgressBar itsProgressBar;
	
	public JobSchedulerMonitor(JobScheduler aJobScheduler)
	{
		itsJobScheduler = aJobScheduler;
		createUI();
		itsMaxUpdaterThread = new MaxUpdaterThread();
	}
	
	@Override
	public void addNotify()
	{
		super.addNotify();
		itsJobScheduler.pQueueSize().addHardListener(this);
	}
	
	@Override
	public void removeNotify()
	{
		itsJobScheduler.pQueueSize().removeListener(this);
		super.removeNotify();
	}

	private void createUI()
	{
		setLayout(new BorderLayout());
		itsProgressBar = new JProgressBar();
		add(itsProgressBar, BorderLayout.CENTER);
		
		JButton theCancelAllButton = new JButton("(X)");
		theCancelAllButton.setToolTipText("Cancel all pending jobs");
		theCancelAllButton.setMargin(UIUtils.NULL_INSETS);
		theCancelAllButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				itsJobScheduler.cancelAll();
			}
		});
		add(theCancelAllButton, BorderLayout.WEST);
		
		update();
	}
	
	private void update()
	{
		itsCurrentVal = itsJobScheduler.pQueueSize().get();
		if (itsCurrentVal > itsCurrentMax) itsCurrentMax = itsCurrentVal;
		itsProgressBar.setMaximum(itsCurrentMax);
		itsProgressBar.setValue(itsCurrentVal);
	}

	public void propertyChanged(IProperty<Integer> aProperty, Integer aOldValue, Integer aNewValue)
	{
		update();
	}

	public void propertyValueChanged(IProperty<Integer> aProperty)
	{
	}

	/**
	 * This thread periodically resets the max.
	 * @author gpothier
	 */
	private class MaxUpdaterThread extends Thread
	{
		public MaxUpdaterThread()
		{
			super("MaxUpdaterThread");
			setDaemon(true);
			start();
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				if (itsJobScheduler.pQueueSize().get() == 0)
				{
					itsCurrentMax = 0;
					update();
				}
				
				Utils.sleep(300);
			}
		}
	}
	
}
