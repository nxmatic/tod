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

import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.ITypeInfo;
import tod.gui.activities.filteredevents.FilterSeed;

/**
 * A factory of {@link ActivitySeed}s.
 * 
 * @author gpothier
 */
public class ActivitySeedFactory 
{
	private static ActivitySeed createSeed(
			ILogBrowser aLog,
			String aTitle,
			IEventFilter aFilter)
	{
		return new FilterSeed(aLog, aTitle, aFilter);
	}
	
	/**
	 * Returns a seed that can be used to view the events that are related to
	 * the specified location info.
	 */
	public static ActivitySeed getDefaultSeed(
			ILogBrowser aLog,
			ILocationInfo aInfo)
	{
		if (aInfo instanceof ITypeInfo)
		{
			ITypeInfo theTypeInfo = (ITypeInfo) aInfo;
			return createSeed(
					aLog,
					"Instantiations of "+theTypeInfo.getName(),
					aLog.createInstantiationsFilter(theTypeInfo));
		}
		else if (aInfo instanceof IBehaviorInfo)
		{
			IBehaviorInfo theBehaviourInfo = (IBehaviorInfo) aInfo;
			return createSeed(
					aLog, 
					"Calls of "+theBehaviourInfo.getName(),
					aLog.createBehaviorCallFilter(theBehaviourInfo));
		}
		else if (aInfo instanceof IFieldInfo)
		{
			IFieldInfo theFieldInfo = (IFieldInfo) aInfo;

			return createSeed(
					aLog, 
					"Assignments of "+theFieldInfo.getName(),
					aLog.createFieldFilter(theFieldInfo));
		}
		else return null;
	}
}
