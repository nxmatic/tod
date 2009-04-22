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

/**
 * This event corresponds to the call and execution of 
 * any behavior (method, constructor, ...).
 * <br/> 
 * Available information will vary depending on the instrumentation 
 * at the caller and callee sites.
 */
public interface IBehaviorCallEvent extends IParentEvent, ICallerSideEvent, ITargetEvent
{
	/**
	 * The arguments passed to the behavior.
	 * <br/>
	 * This information is always available.
	 */
	public Object[] getArguments();
	
	/**
	 * The behavior that is actually executed. 
	 * It might be different than {@link #getCalledBehavior() },
	 * for instance if the caller calls an interface or overridden method.
	 * <br/>
	 * This information is always available.
	 */
	public IBehaviorInfo getExecutedBehavior();
	
	/**
	 * The called behavior.
	 * <br/>
	 * This information is available only if the caller behavior
	 * was instrumented.
	 */
	public IBehaviorInfo getCalledBehavior();
	
	/**
	 * The object on which the behavior was called, or
	 * null if static.
	 * <br/>
	 * This information is always available.
	 */
	public Object getTarget();
	
	/**
	 * The behavior that requested the call.
	 * <br/>
	 * This information is available only if the caller behavior
	 * was instrumented.
	 * @return Calling behavior, or null if not available
	 */
	public IBehaviorInfo getCallingBehavior();
	
	/**
	 * Returns the event that corresponds to the end of this behavior.
	 * This method can return null, for instance if the program
	 * terminates with {@link System#exit(int)}.
	 */
	public IBehaviorExitEvent getExitEvent();
	

	
}
