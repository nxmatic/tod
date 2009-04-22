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
package tod.gui.activities.cflow;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

import tod.Util;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ITypeInfo;
import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.tools.scheduling.IJobScheduler;
import zz.utils.ui.ZLabel;

/**
 * A normal stack node, corresponding to a behavior call event
 * @author gpothier
 */
public class NormalStackNode extends AbstractStackNode
{
	private ITypeInfo itsType;
	private Object[] itsArguments;
	private String itsBehaviorName;

	public NormalStackNode(
			IJobScheduler aJobScheduler, 
			ILogEvent aEvent,
			CallStackPanel aCallStackPanel)
	{
		super(aJobScheduler, aEvent, aCallStackPanel);
	}

	@Override
	public IBehaviorCallEvent getFrameEvent()
	{
		return (IBehaviorCallEvent) super.getFrameEvent();
	}
	
	@Override
	protected void runJob()
	{
		super.runJob();
		
		StringBuilder theBuilder = new StringBuilder();

		// Create caption
		IBehaviorInfo theBehavior = getFrameEvent().getExecutedBehavior();
		if (theBehavior == null) theBehavior = getFrameEvent().getCalledBehavior();
		itsType = theBehavior.getDeclaringType();
		itsArguments = getFrameEvent().getArguments();
		
		// Type.method
		theBuilder.append(Util.getSimpleName(itsType.getName()));
		theBuilder.append(".");
		theBuilder.append(theBehavior.getName());
		
		itsBehaviorName = theBuilder.toString();
	}
	
	@Override
	protected JComponent createHeader()
	{
		JPanel theContainer = new JPanel(GUIUtils.createStackLayout());
		theContainer.setOpaque(false);
		
		// Arguments
//		theBuilder.append("(");
//		
//		if (theArguments != null)
//		{
//			boolean theFirst = true;
//			for (Object theArgument : theArguments)
//			{
//				if (theFirst) theFirst = false;
//				else theBuilder.append(", ");
//				
//				theBuilder.append(getView().getFormatter().formatObject(theArgument));
//			}
//		}
//		else
//		{
//			theBuilder.append("...");
//		}
//		
//		theBuilder.append(")");

		ZLabel theLabel1 = ZLabel.create(
				Util.getPackageName(itsType.getName()), 
				FontConfig.TINY_FONT, 
				Color.DARK_GRAY);
		theLabel1.addMouseListener(this);
		add(theLabel1);
		
		ZLabel theLabel2 = ZLabel.create(
				itsBehaviorName, 
				FontConfig.SMALL_FONT, 
				Color.BLACK);
		theLabel2.addMouseListener(this);
		add(theLabel2);
		
		return theContainer;
	}
}
