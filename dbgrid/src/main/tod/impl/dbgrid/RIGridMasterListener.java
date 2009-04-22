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
package tod.impl.dbgrid;

import zz.utils.monitoring.Monitor.MonitorData;
import zz.utils.srpc.IRemote;

/**
 * A remote interface for a listener of {@link GridMaster}
 * @author gpothier
 */
public interface RIGridMasterListener extends IRemote
{
	/**
	 * Called asynchronously after one ore more events are received.
	 * This method will not be called at short intervals, there will be
	 * typically at least one second between calls.
	 */
	public void eventsReceived();
	
	/**
	 * Called when an exception occurred in the grid.
	 */
	public void exception(Throwable aThrowable);
	
	/**
	 * Called when new monitoring info has been received from a database node
	 */
	public void monitorData(int aNodeId, MonitorData aData);
	
	/**
	 * Called when the trace capture has been enabled or disabled.
	 */
	public void captureEnabled(boolean aEnabled);
}
