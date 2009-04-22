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
package tod.tools.parsers.workingset;

import java.util.ArrayList;
import java.util.List;

public class SetOperation
{
    /**
     * Whether this operation includes or excludes classes.
     */
    private boolean itsInclude;

    /**
     * List of {@link AbstractClassSet}
     */
    private List<AbstractClassSet> itsSubsets = new ArrayList<AbstractClassSet>();

    public SetOperation(String aInclude, List<AbstractClassSet> aSubsets)
    {
        if ("+".equals(aInclude)) itsInclude = true;
        else if ("-".equals(aInclude)) itsInclude = false;
        else throw new RuntimeException("Operation type should be + or -, got: " + aInclude);
        itsSubsets = aSubsets;
    }

    /**
     * Returns whether one of the subsets of this operation accepts the given
     * class.
     */
    public boolean accept(String aClassname)
    {
    	for (AbstractClassSet theSubset : itsSubsets)
		{
            if (theSubset.accept(aClassname)) return true;
        }
        return false;
    }

    public boolean isInclude()
    {
        return itsInclude;
    }

}