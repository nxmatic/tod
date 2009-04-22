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
package tod.core.database.browser;

import tod.Util;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.Hyperlinks;
import tod.tools.monitoring.Monitored;

/**
 * Utilities for working with object ids or refs.
 * @author gpothier
 */
public class ObjectIdUtils
{
	/**
	 * Returns the type of the given object.
	 * Adapted from {@link Hyperlinks#object}
	 */
	@Monitored
	public static String getObjectDescription(
			ILogBrowser aLogBrowser, 
			Object aObject, 
			boolean aShowPackageNames)
	{
		String theId = null;
		// Check if this is a registered object.
		if (aObject instanceof ObjectId)
		{
			ObjectId theObjectId = (ObjectId) aObject;
			theId = theObjectId.getDescription();
			Object theRegistered = aLogBrowser.getRegistered(theObjectId);
			if (theRegistered != null) aObject = theRegistered;
		}
		
		String theDescription;
		
		if (aObject instanceof ObjectId)
		{
			ObjectId theObjectId = (ObjectId) aObject;
			
			ITypeInfo theType = aLogBrowser.createObjectInspector(theObjectId).getType();
			theDescription = aShowPackageNames ? theType.getName() : Util.getSimpleName(theType.getName());
		}
		else if (aObject instanceof String)
		{
			String theString = (String) aObject;
			theDescription = "String: \""+theString+"\"";
		}
		else if (aObject instanceof Throwable)
		{
			Throwable theThrowable = (Throwable) aObject;
			StringBuilder theBuilder = new StringBuilder();
			theBuilder.append(theThrowable.getClass().getSimpleName());
			if (theThrowable.getMessage() != null)
			{
				theBuilder.append('(');
				theBuilder.append(theThrowable.getMessage());
				theBuilder.append(')');
			}
			theDescription = theBuilder.toString();
		}
		else 
		{
			theDescription = ""+aObject;
		}
		
		return theId != null ? theDescription + " (" + theId + ")" : theDescription;
	}
}
