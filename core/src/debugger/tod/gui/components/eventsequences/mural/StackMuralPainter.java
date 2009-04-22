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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.List;

import tod.gui.BrowserData;
import zz.utils.ui.UIUtils;

public class StackMuralPainter extends AbstractMuralPainter
{
	private static StackMuralPainter INSTANCE = new StackMuralPainter();

	public static StackMuralPainter getInstance()
	{
		return INSTANCE;
	}

	private StackMuralPainter()
	{
	}
	
	/**
	 * Paints the mural, stacking the series one above another.
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
			long theTotal = 0;
			for (int j = 0; j < theValues.length; j++) theTotal += theValues[j][i]; 
			theMaxT = Math.max(theMaxT, theTotal);
		}
		
		float theBarWidth = 1f*aBounds.width/theValues[0].length;
		
		GeneralPath[] thePaths = new GeneralPath[theCount];
		for(int i=0;i<theCount;i++) thePaths[i] = new GeneralPath();
		
		Object theOriginalAA = aGraphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		aGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < theValues[0].length; i++)
		{
			float theX = aBounds.x + (i*theBarWidth);
			
			int theCurrentMarkY = theHeight;
			float theCurrentBarHeight = 0;
			
			// Draw marks and compute total bar height
			for (int j=0;j<theCount;j++)
			{
				long t = theValues[j][i];
				BrowserData theBrowserData = aBrowserDatas.get(j);
				
				if (t>0)
				{
					aGraphics.setColor(theBrowserData.color);
					
					aGraphics.fill(makeTrapezoid(
							theX+(theBarWidth/2), 
							theY+theCurrentMarkY, 
							j == 0 ? theBarWidth+4 : theBrowserData.markSize, 
							j== 0 ? theBarWidth : 0,
							theBrowserData.markSize));
					
					float h = (theHeight * t) / theMaxT;
					theCurrentBarHeight += h;
				}
				
				theCurrentMarkY += theBrowserData.markSize;
			}
			
			// Draw proportional bars
			for (int j=theCount-1;j>=0;j--)
			{
				long t = theValues[j][i];
				
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
		aGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, theOriginalAA);
		
		return theValues;
	}
	

}
