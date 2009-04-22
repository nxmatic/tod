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
package tod.impl.dbgrid.db;


import tod.tools.monitoring.MonitoringClient.MonitorId;
import zz.utils.srpc.IRemote;

/**
 * An iterator that buffers data into packets.
 * @param <T> Should be an array type
 * @author gpothier
 */
public interface RIBufferIterator<T> extends IRemote
{
	/**
	 * Fetches elements following the cursor position, and updates the cursor.
	 * @param aCount Maximum number of elements to fetch.
	 * @return The fetched elements, or null if there are no more events.
	 */
	public T next(MonitorId aMonitorId, int aCount);
	
	/**
	 * Fetches elements preceding the cursor position, and updates the cursor.
	 * @param aCount Maximum number of elements to fetch.
	 * @return The fetched elements, or null if there are no more events.
	 */
	public T previous(MonitorId aMonitorId, int aCount);

}
