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
package tod.gui;

import java.awt.Color;

import tod.core.database.browser.IEventBrowser;

/**
 * Data agregate for browsers that are used in an {@link tod.gui.components.eventsequences.mural.EventMural}
 * or a {@link tod.gui.components.TimeScale}. Apart from an {@link tod.core.database.browser.IEventBrowser}
 * it contains a color that indicates how the events of the broswser should be rendered,
 * and the size of an optional marker displayed below columns that contain events.
 * @author gpothier
 */
public class BrowserData
{
	public static final int DEFAULT_MARK_SIZE = 5;
	public static final Color DEFAULT_COLOR = Color.BLACK;
	
	public final IEventBrowser browser;
	public final Color color;
	public final int markSize;
	
	public BrowserData(IEventBrowser aBrowser)
	{
		this(aBrowser, DEFAULT_COLOR, DEFAULT_MARK_SIZE);
	}
	
	public BrowserData(IEventBrowser aBrowser, Color aColor)
	{
		this(aBrowser, aColor, DEFAULT_MARK_SIZE);
	}
	
	public BrowserData(IEventBrowser aBrowser, Color aColor, int aMarkSize)
	{
		browser = aBrowser;
		color = aColor;
		markSize = aMarkSize;
	}
}
