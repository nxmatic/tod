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

import tod.core.database.event.IConstructorChainingEvent;
import tod.gui.IGUIManager;
import tod.gui.kit.html.HtmlElement;
import tod.gui.kit.html.HtmlText;

public class ConstructorChainingNode extends BehaviorCallNode
{

	public ConstructorChainingNode(
			IGUIManager aGUIManager, 
			EventListPanel aListPanel,
			IConstructorChainingEvent aEvent)
	{
		super(aGUIManager, aListPanel, aEvent);
	}

	@Override
	protected IConstructorChainingEvent getEvent()
	{
		return (IConstructorChainingEvent) super.getEvent();
	}
	
	@Override
	protected HtmlElement createFullBehaviorName()
	{
		return createShortBehaviorName();
	}
	
	@Override
	protected HtmlElement createShortBehaviorName()
	{
		String theHeader;
		switch(getEvent().getCallType())
		{
		case SUPER:
			theHeader = "super";
			break;
			
		case THIS:
			theHeader = "this";
			break;
			
		case UNKNOWN:
			theHeader = "this/super";
			break;
			
		default:
			throw new RuntimeException("Not handled: "+getEvent().getCallType());
		}
		
		return HtmlText.create(theHeader);
	}
	
}
