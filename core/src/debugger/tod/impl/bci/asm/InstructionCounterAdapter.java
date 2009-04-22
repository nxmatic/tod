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
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

/**
 * This method adapter counts the input instructions.
 * @author gpothier
 */
public class InstructionCounterAdapter extends MethodAdapter
{
	private int itsCount = 0;
	
	public InstructionCounterAdapter(MethodVisitor mv)
	{
		super(mv);
	}

	public int getCount()
	{
		return itsCount;
	}
	
	protected void insn(int aRank, int aPc)
	{
	}
	
	private void registerInsn()
	{
		Label l = new Label();
		super.visitLabel(l);
		insn(itsCount, l.getOffset());
		itsCount++;
	}

	@Override
	public void visitFieldInsn(int aOpcode, String aOwner, String aName, String aDesc)
	{
		super.visitFieldInsn(aOpcode, aOwner, aName, aDesc);
		registerInsn();
	}

	@Override
	public void visitIincInsn(int aVar, int aIncrement)
	{
		super.visitIincInsn(aVar, aIncrement);
		registerInsn();
	}

	@Override
	public void visitInsn(int aOpcode)
	{
		super.visitInsn(aOpcode);
		registerInsn();
	}

	@Override
	public void visitIntInsn(int aOpcode, int aOperand)
	{
		super.visitIntInsn(aOpcode, aOperand);
		registerInsn();
	}

	@Override
	public void visitJumpInsn(int aOpcode, Label aLabel)
	{
		super.visitJumpInsn(aOpcode, aLabel);
		registerInsn();
	}

	@Override
	public void visitLdcInsn(Object aCst)
	{
		super.visitLdcInsn(aCst);
		registerInsn();
	}

	@Override
	public void visitLookupSwitchInsn(Label aDflt, int[] aKeys, Label[] aLabels)
	{
		super.visitLookupSwitchInsn(aDflt, aKeys, aLabels);
		registerInsn();
	}

	@Override
	public void visitMethodInsn(int aOpcode, String aOwner, String aName, String aDesc)
	{
		super.visitMethodInsn(aOpcode, aOwner, aName, aDesc);
		registerInsn();
	}

	@Override
	public void visitMultiANewArrayInsn(String aDesc, int aDims)
	{
		super.visitMultiANewArrayInsn(aDesc, aDims);
		registerInsn();
	}

	@Override
	public void visitTableSwitchInsn(int aMin, int aMax, Label aDflt, Label[] aLabels)
	{
		super.visitTableSwitchInsn(aMin, aMax, aDflt, aLabels);
		registerInsn();
	}

	@Override
	public void visitTypeInsn(int aOpcode, String aDesc)
	{
		super.visitTypeInsn(aOpcode, aDesc);
		registerInsn();
	}

	@Override
	public void visitVarInsn(int aOpcode, int aVar)
	{
		super.visitVarInsn(aOpcode, aVar);
		registerInsn();
	}
}
