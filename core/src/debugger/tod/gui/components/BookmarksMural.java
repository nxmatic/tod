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
package tod.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import tod.core.IBookmarks;
import tod.core.IBookmarks.Bookmark;
import tod.core.IBookmarks.EventBookmark;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ILogEvent;
import tod.gui.IGUIManager;
import tod.gui.components.eventsequences.GlobalSequenceSeed;
import tod.gui.components.eventsequences.SequenceViewsDock;
import tod.gui.components.eventsequences.mural.IBalloonProvider;
import tod.gui.kit.Bus;
import tod.gui.kit.IBusListener;
import tod.gui.kit.Options;
import tod.gui.kit.StdOptions;
import tod.gui.kit.html.HtmlUtils;
import tod.gui.kit.messages.EventSelectedMsg;
import tod.gui.kit.messages.ShowCFlowMsg;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;

/**
 * This panel displays the bookmarks of an {@link IBookmarks},
 * and lets 
 * @author gpothier
 */
public class BookmarksMural extends JPanel
{
	private final IGUIManager itsGUIManager;
	private final ILogBrowser itsLogBrowser;
	private final IBookmarks itsBookmarks;
	
	private final IEventListener<Void> itsBookmarksListener = new IEventListener<Void>()
	{
		public void fired(IEvent< ? extends Void> aEvent, Void aData)
		{
			eChanged.fire(null);
		}
	};
	
	private ILogEvent itsCurrentEvent;
	
	private SequenceViewsDock itsDock;
	
	private final IFireableEvent<Void> eChanged = new SimpleEvent<Void>();
	
	public BookmarksMural(IGUIManager aGUIManager, ILogBrowser aLogBrowser, IBookmarks aBookmarks)
	{
		itsGUIManager = aGUIManager;
		itsLogBrowser = aLogBrowser;
		itsBookmarks = aBookmarks;

		createUI();
	}
	
	private void createUI()
	{
		itsDock = new SequenceViewsDock(itsGUIManager);
		itsDock.setPreferredStripeHeight(30);
		itsDock.setShowStripeTitle(false);

		setLayout(new BorderLayout());
		add (itsDock, BorderLayout.CENTER);

		itsDock.pSeeds().add(new GlobalSequenceSeed(itsLogBrowser));
		itsDock.getMural(0).setBalloonProvider(new BookmarksBalloonProvider());
	}
	
	public void setCurrentEvent(ILogEvent aCurrentEvent)
	{
		itsCurrentEvent = aCurrentEvent;
		eChanged.fire(null);
	}
	
	/**
	 * Translates bookmarks to balloons.
	 * @author gpothier
	 */
	private class BookmarksBalloonProvider implements IBalloonProvider
	{
		public List<Balloon> getBaloons(long aStartTimestamp, long aEndTimestamp)
		{
			List<Balloon> theResult = new ArrayList<Balloon>();
			
			Iterable<Bookmark> theBookmarks = itsBookmarks.getBookmarks();
			for (Bookmark theBookmark : theBookmarks)
			{
				if (theBookmark instanceof EventBookmark)
				{
					EventBookmark theEventBookmark = (EventBookmark) theBookmark;
					ILogEvent theEvent = theEventBookmark.getItem();
					if (theEvent.equals(itsCurrentEvent)) continue;
					
					long theTimestamp = theEvent.getTimestamp();
					if (theTimestamp < aStartTimestamp || theTimestamp > aEndTimestamp) continue;
					
					String theText = theEventBookmark.name;
					if (theText == null) theText = theEvent.toString();
					
					Color theColor = theEventBookmark.color;
					if (theColor == null) theColor = Color.BLACK;
					
					Balloon theBalloon = new Balloon(
							theEvent,
							"<span style='color: "+HtmlUtils.toString(theColor)+"'>"+theText+"</span>");
					
					theResult.add(theBalloon);
				}
			}
			
			if (itsCurrentEvent != null)
			{
				long theTimestamp = itsCurrentEvent.getTimestamp();
				if (theTimestamp >= aStartTimestamp && theTimestamp <= aEndTimestamp) 
				{
					Balloon theBalloon = new Balloon(itsCurrentEvent, "<span style='color: blue'>X</span>");
					theResult.add(theBalloon);
				}
			}
			
			Collections.sort(theResult, IBalloonProvider.COMPARATOR);
			return theResult;
		}

		public IEvent<Void> eChanged()
		{
			return eChanged;
		}
		
		
	}
}
