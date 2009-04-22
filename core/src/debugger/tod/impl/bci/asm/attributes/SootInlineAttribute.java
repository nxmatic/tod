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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;
import tod.impl.database.structure.standard.TagMap;

/**
 * For bytecodes that correspond to inlined advices, indicates the original shadow
 * and source id.
 * @author gpothier
 */
public class SootInlineAttribute extends SootAttribute<InlineData>
{
	public static final String NAME = "ca.mcgill.sable.InstructionInlineShadowSource";

	public SootInlineAttribute(Entry<InlineData>[] aEntries)
	{
		super(NAME, aEntries);
	}

	public SootInlineAttribute()
	{
		super(NAME);
	}

	@Override
	protected void fillTagMap(TagMap aTagMap, int aStart, int aEnd, InlineData aValue)
	{
		aTagMap.putTagRange(BytecodeTagType.INSTR_SHADOW, aValue.shadow, aStart, aEnd);
		aTagMap.putTagRange(BytecodeTagType.ADVICE_SOURCE_ID, aValue.source, aStart, aEnd);
	}

	@Override
	protected InlineData readValue(DataInputStream aStream) throws IOException
	{
		int theShadow = aStream.readInt();
		int theSource = aStream.readInt();
		return new InlineData(theSource, theShadow);
	}

	@Override
	protected void writeValue(DataOutputStream aStream, InlineData aValue) throws IOException
	{
		aStream.writeInt(aValue.shadow);
		aStream.writeInt(aValue.source);
	}
}
