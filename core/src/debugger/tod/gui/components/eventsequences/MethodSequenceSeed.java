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
package tod.gui.components.eventsequences;

import tod.core.database.browser.IObjectInspector;
import tod.core.database.structure.IBehaviorInfo;
import tod.gui.IGUIManager;

/**
 * Sequence seed for a particular field.
 * @author gpothier
 */
public class MethodSequenceSeed implements IEventSequenceSeed
{
	private IBehaviorInfo itsMethod;
	private final IObjectInspector itsInspector;

	public MethodSequenceSeed(IObjectInspector aInspector, IBehaviorInfo aMethod)
	{
		itsInspector = aInspector;
		itsMethod = aMethod;
	}

	public IEventSequenceView createView(IGUIManager aGUIManager)
	{
		return new MethodSequenceView(aGUIManager, itsInspector, itsMethod);
	}

}
