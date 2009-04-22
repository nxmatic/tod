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

import tod.core.database.event.ILogEvent;
import tod.gui.FontConfig;
import tod.gui.IGUIManager;
import tod.gui.kit.html.HtmlBody;
import tod.gui.kit.html.HtmlText;

public class UnknownEventNode extends AbstractSimpleEventNode
{
	private ILogEvent itsEvent;

	public UnknownEventNode(
			IGUIManager aGUIManager, 
			EventListPanel aListPanel,
			ILogEvent aEvent)
	{
		super(aGUIManager, aListPanel);
		itsEvent = aEvent;
		createUI();
	}
	
	@Override
	protected void createHtmlUI(HtmlBody aBody)
	{
		aBody.add(HtmlText.create("Unknown ("+getEvent()+")", FontConfig.NORMAL, Color.GRAY));
		createDebugInfo(aBody);
	}
	
	@Override
	protected ILogEvent getEvent()
	{
		return itsEvent;
	}
}
