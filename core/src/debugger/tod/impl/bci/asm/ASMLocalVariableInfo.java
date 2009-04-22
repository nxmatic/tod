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
package tod.impl.bci.asm;

import org.objectweb.asm.Label;

import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;

/**
 * Represents an entry of a method's LocalVariableTable attribute. 
 * @see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#5956
 * @author gpothier
 */
public class ASMLocalVariableInfo
{
    private Label itsStart;
    private Label itsEnd;
    private String itsVariableName;
    private String itsVariableTypeDesc;
    private int itsIndex;
    
	public ASMLocalVariableInfo(Label aStart, Label aEnd, String aVariableName, String aVariableTypeDesc, int aIndex)
	{
		itsStart = aStart;
		itsEnd = aEnd;
		itsVariableName = aVariableName;
		itsVariableTypeDesc = aVariableTypeDesc;
		itsIndex = aIndex;
	}
    
	public LocalVariableInfo resolve()
	{
		return new LocalVariableInfo(
				(short) itsStart.getOffset(),
				(short) (itsEnd.getOffset() - itsStart.getOffset()),
				itsVariableName,
				BCIUtils.getClassName(itsVariableTypeDesc),
				(short) itsIndex);
	}
}