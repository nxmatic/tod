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
package tod.core.database.browser;

import tod.core.database.browser.GroupingEventBrowser.IGroupDefinition;
import tod.core.database.event.ICallerSideEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;

/**
 * This grouping definition permits to group together
 * events that correspond to a same joinpoint shadow.
 * @author gpothier
 */
public class JoinpointShadowGroupingDefinition
implements IGroupDefinition<ShadowId>
{
	private static JoinpointShadowGroupingDefinition INSTANCE = new JoinpointShadowGroupingDefinition();

	public static JoinpointShadowGroupingDefinition getInstance()
	{
		return INSTANCE;
	}

	private JoinpointShadowGroupingDefinition()
	{
	}
	
	public ShadowId getGroupKey(ILogEvent aEvent)
	{
		if (aEvent instanceof ICallerSideEvent)
		{
			ICallerSideEvent theEvent = (ICallerSideEvent) aEvent;
			IBehaviorInfo theOperationBehavior = theEvent.getOperationBehavior();
			
			if (theOperationBehavior == null) return null;
			
			Integer theTag = theOperationBehavior.getTag(
					BytecodeTagType.INSTR_SHADOW, 
					theEvent.getOperationBytecodeIndex());
			
			if (theTag == null || theTag == -1) return null;
			
			return new ShadowId(theEvent.getAdviceSourceId(), theTag);
		}
		else return null;
	}
}
