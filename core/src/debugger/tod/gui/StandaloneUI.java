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
package tod.gui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import tod.core.config.TODConfig;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import tod.core.session.SessionTypeManager;
import zz.utils.ui.StackLayout;

public class StandaloneUI extends JPanel
{
	private ISession itsSession;
	private MyTraceView itsTraceView;

	public StandaloneUI(URI aUri)
	{
		TODConfig theConfig = new TODConfig();
		itsTraceView = new MyTraceView();
		itsSession = SessionTypeManager.getInstance().createSession(itsTraceView, aUri, theConfig);
		createUI();
	}

	private void createUI()
	{
		setLayout(new StackLayout());
		JTabbedPane theTabbedPane = new JTabbedPane();
		add (theTabbedPane);
		
		itsTraceView.setSession(itsSession);
		theTabbedPane.addTab("Trace view", itsTraceView);
		
		JComponent theConsole = itsSession.createConsole();
		if (theConsole != null) theTabbedPane.addTab("Console", theConsole);
		
		int theWidth = itsTraceView.getSettings().getIntProperty("StandaloneUI.width", 600);
		int theHeight = itsTraceView.getSettings().getIntProperty("StandaloneUI.height", 400);
		setPreferredSize(new Dimension(theWidth, theHeight));
	}
	
	public void saveSize()
	{
		itsTraceView.getSettings().setProperty("StandaloneUI.width", ""+getWidth());
		itsTraceView.getSettings().setProperty("StandaloneUI.height", ""+getHeight());
		itsTraceView.getSettings().save();
	}

	private class MyTraceView extends MinerUI
	{
		public void gotoSource(ILocationInfo aLocation)
		{
		}

		public void gotoSource(ProbeInfo aProbe)
		{
		}

		@Override
		public <T> T showDialog(DialogType<T> aDialog)
		{
			return SwingDialogUtils.showDialog(this, aDialog);
		}

		public void showPostIt(JComponent aComponent, Dimension aSize)
		{
			JFrame theFrame = new JFrame("TOD Post-It");
			theFrame.setContentPane(aComponent);
			theFrame.pack();
			theFrame.setVisible(true);
		}

		public IExtensionPoints getExtensionPoints()
		{
			return null;
		}
	}
	
	public static void main(String[] args)
	{
		URI theUri = args.length > 0 ? URI.create(args[0]) : null;
		
		JFrame theFrame = new JFrame("TOD");
		final StandaloneUI theUI = new StandaloneUI(theUri);
		theFrame.setContentPane(theUI);
		theFrame.pack();
		theFrame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent aE)
			{
				theUI.saveSize();
			}
		});
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		theFrame.setVisible(true);
	}
}
