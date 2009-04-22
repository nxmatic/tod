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
package tod.gui.activities;

import tod.core.database.browser.ILogBrowser;

/**
 * A seed contains all the information needed to generate a view.
 * A seed can be in two states: inactive and active. When a seed
 * is active, it maintains a reference to a view component.
 * As many seeds can be stored in the browsing history of the GUI,
 * an inactive seed should keep as few references as possible to other
 * objects, so as to free resources. 
 * @author gpothier
 */
public abstract class ActivitySeed
{
	private ILogBrowser itsLog;
	
	public ActivitySeed(ILogBrowser aLog)
	{
		itsLog = aLog;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsLog;
	}
	
	/**
	 * Returns the class of the component that is capable of displaying this seed. 
	 */
	public abstract Class<? extends ActivityPanel> getComponentClass();
	
	/**
	 * A description of the kind (class) of this seed.
	 * Should not include the details of this particular seed.
	 */
	public abstract String getKindDescription();

	/**
	 * A short description for this seed, should not describe the seed kind 
	 * but only the specifics of this particular seed.
	 */
	public abstract String getShortDescription();
}
