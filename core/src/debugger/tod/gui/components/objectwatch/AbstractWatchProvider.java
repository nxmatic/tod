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
package tod.gui.components.objectwatch;

import java.util.List;

import javax.swing.JComponent;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.ICompoundInspector.EntryValue;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import tod.gui.IGUIManager;
import tod.tools.scheduling.IJobScheduler;

/**
 * Provider of watch data.
 * @author gpothier
 */
public abstract class AbstractWatchProvider
{
	private final IGUIManager itsGUIManager;
	private final String itsTitle;
	
	public AbstractWatchProvider(IGUIManager aGUIManager, String aTitle)
	{
		itsTitle = aTitle;
		itsGUIManager = aGUIManager;
	}

	/**
	 * Builds the title of the watch window.
	 * @param aJobProcessor A job processor that can be used if elements
	 * of the title are to be created asynchronously.
	 */
	public abstract JComponent buildTitleComponent(IJobScheduler aJobScheduler);
	
	/**
	 * Returns a title for this watch provider.
	 */
	public String getTitle()
	{
		return itsTitle;
	}
	
	public IGUIManager getGUIManager()
	{
		return itsGUIManager;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return getGUIManager().getSession().getLogBrowser();
	}

	/**
	 * Returns a current object. Currently this is only for
	 * stack frame reconstitution, represents the "this" variable.
	 */
	public abstract ObjectId getCurrentObject();
	
	/**
	 * Returns the currently inspected object, if any.
	 * For stack frames, returns null. 
	 */
	public abstract ObjectId getInspectedObject();
	
	/**
	 * Returns the event that serves as a temporal reference for the watched objects.
	 */
	public abstract ILogEvent getRefEvent();
	
	/**
	 * Returns the number of available entries.
	 * This should not be time consuming.
	 */
	public abstract int getEntryCount();
	
	/**
	 * Returns all or a subset of the available entries.
	 * This might be a time-consuming operation, depending on the 
	 * size of the range and the type of target.
	 */
	public abstract List<Entry> getEntries(int aRangeStart, int aRangeSize);

	public static abstract class Entry
	{
		/**
		 * Returns the name of this entry.
		 * This method should execute quickly.
		 */
		public abstract String getName();
		
		/**
		 * Returns the possible values for this entry.
		 * This might be a time-consuming operation.
		 */
		public abstract EntryValue[] getValue();
		
		/**
		 * Returns the next value taken by this entry.
		 * This might be a time-consuming operation.
		 */
		public abstract EntryValue[] getNextValue();

		/**
		 * Returns the previous value taken by this entry.
		 * This might be a time-consuming operation.
		 */
		public abstract EntryValue[] getPreviousValue();
	}
}
