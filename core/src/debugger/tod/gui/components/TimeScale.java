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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;

import tod.gui.BrowserData;
import tod.gui.IGUIManager;
import tod.gui.components.eventsequences.mural.EventMural;
import zz.utils.properties.IListProperty;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;
import zz.utils.ui.MouseModifiers;
import zz.utils.ui.Orientation;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UIUtils;

/**
 * This component presents an overview of an {@link tod.core.database.browser.IEventBrowser}
 * in a time axis. 
 * @author gpothier
 */
public class TimeScale extends JPanel
{
	private IRWProperty<Long> pSelectionStart = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			repaint();
		}
	};
	
	private IRWProperty<Long> pSelectionEnd = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			repaint();
		}
	};

	
	private IRWProperty<Long> pCurrentPosition = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			repaint();
		}
	};
	
	private EventMural itsMural;
	
	public TimeScale(IGUIManager aGUIManager)
	{
		super (new StackLayout());
		itsMural = new EventMural(aGUIManager, Orientation.HORIZONTAL, 0, Long.MAX_VALUE);
		
		setPreferredSize(new Dimension(20, 30));
		add (itsMural);
		
		addMouseWheelListener(new MouseWheelListener()
				{
					public void mouseWheelMoved(MouseWheelEvent aEvent)
					{
						if (MouseModifiers.hasAlt(aEvent)) return;
						
						boolean theShift = MouseModifiers.hasShift(aEvent);
						int theAmount = aEvent.getWheelRotation();
						
						if (MouseModifiers.hasCtrl(aEvent))
						{
							// zoom
							long mt = getT (aEvent.getX());
							if (! theShift) theAmount *= 5;
							resizeSelection(mt, (float) Math.exp(0.1 * theAmount));
						}
						else
						{
							long s1t = pSelectionStart().get();
							long s2t = pSelectionEnd().get();
							
							long theOffset = (s2t - s1t) * theAmount / 10;
							if (theShift) theOffset /= 5;
							shiftSelection(theOffset);
						}
					}
				});
	}

	/**
	 * Returns the x coordinate that corresponds to the given timestamp.
	 */
	public int getX (long t)
	{
		long t1 = pStart().get();
		long t2 = pEnd().get();
		
		return (int) (getWidth() * (t - t1) / (t2 - t1));
	}
	
	/**
	 * Returns the timestamp that corresponds to a given coordinate.
	 */
	public long getT (int x)
	{
		long t1 = pStart().get();
		long t2 = pEnd().get();

		return t1 + (x * (t2 - t1) / getWidth());
	}
	
	/**
	 * Offsets the current selection without changing its size.
	 * @param aOffset The quantity to add to the bounds
	 */
	protected void shiftSelection(long aOffset)
	{
		long t1 = pStart().get();
		long t2 = pEnd().get();
		
		long s1t = pSelectionStart().get();
		long s2t = pSelectionEnd().get();
		long l = s2t - s1t;
		if (l > t2-t1) l = t2-t1; // just in case.
		
		s1t += aOffset;
		s2t += aOffset;
		
		if (s1t < t1)
		{
			s1t = t1;
			s2t = s1t + l;
		}
		else if (s2t > t2)
		{
			s2t = t2;
			s1t = s2t - l;
		}
		
		pSelectionStart().set(s1t);
		pSelectionEnd().set(s2t);
	}

	/**
	 * Resizes the selected range and centers it 
	 * around a given position.
	 */
	protected void resizeSelection(long aCenter, float aFactor)
	{
		long t1 = pStart().get();
		long t2 = pEnd().get();
		
		long s1t = pSelectionStart().get();
		long s2t = pSelectionEnd().get();
		
		long l = (long) (aFactor * (s2t - s1t));
		if (l > t2-t1) l = t2-t1;
		
		if (aCenter < s1t || aCenter > s2t) aCenter = (s1t + s2t) / 2;
		
		s1t = aCenter - (l / 2);
		s2t = aCenter + (l / 2);
		
		if (s1t < t1)
		{
			s1t = t1;
			s2t = s1t + l;
		}
		else if (s2t > t2)
		{
			s2t = t2;
			s1t = s2t - l;
		}
		
		pSelectionStart().set(s1t);
		pSelectionEnd().set(s2t);
	}

	/**
	 * Returns the property that contains the starting timestamp of 
	 * the currently selected time range.
	 */
	public IRWProperty<Long> pSelectionStart ()
	{
		return pSelectionStart;
	}
	
	/**
	 * Returns the property that contains the ending timestamp of 
	 * the currently selected time range.
	 */
	public IRWProperty<Long> pSelectionEnd ()
	{
		return pSelectionEnd;
	}
	
	/**
	 * Returns the property that contains the current timestamp.
	 */
	public IRWProperty<Long> pCurrentPosition ()
	{
		return pCurrentPosition;
	}
	
	/**
	 * Returns the property that contains the starting timestamp of 
	 * the displayed time range.
	 */
	public IRWProperty<Long> pStart ()
	{
		return itsMural.pStart;
	}
	
	/**
	 * Returns the property that contains the ending timestamp of 
	 * the displayed time range.
	 */
	public IRWProperty<Long> pEnd ()
	{
		return itsMural.pEnd;
	}
	
	/**
	 * Returns the property that contains the list of event browsers
	 * displayed in this timescale.
	 */
	public IListProperty<BrowserData> pEventBrowsers ()
	{
		return itsMural.pEventBrowsers;
	}
	
	
	/**
	 * We override paint and not paintComponent because we want to paint after children.
	 */
	@Override
	public void paint(Graphics aG)
	{
		super.paint(aG);
		Graphics2D theGraphics = (Graphics2D) aG;
		int h = getHeight();
		
		if (pSelectionStart().get() != null && pSelectionEnd().get() != null)
		{
			long s1t = pSelectionStart().get();
			long s2t = pSelectionEnd().get();
			
			int s1x = getX(s1t);
			int s2x = getX(s2t);
			
			theGraphics.setComposite(UIUtils.ALPHA_04);
			theGraphics.setColor(Color.BLUE);
			theGraphics.fillRect(s1x, 0, s2x - s1x, h);
			
			theGraphics.setComposite(UIUtils.ALPHA_OPAQUE);
		}
		
		if (pCurrentPosition().get() != null)
		{
			long pt = pCurrentPosition().get();
			int px = getX(pt);
		}
		

	}
	
}
