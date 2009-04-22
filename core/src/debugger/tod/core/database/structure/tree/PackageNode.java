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

import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ILocationInfo;
import zz.utils.tree.SimpleTree;

public class PackageNode extends LocationNode
{
	public PackageNode(SimpleTree<ILocationInfo> aTree, PackageInfo aValue)
	{
		super(aTree, false, aValue);
	}
	
	/**
	 * Retrieves the package node corresponding to the given name,
	 * creating it if needed.
	 */
	public PackageNode getPackageNode(String aName)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(),
				aName, 
				PackageComparator.PACKAGE);
		
		if (theIndex >= 0) 
		{
			// return existing node
			return (PackageNode) pChildren().get(theIndex);
		}
		else
		{
			// create new node
			PackageInfo thePackage = new PackageInfo(aName);
			PackageNode theNode = new PackageNode(getTree(), thePackage);
			pChildren().add(-theIndex-1, theNode);
			return theNode;
		}
	}
	
	/**
	 * Retrieves the class node corresponding to the given name.
	 */
	public ClassNode getClassNode(String aName)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(), 
				aName,
				PackageComparator.CLASS);
		
		if (theIndex < 0) throw new RuntimeException("Class node not found: "+aName); 
		return (ClassNode) pChildren().get(theIndex);
	}
	
	/**
	 * Adds a new class node
	 */
	public ClassNode addClassNode(IClassInfo aClassInfo, boolean aShowFields, boolean aShowBehaviors)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(), 
				aClassInfo.getName(),
				PackageComparator.CLASS);
		
		if (theIndex >= 0) throw new RuntimeException("Class node already exists: "+aClassInfo); 

		ClassNode theNode = new ClassNode(getTree(), aClassInfo, aShowFields, aShowBehaviors);
		pChildren().add(-theIndex-1, theNode);
		return theNode;
	}
	
	
	/**
	 * Adds a new class node
	 */
	public AspectNode addAspectNode(IAspectInfo aAspect, boolean aShowAdvices)
	{
		int theIndex = Collections.binarySearch(
				pChildren().get(), 
				aAspect.getName(),
				PackageComparator.CLASS);
		
		if (theIndex >= 0) throw new RuntimeException("Aspect node already exists: "+aAspect); 
		
		AspectNode theNode = new AspectNode(getTree(), aAspect, aShowAdvices);
		pChildren().add(-theIndex-1, theNode);
		return theNode;
	}
	
}