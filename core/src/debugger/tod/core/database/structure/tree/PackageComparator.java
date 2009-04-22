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

import java.util.Comparator;

import tod.core.database.structure.ILocationInfo;
import zz.utils.tree.SimpleTreeNode;

/**
 * Compares packages and classes.
 * Packages are always before classes, otherwise lexicographic order is used.
 * @author gpothier
 */
public class PackageComparator implements Comparator
{
	public static PackageComparator PACKAGE = new PackageComparator(true);
	public static PackageComparator CLASS = new PackageComparator(false);

	/**
	 * If true, compares against package names (package names always appear before
	 * class names).
	 */
	private boolean itsForPackage;
	
	private PackageComparator(boolean aForPackage)
	{
		itsForPackage = aForPackage;
	}
	
	public int compare(Object o1, Object o2)
	{
		SimpleTreeNode<ILocationInfo> node = (SimpleTreeNode<ILocationInfo>) o1;
		String name = (String) o2;
		
		ILocationInfo l = node.pValue().get();
		boolean p = l instanceof PackageInfo;
		
		if (p != itsForPackage) return p ? -1 : 1;
		else return l.getName().compareTo(name);
	}
}