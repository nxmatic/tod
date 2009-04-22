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

/**
 * This message is sent when an event is activated (eg. double clicked).
 * @author gpothier
 */
public class EventActivatedMsg extends Message
{
	public static final String ID = "tod.eventActivated";

	/**
	 * The activated event.
	 */
	private final ILogEvent itsEvent;
	
	/**
	 * The way the event was selected (selection, stepping...)
	 */
	private final ActivationMethod itsSelectionMethod;
	
	public EventActivatedMsg(ILogEvent aEvent, ActivationMethod aMethod)
	{
		super(ID);
		itsEvent = aEvent;
		itsSelectionMethod = aMethod;
	}

	public ILogEvent getEvent()
	{
		return itsEvent;
	}

	public ActivationMethod getActivationMethod()
	{
		return itsSelectionMethod;
	}

	public static abstract class ActivationMethod
	{
		public static final ActivationMethod DOUBLE_CLICK = new AM_DoubleClick();
	}
	
	public static class AM_DoubleClick extends ActivationMethod
	{
		private AM_DoubleClick()
		{
		}
	}
}
