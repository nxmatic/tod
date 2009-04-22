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
import tod.core.database.structure.IBehaviorInfo;
import tod.gui.IGUIManager;
import tod.gui.kit.html.HtmlElement;
import tod.gui.kit.html.HtmlGroup;
import tod.gui.kit.html.HtmlText;
import tod.gui.kit.html.HtmlUtils;

public class MethodCallNode extends BehaviorCallNode
{
	public MethodCallNode(
			IGUIManager aGUIManager, 
			EventListPanel aListPanel,
			IBehaviorCallEvent aEvent)
	{
		super(aGUIManager, aListPanel, aEvent);
	}

	protected HtmlElement createShortBehaviorName()
	{
		IBehaviorInfo theBehavior = getBehavior();
		return HtmlText.create(HtmlUtils.escapeHTML(
				theBehavior != null ? theBehavior.getName() : "Error: null behavior"));
	}
	
	protected HtmlElement createFullBehaviorName()
	{
		HtmlGroup theGroup = new HtmlGroup();
		IBehaviorInfo theBehavior = getBehavior();
		if (showPackageNames()) theGroup.add(createPackageName());
		theGroup.addText(Util.getSimpleName(getBehavior().getDeclaringType().getName()));
		theGroup.addText(".");
		theGroup.addText(HtmlUtils.escapeHTML(theBehavior.getName()));
		
		return theGroup;
	}

	@Override
	protected String getResultPrefix()
	{
		return "Returned";
	}
	

}
