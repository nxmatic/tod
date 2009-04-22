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
package tod.tools.formatting;

import java.util.List;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.ICompoundInspector.EntryValue;
import tod.core.database.browser.IObjectInspector.IEntryInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ObjectId;
import tod.gui.Hyperlinks;
import tod.gui.IGUIManager;
import tod.gui.formatter.CustomFormatterRegistry;
import tod.tools.monitoring.Monitored;
import zz.utils.Utils;

/**
 * Represents an object of the debugged VM at a given point in time.
 * @author gpothier
 */
public class ReconstitutedObject
{
	private final IGUIManager itsGUIManager;
	private final IObjectInspector itsInspector;
	private boolean itsClassValid = false;
	private IClassInfo itsClass;
	
	public ReconstitutedObject(IGUIManager aGUIManager, IObjectInspector aInspector)
	{
		itsGUIManager = aGUIManager;
		itsInspector = aInspector;
	}
	
	protected IClassInfo getType()
	{
		if (! itsClassValid)
		{
			itsClass = itsInspector != null ? (IClassInfo) itsInspector.getType() : null;
			itsClassValid = true;
		}
		
		return itsClass;
	}
	
	private IEntryInfo getEntry(String aFieldName)
	{
		List<IEntryInfo> theEntries = itsInspector.getEntries(0, Integer.MAX_VALUE);
		for (IEntryInfo theEntry : theEntries)
		{
			if (theEntry.getName().equals(aFieldName)) return theEntry; 
		}
		return null;
	}

	@Monitored
	public Object get(String aFieldName)
	{
		IEntryInfo theEntry = getEntry(aFieldName);
		
		if (theEntry == null) 
		{
			if (getType().isInScope()) Utils.rtex("Field %s not found in class %s.", aFieldName, getType().getName());
			else Utils.rtex("Class %s is not in scope, cannot access field %s.", getType().getName(), aFieldName);
		}
		
		EntryValue[] theEntryValues = itsInspector.getEntryValue(theEntry);
		if (theEntryValues == null || theEntryValues.length > 1) throw new RuntimeException("What do we do? "+theEntryValues);
		
		Object theValue = theEntryValues.length == 1 ? theEntryValues[0].getValue() : null;
		
		ILogBrowser theLogBrowser = itsInspector.getLogBrowser();
		
		// Check if this is a registered object.
		if (theValue instanceof ObjectId)
		{
			ObjectId theObjectId = (ObjectId) theValue;
			Object theRegistered = theLogBrowser.getRegistered(theObjectId);
			if (theRegistered != null) theValue = theRegistered;
		}

		if (theValue instanceof ObjectId)
		{
			ObjectId theObjectId = (ObjectId) theValue;
			IObjectInspector theInspector = theLogBrowser.createObjectInspector(theObjectId);
			theInspector.setReferenceEvent(itsInspector.getReferenceEvent());
			return FormatterFactory.getInstance().wrap(itsGUIManager, theInspector);
		}
		else if (theValue != null)
		{
			return Hyperlinks.object(
					itsGUIManager, 
					Hyperlinks.TEXT, 
					itsGUIManager != null ? itsGUIManager.getJobScheduler() : null,
					null,
					theValue,
					itsInspector.getReferenceEvent(), 
					false);
		}
		else return "null";
	}
	
	/**
	 * Formats this reconstituted object using the custom formatters.
	 */
	public String format()
	{
		return CustomFormatterRegistry.formatObjectShort(itsGUIManager, itsInspector, false);
	}
	
	@Override
	public boolean equals(Object aObj)
	{
		if (aObj instanceof ReconstitutedObject)
		{
			ReconstitutedObject theOther = (ReconstitutedObject) aObj;
			return theOther.itsInspector.getObject().equals(itsInspector.getObject());
		}
		else return false;
	}
	
	@Override
	public String toString()
	{
		if (itsInspector == null) return "Reconstitution of null";
		return "Reconstitution of "+itsInspector.getObject()+" at "+itsInspector.getReferenceEvent().getTimestamp();
	}
}
