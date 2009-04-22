/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package imageviewer2007;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A {@link JPanel} with reasonable scrolling behavior.
 * @author gpothier
 */
public class ScrollablePanel extends JPanel 
implements Scrollable
{
	public ScrollablePanel()
	{
	}
	
	/**
	 * Creates a scrollable panel with a single child.
	 */
	public ScrollablePanel(JComponent aComponent)
	{
		super(new BorderLayout());
		add(aComponent);
	}

	public ScrollablePanel(LayoutManager aLayout)
	{
		super(aLayout);
	}

	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
	
	public boolean getScrollableTracksViewportWidth()
	{
		return false;
	}

	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	public int getScrollableBlockIncrement(Rectangle aVisibleRect, int aOrientation, int aDirection)
	{
		switch (aOrientation)
		{
		case SwingConstants.HORIZONTAL:
			return 80 * aVisibleRect.width / 100;
			
		case SwingConstants.VERTICAL:
			return 80 * aVisibleRect.height / 100;
			
		default:
			throw new RuntimeException();
		}
	}

	public int getScrollableUnitIncrement(Rectangle aVisibleRect, int aOrientation, int aDirection)
	{
		switch (aOrientation)
		{
		case SwingConstants.HORIZONTAL:
			return 10 * aVisibleRect.width / 100;
			
		case SwingConstants.VERTICAL:
			return 10 * aVisibleRect.height / 100;
			
		default:
			throw new RuntimeException();
		}
	}
	
}