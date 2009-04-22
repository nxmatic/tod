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
package tod.impl.bci.asm.attributes;

import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;
import tod.impl.database.structure.standard.TagMap;

/**
 * This attribute determines the role of each bytecode
 * @author gpothier
 */
public class SootInstructionKindAttribute extends SootIntAttribute
{
	public static final String NAME = "ca.mcgill.sable.InstructionKind";

	public SootInstructionKindAttribute(Entry<Integer>[] aEntries)
	{
		super(NAME, aEntries);
	}

	public SootInstructionKindAttribute()
	{
		super(NAME);
	}

	@Override
	protected void fillTagMap(TagMap aTagMap, int aStart, int aEnd, Integer aValue)
	{
		aTagMap.putTagRange(BytecodeTagType.ROLE, getRole(aValue), aStart, aEnd);
	}
	
	/**
	 * Returns the TOD {@link BytecodeRole} that corresponds to the given soot instruction kind
	 * constant.
	 */
	private static BytecodeRole getRole(int aSootConstant) 
	{
		// Constants come from abc.weaving.tagkit.InstructionKindTag
		switch (aSootConstant)
		{
		case 0: return BytecodeRole.BASE_CODE;
		case 1: return BytecodeRole.ADVICE_EXECUTE;
		case 2: return BytecodeRole.ADVICE_ARG_SETUP;
		case 3: return BytecodeRole.ADVICE_TEST;
		case 4: return BytecodeRole.AFTER_THROWING_HANDLER;
		case 5: return BytecodeRole.EXCEPTION_SOFTENER;
		case 36: return BytecodeRole.INLINED_ADVICE;
		case 41: return BytecodeRole.CONTEXT_EXPOSURE;
		case 42: return BytecodeRole.PARAMETER_BACKUP;
		default: return BytecodeRole.UNKNOWN;
		}
	}

}
