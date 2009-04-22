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

import java.util.Map;
import java.util.StringTokenizer;

import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase;
import zz.utils.tree.SimpleTree;
import zz.utils.tree.SimpleTreeNode;

/**
 * Provides various utiliy methods to create trees of some
 * structural nodes.
 * @author gpothier
 */
public class StructureTreeBuilders
{
	/**
	 * Creates a tree of packages and classes. 
	 * Classes can optionally have their members as children
	 * @return
	 */
	public static SimpleTree<ILocationInfo> createClassTree(
			IStructureDatabase aStructureDatabase,
			boolean aShowFields, 
			boolean aShowBehaviors)
	{
		IClassInfo[] theClasses = aStructureDatabase.getClasses();
		
		SimpleTree<ILocationInfo> theTree = new SimpleTree<ILocationInfo>()
		{
			protected SimpleTreeNode<ILocationInfo> createRoot()
			{
				return new PackageNode(this, new PackageInfo("Classes"));
			}
		};
		PackageNode theRoot = (PackageNode) theTree.getRoot();
		
		for (IClassInfo theClass : theClasses)
		{
			String theName = theClass.getName();
			StringTokenizer theTokenizer = new StringTokenizer(theName, ".");
			
			PackageNode theCurrentNode = theRoot;
			while (theTokenizer.hasMoreTokens())
			{
				String theToken = theTokenizer.nextToken();
				if (theTokenizer.hasMoreTokens())
				{
					// Token is still part of package name
					theCurrentNode = theCurrentNode.getPackageNode(theToken);
				}
				else
				{
					// We reached the class name
					theCurrentNode.addClassNode(theClass, aShowFields, aShowBehaviors);
				}
			}
		}
		
		return theTree;
	}

	/**
	 * Creates a tree of packages, aspects and optionally advices. 
	 */
	public static SimpleTree<ILocationInfo> createAspectTree(
			IStructureDatabase aStructureDatabase,
			boolean aShowAdvices)
	{
		Map<String, IAspectInfo> theAspectInfoMap = aStructureDatabase.getAspectInfoMap();
		
		SimpleTree<ILocationInfo> theTree = new SimpleTree<ILocationInfo>()
		{
			protected SimpleTreeNode<ILocationInfo> createRoot()
			{
				return new PackageNode(this, new PackageInfo("Aspects"));
			}
		};
		PackageNode theRoot = (PackageNode) theTree.getRoot();
		
		for (IAspectInfo theAspect : theAspectInfoMap.values())
		{
			String theName = theAspect.getName();
			StringTokenizer theTokenizer = new StringTokenizer(theName, ".");
			
			PackageNode theCurrentNode = theRoot;
			while (theTokenizer.hasMoreTokens())
			{
				String theToken = theTokenizer.nextToken();
				if (theTokenizer.hasMoreTokens())
				{
					// Token is still part of package name
					theCurrentNode = theCurrentNode.getPackageNode(theToken);
				}
				else
				{
					// We reached the class name
					theCurrentNode.addAspectNode(theAspect, aShowAdvices);
				}
			}
		}
		
		return theTree;
			}
	
}
