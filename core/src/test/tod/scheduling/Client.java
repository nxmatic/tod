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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import tod.Util;
import tod.gui.GUIUtils;
import tod.gui.kit.AsyncPanel;
import tod.tools.monitoring.MonitoringClient.MonitorId;
import tod.tools.scheduling.JobScheduler;
import tod.tools.scheduling.JobSchedulerMonitor;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.srpc.RIRegistry;

public class Client extends JPanel
{
	private RIServer itsServer;
	private JComponent itsResultContainer;
	private JobScheduler itsJobScheduler = new JobScheduler();
	
	public Client(RIServer aServer)
	{
		itsServer = aServer;
		createUI();
	}

	private void createUI()
	{
		setLayout(new BorderLayout());
		
		JPanel theNorthPanel = new JPanel(GUIUtils.createSequenceLayout());
		add(theNorthPanel, BorderLayout.NORTH);

		JButton theTask1Button = new JButton("Task!");
		theTask1Button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				addTask(JobPriority.EXPLICIT);
			}
		});
		
		theNorthPanel.add(theTask1Button);
		
		JButton theTask2Button = new JButton("(task)");
		theTask2Button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				addTask(JobPriority.AUTO);
			}
		});
		
		theNorthPanel.add(theTask2Button);
		
		JButton theCancelButton = new JButton("Cancel");
		theCancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				itsJobScheduler.cancelAll();
			}
		});
		
		theNorthPanel.add(theCancelButton);
		
		theNorthPanel.add(new JobSchedulerMonitor(itsJobScheduler));
		
		itsResultContainer = new JPanel(GUIUtils.createStackLayout());
		add(itsResultContainer, BorderLayout.CENTER);
	}
	
	private void addTask(final JobPriority aJobPriority)
	{
		itsResultContainer.add(new AsyncPanel(itsJobScheduler, aJobPriority)
		{
			private int itsResult;
			
			@Override
			protected void runJob()
			{
				itsResult = itsServer.doTask(MonitorId.get(), (int) (Math.random()*5)+2);
			}
			
			@Override
			protected void createUI()
			{
				setLayout(GUIUtils.createSequenceLayout());
				add(GUIUtils.createLabel(aJobPriority+"..."));
			}

			@Override
			protected void updateSuccess()
			{
				add(new JLabel("Done: "+itsResult));
			}
			
			@Override
			protected void updateCancelled()
			{
				add(new JLabel("Cancelled!"));
			}
			
			@Override
			protected void updateFailure()
			{
				add(new JLabel("Failure!"));
			}
		});
		
		itsResultContainer.revalidate();
		itsResultContainer.repaint();
	}
	
	public static void main(String[] args) throws Exception
	{
		RIRegistry theRoot = Util.getRemoteSRPCRegistry("localhost", Util.TOD_SRPC_PORT);
		RIServer theServer = (RIServer) theRoot.lookup("server");
		
		JFrame theFrame = new JFrame("Scheduling test");
		theFrame.setContentPane(new Client(theServer));
		theFrame.pack();
		theFrame.setVisible(true);
	}
}
