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
package tod.gui.activities.dyncross;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.LocationUtils;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IBehaviorExitEvent;
import tod.core.database.event.ILogEvent;
import tod.gui.BrowserData;
import tod.gui.activities.dyncross.DynamicCrosscuttingSeed.Highlight;
import tod.gui.components.eventsequences.mural.AbstractMuralPainter;
import tod.gui.components.eventsequences.mural.EventMural;
import zz.utils.cache.MRUBuffer;
import zz.utils.list.IList;
import zz.utils.list.NakedLinkedList.Entry;
import zz.utils.ui.UIUtils;

public class AdviceCFlowMuralPainter extends AbstractMuralPainter
{
	private IList<Highlight> itsHighlightsProperty;
	private Map<EventMural, MuralCache> itsMuralCaches = new WeakHashMap<EventMural, MuralCache>();
	
	public AdviceCFlowMuralPainter(IList<Highlight> aHighlightsProperty)
	{
		itsHighlightsProperty = aHighlightsProperty;
	}

	@Override
	public long[][] paintMural(
			EventMural aMural, 
			Graphics2D aGraphics, 
			Rectangle aBounds, 
			long t1,
			long t2,
			List<BrowserData> aBrowserDatas)
	{
		long[][] theValues = getValues(aBounds, t1, t2, aBrowserDatas);
		int theCount = theValues.length;
		if (theCount == 0) return theValues;
		
		int theTotalMarkSize = 0;
		int[] theMarkYs = new int[theCount];
		int i = 0;
		for (BrowserData theBrowserData : aBrowserDatas) 
		{
			theMarkYs[i++] = theTotalMarkSize;
			theTotalMarkSize += theBrowserData.markSize;
		}
		
		int theHeight = aBounds.height-theTotalMarkSize;
		int theY = aBounds.y;
		
		long theMaxT = 4; // We want to be able to see when a bar corresponds to only one event.
		
		// Determine maximum value
		for (i = 0; i < theValues[0].length; i++)
		{
			long theTotal = 0;
			for (int j = 0; j < theValues.length; j++) theTotal += theValues[j][i]; 
			theMaxT = Math.max(theMaxT, theTotal);
		}
		
		float theBarWidth = 1f*aBounds.width/theValues[0].length;
		
		aGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		MuralCache theMuralCache = null;
		
		// Draw aspect marks
		for (int k=0;k<theCount;k++)
		{
			boolean theSkip0 = false;
			for (int x = 0; x < theValues[0].length; x++)
			{
				float theX = aBounds.x + (x*theBarWidth);
				long v = theValues[k][x];
				BrowserData theBrowserData = aBrowserDatas.get(k);
				
				if (v > 0)
				{
					// There is something here so we draw the mark anyway
					aGraphics.setColor(theBrowserData.color);
					
					aGraphics.fill(makeTrapezoid(
							theX+(theBarWidth/2), 
							theHeight+theMarkYs[k], 
							k == 0 ? theBarWidth+4 : theBrowserData.markSize, 
							k== 0 ? theBarWidth : 0,
							theBrowserData.markSize));
					
					theSkip0 = false;
				}
				else if (k > 0)
				{
					if (theSkip0) continue;
					theSkip0 = true;
					
					// Nothing at this location, check if previous event expands in time.
					
					if (theMuralCache == null) theMuralCache = getMuralCache(aMural);
					Highlight theHighlight = itsHighlightsProperty.get(k-1);
					HighlightCache theCache = theMuralCache.getCache(theHighlight);
					
					// Find out timestamp of current location
					long w = t2-t1;
					long t = t1+(long)(1f*w*theX/aBounds.width);
					
					Range theRange = theCache.getRange(t, theBrowserData);
					if (theRange.inCFlow)
					{
						long dt = theRange.t2-t1;
						int theNextX = (int) (1f*dt*aBounds.width/w);
						aGraphics.setColor(theBrowserData.color);
						aGraphics.fill(new Rectangle2D.Float(
								theX, 
								theHeight+theMarkYs[k]+1, 
								theNextX-theX, 
								theBrowserData.markSize-1));
					}
				}
				
				
			}
		}
		
		for (int x = 0; x < theValues[0].length; x++)
		{
			float theX = aBounds.x + (x*theBarWidth);
			
			int theCurrentMarkY = theHeight;
			float theCurrentBarHeight = 0;
			
			// Draw marks and compute total bar height
			for (int k=0;k<theCount;k++)
			{
				long t = theValues[k][x];
				BrowserData theBrowserData = aBrowserDatas.get(k);
				
				if (t>0)
				{
					aGraphics.setColor(theBrowserData.color);
					
//					aGraphics.fill(makeTrapezoid(
//							theX+(theBarWidth/2), 
//							theY+theCurrentMarkY, 
//							k == 0 ? theBarWidth+4 : theBrowserData.markSize, 
//							k== 0 ? theBarWidth : 0,
//							theBrowserData.markSize));
					
					float h = (theHeight * t) / theMaxT;
					theCurrentBarHeight += h;
				}
				
				theCurrentMarkY += theBrowserData.markSize;
			}
			
			// Draw proportional bars
			for (int j=theCount-1;j>=0;j--)
			{
				long t = theValues[j][x];
				
				if (t>0)
				{
					BrowserData theBrowserData = aBrowserDatas.get(j);

					float h = (theHeight * t) / theMaxT;
					aGraphics.setColor(UIUtils.getLighterColor(theBrowserData.color, 0.7f));
					
					aGraphics.fill(makeTriangle(
							theX+(theBarWidth/2), 
							theHeight-theCurrentBarHeight, 
							theBarWidth, 
							theCurrentBarHeight));
					
					theCurrentBarHeight -= h;
				}
			}
		}
		
		return theValues;
	}

	private MuralCache getMuralCache(EventMural aMural)
	{
		MuralCache theMuralCache = itsMuralCaches.get(aMural);
		if (theMuralCache == null)
		{
			theMuralCache = new MuralCache();
			itsMuralCaches.put(aMural, theMuralCache);
		}
		return theMuralCache;
	}
	
	private static boolean contains(int[] aArray, int aValue)
	{
		if (aArray == null) return false;
		for(int theValue : aArray) if (theValue == aValue) return true;
		return false;
	}
	
	private static boolean startsWith(int[] aArray, int[] aPrefix)
	{
		if (aArray.length < aPrefix.length) return false;
		for(int i=0;i<aPrefix.length;i++) if (aArray[i] != aPrefix[i]) return false;
		return true;
	}
	
	private Range computeRangeInfo(
			long t,
			Highlight aHighlight,
			BrowserData aBrowserData)
	{
		// Get surrounding events
		IEventBrowser theBrowser = aBrowserData.browser.clone();
		
		ILogEvent thePrevious = null;
		ILogEvent theNext = null;
		
		theBrowser.setNextTimestamp(t);
		if (theBrowser.hasPrevious())
		{
			thePrevious = theBrowser.previous();
			theBrowser.next();
		}
		
		if (theBrowser.hasNext()) theNext = theBrowser.next();
		
		long t1 = thePrevious != null ? thePrevious.getTimestamp() : 0;
		long t2 = theNext != null ? theNext.getTimestamp() : Long.MAX_VALUE;
		
		if (thePrevious == null) return new Range(t1, t2, false);
		
		// Check if previous event is really in advice cflow, or only some shadow activity
		boolean theInCFlow = false;
		for (int theSrcId : LocationUtils.getAdviceSourceIds(aHighlight.getLocation()))
		{
			if (contains(thePrevious.getAdviceCFlow(), theSrcId)) 
			{
				theInCFlow = true;
				break;
			}
		}
		if (! theInCFlow) return new Range(t1, t2, false);
		
		// Check if the previous event is a return/after event that ends an advice cflow
		if (thePrevious instanceof IBehaviorExitEvent)
		{
			IBehaviorCallEvent theCallEvent = 
				(IBehaviorCallEvent) theBrowser.getLogBrowser().getEvent(thePrevious.getParentPointer());
			
			if (theCallEvent == null || 
					contains(LocationUtils.getAdviceSourceIds(aHighlight.getLocation()), theCallEvent.getAdviceSourceId())) 
			{
				return new Range(t1, t2, false);
			}
		}
		
		return new Range(t1, t2, theNext != null);
	}
	
	/**
	 * Contains cached data relative to a particular mural.
	 * @author gpothier
	 */
	private class MuralCache
	{
		private Map<Highlight, HighlightCache> itsCaches = new HashMap<Highlight, HighlightCache>();
		
		public HighlightCache getCache(Highlight aHighlight)
		{
			HighlightCache theCache = itsCaches.get(aHighlight);
			if (theCache == null)
			{
				theCache = new HighlightCache(aHighlight);
				itsCaches.put(aHighlight, theCache);
			}
			return theCache;
		}
	}
	
	/**
	 * Contains cached data relative to a particular highlight in a particular mural.
	 * @author gpothier
	 */
	private class HighlightCache
	{
		private List<Entry<Range>> itsRanges = new ArrayList<Entry<Range>>();
		private MRUBuffer<Void, Range> itsMRUBuffer = new MRUBuffer<Void, Range>(100, false)
		{
			@Override
			protected Range fetch(Void aId)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			protected Void getKey(Range aValue)
			{
				throw new UnsupportedOperationException();
			}
			
			@Override
			protected void dropped(Range aValue)
			{
				itsRanges.remove(aValue);
			}
		};
		
		private Highlight itsHighlight;
		
		public HighlightCache(Highlight aHighlight)
		{
			itsHighlight = aHighlight;
		}

		public Range getRange(long t, BrowserData aBrowserData) 
		{
			Range theInstant = new Range(t);
			int theIndex = Collections.binarySearch(itsRanges, new Entry<Range>(theInstant), RangeEntryComparator.getInstance());
			if (theIndex >= 0) 
			{
				Entry<Range> theEntry = itsRanges.get(theIndex);
				itsMRUBuffer.use(theEntry);
				return theEntry.getValue();
			}
			else
			{
				Range theRange = computeRangeInfo(t, itsHighlight, aBrowserData);
				
				int theInsert = -theIndex-1;
				if (theInsert > 0)
				{
					Range thePrevious = itsRanges.get(theInsert-1).getValue();
					if (! (thePrevious.t2 <= theRange.t1))
					{
						assert false;
					}
				}
				
				if (theInsert < itsRanges.size())
				{
					Range theNext = itsRanges.get(theInsert).getValue();
					assert theRange.t2 <= theNext.t1;
				}
				
				Entry<Range> theEntry = itsMRUBuffer.add(theRange);
				itsRanges.add(theInsert, theEntry);
				return theRange;
			}
		}
		
	}

	private static class Range
	{
		public final long t1;
		public final long t2;
		
		public final boolean inCFlow;
		
		private Range(long aT)
		{
			t1 = t2 = aT;
			inCFlow = false;
		}
		
		public Range(long aT1, long aT2, boolean aInCFlow)
		{
			t1 = aT1;
			t2 = aT2;
			assert t2 > t1; 
			
			inCFlow = aInCFlow;
		}
	}
	
	private static class RangeEntryComparator implements Comparator<Entry<Range>>
	{
		private static final RangeEntryComparator INSTANCE = new RangeEntryComparator();

		public static RangeEntryComparator getInstance()
		{
			return INSTANCE;
		}

		private RangeEntryComparator()
		{
		}
		
		public int compare(Entry<Range> e1, Entry<Range> e2)
		{
			Range r1 = e1.getValue();
			Range r2 = e2.getValue();
			
			if (r1.t1 == r1.t2 && r2.t1 <= r1.t1 && r1.t1 <= r2.t2) return 0;
			else if (r2.t1 == r2.t2 && r1.t1 <= r2.t1 && r2.t1 <= r1.t2) return 0;
			else if (r2.t1 == r1.t1 && r2.t2 == r1.t2) return 0;
			else if (r1.t1 >= r2.t2) return 1;
			else if (r2.t1 >= r1.t2) return -1;
			else throw new RuntimeException("Overlap");
		}
		
	}
	
}
