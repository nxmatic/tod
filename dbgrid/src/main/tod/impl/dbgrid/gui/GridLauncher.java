///*
//TOD - Trace Oriented Debugger.
//Copyright (c) 2006-2008, Guillaume Pothier
//All rights reserved.
//
//This program is free software; you can redistribute it and/or 
//modify it under the terms of the GNU General Public License 
//version 2 as published by the Free Software Foundation.
//
//This program is distributed in the hope that it will be useful, 
//but WITHOUT ANY WARRANTY; without even the implied warranty of 
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License 
//along with this program; if not, write to the Free Software 
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
//MA 02111-1307 USA
//
//Parts of this work rely on the MD5 algorithm "derived from the 
//RSA Data Security, Inc. MD5 Message-Digest Algorithm".
//*/
//package tod.impl.dbgrid.gui;
//
//import java.awt.event.ActionEvent;
//import java.io.InputStreamReader;
//import java.io.Reader;
//
//import javax.swing.JButton;
//import javax.swing.JComponent;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JSplitPane;
//import javax.swing.JTabbedPane;
//import javax.swing.JTextArea;
//import javax.swing.text.BadLocationException;
//import javax.swing.text.Document;
//
//import tod.impl.dbgrid.DBProcessManager;
//import tod.impl.dbgrid.DBProcessManager.IDBProcessListener;
//import zz.utils.SimpleAction;
//import zz.utils.StreamPipe;
//import zz.utils.ui.StackLayout;
//
///**
// * A simple launcher GUI for standalone database.
// * @author gpothier
// */
//public class GridLauncher extends JPanel
//{
//	private JPanel itsConsoleContainer;
//	
//	private DBProcessManager itsProcessManager;
//	
//	
//	private Document itsDocument;
//	
//	private SimpleAction itsStartAction = new SimpleAction("start")
//	{
//		public void actionPerformed(ActionEvent aE)
//		{
//			start();
//		}
//	};
//	
//	private SimpleAction itsStopAction = new SimpleAction("stop")
//	{
//		public void actionPerformed(ActionEvent aE)
//		{
//			stop();
//		}
//	};
//	
//	private StreamPipe itsOutputPipe = new StreamPipe();
//	private StreamPipe itsErrorPipe = new StreamPipe();
//	
//	private TextConsoleFeeder itsFeeder;
//	
//	public GridLauncher(DBProcessManager aProcessManager)
//	{
//		itsProcessManager = aProcessManager;
//		createUI();
//		itsProcessManager.addListener(new IDBProcessListener()
//		{
//			public void started()
//			{
//				updateActions();
//				setPipes(new StreamPipe(), new StreamPipe());
//				itsFeeder = new TextConsoleFeeder(itsDocument);
//			}
//
//			public void stopped()
//			{
//				setPipes(null, null);
//				updateActions();
//			}
//		});
//	}
//
//	private void setPipes(StreamPipe aOutputPipe, StreamPipe aErrorPipe)
//	{
//		// Setup Output pipe
//		if (itsOutputPipe != null) 
//		{
//			itsProcessManager.removeOutputStream(itsOutputPipe.getOutputStream());
//			itsOutputPipe.close();
//		}
//		itsOutputPipe = aOutputPipe;
//		if (itsOutputPipe != null) 
//			itsProcessManager.addOutputStream(itsOutputPipe.getOutputStream());
//		
//		// Setup error pipe
//		if (itsErrorPipe != null)
//		{
//			itsProcessManager.removeErrorStream(itsErrorPipe.getOutputStream());
//			itsErrorPipe.close();
//		}
//		itsErrorPipe = aErrorPipe;
//		if (itsErrorPipe != null) 
//			itsProcessManager.addErrorStream(itsErrorPipe.getOutputStream());
//	}
//	
//	@Override
//	public void addNotify()
//	{
//		super.addNotify();
//	}
//	
//	@Override
//	public void removeNotify()
//	{
//		super.removeNotify();
//		setPipes(null, null);
//		
//		if (itsFeeder != null) itsFeeder.kill();
//		itsFeeder = null;
//	}
//	
//	private void updateActions()
//	{
//		itsStartAction.setEnabled(! itsProcessManager.isAlive());
//		itsStopAction.setEnabled(itsProcessManager.isAlive());
//	}
//	
//	private void createUI()
//	{
//		JSplitPane thePane = new JSplitPane();
//		setLayout(new StackLayout());
//		add(thePane);
//		
//		thePane.setLeftComponent(createButtonsPane());
//		thePane.setRightComponent(createTabPane());
//		
//		updateActions();
//	}
//	
//	private JComponent createTabPane()
//	{
//		JTabbedPane thePane = new JTabbedPane();
//		
//		JTextArea itsTextArea = new JTextArea();
//		itsTextArea.setEditable(false);
//		itsDocument = itsTextArea.getDocument();
//		thePane.addTab("Console", new JScrollPane(itsTextArea));
//		
//		itsConsoleContainer = new JPanel(new StackLayout());
//		thePane.addTab("Status", itsConsoleContainer);
//		
//		return thePane;
//	}
//	
//	private JComponent createButtonsPane()
//	{
//		JPanel thePane = new JPanel();
//
//		thePane.add(new JButton(itsStartAction));
//		thePane.add(new JButton(itsStopAction));
//		
//		return thePane;
//	}
//	
//	/**
//	 * Returns the process manager used by this launcher.
//	 */
//	public DBProcessManager getProcessManager()
//	{
//		return itsProcessManager;
//	}
//	
//	protected void start()
//	{
//		try
//		{
//			itsDocument.remove(0, itsDocument.getLength());
//		}
//		catch (BadLocationException e)
//		{
//		}
//		itsProcessManager.start();
//	}
//	
//	protected void stop()
//	{
//		itsProcessManager.stop();		
//	}
//	
//	/**
//	 * Feeds the text obtained from the {@link DBProcessManager}'s output
//	 * and error streams to a {@link Document}
//	 * @author gpothier
//	 */
//	private class TextConsoleFeeder extends Thread
//	{
//		private Document itsDocument;
//		
//		public TextConsoleFeeder(Document aDocument)
//		{
//			super("TextConsoleFeeder");
//			itsDocument = aDocument;
//			setDaemon(true);
//			start();
//		}
//
//		public synchronized void kill()
//		{
//			itsDocument = null;
//		}
//		
//		@Override
//		public synchronized void run()
//		{
//			Reader theOutputReader = new InputStreamReader(itsOutputPipe.getInputStream());
//			Reader theErrorReader = new InputStreamReader(itsErrorPipe.getInputStream());
//			try
//			{
//				while (itsDocument != null)
//				{
//					processReader(theOutputReader);
//					processReader(theErrorReader);
//					wait(200);
//				}
//			}
//			catch (InterruptedException e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//		
//		private void processReader(Reader aReader) 
//		{
//			try
//			{
//				char[] theBuffer = new char[4096];
//				int c;
//				while ((c = aReader.read(theBuffer)) > 0)
//				{
//					String theString = new String(theBuffer, 0, c);
//					itsDocument.insertString(itsDocument.getLength(), theString, null);
//				}
//			}
//			catch (Exception e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//	}
//}
