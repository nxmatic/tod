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
package tod.impl.common;

import java.util.ArrayList;
import java.util.List;

import tod.core.IBookmarks;
import zz.utils.FireableTreeModel;
import zz.utils.notification.IEvent;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;

public class Bookmarks implements IBookmarks
{
	private final List<Bookmark> itsBookmarks = new ArrayList<Bookmark>();
	private final IFireableEvent<Void> eChanged = new SimpleEvent<Void>();
	
	public void addBookmark(Bookmark aBookmark)
	{
		itsBookmarks.add(aBookmark);
		eChanged.fire(null);
	}

	public void removeBookmark(Bookmark aBookmark)
	{
		itsBookmarks.remove(aBookmark);
		eChanged.fire(null);
	}

	public IEvent<Void> eChanged()
	{
		return eChanged;
	}

	public Iterable<Bookmark> getBookmarks()
	{
		return itsBookmarks;
	}

}
