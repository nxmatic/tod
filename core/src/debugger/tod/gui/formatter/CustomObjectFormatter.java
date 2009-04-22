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
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import tod.core.database.browser.IObjectInspector;
import tod.core.database.structure.ITypeInfo;
import tod.gui.IGUIManager;
import tod.tools.formatting.FormatterFactory;
import tod.tools.formatting.IPyObjectFormatter;
import tod.tools.formatting.ReconstitutedObject;
import zz.utils.Utils;

/**
 * An object formatter defined by the user.
 * @author gpothier
 */
public class CustomObjectFormatter implements Serializable
{
	private static final long serialVersionUID = 3583639814851031766L;
	
	private final CustomFormatterRegistry itsRegistry;
	
	private String itsName;
	
	/**
	 * A Python code snippet that performs the formatting
	 * (short version, should only output one line).
	 * @see FormatterFactory
	 */
	private String itsShortCode;
	
	private transient IPyObjectFormatter itsPyShortFormatter;
	
	/**
	 * A Python code snippet that performs the formatting.
	 * (optional long version, can output multiple lines.)
	 * @see FormatterFactory
	 */
	private String itsLongCode;
	
	private transient IPyObjectFormatter itsPyLongFormatter;
	
	/**
	 * The names of the types handled by this formatter.
	 * We keep names instead of {@link ITypeInfo} because the id of types
	 * can change from one session to the next.
	 */
	private Set<String> itsRecognizedTypes = new HashSet<String>();

	CustomObjectFormatter(CustomFormatterRegistry aRegistry)
	{
		itsRegistry = aRegistry;
	}

	public String getName()
	{
		return itsName;
	}

	public void setName(String aName)
	{
		itsName = aName;
	}


	public String getShortCode()
	{
		return itsShortCode;
	}

	public void setShortCode(String aShortCode)
	{
		itsShortCode = aShortCode;
		itsPyShortFormatter = null;
	}

	public String getLongCode()
	{
		return itsLongCode;
	}

	public void setLongCode(String aLongCode)
	{
		itsLongCode = aLongCode;
		itsPyLongFormatter = null;
	}

	public Iterable<String> getRecognizedTypes()
	{
		return itsRecognizedTypes;
	}

	public void clearRecognizedTypes()
	{
		Iterator<String> theIterator = itsRecognizedTypes.iterator();
		while (theIterator.hasNext())
		{
			String theType = theIterator.next();
			theIterator.remove();
			itsRegistry.unregister(this, theType);
		}
	}
	
	public void addRecognizedType(String aType)
	{
		if (itsRecognizedTypes.add(aType)) itsRegistry.register(this, aType);
	}
	
	public void removeRecognizedType(String aType)
	{
		if (itsRecognizedTypes.remove(aType)) itsRegistry.unregister(this, aType);
	}
	
	/**
	 * Returns one of the formatters.
	 */
	private IPyObjectFormatter getFormatter(boolean aLong)
	{
		if (aLong && itsLongCode != null)
		{
			if (itsPyLongFormatter == null)
				itsPyLongFormatter = FormatterFactory.getInstance().createFormatter(itsLongCode);
			return itsPyLongFormatter;
		}
		else
		{
			if (itsPyShortFormatter == null)
				itsPyShortFormatter = FormatterFactory.getInstance().createFormatter(itsShortCode);
			return itsPyShortFormatter;
		}
	}
	
	/**
	 * Formats the given object, using the short formatter.
	 */
	public String formatShort(IGUIManager aGUIManager, IObjectInspector aInspector)
	{
		return format(false, aGUIManager, aInspector);
	}
	
	/**
	 * Formats the given object, using the long formatter.
	 */
	public String formatLong(IGUIManager aGUIManager, IObjectInspector aInspector)
	{
		return format(true, aGUIManager, aInspector);
	}

	private String format(boolean aLong, IGUIManager aGUIManager, IObjectInspector aInspector)
	{
		IPyObjectFormatter theFormatter;
		try
		{
			theFormatter = getFormatter(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "**Bad formatter: "+Utils.getRootCause(e).getMessage()+"**";
		}
		
		try
		{
			return ""+theFormatter.format(new ReconstitutedObject(aGUIManager, aInspector));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "**Formatting error: "+Utils.getRootCause(e).getMessage()+"**";
		}
	}
}
