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
package tod.core.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a filter that accepts specific classes.
 * @author gpothier
 */
public class ClassFilter implements ClassSelector
{
	private List<ClassSelector> itsSelectors = 
		new ArrayList<ClassSelector>();

	/**
	 * Indicates if this filter is currently empty,
	 * ie. doesn't accept any class.
	 */
	public boolean isEmpty() 
	{
		return itsSelectors.isEmpty();
	}
	
	/**
	 * Enables logging for the specified package.
	 * @param aRecursive Whether to consider subpackages
	 */
	public void addPackage (String aPackageName, boolean aRecursive)
	{
		StaticConfig.getInstance().checkState();
		itsSelectors.add (new PackageCS(aPackageName, aRecursive));
	}
	
	/**
	 * Enables logging for the specified packages.
	 * @param aRecursive Whether to consider subpackages
	 */
	public void addPackages (String[] aPackageNames, boolean aRecursive)
	{
		StaticConfig.getInstance().checkState();
		itsSelectors.add (new PackageCS(aPackageNames, aRecursive));
	}

	/**
	 * Enables logging for the specified class.
	 */
	public void addClass (String aClassName)
	{
		StaticConfig.getInstance().checkState();
		itsSelectors.add (new NameCS(aClassName));
	}

	/**
	 * Enables logging for the specified classes.
	 */
	public void addClasses (String[] aClassNames)
	{
		StaticConfig.getInstance().checkState();
		itsSelectors.add (new NameCS(aClassNames));
	}
	
	public boolean accept(String aName)
	{
		for (ClassSelector theSelector : itsSelectors)
		{
			if (theSelector.accept(aName)) return true;
		}
		return false;
	}



}
