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
package tod.core.database.structure.analysis;

import tod.core.database.structure.IBehaviorInfo;

/**
 * Utility class that provides a disassembled version of a behavior's
 * bytecode.
 * @author gpothier
 */
public class DisassembledBehavior
{
	private final IBehaviorInfo itsBehavior;
	private final Instruction[] itsInstructions;

	public DisassembledBehavior(IBehaviorInfo aBehavior, Instruction[] aInstructions)
	{
		itsBehavior = aBehavior;
		itsInstructions = aInstructions;
	}
	
	public IBehaviorInfo getBehavior()
	{
		return itsBehavior;
	}

	public Instruction[] getInstructions()
	{
		return itsInstructions;
	}
	
	/**
	 * Information regarding a single bytecode instruction.
	 * @author gpothier
	 */
	public static class Instruction
	{
		/**
		 * Address of the instruction in the behavior's bytecode.
		 */
		public final int pc;
		
		/**
		 * Bytecode mnemonic and arguments in human-readable form
		 */
		public final String text;
		
		/**
		 * Whether this instruction is actually a label.
		 */
		public final boolean label;

		public Instruction(int aPc, String aText, boolean aLabel)
		{
			pc = aPc;
			text = aText;
			label = aLabel;
		}
		
		@Override
		public String toString()
		{
			return pc + " " + text;
		}
	}


}
