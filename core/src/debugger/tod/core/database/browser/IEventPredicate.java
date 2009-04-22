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
 * A predicate, or filter, that accepts or rejects events 
 * @author gpothier
 */
public interface IEventPredicate
{
	/**
	 * Whether the given event matches the predicate.
	 * @param aEvent The event to check. Note that when used as part 
	 * of an event filter, the information of the event might be incomplete,
	 * in particular the behavior/field/class/thread infos might
	 * have only the id field. 
	 */
	public boolean match(ILogEvent aEvent);
}
