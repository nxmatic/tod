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

import tod.Util;
import tod.core.database.event.IExceptionGeneratedEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.gui.FontConfig;
import tod.gui.Hyperlinks;
import tod.gui.IGUIManager;
import tod.gui.kit.StdOptions;
import tod.gui.kit.html.HtmlBody;
import tod.gui.kit.html.HtmlText;

public class ExceptionGeneratedNode extends AbstractSimpleEventNode
{
	private IExceptionGeneratedEvent itsEvent;

	public ExceptionGeneratedNode(
			IGUIManager aGUIManager, 
			EventListPanel aListPanel,
			IExceptionGeneratedEvent aEvent)
	{
		super(aGUIManager, aListPanel);
		itsEvent = aEvent;
		createUI();
	}
	
	@Override
	protected void createHtmlUI(HtmlBody aBody)
	{
		boolean theRed = getListPanel() != null ?
				getListPanel().getCurrentValue(StdOptions.EXCEPTION_EVENTS_RED)
				: false;
		
		aBody.add(HtmlText.create(
				"Exception: ", 
				FontConfig.NORMAL, 
				theRed ? Color.RED : Color.BLACK));
		
		aBody.add(Hyperlinks.object(
				getGUIManager(),
				Hyperlinks.HTML,
				getJobScheduler(),
				itsEvent.getException(),
				itsEvent,
				showPackageNames()));

		boolean theLocation = getListPanel() != null ?
				getListPanel().getCurrentValue(StdOptions.SHOW_EVENTS_LOCATION)
				: false;
				
		if (theLocation)
		{
			IBehaviorInfo theBehavior = getEvent().getOperationBehavior();
			String theBehaviorName = theBehavior != null ? 
					Util.getSimpleName(theBehavior.getDeclaringType().getName()) + "." + theBehavior.getName() 
					: "<unknown>"; 
			
			aBody.addText(" (in "+theBehaviorName+")");
		}
				
		createDebugInfo(aBody);
	}
	
	@Override
	protected IExceptionGeneratedEvent getEvent()
	{
		return itsEvent;
	}

}
