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
package tod.gui.kit;

import java.awt.Color;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.IGUIManager;
import tod.gui.SeedHyperlink;
import tod.gui.activities.ActivitySeed;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.ui.text.XFont;

/**
 * A hyperlink representing an object.
 * Object details are fetched asynchronously through the provided
 * {@link JobProcessor}.
 * @author gpothier
 */
public class ObjectHyperlink extends SeedHyperlink
{
	private final ObjectId itsObject;

	public ObjectHyperlink(
			IGUIManager aGUIManager,
			ActivitySeed aSeed,
			final ILogBrowser aLogBrowser,
			IJobScheduler aJobScheduler,
			ObjectId aObject, 
			XFont aFont)
	{
		super(aGUIManager, aSeed, "... (" + aObject + ")", XFont.DEFAULT_XUNDERLINED, Color.BLUE);
		itsObject = aObject;
		
		aJobScheduler.submit(JobPriority.AUTO, new Runnable()
			{
				public void run()
				{
					ITypeInfo theType = aLogBrowser.createObjectInspector(itsObject).getType();
					String theText = theType.getName() + " (" + itsObject + ")";
					setText(theText);
				}
			});
		
	}
}
