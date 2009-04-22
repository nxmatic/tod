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
package tod.impl.dbgrid.aggregator;

import tod.impl.database.BufferedBidiIterator;
import tod.impl.dbgrid.DebuggerGridConfig;
import tod.impl.dbgrid.db.RIBufferIterator;
import tod.impl.dbgrid.dispatch.RINodeConnector.StringSearchHit;
import tod.tools.monitoring.MonitoringClient.MonitorId;

/**
 * An iterator that returns string search results provided by a 
 * buffer iterator.
 * @author gpothier
 */
public class StringHitsIterator extends BufferedBidiIterator<StringSearchHit[], Long> 
{
	private RIBufferIterator<StringSearchHit[]> itsSourceIterator;

	public StringHitsIterator(RIBufferIterator<StringSearchHit[]> aSourceIterator)
	{
		itsSourceIterator = aSourceIterator;
	}

	@Override
	protected StringSearchHit[] fetchNextBuffer()
	{
		return itsSourceIterator.next(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
	}

	@Override
	protected StringSearchHit[] fetchPreviousBuffer()
	{
		return itsSourceIterator.previous(MonitorId.get(), DebuggerGridConfig.QUERY_ITERATOR_BUFFER_SIZE);
	}

	@Override
	protected Long get(StringSearchHit[] aBuffer, int aIndex)
	{
		return aBuffer[aIndex].getObjectId();
	}

	@Override
	protected int getSize(StringSearchHit[] aBuffer)
	{
		return aBuffer.length;
	}
	
	
	
}
