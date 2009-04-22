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
package tod.gui.settings;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import tod.gui.components.eventlist.IntimacyLevel;
import zz.utils.FireableTreeModel;
import zz.utils.notification.IEvent;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;

/**
 * Holds the intimacy/obliviousness settings for each aspect/advice.
 * @author gpothier
 */
public class IntimacySettings implements Serializable
{
	private Map<Integer, IntimacyLevel> itsIntimacyMap = new HashMap<Integer, IntimacyLevel>();
	
	/**
	 * This event is fired whenever the intimacy settings change.
	 */
	public final IEvent<Void> eChanged = new SimpleEvent<Void>();
	
	public IntimacyLevel getIntimacyLevel(int aAdviceSourceId)
	{
		return itsIntimacyMap.get(aAdviceSourceId);
	}
	
	public void setIntimacyLevel(int aAdviceSourceId, IntimacyLevel aLevel)
	{
		itsIntimacyMap.put(aAdviceSourceId, aLevel);
		((IFireableEvent<Void>) eChanged).fire(null);
	}
	
	public void clear()
	{
		itsIntimacyMap.clear();
		((IFireableEvent<Void>) eChanged).fire(null);		
	}
}
