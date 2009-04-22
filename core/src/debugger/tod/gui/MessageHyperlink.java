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

import tod.gui.kit.Bus;
import tod.gui.kit.messages.Message;
import zz.utils.ui.ZHyperlink;
import zz.utils.ui.text.XFont;

public class MessageHyperlink extends ZHyperlink
{
	private Message itsMessage;
	
	public MessageHyperlink(String aText, XFont aFont, Color aColor, Message aMessage)
	{
		super(aText, aFont, aColor);
		itsMessage = aMessage;
	}

	
	public void setMessage(Message aMessage)
	{
		itsMessage = aMessage;
	}


	@Override
	protected void traverse()
	{
		Bus.get(this).postMessage(itsMessage);
	}
	
	/**
	 * Creates a new flow text with default size computer.
	 */
	public static MessageHyperlink create(
			Message aMessage, 
			String aText, 
			XFont aFont, 
			Color aColor)
	{
		XFont theFont = aFont.isUnderline() ?
				new XFont(aFont.getAWTFont(), false) 
				: aFont;
		
		MessageHyperlink theHyperlink = new MessageHyperlink(aText, theFont, aColor, aMessage);
		
		return theHyperlink;
	}
	
	/**
	 * Creates a new flow text with default size computer and font.
	 */
	public static MessageHyperlink create(Message aMessage, String aText, Color aColor)
	{
		return create(aMessage, aText, XFont.DEFAULT_XUNDERLINED, aColor);
	}

	/**
	 * Creates a new flow text with default size computer and default font
	 * of the given size.
	 */
	public static MessageHyperlink create(Message aMessage, String aText, float aFontSize, Color aColor)
	{
		return create(aMessage, aText, XFont.DEFAULT_XUNDERLINED.deriveFont(aFontSize), aColor);
	}

}
