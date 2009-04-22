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
package tod.gui.activities.structure;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.ILocationInfo;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * Seed for {@link StructureActivityPanel}
 * @author gpothier
 */
public class StructureSeed extends ActivitySeed
{
	private final IRWProperty<ILocationInfo> pSelectedLocation = new SimpleRWProperty<ILocationInfo>();

	public StructureSeed(ILogBrowser aLog)
	{
		super(aLog);
	}
	
	public StructureSeed(ILogBrowser aLog, ILocationInfo aLocation)
	{
		this(aLog);
		pSelectedLocation.set(aLocation);
	}
	
	/**
	 * This property defines the currently selected location (class, method, etc.).
	 */
	public IRWProperty<ILocationInfo> pSelectedLocation()
	{
		return pSelectedLocation;
	}

	@Override
	public Class< ? extends ActivityPanel> getComponentClass()
	{
		return StructureActivityPanel.class;
	}

	@Override
	public String getKindDescription()
	{
		return "Classes";
	}

	@Override
	public String getShortDescription()
	{
		ILocationInfo theLocation = pSelectedLocation.get();
		return theLocation != null ? ""+theLocation : null;
	}

	
	
}
