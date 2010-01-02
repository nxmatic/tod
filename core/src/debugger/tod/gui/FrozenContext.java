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
package tod.gui;

import tod.core.database.event.ILogEvent;
import tod.gui.kit.Bus;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * A context that cannot be modified.
 * @author gpothier
 */
public class FrozenContext implements IContext
{
	private Bus itsBus;
	private IGUIManager itsGUIManager;
	private IRWProperty<ILogEvent> pSelectedEvent;
	
	private FrozenContext(Bus aBus, IGUIManager aManager, IRWProperty<ILogEvent> aSelectedEvent)
	{
		itsBus = aBus;
		itsGUIManager = aManager;
		pSelectedEvent = aSelectedEvent;
	}

	public Bus getBus()
	{
		return itsBus;
	}

	public IGUIManager getGUIManager()
	{
		return itsGUIManager;
	}

	public IRWProperty<ILogEvent> pSelectedEvent()
	{
		return pSelectedEvent;
	}

	/**
	 * Creates a frozen context that is in the same state as the given context.
	 */
	public static FrozenContext create(IContext aSource)
	{
		IRWProperty<ILogEvent> theProperty= new SimpleRWProperty<ILogEvent>(aSource.pSelectedEvent().get())
		{
			@Override
			protected Object canChange(ILogEvent aOldValue, ILogEvent aNewValue)
			{
				// Note: that might change, but for now:
				// don't just reject, throw an exception so we know who tries to change the value.
				throw new RuntimeException("read-only");
			}
		};
			
		return new FrozenContext(aSource.getBus(), aSource.getGUIManager(), theProperty);
	}
}
