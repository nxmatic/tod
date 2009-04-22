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

import java.util.HashMap;
import java.util.Map;

import tod.core.database.browser.IObjectInspector;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMemberInfo;
import tod.gui.IGUIManager;

/**
 * A {@link tod.gui.components.eventsequences.SequenceViewsDock} specialized for displaying members of a type.
 * @author gpothier
 */
public class MembersDock extends SequenceViewsDock
{
	private Map<IMemberInfo, Integer> itsMembersMap = new HashMap<IMemberInfo, Integer>();
	
	public MembersDock(IGUIManager aGUIManager)
	{
		super (aGUIManager);
	}

	protected IEventSequenceSeed createSeed (IObjectInspector aInspector, IMemberInfo aMember)
	{
		if (aMember instanceof IFieldInfo)
		{
			IFieldInfo theField = (IFieldInfo) aMember;
			return new FieldSequenceSeed(aInspector, theField);
		}
		else if (aMember instanceof IBehaviorInfo)
		{
			IBehaviorInfo theBehavior = (IBehaviorInfo) aMember;
			switch (theBehavior.getBehaviourKind())
			{
			case METHOD:
				return new MethodSequenceSeed(aInspector, theBehavior);
			default:
				throw new RuntimeException("Not handled: "+theBehavior.getBehaviourKind());
			}
		}
		else 				
			throw new RuntimeException("Not handled: "+aMember);
	}
	
	public void addMember (IObjectInspector aInspector, IMemberInfo aMember)
	{
		IEventSequenceSeed theSeed = createSeed(aInspector, aMember);
		int theIndex = pSeeds().size();
		pSeeds().add(theIndex, theSeed);
		itsMembersMap.put(aMember, theIndex);
	}

	public void removeMember (IMemberInfo aMember)
	{
		Integer theIndex = itsMembersMap.get(aMember);
		if (theIndex == null) throw new RuntimeException("Seed not found for member: "+aMember);
		pSeeds().remove(theIndex.intValue());
	}
}
