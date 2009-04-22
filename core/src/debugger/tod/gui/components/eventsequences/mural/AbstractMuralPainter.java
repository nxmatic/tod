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
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.List;

import tod.gui.BrowserData;
import tod.utils.TODUtils;

public abstract class AbstractMuralPainter
{
	/**
	 * The width, in pixels, of each drawn bar.
	 */
	private static final int BAR_WIDTH = 3;
	

	public abstract long[][] paintMural(
			EventMural aMural, 
			Graphics2D aGraphics, 
			Rectangle aBounds, 
			long aT1, 
			long aT2, 
			List<BrowserData> aBrowserDatas);

	protected static Shape makeTriangle(float aX, float aY, float aBaseW, float aHeight)
	{
		GeneralPath thePath = new GeneralPath();
		thePath.moveTo(aX, aY);
		thePath.lineTo(aX+(aBaseW/2), aY+aHeight);
		thePath.lineTo(aX-(aBaseW/2), aY+aHeight);
		thePath.closePath();
		
		return thePath;
	}
	
	protected static Shape makeTrapezoid(float aX, float aY, float aBaseW, float aTopW, float aHeight)
	{
		GeneralPath thePath = new GeneralPath();
		thePath.moveTo(aX-(aTopW/2), aY);
		thePath.lineTo(aX+(aTopW/2), aY);
		thePath.lineTo(aX+(aBaseW/2), aY+aHeight);
		thePath.lineTo(aX-(aBaseW/2), aY+aHeight);
		thePath.closePath();
		
		return thePath;
	}
	
	protected long[][] getValues(Rectangle aBounds, long aT1, long aT2, List<BrowserData> aBrowserData)
	{
		if (aT1 == aT2) return null;
		long[][] theValues = new long[aBrowserData.size()][];
		
		int theSamplesCount = aBounds.width / BAR_WIDTH;
		
		int i = 0;
		for (BrowserData theBrowserData : aBrowserData)
		{
			// TODO: check conversion
			TODUtils.log(2, "[EventMural] Requesting counts: "+aT1+"-"+aT2);
			theValues[i] = theBrowserData.browser.getEventCounts(aT1, aT2, theSamplesCount, false);
			if (theValues[i] == null)
			{
				System.out.println("AbstractMuralPainter.getValues()");
			}
			i++;
		}
		
		return theValues;
	}


}
