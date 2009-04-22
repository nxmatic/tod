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
package tod.core.database.event;

import tod.core.ILogCollector;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IThreadInfo;

/**
 * Root of the interface graph of logging events.
 * @author gpothier
 */
public interface ILogEvent 
{
	/**
	 * Returns a pointer to this event.
	 * @see ILogBrowser#getEvent(ExternalPointer)
	 */
	public ExternalPointer getPointer();
	
	/**
	 * Identifies the host in which the event occurred.
	 */
	public IHostInfo getHost();
	
	/**
	 * Identifies the thread in which the event occured.
	 */
	public IThreadInfo getThread();
	
	/**
	 * Depth of this event in its control flow stack.
	 */
	public int getDepth();
	
	/**
	 * Timestamp of the event. Its absolute value has no
	 * meaning, but the difference between two timestamps
	 * is a duration in nanoseconds.
	 */
	public long getTimestamp();

	/**
	 * Returns a pointer to the parent event.
	 * Note that this method is more efficient than {@link #getParent()}.
	 */
	public ExternalPointer getParentPointer();
	
	/**
	 * Returns behavior call event corresponding to the behavior execution
	 * during which this event occurred.
	 * Note that calling this method might cause a database access, at
	 * least the first time it is called (implementations of this method 
	 * should cache the result).
	 * If only the identity of the parent event is needed, use 
	 * {@link #getParentPointer()} instead.
	 */
	public IBehaviorCallEvent getParent();
	
	/**
	 * Returns the stack of advices this event is in the cflow of.
	 * @return An array of advice source ids, or null if not in the cflow of any advice.
	 */
	public int[] getAdviceCFlow();
}
