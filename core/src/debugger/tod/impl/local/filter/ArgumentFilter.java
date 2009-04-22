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

import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.IFieldWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import tod.impl.local.LocalBrowser;

/**
 * @author gpothier
 */
public class ArgumentFilter extends AbstractStatelessFilter
{
	private ObjectId itsObjectId;
	
	public ArgumentFilter(LocalBrowser aBrowser, ObjectId aObjectId)
	{
		super (aBrowser);
		itsObjectId = aObjectId;
	}
	
	public boolean accept(ILogEvent aEvent)
	{
		if (aEvent instanceof IBehaviorCallEvent)
		{
			IBehaviorCallEvent theEvent = (IBehaviorCallEvent) aEvent;
			for (Object theArgument : theEvent.getArguments())
			{
				if (itsObjectId.equals(theArgument)) return true;
			}
			return false;
		}
		else if (aEvent instanceof IFieldWriteEvent)
		{
			IFieldWriteEvent theEvent = (IFieldWriteEvent) aEvent;
			return itsObjectId.equals(theEvent.getValue());
		}
		else return false;
	}
}
