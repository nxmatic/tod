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
package tod.gui.components.eventsequences.mural;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import tod.gui.BrowserData;
import zz.utils.ui.UIUtils;

public class AvgMuralPainter extends AbstractMuralPainter
{
	private static AvgMuralPainter INSTANCE = new AvgMuralPainter();

	public static AvgMuralPainter getInstance()
	{
		return INSTANCE;
	}

	private AvgMuralPainter()
	{
	}
	
	/**
	 * Paints the mural, summing the values of all series and averaging the colors
	 */
	@Override
	public long[][] paintMural(
			EventMural aMural, 
			Graphics2D aGraphics, 
			Rectangle aBounds, 
			long aT1, 
			long aT2, List<BrowserData> aBrowserDatas)
	{
		long[][] theValues = getValues(aBounds, aT1, aT2, aBrowserDatas);
		if (theValues.length == 0) return theValues;
		
		int theHeight = aBounds.height;
		int theY = aBounds.y;
		int bh = 4; // base height

		long theMaxT = 0;
		
		// Determine maximum value
		for (int i = 0; i < theValues[0].length; i++)
		{
			long t = 0; 
			for (int j = 0; j < theValues.length; j++) t += theValues[j][i];
			theMaxT = Math.max(theMaxT, t);
		}
		
		for (int i = 0; i < theValues[0].length; i++)
		{
			int t = 0; // Total for current column
			int r = 0;
			int g = 0;
			int b = 0;
			
			for (int j = 0; j < theValues.length; j++)
			{
				long theValue = theValues[j][i];
				Color theColor = aBrowserDatas.get(j).color;
				
				t += theValue;
				r += theValue * theColor.getRed();
				g += theValue * theColor.getGreen();
				b += theValue * theColor.getBlue();
			}
			
			if (t == 0) continue;
			
			Color c1 = new Color(r/t, g/t, b/t);
			Color c2 = UIUtils.getLighterColor(c1);

			// Draw main bar
			aGraphics.setColor(c1);
			aGraphics.fillRect(aBounds.x + i, theY+theHeight-bh, 1, bh);
			
			// Draw proportional bar
			int h = (int) ((theHeight-bh) * t / theMaxT);
			aGraphics.setColor(c2);
			aGraphics.fillRect(aBounds.x + i, theY+theHeight-bh-h, 1, h);
		}
		
		return theValues;
	}
	

}
