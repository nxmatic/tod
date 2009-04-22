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

import java.util.List;

/**
 * A filter that contains a number of subfilters.
 * This is useful for union and intersection of filters.
 * @author gpothier
 */
public interface ICompoundFilter extends IEventFilter
{
	/**
	 * Returns the list of all sub-filters.
	 * @return The lits that backs this compound filter.
	 */
	public List<IEventFilter> getFilters();
	
	/**
	 * Adds a filter to this compound filter.
	 * @param aFilter The filter to add
	 * @throws IllegalStateException Thrown if it is not possible anymore
	 * to change this filter. Implementation might not allow modifications
	 * to filters that have already runned.
	 */
	public void add (IEventFilter aFilter) throws IllegalStateException;
	
	/**
	 * Removes a filter from this compound filter.
	 * @param aFilter The filter to remove
	 * @throws IllegalStateException Thrown if it is not possible anymore
	 * to change this filter. Implementation might not allow modifications
	 * to filters that have already runned.
	 */
	public void remove (IEventFilter aFilter) throws IllegalStateException;
}
