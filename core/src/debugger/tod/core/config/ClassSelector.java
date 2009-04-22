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

import tod.tools.parsers.ParseException;
import tod.tools.parsers.workingset.AbstractClassSet;
import tod.tools.parsers.workingset.WorkingSetFactory;
import zz.utils.IFilter;

/**
 * A class selector is a filter that accepts or rejects fully qualified
 * class names.
 * @author gpothier
 */
public interface ClassSelector extends IFilter<String>
{
	/**
	 * A class selector that accepts any class.
	 * @author gpothier
	 */
	public static class AllCS implements ClassSelector
	{
		private static AllCS INSTANCE = new AllCS();

		public static AllCS getInstance()
		{
			return INSTANCE;
		}

		private AllCS()
		{
		}
		
		public boolean accept(String aValue)
		{
			return true;
		}
	}
	
	/**
	 * A class selector that accepts classes that have a specific name.
	 * @author gpothier
	 */
	public static class NameCS implements ClassSelector
	{
		private String[] itsNames;

		public NameCS(String aName)
		{
			itsNames = new String[] {aName};
		}

		public NameCS(String... aNames)
		{
			itsNames = aNames;
		}
		
		public boolean accept(String aValue)
		{
			for (String theName : itsNames) if (theName.equals(aValue)) return true;
			return false;
		}
	}
	
	/**
	 * A class selector that accepts only classes that belong to specific packages.
	 * @author gpothier
	 */
	public static class PackageCS implements ClassSelector
	{
	    /**
	     * Indicates if the subpackages should be accepted as well.
	     */
	    private boolean itsRecursive;
	    
	    private String[] itsNames;

	    public PackageCS(String aName, boolean aRecursive)
	    {
	    	this(new String[] {aName}, aRecursive);
	    }

	    public PackageCS(String[] aNames, boolean aRecursive)
	    {
	    	itsNames = aNames;
	        itsRecursive = aRecursive;
	    }

	    public boolean accept(String aName)
		{
	    	for(String theName : itsNames) 
	    	{
	    		if (acceptName(theName, aName)) return true;
	    	}
	    	return false;
		}

		protected boolean acceptName(String aReferenceName, String aCandidateName)
	    {
	        if (itsRecursive) return aCandidateName.startsWith(aReferenceName);
	        else
	        {
	            int theLength = aReferenceName.length();
	            return aCandidateName.startsWith(aReferenceName) && aCandidateName.lastIndexOf('.') <= theLength;
	        }
	    }
	}
	
	/**
	 * A class selector base on a working set.
	 * @author gpothier
	 */
	public static class WorkingSetClassSelector implements ClassSelector
	{
	    private AbstractClassSet itsClassSet;

	    public WorkingSetClassSelector(String aWorkingSet) throws ParseException
	    {
	        itsClassSet = WorkingSetFactory.parseWorkingSet(aWorkingSet);
	    }

	    public boolean accept(String aName)
	    {
	        return itsClassSet.accept(aName);
	    }

	}
}
