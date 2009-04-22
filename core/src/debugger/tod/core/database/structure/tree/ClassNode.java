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
package tod.core.database.structure.tree;

import java.util.Collections;

import tod.Util;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.ILocationInfo;
import zz.utils.tree.SimpleTree;

public class ClassNode extends LocationNode
{
	private final boolean itsShowFields;
	private final boolean itsShowBehaviors;

	public ClassNode(
			SimpleTree<ILocationInfo> aTree, 
			IClassInfo aClass, 
			boolean aShowFields,
			boolean aShowBehaviors)
	{
		super(aTree, ! (aShowFields || aShowBehaviors), aClass);
		itsShowFields = aShowFields;
		itsShowBehaviors = aShowBehaviors;
	}

	public IClassInfo getClassInfo()
	{
		return (IClassInfo) getLocation();
	}
	
	@Override
	protected void init()
	{
		System.out.println("Init for "+getClassInfo());
		
		if (itsShowFields) for(IFieldInfo theField : getClassInfo().getFields())
			addFieldNode(theField);

		if (itsShowBehaviors) for(IBehaviorInfo theBehavior : getClassInfo().getBehaviors())
			addBehaviorNode(theBehavior);
	}
	
	/**
	 * Adds a new behavior node
	 */
	public BehaviorNode addBehaviorNode(IBehaviorInfo aBehavior)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(), 
				Util.getFullName(aBehavior),
				MemberNodeComparator.BEHAVIOR);
		
		if (theIndex >= 0) throw new RuntimeException("Behavior already exists: "+aBehavior); 
		BehaviorNode theNode = new BehaviorNode(getTree(), aBehavior);

		pChildren().add(-theIndex-1, theNode);
		return theNode;
	}
	
	/**
	 * Adds a new field node
	 */
	public FieldNode addFieldNode(IFieldInfo aField)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(), 
				aField.getName(),
				MemberNodeComparator.FIELD);
		
		if (theIndex >= 0) throw new RuntimeException("Field already exists: "+aField); 
		FieldNode theNode = new FieldNode(getTree(), aField);

		pChildren().add(-theIndex-1, theNode);
		return theNode;
	}

}