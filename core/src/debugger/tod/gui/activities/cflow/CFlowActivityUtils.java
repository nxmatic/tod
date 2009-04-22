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

import javax.swing.JPanel;

import tod.core.database.event.IBehaviorCallEvent;
import tod.gui.Hyperlinks;
import tod.gui.IGUIManager;
import tod.tools.scheduling.IJobScheduler;
import zz.utils.ui.ZLabel;
import zz.utils.ui.text.XFont;

public class CFlowActivityUtils
{
	/**
	 * Adds the hyperlinks representing the behavior's arguments to the given container.
	 */
	public static void addArguments(
			IGUIManager aGUIManager,
			IJobScheduler aJobScheduler,
			JPanel aContainer,
			IBehaviorCallEvent aRefEvent,
			Object[] aArguments, 
			XFont aFont,
			boolean aShowPackageNames)
	{
		aContainer.add(ZLabel.create("(", aFont, Color.BLACK));
		
		if (aArguments != null)
		{
			boolean theFirst = true;
			for (Object theArgument : aArguments)
			{
				if (theFirst) theFirst = false;
				else aContainer.add(ZLabel.create(", ", aFont, Color.BLACK));
				
				aContainer.add(Hyperlinks.object(
						aGUIManager,
						Hyperlinks.SWING, 
						aJobScheduler,
						theArgument, 
						aRefEvent,
						aShowPackageNames));
			}
		}
		else
		{
			aContainer.add(ZLabel.create("...", aFont, Color.BLACK));
		}
		
		aContainer.add(ZLabel.create(")", aFont, Color.BLACK));
	}
	

}
