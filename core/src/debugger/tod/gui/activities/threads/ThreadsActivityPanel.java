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
package tod.gui.activities.threads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import tod.core.database.structure.IThreadInfo;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.activities.ActivityPanel;
import tod.gui.components.eventsequences.SequenceViewsDock;
import tod.gui.components.eventsequences.ThreadSequenceSeed;
import zz.utils.Utils;

/**
 * A view that lets the user select a thread and displays all the events 
 * of this thread.
 * @author gpothier
 */
public class ThreadsActivityPanel extends ActivityPanel<ThreadsSeed>
{
	private SequenceViewsDock itsDock;
	private Map<IThreadInfo, ThreadSequenceSeed> itsSeedsMap = 
		new HashMap<IThreadInfo, ThreadSequenceSeed>();
	
	private JLabel itsEventsCountLabel;
	private JLabel itsDroppedEventsCountLabel;
	private long itsLastEventCount = -1;
	private int itsLastThreadCount = -1;

	
	private Timer itsTimer;
	
	public ThreadsActivityPanel(IContext aContext)
	{
		super(aContext);
		createUI();
	}
	
	@Override
	protected void connectSeed(ThreadsSeed aSeed)
	{
		connect(aSeed.pRangeStart(), itsDock.pStart());
		connect(aSeed.pRangeEnd(), itsDock.pEnd());
		
		itsTimer.start();
		update();
	}

	@Override
	protected void disconnectSeed(ThreadsSeed aSeed)
	{
		itsTimer.stop();

		disconnect(aSeed.pRangeStart(), itsDock.pStart());
		disconnect(aSeed.pRangeEnd(), itsDock.pEnd());
	}

	private void createUI()
	{
		itsDock = new SequenceViewsDock(getGUIManager());
		itsEventsCountLabel = new JLabel();
		itsDroppedEventsCountLabel = new JLabel();
		itsDroppedEventsCountLabel.setForeground(Color.RED);
		
		JPanel theCountsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		theCountsPanel.add(itsEventsCountLabel);
		theCountsPanel.add(itsDroppedEventsCountLabel);
		
		setLayout(new BorderLayout());
		add (theCountsPanel, BorderLayout.NORTH);
		add (itsDock, BorderLayout.CENTER);
		
		itsTimer = new Timer(1000, new ActionListener()
						{
							public void actionPerformed(ActionEvent aE)
							{
								update();
							}
						});
		
	}
	
	private void update()
	{
		long theEventCount = getLogBrowser().getEventsCount();

		List<IThreadInfo> theThreads = new ArrayList<IThreadInfo>();
		Utils.fillCollection(theThreads, getLogBrowser().getThreads());
		int theThreadCount = theThreads.size();
		
		if (theEventCount != itsLastEventCount || theThreadCount != itsLastThreadCount)
		{
			itsLastEventCount = theEventCount;
			itsLastThreadCount = theThreadCount;
			
			itsEventsCountLabel.setText(String.format("Events registered: %,d", theEventCount));
			
			long theDropped = getLogBrowser().getDroppedEventsCount();
			if (theDropped > 0) itsDroppedEventsCountLabel.setText("DROPPED: "+theDropped);
			
			Collections.sort(theThreads, IThreadInfo.ThreadIdComparator.getInstance());
			
			for (IThreadInfo theThread : theThreads)
			{
				ThreadSequenceSeed theSeed = itsSeedsMap.get(theThread);
				if (theSeed == null)
				{
					theSeed = new ThreadSequenceSeed(getLogBrowser(), theThread);
					itsSeedsMap.put(theThread, theSeed);
					
					itsDock.pSeeds().add(theSeed);
				}
			}
		}
	}
	
}
