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
package tod.gui.components.eventsequences;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JComponent;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.event.EventComparator;
import tod.core.database.event.ILogEvent;
import tod.gui.BrowserData;
import tod.gui.IGUIManager;
import tod.gui.components.eventsequences.mural.EventMural;
import zz.utils.Cleaner;
import zz.utils.ui.Orientation;

public class BaloonMural extends EventMural
{
	private static final int MAX_BALOONS = 20;
	
	private Cleaner itsBaloonsCleaner = new Cleaner()
	{
		@Override
		protected void clean()
		{
			updateBaloons();
		}
	};

	public BaloonMural(
			IGUIManager aGUIManager,
			Orientation aOrientation,
			long aFirstTimestamp,
			long aLastTimestamp)
	{
		super(aGUIManager, aOrientation, aFirstTimestamp, aLastTimestamp);
	}

	public BaloonMural(
			IGUIManager aGUIManager, 
			Orientation aOrientation,
			IEventBrowser aBrowser,
			long aFirstTimestamp,
			long aLastTimestamp)
	{
		super(aGUIManager, aOrientation, aBrowser, aFirstTimestamp, aLastTimestamp);
	}

	protected void markDirty()
	{
		super.markDirty();
		itsBaloonsCleaner.markDirty();
	}
	
	/**
	 * Updates the set of displayed baloons.
	 */
	protected void updateBaloons()
	{
		removeAll();
		if (! isReady()) return;
		
		// Create the multibrowser.
		ArrayList<IEventBrowser> theBrowsers = new ArrayList<IEventBrowser>(pEventBrowsers.size());
		for (BrowserData theBrowserData : pEventBrowsers)
		{
			theBrowsers.add (theBrowserData.browser);
		}
		MultiBrowser theBrowser = new MultiBrowser(theBrowsers);
		
		// Get parameters
		int w = getWidth();
		int x = 0;
		
		long t1 = pStart.get();
		long t2 = pEnd.get();
		
		if (t1 == t2) return;
		
		long t = t1;
		
		// Start placing baloons
		theBrowser.setCursor(t);
		
		SpaceManager theManager = new SpaceManager(getHeight());
		
		int i = 0;
		while (theBrowser.hasNext() && i<MAX_BALOONS)
		{
			i++;
			ILogEvent theEvent = theBrowser.next();
			t = theEvent.getTimestamp();
			if (t > t2) break;
			
			x = (int) (w * (t - t1) / (t2 - t1));
			
			Range theRange = theManager.getAvailableRange(x);
			if (theRange == null) continue;
			
			JComponent theBaloon = getBaloon(theEvent);
			
			if (theBaloon != null)
			{
				Rectangle2D theBaloonBounds = theBaloon.getBounds(null);
				
				if (theBaloonBounds.getHeight() > theRange.getSpan()) continue;

				int by = (int) theRange.getStart();
				double bw = theBaloonBounds.getWidth();
				double bh = theBaloonBounds.getHeight();
				
				theBaloon.setLocation(x, by);
				add(theBaloon);
				
				theManager.occupy(x, by, bw, bh);
			}
		}
	}
	
	/**
	 * Returns a baloon (tooltip) for the specified event.
	 * @return A Graphic object to display as a baloon next to the representation
	 * of the specified event, or null if no baloon is available.
	 */
	protected JComponent getBaloon(ILogEvent aEvent)
	{
		return null;
	}


	/**
	 * This class permits to place baloons according to available space.
	 * TODO: Implement a better algorithm, for now we use discrete scanlines.
	 * @author gpothier
	 */
	private static class SpaceManager
	{
		private static double K = 4.0;
		private double itsHeight;
		private double[] itsLines;
		
		public SpaceManager(double aHeight)
		{
			itsHeight = aHeight;
			itsLines = new double[(int) (itsHeight/K)];
			
			for (int i = 0; i < itsLines.length; i++) itsLines[i] = -1;
		}

		/**
		 * Returns the biggest available range at the specified position.
		 * @return A {@link Range}, or null if there is no space.
		 */
		public Range getAvailableRange (double aX)
		{
			Range theBiggestRange = null;
			double theStart = -1;
			double theEnd = -1;
			
			for (int i = 0; i < itsLines.length; i++)
			{
				double x = itsLines[i];
				
				if (theStart < 0)
				{
					if (x < aX) theStart = i*K;
				}
				else
				{
					if (x < aX) theEnd = i*K;
					else
					{
						Range theRange = new Range(theStart, theEnd);
						if (theBiggestRange == null || theRange.getSpan() > theBiggestRange.getSpan())
							theBiggestRange = theRange;
						
						theStart = theEnd = -1;
					}
				}
			}
			
			if (theBiggestRange == null && theStart >= 0)
			{
				theBiggestRange = new Range(theStart, theEnd);
			}
			
			return theBiggestRange;
		}
		
		/**
		 * Marks the given bounds as occupied 
		 */
		public void occupy(double aX, double aY, double aW, double aH)
		{
			double theY1 = aY;
			double theY2 = aY+aH;
			
			int theI1 = (int) (theY1 / K);
			int theI2 = (int) (theY2 / K);
			
			for (int i=theI1;i<=theI2;i++) itsLines[i] = aX+aW;
		}
		
	}
	
	private static class Range
	{
		private double itsStart;
		private double itsEnd;
		
		public Range(double aStart, double aEnd)
		{
			itsStart = aStart;
			itsEnd = aEnd;
		}
		
		public double getEnd()
		{
			return itsEnd;
		}

		public double getStart()
		{
			return itsStart;
		}

		public boolean intersects (Range aRange)
		{
			return aRange.getStart() <= getEnd() || getStart() <= aRange.getEnd();
		}
		
		public boolean contains (Range aRange)
		{
			return aRange.getStart() >= getStart() && aRange.getEnd() <= getEnd();			
		}
		
		public double getSpan()
		{
			return itsEnd - itsStart;
		}
	}
	
	/**
	 * Helper class that permits to obtain a sequence of events from 
	 * multiple browsers
	 * @author gpothier
	 */
	private static class MultiBrowser
	{
		private List<IEventBrowser> itsBrowsers;
		
		private Set<IEventBrowser> itsNonEmptyBrowsers = new HashSet<IEventBrowser>();
		
		private SortedMap<ILogEvent, IEventBrowser> itsNextEvents = 
			new TreeMap<ILogEvent, IEventBrowser>(EventComparator.getInstance());
		
		public MultiBrowser(List<IEventBrowser> aBrowsers)
		{
			itsBrowsers = aBrowsers;
		}

		public void setCursor(long aT)
		{
			if (itsNextEvents.isEmpty())
			{
				init(aT);
				return;
			}
			
			ILogEvent theFirstEvent = itsNextEvents.firstKey();
			ILogEvent theLastEvent = itsNextEvents.lastKey();
			
			if (aT < theFirstEvent.getTimestamp())
			{
				init(aT);
			}
			else if (aT > theLastEvent.getTimestamp())
			{
				skip(aT);
			}
			else
			{
				// already in map
				ILogEvent theEvent;
				do
				{
					theEvent = peekNext();
					if (theEvent.getTimestamp() < aT) consume(theEvent);
					else break;
				} while (true);
			}
		}
		
		private void init(long aT)
		{
			itsNextEvents.clear();
			for (IEventBrowser theBrowser : itsBrowsers)
			{
				theBrowser.setNextTimestamp(aT);
				if (theBrowser.hasNext())
				{
					itsNonEmptyBrowsers.add(theBrowser);
					itsNextEvents.put (theBrowser.next(), theBrowser);
				}
			}
		}
		
		private void skip(long aT)
		{
			itsNextEvents.clear();
			for (Iterator<IEventBrowser> theIterator = itsNonEmptyBrowsers.iterator();theIterator.hasNext();)
			{
				IEventBrowser theBrowser = theIterator.next();
				
				theBrowser.setNextTimestamp(aT);
				if (theBrowser.hasNext())
				{
					itsNextEvents.put (theBrowser.next(), theBrowser);
				}
				else theIterator.remove();
			}
		}
		
		/**
		 * Determine if there are more events in this browser
		 */
		public boolean hasNext()
		{
			return ! itsNextEvents.isEmpty();
		}

		/**
		 * Returns the next event and advances the cursor.
		 */
		public ILogEvent next()
		{
			ILogEvent theEvent = peekNext();
			consume(theEvent);
			
			return theEvent;
		}
		
		private ILogEvent peekNext()
		{
			return itsNextEvents.firstKey();
		}
		
		private void consume(ILogEvent aEvent)
		{
			IEventBrowser theBrowser = itsNextEvents.remove(aEvent);
			if (theBrowser.hasNext()) itsNextEvents.put (theBrowser.next(), theBrowser);
			else itsNonEmptyBrowsers.remove(theBrowser);
		}
	}


}
