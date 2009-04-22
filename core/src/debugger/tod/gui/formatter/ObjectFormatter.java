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
package tod.gui.formatter;

import tod.Util;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.Hyperlinks;
import zz.utils.AbstractFormatter;

/**
 * @author gpothier
 */
public class ObjectFormatter extends AbstractFormatter
{
	private ILogBrowser itsLogBrowser;

	public ObjectFormatter(ILogBrowser aLogBrowser)
	{
		itsLogBrowser = aLogBrowser;
	}

	/**
	 * This method mimics {@link Hyperlinks#object(tod.gui.IGUIManager, tod.core.database.browser.ILogBrowser, Object, zz.utils.ui.text.XFont)}
	 */
	protected String getText(Object aObject, boolean aHtml)
	{
		// This is only to reduce the amount of modifications to the original method
		Object theCurrentObject = null; 
		
		// Check if this is a registered object.
		if (aObject instanceof ObjectId)
		{
			ObjectId theObjectId = (ObjectId) aObject;
			Object theRegistered = itsLogBrowser.getRegistered(theObjectId);
			if (theRegistered != null) aObject = theRegistered;
		}
		
		if (aObject instanceof ObjectId)
		{
			ObjectId theId = (ObjectId) aObject;
			
			ITypeInfo theType = itsLogBrowser.createObjectInspector(theId).getType();

			String theText;
			if (theCurrentObject != null && theCurrentObject.equals(aObject)) theText = "this";
			else theText = Util.getPrettyName(theType.getName()) + " (" + theId + ")";

			return theText;
		}
		else if (aObject instanceof String)
		{
			String theString = (String) aObject;
			return "\""+theString+"\"";
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
			return theBuilder.toString();
		}
		else 
		{
			return ""+aObject;
		}
	}

}
