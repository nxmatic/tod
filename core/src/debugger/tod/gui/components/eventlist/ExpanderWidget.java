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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

/**
 * A widget that permits dive into the event to which it is
 * attached
 * @author gpothier
 */
public class ExpanderWidget extends JPanel
{
	public static final int WIDTH = 10;
	public static final int THICKNESS = 4;
	
	public ExpanderWidget(Color aColor)
	{
		setForeground(aColor);
		setOpaque(false);
		setPreferredSize(new Dimension(WIDTH, 10));
	}
	
	public void setColor(Color aColor)
	{
		setForeground(aColor);
	}

	@Override
	protected void paintComponent(Graphics aG)
	{
		super.paintComponent(aG);

		aG.setColor(getForeground());
		aG.fillRect(
				getWidth()/2 - THICKNESS/2, 
				1,
				THICKNESS,
				getHeight() - 2);
	}
}
