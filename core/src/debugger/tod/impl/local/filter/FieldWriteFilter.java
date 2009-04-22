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
package tod.impl.local.filter;

import tod.core.database.event.IFieldWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IFieldInfo;
import tod.impl.local.LocalBrowser;

/**
 * Field write  filter.
 * @author gpothier
 */
public class FieldWriteFilter extends AbstractStatelessFilter
{
	private IFieldInfo itsFieldInfo;
	
	/**
	 * Creates a filter that accepts any field write event.
	 */
	public FieldWriteFilter(LocalBrowser aBrowser)
	{
		this (aBrowser, null);
	}

	/**
	 * Creates a filter that accepts only the field write events 
	 * for a particular field.
	 */
	public FieldWriteFilter(LocalBrowser aBrowser, IFieldInfo aFieldInfo)
	{
		super (aBrowser);
		itsFieldInfo = aFieldInfo;
	}
	
	public boolean accept(ILogEvent aEvent)
	{
		if (aEvent instanceof IFieldWriteEvent)
		{
			IFieldWriteEvent theEvent = (IFieldWriteEvent) aEvent;
			return itsFieldInfo == null 
				|| theEvent.getField() == itsFieldInfo;
		}
		else return false;
	}

}
