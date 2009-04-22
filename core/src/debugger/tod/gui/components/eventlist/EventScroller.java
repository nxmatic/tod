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
package tod.gui.components.eventlist;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicArrowButton;

import tod.core.database.browser.IEventBrowser;
import tod.gui.IGUIManager;
import zz.utils.notification.IEvent;
import zz.utils.notification.SimpleEvent;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;
import zz.utils.ui.Autorepeat;
import zz.utils.ui.StackLayout;

/**
 * A widget that permits to scroll in an {@link IEventBrowser}.
 * There are two scrolling modes:
 * <li>Unit scrolling: moves forward or backwards by a certain amount of events
 * <li>Track scrolling: moves to a given timestamp
 * @author gpothier
 */
public class EventScroller extends JPanel
{
	private static final int THICKNESS = 25;
	
	public static enum UnitScroll
	{
		UP, PAGE_UP, DOWN, PAGE_DOWN
	}
	
	private SimpleEvent<UnitScroll> eUnitScroll = new SimpleEvent<UnitScroll>();
	private IRWProperty<Long> pTrackScroll = new SimpleRWProperty<Long>()
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			if (itsUpdating) return;
			itsSlider.setValue((int) ((aNewValue-itsStart)/itsSliderFactor));
		}
	};
	
	private final IGUIManager itsGUIManager;
	private IEventBrowser itsBrowser;
	
	private Adjustable itsSlider;
	private int itsLastValue;
	
	private long itsStart;
	private long itsEnd;
	private boolean itsUpdating = false;
		
	/**
	 * If p is the position of the slider the timestamp t is:
	 * p*{@link #itsSliderFactor} + {@link #itsStart} 
	 */
	private float itsSliderFactor;

	public EventScroller(IGUIManager aGUIManager)
	{
		itsGUIManager = aGUIManager;
		createUI();		
	}
	
	public EventScroller(IGUIManager aGUIManager, IEventBrowser aBrowser, long aStart, long aEnd)
	{
		this(aGUIManager);
		set(aBrowser, aStart, aEnd);
	}

	public IGUIManager getGUIManager()
	{
		return itsGUIManager;
	}
	
	private void createUI()
	{
		// Setup slider
		itsSlider = new JScrollBar(JScrollBar.VERTICAL);
		itsSlider.setUnitIncrement(1);
		itsSlider.setBlockIncrement(2);
		itsSlider.addAdjustmentListener(new AdjustmentListener()
		{
			
			public void adjustmentValueChanged(AdjustmentEvent aE)
			{
				switch(aE.getAdjustmentType())
				{
				case AdjustmentEvent.UNIT_DECREMENT:
					eUnitScroll.fire(UnitScroll.UP);
					break;
					
				case AdjustmentEvent.UNIT_INCREMENT:
					eUnitScroll.fire(UnitScroll.DOWN);
					break;
					
				case AdjustmentEvent.BLOCK_DECREMENT:
					eUnitScroll.fire(UnitScroll.UP);
					eUnitScroll.fire(UnitScroll.UP);
					eUnitScroll.fire(UnitScroll.UP);
					break;
					
				case AdjustmentEvent.BLOCK_INCREMENT:
					eUnitScroll.fire(UnitScroll.DOWN);
					eUnitScroll.fire(UnitScroll.DOWN);
					eUnitScroll.fire(UnitScroll.DOWN);
					break;
					
				case AdjustmentEvent.TRACK:
					int theDelta = itsSlider.getValue()-itsLastValue;
					switch(theDelta)
					{
					case 0:
						break;
						
					case -1:
						eUnitScroll.fire(UnitScroll.UP);
						break;
						
					case 1:
						eUnitScroll.fire(UnitScroll.DOWN);
						break;
						
					case -2:
						eUnitScroll.fire(UnitScroll.UP);
						eUnitScroll.fire(UnitScroll.UP);
						eUnitScroll.fire(UnitScroll.UP);
						break;
						
					case 2:
						eUnitScroll.fire(UnitScroll.DOWN);
						eUnitScroll.fire(UnitScroll.DOWN);
						eUnitScroll.fire(UnitScroll.DOWN);
						break;

					default:
						long theTimestamp = (long)(itsSliderFactor * itsSlider.getValue()) + itsStart;
						itsUpdating = true;
						pTrackScroll.set(theTimestamp);
						itsUpdating = false;
						break;
						
					}
					break;
					
				default:
					throw new RuntimeException("Not handled");
				}
				
				itsLastValue = itsSlider.getValue();
			}
		});
		
		setLayout(new StackLayout());
		add((Component) itsSlider, BorderLayout.CENTER);
	}

	public void set(IEventBrowser aBrowser, long aStart, long aEnd)
	{
		itsBrowser = aBrowser;
		itsStart = aStart;
		itsEnd = aEnd;

		// Setup slider
		long theDelta = itsEnd-itsStart;
		itsSliderFactor = 1;
		if (theDelta > 0)
		{
			while (theDelta > Integer.MAX_VALUE)
			{
				theDelta /= 2;
				itsSliderFactor *= 2;
			}
			
			// We want the largest possible delta so that we can assume
			// a track scroll of 1 is an up movement.
			while (theDelta < Integer.MAX_VALUE/2)
			{
				theDelta *= 2;
				itsSliderFactor /= 2;
			}
		}
		itsSlider.setMinimum(0);
		itsSlider.setMaximum((int) theDelta);
		itsSlider.setValue(0);
	}
	
	/**
	 * This property holds the current tracker timestamp.
	 * The client is responsible of updating the tracker position
	 * according to unit scrolls.
	 */
	public IRWProperty<Long> pTrackScroll()
	{
		return pTrackScroll;
	}

	/**
	 * This event is fired when the user performs unit/block scrolling
	 */
	public IEvent<UnitScroll> eUnitScroll()
	{
		return eUnitScroll;
	}

	
	
}
