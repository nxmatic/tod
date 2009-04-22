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

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;


/**
 * This interface provide methods that give information
 * about a caller-side event.
 * @author gpothier
 */
public interface ICallerSideEvent extends ILogEvent
{
	/**
	 * Returns the information about the location of this event.
	 */
	public ProbeInfo getProbeInfo();
	
	/**
	 * Bytecode index of the call within the calling behavior.
	 * <br/>
	 * This information is available only if the caller behavior
	 * was instrumented.
	 * @return Index of the call, or -1 if not available
	 * @see #getCallingBehavior()
	 */
	public int getOperationBytecodeIndex();
	
	/**
	 * Returns the id of the behavior that performed the operation.
	 */
	public IBehaviorInfo getOperationBehavior();

	/**
	 * Returns the advice source id of the advice that caused the generation of the
	 * code that generated the event.
	 */
	public int getAdviceSourceId();
}
