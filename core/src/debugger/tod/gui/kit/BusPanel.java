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
package tod.gui.kit;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import zz.utils.ui.MousePanel;

/**
 * A panel that has a bus (but does not own it)
 * @author gpothier
 */
public class BusPanel extends MousePanel
{
	private final Bus itsBus;

	public BusPanel(Bus aBus)
	{
		itsBus = aBus;
	}

	public BusPanel(LayoutManager aLayout, Bus aBus)
	{
		super(aLayout);
		itsBus = aBus;
	}

	public BusPanel(boolean aIsDoubleBuffered, Bus aBus)
	{
		super(aIsDoubleBuffered);
		itsBus = aBus;
	}

	public BusPanel(LayoutManager aLayout, boolean aIsDoubleBuffered, Bus aBus)
	{
		super(aLayout, aIsDoubleBuffered);
		itsBus = aBus;
	}

	/**
	 * Returns the bus used by this panel.
	 */
	public Bus getBus()
	{
		return itsBus;
	}
	
	
}
