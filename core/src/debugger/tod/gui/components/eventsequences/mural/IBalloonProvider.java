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
package tod.gui.components.eventsequences.mural;

import java.util.Comparator;
import java.util.List;

import tod.core.database.event.ILogEvent;

import zz.utils.notification.IEvent;

/**
 * Provides ballons for murals
 * @author gpothier
 */
public interface IBalloonProvider
{
	/**
	 * Gets the balloons to show in the given interval.
	 */
	public List<Balloon> getBaloons(long aStartTimestamp, long aEndTimestamp);
	
	/**
	 * Returns an event that is fired when the set of balloons changes.
	 */
	public IEvent<Void> eChanged();
	
	public static class Balloon
	{
		private ILogEvent itsEvent;
		
		/**
		 * HTML text of the balloon
		 */
		private final String itsText;

		public Balloon(ILogEvent aEvent, String aText)
		{
			itsEvent = aEvent;
			itsText = aText;
		}

		public ILogEvent getEvent()
		{
			return itsEvent;
		}

		public long getTimestamp()
		{
			return getEvent().getTimestamp();
		}
		
		public String getText()
		{
			return itsText;
		}
		
		

	}

	/**
	 * Compares the timestamp of balloons.
	 */
	public static Comparator<Balloon> COMPARATOR = new Comparator<Balloon>()
	{
		public int compare(Balloon aO1, Balloon aO2)
		{
			long dt = aO1.getTimestamp() - aO2.getTimestamp();
			
			if (dt < 0) return -1;
			else if (dt == 0) return 0;
			else return 1;
		}
	};
}
