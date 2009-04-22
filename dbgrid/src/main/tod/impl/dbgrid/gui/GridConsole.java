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
package tod.impl.dbgrid.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import tod.impl.dbgrid.RIGridMaster;
import tod.impl.dbgrid.RIGridMasterListener;
import zz.utils.ListMap;
import zz.utils.SimpleAction;
import zz.utils.monitoring.MonitorUI;
import zz.utils.monitoring.Monitor.MonitorData;

/**
 * Provides a monitoring console for the distributed database.
 * @author gpothier
 */
public class GridConsole extends JPanel
{
	static
	{
		System.out.println("GridConsole loaded by: "+GridConsole.class.getClassLoader());
	}
	
	private RIGridMaster itsMaster;
	private MonitorUI itsMonitorUI;

	/**
	 * Monitor data, per node. 
	 */
	private ListMap<Integer, MonitorData> itsMonitorData = 
		new ListMap<Integer, MonitorData>();
	
	public GridConsole(RIGridMaster aMaster)
	{
		itsMaster = aMaster;
		createUI();
		
		itsMaster.addListener(new MasterListener());
	}
	

	private void createUI()
	{
		itsMonitorUI = new MonitorUI();
		setLayout(new BorderLayout());
		
		add(createToolbar(), BorderLayout.NORTH);
		add(itsMonitorUI, BorderLayout.CENTER);
	}
	
	private JComponent createToolbar()
	{
		JPanel theToolbar = new JPanel();
		JButton theClearButton = new JButton(new SimpleAction("Clear DB")
		{
			public void actionPerformed(ActionEvent aE)
			{
				itsMaster.clear();
			}
		});
		
		theToolbar.add(theClearButton);
		
		return theToolbar;
	}

	public void monitorData(int aNodeId, MonitorData aData)
	{
		itsMonitorData.add(aNodeId, aData);
		itsMonitorUI.setData(aData);
	}
	
	private class MasterListener implements RIGridMasterListener
	{
		private static final long serialVersionUID = -1912140049548993769L;

		public void eventsReceived()
		{
		}

		public void exception(Throwable aThrowable)
		{
		}

		public void monitorData(int aNodeId, MonitorData aData) 
		{
			GridConsole.this.monitorData(aNodeId, aData);
		}

		public void captureEnabled(boolean aEnabled)
		{
		}
		
	}
}
