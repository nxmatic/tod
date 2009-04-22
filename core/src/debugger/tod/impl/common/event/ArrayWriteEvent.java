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
package tod.impl.common.event;

import tod.core.ILogCollector;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.IArrayWriteEvent;

/**
 * @author gpothier
 */
public class ArrayWriteEvent extends Event implements IArrayWriteEvent
{
	private Object itsTarget;
	private int itsIndex;
	private Object itsValue;
	
	
	public ArrayWriteEvent(ILogBrowser aLogBrowser)
	{
		super(aLogBrowser);
	}

	public int getIndex()
	{
		return itsIndex;
	}

	public void setIndex(int aIndex)
	{
		itsIndex = aIndex;
	}

	public Object getTarget()
	{
		return itsTarget;
	}

	public void setTarget(Object aTarget)
	{
		itsTarget = aTarget;
	}

	public Object getValue()
	{
		return itsValue;
	}
	
	public void setValue(Object aValue)
	{
		itsValue = aValue;
	}
}
