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

import tod.core.database.event.ILogEvent;

/**
 * Inspector of coumpound entities such as objects or stack frames.
 * A compound entity is composed of entries (eg. variables, fields).
 * @author gpothier
 */
public interface ICompoundInspector<E>
{
	/**
	 * Returns the log browser that created this inspector.
	 */
	public ILogBrowser getLogBrowser();
	
	/**
	 * Sets the reference event of this inspector. Values of entries 
	 * obtained by {@link #getEntryValue(Object)} 
	 * are the values they had at the moment
	 * the reference event was executed.
	 */
	public void setReferenceEvent(ILogEvent aEvent);
	
	/**
	 * Returns the current reference event of this inspector.
	 * Values are reconstituted at the time the reference event occurred.
	 */
	public ILogEvent getReferenceEvent();
	
	/**
	 * Returns the possible values of the specified entry at the time the 
	 * current event was executed.
	 * @return An array of possible values. If there is more than one value,
	 * it means that it was impossible to retrieve an unambiguous value.
	 * This can happen for instance if several write events have
	 * the same timestamp.
	 */
	public EntryValue[] getEntryValue(E aEntry);
	
	/**
	 * Moves the reference event to the next setter, and returns the corresponding value.
	 * Note: the reference event is undefined after calling this method.
	 * However, calling this method repeatedly has the expected effect.
	 */
	public EntryValue[] nextEntryValue(E aEntry);
	
	/**
	 * Moves the reference event to the previous setter, and returns the corresponding value.
	 * Note: the reference event is undefined after calling this method.
	 * However, calling this method repeatedly has the expected effect.
	 */
	public EntryValue[] previousEntryValue(E aEntry);
	
	/**
	 * Groups actual entry value and setter event 
	 * @author gpothier
	 */
	public static class EntryValue
	{
		/**
		 * The value of the entry at a particular point in time.
		 */
		private final Object itsValue;
		
		/**
		 * The event that assigned the entry its value, if available.
		 */
		private final ILogEvent itsSetter;
		
		public EntryValue(Object aValue, ILogEvent aSetter)
		{
			itsValue = aValue;
			itsSetter = aSetter;
		}

		public Object getValue()
		{
			return itsValue;
		}

		public ILogEvent getSetter()
		{
			return itsSetter;
		}
	}

}
