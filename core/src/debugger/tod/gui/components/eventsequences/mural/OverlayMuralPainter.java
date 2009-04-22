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
import java.awt.RenderingHints;
import java.util.List;

import tod.gui.BrowserData;
import zz.utils.ui.UIUtils;

public class OverlayMuralPainter extends AbstractMuralPainter
{
	private static OverlayMuralPainter INSTANCE = new OverlayMuralPainter();

	public static OverlayMuralPainter getInstance()
	{
		return INSTANCE;
	}

	private OverlayMuralPainter()
	{
	}
	
	/**
	 * Paints the mural, overlaying all the series.
	 */
	public long[][] paintMural(
			EventMural aMural, 
			Graphics2D aGraphics, 
			Rectangle aBounds, 
			long aT1, 
			long aT2, List<BrowserData> aBrowserDatas)
	{
		long[][] theValues = getValues(aBounds, aT1, aT2, aBrowserDatas);
		
		int theCount = theValues.length;
		if (theCount == 0) return theValues;
		
		int theTotalMarkSize = 0;
		for (BrowserData theBrowserData : aBrowserDatas) theTotalMarkSize += theBrowserData.markSize;
		
		int theHeight = aBounds.height-theTotalMarkSize;
		int theY = aBounds.y;
		
		long theMaxT = 4; // We want to be able to see when a bar corresponds to only one event.
		
		// Determine maximum value
		for (int i = 0; i < theValues[0].length; i++)
		{
			for (int j = 0; j < theValues.length; j++) theMaxT = Math.max(theMaxT, theValues[j][i]);
		}
		
		Object theOriginalAA = aGraphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		for (int i = 0; i < theValues[0].length; i++)
		{
			int theCurrentMarkY = theHeight;
			for (int j=0;j<theCount;j++)
			{
				long t = theValues[j][i];
				BrowserData theBrowserData = aBrowserDatas.get(j);
				Color c1 = theBrowserData.color;
				Color c2 = UIUtils.getLighterColor(c1);
				
				if (t>0)
				{
					// Draw mark
					aGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					aGraphics.setColor(c1);
					aGraphics.fill(makeTriangle(aBounds.x + i, theY+theCurrentMarkY, theBrowserData.markSize, 0));
					
					// Draw proportional bar
					aGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
					int h = (int) ((theHeight * t) / theMaxT);
					aGraphics.setColor(c2);
					aGraphics.fillRect(aBounds.x + i, theY+theHeight-h, 1, h);
				}

				theCurrentMarkY += theBrowserData.markSize;
			}
		}
		aGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, theOriginalAA);
		
		return theValues;
	}
	

}
