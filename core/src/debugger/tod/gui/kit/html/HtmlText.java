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
package tod.gui.kit.html;

import java.awt.Color;

import tod.gui.FontConfig;

public class HtmlText extends HtmlElement
{
	public static final int FONT_WEIGHT_NORMAL = 400;
	public static final int FONT_WEIGHT_BOLD = 800;
	
	/**
	 * Relative font size, in percent. 
	 */
	private int itsFontSize = FontConfig.NORMAL;
	private int itsFontWeight = FONT_WEIGHT_NORMAL;
	private Color itsColor;
	private StringBuilder itsExtraStyle;

	private String itsText;
	
	public HtmlText()
	{
	}

	public void setText(String aText)
	{
		itsText = aText;
		update();
	}

	public void setFontSize(int aFontSize)
	{
		itsFontSize = aFontSize;
	}

	public void setFontWeight(int aFontWeight)
	{
		itsFontWeight = aFontWeight;
	}

	public void setColor(Color aColor)
	{
		itsColor = aColor;
	}
	
	public void addExtraStyle(String aKey, String aValue)
	{
		if (itsExtraStyle == null) itsExtraStyle = new StringBuilder();
		itsExtraStyle.append(aKey+": "+aValue+"; ");
	}

	@Override
	public void render(StringBuilder aBuilder)
	{
		aBuilder.append("<span id='");
		aBuilder.append(getId());
		aBuilder.append("' style='");
		
		if (itsFontSize != 100)
		{
			aBuilder.append("font-size: ");
			aBuilder.append(itsFontSize);
			aBuilder.append("%; ");
		}
		
		if (itsColor != null)
		{
			aBuilder.append("color: ");
			aBuilder.append(HtmlUtils.toString(itsColor));
			aBuilder.append("; ");
		}
		
		if (itsFontWeight != FONT_WEIGHT_NORMAL)
		{
			aBuilder.append("font-weight: ");
			aBuilder.append(itsFontWeight);
			aBuilder.append("; "); 
		}
		
		if (itsExtraStyle != null) aBuilder.append(itsExtraStyle);
		
		aBuilder.append("'>");
		aBuilder.append(itsText);
		aBuilder.append("</span>");
	}
	
	public static HtmlText create(String aText, int aFontSize, int aFontWeight, Color aColor)
	{
		HtmlText theText = new HtmlText();
		theText.setText(aText);
		theText.setFontSize(aFontSize);
		theText.setColor(aColor);
		theText.setFontWeight(aFontWeight);
		return theText;
	}
	
	public static HtmlText create(String aText, int aFontSize, Color aColor)
	{
		return create(aText, aFontSize, FONT_WEIGHT_NORMAL, aColor);
	}
	
	public static HtmlText create(String aText, Color aColor)
	{
		return create(aText, FontConfig.NORMAL, FONT_WEIGHT_NORMAL, aColor);
	}
	
	public static HtmlText create(String aText)
	{
		return create(aText, FontConfig.NORMAL, Color.BLACK);
	}
	
	/**
	 * Creates a new html text element using {@link String#format(String, Object...)}.
	 */
	public static HtmlText createf(String aFormat, Object... aArgs)
	{
		return create(String.format(aFormat, aArgs));
	}
}
