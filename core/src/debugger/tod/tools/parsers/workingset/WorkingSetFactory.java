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

import java.io.StringReader;

import tod.tools.parsers.ParseException;
import tod.tools.parsers.WorkingSetParser;


/**
 * This class parses working sets.
 * 
 * 
 * Pseudo Grammar for working set parsing is:
 *  _classSet = _simpleSet | _coumpoundSet
 *  _simpleSet = _className   -> new SimpleClassSet(_className)
 *  _className =  id(.id)*(."*"("*")?)?
 *  _coumpoundSet =  [ (_op)+ ]   -> new CoupoundSet(_op)
 *  _op = "+" classSet (":" classSet)*    -> new  SetOperation("+", listOfClassSet)
 *        | -> new  SetOperation("+", listOfClassSet)
 * 
 * 
 *   If the first operation is an include, default is accept nothing
 *   If the first operation is an exclude, default is accept all.
 * 
 * 
 * @author gpothier
 */
public class WorkingSetFactory
{
    static
    {
        new WorkingSetParser(new StringReader(""));
    }

    public static AbstractClassSet parseWorkingSet(String aString) throws ParseException
    {
        WorkingSetParser.ReInit(new StringReader(aString));
        return WorkingSetParser.classSet();
    }

    /**
     * Creates an appropriate class set for the given class name.
     */
    public static AbstractClassSet createClassSet(String aClassName) throws ParseException
    {
        if (aClassName.endsWith(".**")) 
        	return new RecursivePackageSet(aClassName.substring(0, aClassName.length() - 3));
        else if (aClassName.endsWith(".*")) 
        	return new SinglePackageSet(aClassName.substring(0, aClassName.length() - 2));
        else if (aClassName.endsWith(".")) 
        	throw new ParseException("class/package name cannot end with '.': "+ aClassName);
        else return new SingleClassSet(aClassName);
    }
}
