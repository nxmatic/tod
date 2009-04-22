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

import tod.core.database.event.IInstantiationEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.local.LocalBrowser;

/**
 * Instantiation-related filter.
 * @author gpothier
 */
public class InstantiationFilter extends AbstractStatelessFilter
{
	private ITypeInfo itsTypeInfo;
	private ObjectId itsObject;
	
	/**
	 * Creates a filter that accepts any instantiation event.
	 */
	public InstantiationFilter(LocalBrowser aBrowser)
	{
		super (aBrowser);
	}

	/**
	 * Creates a filer that accepts only the instantiation events
	 * for a specific type.
	 */
	public InstantiationFilter(LocalBrowser aBrowser, ITypeInfo aTypeInfo)
	{
		super (aBrowser);
		itsTypeInfo = aTypeInfo;
	}
	
	/**
	 * Creates a filer that accepts only the instantiation events
	 * for a specific object.
	 */
	public InstantiationFilter(LocalBrowser aBrowser, ObjectId aObject)
	{
		super (aBrowser);
		itsObject = aObject;
	}
	
	public boolean accept(ILogEvent aEvent)
	{
		if (aEvent instanceof IInstantiationEvent)
		{
			IInstantiationEvent theEvent = (IInstantiationEvent) aEvent;
			
			if (itsTypeInfo != null && theEvent.getType() != itsTypeInfo) return false;
			if (itsObject != null && ! itsObject.equals(theEvent.getInstance())) return false;
			return true;
		}
		else return false;
	}

}
