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

import javax.swing.Action;

import tod.gui.BrowserNavigator;
import tod.gui.activities.ActivitySeed;

/**
 * A browser-like back button that can also show the navigation stack
 * @author gpothier
 */
public class NavForwardButton extends AbstractNavButton
{
	public NavForwardButton(BrowserNavigator<ActivitySeed> aNavigator)
	{
		super(aNavigator);
	}

	@Override
	protected Action getAction()
	{
		return getNavigator().getForwardAction();
	}

	@Override
	protected Iterable<ActivitySeed> getSeedsStack()
	{
		return getNavigator().getForwardSeeds();
	}
	
}
