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

public class CompoundClassSet extends AbstractClassSet
{
    /**
     * List of {@link SetOperation}
     */
    private List<SetOperation> itsOperations = new ArrayList<SetOperation>();

    public CompoundClassSet(List<SetOperation> aOperations)
    {
        itsOperations = aOperations;
    }

    public boolean accept(String aClassname)
    {
        SetOperation theFirstOperation = itsOperations.get(0);

        for (int i = itsOperations.size() - 1; i >= 0; i--)
        {
            SetOperation theOperation = itsOperations.get(i);

            if (theOperation.accept(aClassname)) return theOperation.isInclude();
        }

        // If the first operation is an include, default is accept nothing
        // If the first operation is an exclude, default is accept all.
        return !theFirstOperation.isInclude();
    }
}