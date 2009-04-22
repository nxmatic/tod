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
package tod.gui.components.highlighter;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;
import tod.gui.BrowserData;
import tod.gui.IGUIManager;
import tod.gui.MinerUI;
import tod.gui.components.eventsequences.IEventSequenceSeed;
import tod.gui.components.eventsequences.SequenceViewsDock;
import tod.gui.components.eventsequences.mural.AbstractMuralPainter;
import zz.utils.list.IList;
import zz.utils.list.IListListener;
import zz.utils.list.ZArrayList;
import zz.utils.properties.IRWProperty;

public class EventHighlighter extends JPanel
implements ActionListener
{
	private static final String PROPERTY_INITIAL_CONTEXT = "EventHighlighterView.initialContext";
	
	private final IGUIManager itsGUIManager;
	private final ILogBrowser itsLogBrowser;
	
	public final IList<BrowserData> pHighlightBrowsers = new ZArrayList<BrowserData>()
	{
		@Override
		protected void elementAdded(int aIndex, BrowserData aElement)
		{
			for (IEventSequenceSeed theSeed : itsDock.pSeeds())
			{
				HighlighterSequenceSeed theHighlighterSeed = (HighlighterSequenceSeed) theSeed;
				theHighlighterSeed.pForegroundBrowsers.add(aIndex, aElement);
			}
		}
		
		@Override
		protected void elementRemoved(int aIndex, BrowserData aElement)
		{
			for (IEventSequenceSeed theSeed : itsDock.pSeeds())
			{
				HighlighterSequenceSeed theHighlighterSeed = (HighlighterSequenceSeed) theSeed;
				theHighlighterSeed.pForegroundBrowsers.remove(aIndex);
			}
		}
	};
	
	private JRadioButton itsGlobalButton;
	private JRadioButton itsPerHostButton;
	private JRadioButton itsPerThreadButton;
	
	private SequenceViewsDock itsDock;

	public EventHighlighter(
			IGUIManager aGUIManager, 
			ILogBrowser aLogBrowser)
	{
		super(new BorderLayout());
		itsGUIManager = aGUIManager;
		itsLogBrowser = aLogBrowser;
		createUI();
	}
	
	public IGUIManager getGUIManager()
	{
		return itsGUIManager;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsLogBrowser;
	}
	
	public IRWProperty<Long> pStart()
	{
		return itsDock.pStart();
	}
	
	public IRWProperty<Long> pEnd()
	{
		return itsDock.pEnd();
	}
	
	/**
	 * Returns the kind of context to display at startup. 
	 */
	protected Context getInitialContext()
	{
		String theName = getGUIManager().getSettings().getStringProperty(PROPERTY_INITIAL_CONTEXT, Context.PER_HOST.toString());
		return Context.valueOf(theName);
	}
	
	public static enum Context
	{
		GLOBAL, PER_HOST, PER_THREAD;
	}

	private void createUI()
	{
		itsDock = new SequenceViewsDock(getGUIManager());
		add(itsDock, BorderLayout.CENTER);
		
		itsDock.pStart().set(getLogBrowser().getFirstTimestamp());
		itsDock.pEnd().set(getLogBrowser().getLastTimestamp());

		
		ButtonGroup theGroup = new ButtonGroup();
		
		itsGlobalButton = new JRadioButton("Global");
		theGroup.add(itsGlobalButton);
		
		itsPerHostButton = new JRadioButton("Hosts");
		theGroup.add(itsPerHostButton);
		
		itsPerThreadButton = new JRadioButton("Threads");
		theGroup.add(itsPerThreadButton);

		Context theInitialContext = getInitialContext();
		switch(theInitialContext)
		{
		case GLOBAL:
			itsGlobalButton.setSelected(true);
			global();
			break;
			
		case PER_HOST:
			itsPerHostButton.setSelected(true);
			perHost();
			break;
			
		case PER_THREAD:
			itsPerThreadButton.setSelected(true);
			perThread();
			break;
			
		default:
			throw new RuntimeException("Not handled: "+theInitialContext);
		}

		itsGlobalButton.addActionListener(this);
		itsPerHostButton.addActionListener(this);
		itsPerThreadButton.addActionListener(this);

		
		JPanel theButtonsPanel = new JPanel();
		theButtonsPanel.add(itsGlobalButton);
		theButtonsPanel.add(itsPerHostButton);
		theButtonsPanel.add(itsPerThreadButton);
		add(theButtonsPanel, BorderLayout.NORTH);			
		
		setupBrowsers();
	}
	
	public void setMuralPainter(AbstractMuralPainter aMuralPainter)
	{
		itsDock.setMuralPainter(aMuralPainter);
	}
	
	public void actionPerformed(ActionEvent aE)
	{
		Object theSource = aE.getSource();
		if (theSource == itsGlobalButton) setContext(Context.GLOBAL);
		else if (theSource == itsPerHostButton) setContext(Context.PER_HOST);
		else if (theSource == itsPerThreadButton) setContext(Context.PER_THREAD);
		else throw new RuntimeException("Not handled: "+theSource);
	}
	
	/**
	 * Reloads the current context.
	 */
	public void reloadContext()
	{
		setContext(getInitialContext());
	}
	
	public void setContext(Context aContext)
	{
		switch(aContext) 
		{
		case GLOBAL:
			global();
			break;
			
		case PER_HOST:
			perHost();
			break;
			
		case PER_THREAD:
			perThread();
			break;
			
		default: throw new RuntimeException("Not handled: "+aContext); 
		}

		getGUIManager().getSettings().setProperty(PROPERTY_INITIAL_CONTEXT, aContext.toString());
	}
	
	protected IEventSequenceSeed createGlobalSeed()
	{
		return new HighlighterSequenceSeed("Global", getLogBrowser().createBrowser(), null);
	}
	
	/**
	 * Sets the global aggregation mode.
	 */
	protected void global()
	{
		itsDock.pSeeds().clear();
		itsDock.pSeeds().add(createGlobalSeed());

		setupBrowsers();
	}
	
	protected IEventSequenceSeed createHostSeed(IHostInfo aHost)
	{
		IEventFilter theFilter = getLogBrowser().createHostFilter(aHost);
		return new HighlighterSequenceSeed(
				aHost.getName(),
				getLogBrowser().createBrowser(theFilter),
				null);				
		
	}
	
	
	/**
	 * Sets the per host aggregation mode.
	 */
	protected void perHost()
	{
		itsDock.pSeeds().clear();
		
		for(IHostInfo theHost : getLogBrowser().getHosts())
		{
			itsDock.pSeeds().add(createHostSeed(theHost));				
		}
		
		setupBrowsers();
	}
	
	protected IEventSequenceSeed createThreadSeed(IThreadInfo aThread)
	{ 
		IEventFilter theFilter = getLogBrowser().createThreadFilter(aThread);
		return new HighlighterSequenceSeed(
				"["+aThread.getHost().getName()+"] \""+aThread.getName() + "\"",
				getLogBrowser().createBrowser(theFilter),
				null);				
	}
	
	/**
	 * Sets the per thread aggregation mode.
	 */
	protected void perThread()
	{
		itsDock.pSeeds().clear();
		
		for(IThreadInfo theThread : getLogBrowser().getThreads())
		{
			itsDock.pSeeds().add(createThreadSeed(theThread));				
		}
		
		setupBrowsers();
	}

	/**
	 * Initial forwarding of highlight browsers to the seeds.
	 */
	private void setupBrowsers()
	{
		for (IEventSequenceSeed theSeed : itsDock.pSeeds())
		{
			HighlighterSequenceSeed theHighlighterSeed = (HighlighterSequenceSeed) theSeed;
			for (BrowserData theBrowserData : pHighlightBrowsers)
			{
				theHighlighterSeed.pForegroundBrowsers.add(theBrowserData);
			}
		}
	}
}
