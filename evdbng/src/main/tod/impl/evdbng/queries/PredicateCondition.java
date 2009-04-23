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
package tod.impl.evdbng.queries;


import tod.core.database.browser.IEventPredicate;
import tod.impl.database.AbstractFilteredBidiIterator;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.messages.GridEvent;
import tod.impl.evdbng.db.IEventList;
import tod.impl.evdbng.db.Indexes;
import tod.impl.evdbng.db.file.Tuple;

/**
 * Represents a condition that filters events based on a predicate
 * @author gpothier
 */
public class PredicateCondition extends SimpleCondition<Tuple>
{
	private static final long serialVersionUID = 2132857394221092337L;
	
	private final EventCondition<Tuple> itsBaseCondition;
	private final IEventPredicate itsPredicate;


	public PredicateCondition(
			EventCondition<? extends Tuple> aBaseCondition, 
			IEventPredicate aPredicate)
	{
		itsBaseCondition = (EventCondition) aBaseCondition;
		itsPredicate = aPredicate;
	}

	
	@Override
	public IBidiIterator<Tuple> createTupleIterator(
			final IEventList aEventList, 
			Indexes aIndexes, 
			long aEventId)
	{
		IBidiIterator<Tuple> theBaseIterator = itsBaseCondition.createTupleIterator(aEventList, aIndexes, aEventId);
		
		return new AbstractFilteredBidiIterator<Tuple, Tuple>(theBaseIterator)
		{
			@Override
			protected Object transform(Tuple aInput)
			{
				long theEventId = aInput.getKey();
				assert theEventId < Integer.MAX_VALUE;
				
				GridEvent theEvent = aEventList.getEvent((int) theEventId);
				return itsPredicate.match(theEvent.toLogEvent(null)) ? aInput : REJECT;
			}
		};
	}

	@Override
	public boolean _match(GridEvent aEvent)
	{
		return itsPredicate.match(aEvent.toLogEvent(null));
	}
	
	@Override
	protected String toString(int aIndent)
	{
		return String.format("Predicate = %s on %s", itsPredicate, itsBaseCondition);
	}

}
