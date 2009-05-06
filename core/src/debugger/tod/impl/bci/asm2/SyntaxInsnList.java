/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.bci.asm2;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Helper class that permits to write prettier code.
 * Label handling: a separate {@link LabelManager} provides the {@link Label}s
 * associated to the textual labels given to this {@link SyntaxInsnList}. The {@link LabelManager}
 * should be shared by all the {@link SyntaxInsnList} that operate on the same method.
 * @author gpothier
 */
public class SyntaxInsnList extends InsnList
{
	private final LabelManager itsLabelManager;
	private final MethodVisitor itsVisitor;
	
	public SyntaxInsnList(LabelManager aLabelManager)
	{
		itsLabelManager = aLabelManager;
		MethodNode theNode = new MethodNode();
		theNode.instructions = this;
		itsVisitor = theNode;
	}
	
	public Label getLabel(String label)
	{
		return itsLabelManager.getLabel(label);
	}

	/**
	 * Generates the bytecode that pushes the given value onto the stack
	 */
	public void pushInt (int aValue)
	{
		switch (aValue)
		{
		case -1:
			itsVisitor.visitInsn(Opcodes.ICONST_M1);
			return;
			
		case 0:
			itsVisitor.visitInsn(Opcodes.ICONST_0);
			return;
			
		case 1:
			itsVisitor.visitInsn(Opcodes.ICONST_1);
			return;
			
		case 2:
			itsVisitor.visitInsn(Opcodes.ICONST_2);
			return;
			
		case 3:
			itsVisitor.visitInsn(Opcodes.ICONST_3);
			return;
			
		case 4:
			itsVisitor.visitInsn(Opcodes.ICONST_4);
			return;
			
		case 5:
			itsVisitor.visitInsn(Opcodes.ICONST_5);
			return;
			
		}
		
		if (aValue >= Byte.MIN_VALUE && aValue <= Byte.MAX_VALUE)
			itsVisitor.visitIntInsn(Opcodes.BIPUSH, aValue);
		else if (aValue >= Short.MIN_VALUE && aValue <= Short.MAX_VALUE)
			itsVisitor.visitIntInsn(Opcodes.SIPUSH, aValue);
		else
			itsVisitor.visitLdcInsn(new Integer(aValue));
	}
	
	/**
	 * Generates the bytecode that pushes the given value onto the stack
	 */
	public void pushLong (long aValue)
	{
		if (aValue == 0)
		{
			itsVisitor.visitInsn(Opcodes.LCONST_0);
			return;
		}
		else if (aValue == 1)
		{
			itsVisitor.visitInsn(Opcodes.LCONST_1);
			return;
		}
		
		itsVisitor.visitLdcInsn(new Long(aValue));
	}

	/*
	 * Simple instructions
	 */
	
	public void NOP()
	{
		itsVisitor.visitInsn(Opcodes.NOP);
	}
	
	public void ACONST_NULL()
	{
		itsVisitor.visitInsn(Opcodes.ACONST_NULL);
	}
	
	public void IALOAD()
	{
		itsVisitor.visitInsn(Opcodes.IALOAD);
	}
	
	public void LALOAD()
	{
		itsVisitor.visitInsn(Opcodes.LALOAD);
	}
	
	public void FALOAD()
	{
		itsVisitor.visitInsn(Opcodes.FALOAD);
	}
	
	public void DALOAD()
	{
		itsVisitor.visitInsn(Opcodes.DALOAD);
	}
	
	public void AALOAD()
	{
		itsVisitor.visitInsn(Opcodes.AALOAD);
	}
	
	public void BALOAD()
	{
		itsVisitor.visitInsn(Opcodes.BALOAD);
	}
	
	public void CALOAD()
	{
		itsVisitor.visitInsn(Opcodes.CALOAD);
	}
	
	public void SALOAD()
	{
		itsVisitor.visitInsn(Opcodes.SALOAD);
	}
	
	public void IASTORE()
	{
		itsVisitor.visitInsn(Opcodes.IASTORE);
	}
	
	public void LASTORE()
	{
		itsVisitor.visitInsn(Opcodes.LASTORE);
	}
	
	public void FASTORE()
	{
		itsVisitor.visitInsn(Opcodes.FASTORE);
	}
	
	public void DASTORE()
	{
		itsVisitor.visitInsn(Opcodes.DASTORE);
	}
	
	public void AASTORE()
	{
		itsVisitor.visitInsn(Opcodes.AASTORE);
	}
	
	public void BASTORE()
	{
		itsVisitor.visitInsn(Opcodes.BASTORE);
	}
	
	public void CASTORE()
	{
		itsVisitor.visitInsn(Opcodes.CASTORE);
	}
	
	public void SASTORE()
	{
		itsVisitor.visitInsn(Opcodes.SASTORE);
	}
	
	public void POP()
	{
		itsVisitor.visitInsn(Opcodes.POP);
	}
	
	public void POP2()
	{
		itsVisitor.visitInsn(Opcodes.POP2);
	}
	
	public void DUP()
	{
		itsVisitor.visitInsn(Opcodes.DUP);
	}
	
	public void DUP_X1()
	{
		itsVisitor.visitInsn(Opcodes.DUP_X1);
	}
	
	public void DUP_X2()
	{
		itsVisitor.visitInsn(Opcodes.DUP_X2);
	}
	
	public void DUP2()
	{
		itsVisitor.visitInsn(Opcodes.DUP2);
	}
	
	public void DUP2_X1()
	{
		itsVisitor.visitInsn(Opcodes.DUP2_X1);
	}
	
	public void DUP2_X2()
	{
		itsVisitor.visitInsn(Opcodes.DUP2_X2);
	}
	
	public void SWAP()
	{
		itsVisitor.visitInsn(Opcodes.SWAP);
	}
	
	public void ATHROW()
	{
		itsVisitor.visitInsn(Opcodes.ATHROW);
	}
	
	public void RETURN()
	{
		itsVisitor.visitInsn(Opcodes.RETURN);
	}
	
	public void RETURN(Type type)
	{
		itsVisitor.visitInsn(type.getOpcode(Opcodes.IRETURN));
	}
	
	/*
	 * Var instructions
	 */
	
	public void ILOAD(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.ILOAD, var);
	}
	
	public void ILOAD(Type type, int var)
	{
		itsVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), var);
	}
	
	public void LLOAD(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.LLOAD, var);
	}
	
	public void FLOAD(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.FLOAD, var);
	}
	
	public void DLOAD(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.DLOAD, var);
	}
	
	public void ALOAD(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.ALOAD, var);
	}
	
	public void ISTORE(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.ISTORE, var);
	}
	
	public void ISTORE(Type type, int var)
	{
		itsVisitor.visitVarInsn(type.getOpcode(Opcodes.ISTORE), var);
	}
	
	public void LSTORE(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.LSTORE, var);
	}
	
	public void FSTORE(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.FSTORE, var);
	}
	
	public void DSTORE(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.DSTORE, var);
	}
	
	public void ASTORE(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.ASTORE, var);
	}
	
	public void RET(int var)
	{
		itsVisitor.visitVarInsn(Opcodes.RET, var);
	}
	
	/*
	 * Method instructions
	 */
	
	public void INVOKEINTERFACE(String owner, String name, String desc)
	{
		itsVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, desc);
	}
	
	public void INVOKESPECIAL(String owner, String name, String desc)
	{
		itsVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, desc);
	}
	
	public void INVOKESTATIC(String owner, String name, String desc)
	{
		itsVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc);
	}
	
	public void INVOKEVIRTUAL(String owner, String name, String desc)
	{
		itsVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, desc);
	}
	
	/*
	 * Jump instructions
	 */
	
	public void IFEQ(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFEQ, getLabel(label));
	}
	
	public void IFNE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFNE, getLabel(label));
	}
	
	public void IFLT(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFLT, getLabel(label));
	}
	
	public void IFGE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFGE, getLabel(label));
	}
	
	public void IFGT(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFGT, getLabel(label));
	}
	
	public void IFLE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFLE, getLabel(label));
	}
	
	public void IF_ICMPEQ(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, getLabel(label));
	}
	
	public void IF_ICMPNE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, getLabel(label));
	}
	
	public void IF_ICMPLT(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, getLabel(label));
	}
	
	public void IF_ICMPGE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, getLabel(label));
	}
	
	public void IF_ICMPGT(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, getLabel(label));
	}
	
	public void IF_ICMPLE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, getLabel(label));
	}
	
	public void IF_ACMPEQ(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, getLabel(label));
	}
	
	public void IF_ACMPNE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, getLabel(label));
	}
	
	public void GOTO(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.GOTO, getLabel(label));
	}
	
	public void JSR(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.JSR, getLabel(label));
	}
	
	public void IFNULL(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFNULL, getLabel(label));
	}
	
	public void IFNONNULL(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFNONNULL, getLabel(label));
	}
	
	/**
	 * Inserts a label corresponding to the given name.
	 * The {@link LabelManager} is used to retrieve the actual label.
	 */
	public Label label(String label)
	{
		Label theLabel = getLabel(label);
		itsVisitor.visitLabel(theLabel);
		return theLabel;
	}
}
