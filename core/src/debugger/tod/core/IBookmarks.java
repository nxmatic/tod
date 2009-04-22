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
package tod.core;

import java.awt.Color;
import java.awt.Shape;
import java.util.Comparator;

import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import zz.utils.notification.IEvent;

/**
 * The model for bookmarks.
 * Events and objects can be bookmarked. A name, color or symbol can be
 * associated with each bookmarked item.
 * @author gpothier
 */
public interface IBookmarks
{
	/**
	 * Returns all registered bookmarks.
	 */
	public Iterable<Bookmark> getBookmarks();
	
	public void addBookmark(Bookmark aBookmark);
	public void removeBookmark(Bookmark aBookmark);
	
	/**
	 * This event is fired whenever bookmarks are added or removed.
	 */
	public IEvent<Void> eChanged();
	
	
	public static abstract class Bookmark<T>
	{
		public final String name;
		public final Color color;
		public final Shape shape;
		
		public Bookmark(Color aColor, String aName, Shape aShape)
		{
			color = aColor;
			name = aName;
			shape = aShape;
		}
		
		/**
		 * Returns the bookmarked item
		 */
		public abstract T getItem();
	}
	
	public static class EventBookmark extends Bookmark<ILogEvent>
	{
		private ILogEvent itsEvent;
		
		/**
		 * Whether the control flow of the event should be marked
		 * (only meaningful for behavior call events)
		 */
		private boolean itsMarkControlFlow;

		public EventBookmark(Color aColor, String aName, Shape aShape, ILogEvent aEvent, boolean aMarkControlFlow)
		{
			super(aColor, aName, aShape);
			itsEvent = aEvent;
			itsMarkControlFlow = aMarkControlFlow;
		}
		
		@Override
		public ILogEvent getItem()
		{
			return itsEvent;
		}
		
		public boolean getMarkControlFlow()
		{
			return itsMarkControlFlow;
		}
	}
	
	public static class ObjectBookmark extends Bookmark<ObjectId>
	{
		private ObjectId itsObject;

		public ObjectBookmark(Color aColor, String aName, Shape aShape, ObjectId aObject)
		{
			super(aColor, aName, aShape);
			itsObject = aObject;
		}
		
		@Override
		public ObjectId getItem()
		{
			return itsObject;
		}
	}
	
	/**
	 * A comparator for event bookmarks.
	 */
	public static final Comparator<EventBookmark> EVENT_COMPARATOR = new Comparator<EventBookmark>()
	{
		public int compare(EventBookmark aO1, EventBookmark aO2)
		{
			long dt = aO1.getItem().getTimestamp() - aO2.getItem().getTimestamp();
			if (dt < 0) return -1;
			else if (dt == 0) return 0;
			else return 1;
		}
	};
}
