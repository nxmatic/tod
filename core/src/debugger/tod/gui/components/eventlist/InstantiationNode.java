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

import tod.Util;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IInstantiationEvent;
import tod.core.database.structure.ITypeInfo;
import tod.gui.IGUIManager;
import tod.gui.kit.html.HtmlElement;
import tod.gui.kit.html.HtmlText;

public class InstantiationNode extends BehaviorCallNode
{

	public InstantiationNode(
			IGUIManager aGUIManager, 
			EventListPanel aListPanel,
			IBehaviorCallEvent aEvent)
	{
		super(aGUIManager, aListPanel, aEvent);
	}

	public IInstantiationEvent getEvent()
	{
		return (IInstantiationEvent) super.getEvent();
	}
	
	@Override
	protected HtmlElement createBehaviorNamePrefix()
	{
		return HtmlText.create("new ");
	}
	
	@Override
	protected HtmlElement createFullBehaviorName()
	{
		return createShortBehaviorName();
	}
	
	@Override
	protected HtmlElement createShortBehaviorName()
	{
		ITypeInfo theType = getEvent().getType();
		return HtmlText.create(showPackageNames() ?
				theType.getName()
				: Util.getSimpleInnermostName(theType.getName()));
	}
	
	@Override
	protected String getResultPrefix()
	{
		return "Created";
	}

	@Override
	protected Object getResult()
	{
		return getEvent().getInstance();
	}
}
