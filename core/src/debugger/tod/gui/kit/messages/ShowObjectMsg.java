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
package tod.gui.kit.messages;

import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;

/**
 * A message that transmits a request to show an object
 * @author gpothier
 */
public class ShowObjectMsg extends Message
{
	public static final String ID = "tod.showObject";
	
	/**
	 * A title for the object.
	 */
	private final String itsTitle;
	
	/**
	 * The object to show.
	 */
	private final ObjectId itsObjectId;
	
	/**
	 * The reference event that indicates which version of the object must be shown.
	 */
	private final ILogEvent itsRefEvent;

	public ShowObjectMsg(String aTitle, ObjectId aObjectId, ILogEvent aRefEvent)
	{
		super(ID);
		itsTitle = aTitle;
		itsObjectId = aObjectId;
		itsRefEvent = aRefEvent;
	}

	public String getTitle()
	{
		return itsTitle;
	}
	
	public ObjectId getObjectId()
	{
		return itsObjectId;
	}

	public ILogEvent getRefEvent()
	{
		return itsRefEvent;
	}
}
