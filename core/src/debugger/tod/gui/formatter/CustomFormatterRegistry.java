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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import tod.Util;
import tod.core.DebugFlags;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.IGUIManager;
import zz.utils.ListMap;

/**
 * Manages the set of available custom object formatters.
 * @see CustomObjectFormatter
 * @author gpothier
 */
public class CustomFormatterRegistry implements Serializable
{
	private static final long serialVersionUID = 3343639814828874536L;
	
	private List<CustomObjectFormatter> itsFormatters = new ArrayList<CustomObjectFormatter>();
	
	private ListMap<String, CustomObjectFormatter> itsFormattersMap =
		new ListMap<String, CustomObjectFormatter>();
	
	/**
	 * Returns the default formatter for the given type.
	 */
	public CustomObjectFormatter getFormatter(ITypeInfo aType)
	{
		List<CustomObjectFormatter> theList = itsFormattersMap.get(aType.getName());
		if (theList == null || theList.isEmpty()) return null;
		else return theList.get(0);
	}
	
	/**
	 * Returns the list of formatters.
	 */
	public List<CustomObjectFormatter> getFormatters()
	{
		return itsFormatters;
	}
	
	public CustomObjectFormatter createFormatter()
	{
		CustomObjectFormatter theFormatter = new CustomObjectFormatter(this);
		return theFormatter;
	}
	
	/**
	 * Registers a formatter for the given type.
	 */
	void register(CustomObjectFormatter aFormatter, String aType)
	{
		itsFormattersMap.add(aType, aFormatter);
	}
	
	void unregister(CustomObjectFormatter aFormatter, String aType)
	{
		itsFormattersMap.remove(aType, aFormatter);
	}
	
	/**
	 * Formats the given object using the formatters/log browser of the given gui manager.
	 */
	public static String formatObjectShort(
			IGUIManager aGUIManager, 
			IObjectInspector aInspector,
			boolean aShowPackageNames)
	{
		ITypeInfo theType = aInspector.getType();
		
		CustomFormatterRegistry theRegistry = aGUIManager.getSettings().getCustomFormatterRegistry();
		CustomObjectFormatter theFormatter = theRegistry.getFormatter(theType);
		
		if (theFormatter != null)
		{
			return theFormatter.formatShort(aGUIManager, aInspector);
		}
		else
		{		
			String theName = aShowPackageNames ? theType.getName() : Util.getSimpleName(theType.getName());
			long theId = aInspector.getObject().getId();
			
			String theIdText = DebugFlags.IGNORE_HOST ?
					""+ObjectId.getObjectId(theId)
					: ObjectId.getObjectId(theId) +"." +ObjectId.getHostId(theId);
					
			return theName + " (" + theIdText + ")";
		}
	}

}
