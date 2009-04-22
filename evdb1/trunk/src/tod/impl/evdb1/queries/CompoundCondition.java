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
package tod.impl.evdb1.queries;

import java.util.ArrayList;
import java.util.List;

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventFilter;

import zz.utils.Utils;

/**
 * A condition that is compound of various other {@link EventCondition}s.
 * @author gpothier
 */
public abstract class CompoundCondition extends EventCondition
implements ICompoundFilter
{
	private static final long serialVersionUID = 115527489652079546L;

	private List<EventCondition> itsConditions = new ArrayList<EventCondition>();

	protected List<EventCondition> getConditions()
	{
		return itsConditions;
	}
	
	public void addCondition(EventCondition aCondition)
	{
		assert aCondition != null;
		itsConditions.add(aCondition);
	}
	
	public final void add(IEventFilter aFilter) throws IllegalStateException
	{
		addCondition((EventCondition) aFilter);
	}

	public final List<IEventFilter> getFilters()
	{
		return (List) itsConditions;
	}

	public final void remove(IEventFilter aFilter) throws IllegalStateException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected String toString(int aIndent)
	{
		StringBuilder theBuilder = new StringBuilder();
		Utils.indentln(theBuilder, aIndent);
		theBuilder.append(getClass().getSimpleName());
		
		for (EventCondition theCondition : getConditions())
		{
			Utils.indentln(theBuilder, aIndent+2);
			theBuilder.append(theCondition.toString(aIndent+2));
		}
		
		theBuilder.append('\n');
		return theBuilder.toString();
	}
	
	@Override
	public int getClausesCount()
	{
		int theCount = 0;
		for (EventCondition theCondition : getConditions()) 
		{
			theCount += theCondition.getClausesCount();
		}
		
		return theCount;
	}
}
