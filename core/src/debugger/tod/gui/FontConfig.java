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

import java.awt.Font;

import zz.utils.ui.text.XFont;

public class FontConfig
{
	private static final float ZOOM = 1.0f;
	
	public static final XFont STD_FONT = XFont.DEFAULT_XPLAIN.deriveFont(13*ZOOM);
	
	/**
	 * Standard font with underline
	 */
	public static final XFont STD_FONT_U = new XFont(STD_FONT.getAWTFont(), true);
	
	public static final XFont STD_HEADER_FONT = XFont.DEFAULT_XPLAIN.deriveFont(Font.BOLD, 13*ZOOM);
	public static final XFont SMALL_FONT = XFont.DEFAULT_XPLAIN.deriveFont(12*ZOOM);
	public static final XFont TINY_FONT = XFont.DEFAULT_XPLAIN.deriveFont(10*ZOOM);
	
	public static final int SMALL = (int) (60*ZOOM);
	public static final int NORMAL = (int) (80*ZOOM);
	public static final int BIG = (int) (100*ZOOM);
}
