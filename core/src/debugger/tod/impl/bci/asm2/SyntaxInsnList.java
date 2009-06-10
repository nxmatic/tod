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

import com.sun.org.apache.bcel.internal.generic.ICONST;

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
	public void pushFloat(float aValue)
	{
		if (aValue == 0)
		{
			itsVisitor.visitInsn(Opcodes.FCONST_0);
			return;
		}
		else if (aValue == 1)
		{
			itsVisitor.visitInsn(Opcodes.FCONST_1);
			return;
		}
		else if (aValue == 2)
		{
			itsVisitor.visitInsn(Opcodes.FCONST_2);
			return;
		}
		
		itsVisitor.visitLdcInsn(new Float(aValue));
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
	
	/**
	 * Generates the bytecode that pushes the given value onto the stack
	 */
	public void pushDouble (double aValue)
	{
		if (aValue == 0)
		{
			itsVisitor.visitInsn(Opcodes.DCONST_0);
			return;
		}
		else if (aValue == 1)
		{
			itsVisitor.visitInsn(Opcodes.DCONST_1);
			return;
		}
		
		itsVisitor.visitLdcInsn(new Double(aValue));
	}
	
	/**
	 * Pushes the default value for the given type onto the stack
	 * (ie. 0 for scalars, null for refs).
	 */
	public void pushDefaultValue(Type aType)
	{
		switch(aType.getSort())
		{
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
        case Type.INT:
        	pushInt(0);
        	break;
        	
        case Type.FLOAT:
        	pushFloat(0);
        	break;
        	
        case Type.LONG:
        	pushLong(0);
        	break;
        	
        case Type.DOUBLE:
        	pushDouble(0);
        	break;

        case Type.OBJECT:
        case Type.ARRAY:
        	ACONST_NULL();
        	break;
        	
        default:
            throw new RuntimeException("Not handled: "+aType);
		}
	}
	
	/*
	 * LDC
	 */
	
	public void LDC(Object cst)
	{
		itsVisitor.visitLdcInsn(cst);
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
	
	public void POP(Type type)
	{
		switch(type.getSize())
		{
		case 1: POP(); break;
		case 2: POP2(); break;
		default: throw new RuntimeException("Bad size");
		}
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
	
	public void DUP(Type type)
	{
		switch(type.getSize())
		{
		case 1: DUP(); break;
		case 2: DUP2(); break;
		default: throw new RuntimeException("Bad size");
		}
	}
	
	public void SWAP()
	{
		itsVisitor.visitInsn(Opcodes.SWAP);
	}
	
	public void IADD()
	{
		itsVisitor.visitInsn(Opcodes.IADD);
	}
	
	public void LADD()
	{
		itsVisitor.visitInsn(Opcodes.LADD);
	}
	
	public void FADD()
	{
		itsVisitor.visitInsn(Opcodes.FADD);
	}
	
	public void DADD()
	{
		itsVisitor.visitInsn(Opcodes.DADD);
	}
	
	public void ISUB()
	{
		itsVisitor.visitInsn(Opcodes.ISUB);
	}
	
	public void LSUB()
	{
		itsVisitor.visitInsn(Opcodes.LSUB);
	}
	
	public void FSUB()
	{
		itsVisitor.visitInsn(Opcodes.FSUB);
	}
	
	public void DSUB()
	{
		itsVisitor.visitInsn(Opcodes.DSUB);
	}
	
	public void IMUL()
	{
		itsVisitor.visitInsn(Opcodes.IMUL);
	}
	
	public void LMUL()
	{
		itsVisitor.visitInsn(Opcodes.LMUL);
	}
	
	public void FMUL()
	{
		itsVisitor.visitInsn(Opcodes.FMUL);
	}
	
	public void DMUL()
	{
		itsVisitor.visitInsn(Opcodes.DMUL);
	}
	
	public void IDIV()
	{
		itsVisitor.visitInsn(Opcodes.IDIV);
	}
	
	public void LDIV()
	{
		itsVisitor.visitInsn(Opcodes.LDIV);
	}
	
	public void FDIV()
	{
		itsVisitor.visitInsn(Opcodes.FDIV);
	}
	
	public void DDIV()
	{
		itsVisitor.visitInsn(Opcodes.DDIV);
	}
	
	public void IREM()
	{
		itsVisitor.visitInsn(Opcodes.IREM);
	}
	
	public void LREM()
	{
		itsVisitor.visitInsn(Opcodes.LREM);
	}
	
	public void FREM()
	{
		itsVisitor.visitInsn(Opcodes.FREM);
	}
	
	public void DREM()
	{
		itsVisitor.visitInsn(Opcodes.DREM);
	}
	
	public void INEG()
	{
		itsVisitor.visitInsn(Opcodes.INEG);
	}
	
	public void LNEG()
	{
		itsVisitor.visitInsn(Opcodes.LNEG);
	}
	
	public void FNEG()
	{
		itsVisitor.visitInsn(Opcodes.FNEG);
	}
	
	public void DNEG()
	{
		itsVisitor.visitInsn(Opcodes.DNEG);
	}
	
	public void ISHL()
	{
		itsVisitor.visitInsn(Opcodes.ISHL);
	}
	
	public void LSHL()
	{
		itsVisitor.visitInsn(Opcodes.LSHL);
	}
	
	public void ISHR()
	{
		itsVisitor.visitInsn(Opcodes.ISHR);
	}
	
	public void LSHR()
	{
		itsVisitor.visitInsn(Opcodes.LSHR);
	}
	
	public void IUSHR()
	{
		itsVisitor.visitInsn(Opcodes.IUSHR);
	}
	
	public void LUSHR()
	{
		itsVisitor.visitInsn(Opcodes.LUSHR);
	}
	
	public void IAND()
	{
		itsVisitor.visitInsn(Opcodes.IAND);
	}
	
	public void LAND()
	{
		itsVisitor.visitInsn(Opcodes.LAND);
	}
	
	public void IOR()
	{
		itsVisitor.visitInsn(Opcodes.IOR);
	}
	
	public void LOR()
	{
		itsVisitor.visitInsn(Opcodes.LOR);
	}
	
	public void IXOR()
	{
		itsVisitor.visitInsn(Opcodes.IXOR);
	}
	
	public void LXOR()
	{
		itsVisitor.visitInsn(Opcodes.LXOR);
	}
	
	public void I2L()
	{
		itsVisitor.visitInsn(Opcodes.I2L);
	}
	
	public void I2F()
	{
		itsVisitor.visitInsn(Opcodes.I2F);
	}
	
	public void I2D()
	{
		itsVisitor.visitInsn(Opcodes.I2D);
	}
	
	public void L2I()
	{
		itsVisitor.visitInsn(Opcodes.L2I);
	}
	
	public void L2F()
	{
		itsVisitor.visitInsn(Opcodes.L2F);
	}
	
	public void L2D()
	{
		itsVisitor.visitInsn(Opcodes.L2D);
	}
	
	public void F2I()
	{
		itsVisitor.visitInsn(Opcodes.F2I);
	}
	
	public void F2L()
	{
		itsVisitor.visitInsn(Opcodes.F2L);
	}
	
	public void F2D()
	{
		itsVisitor.visitInsn(Opcodes.F2D);
	}
	
	public void D2I()
	{
		itsVisitor.visitInsn(Opcodes.D2I);
	}
	
	public void D2L()
	{
		itsVisitor.visitInsn(Opcodes.D2L);
	}
	
	public void D2F()
	{
		itsVisitor.visitInsn(Opcodes.D2F);
	}
	
	public void I2B()
	{
		itsVisitor.visitInsn(Opcodes.I2B);
	}
	
	public void I2C()
	{
		itsVisitor.visitInsn(Opcodes.I2C);
	}
	
	public void I2S()
	{
		itsVisitor.visitInsn(Opcodes.I2S);
	}
	
	public void LCMP()
	{
		itsVisitor.visitInsn(Opcodes.LCMP);
	}
	
	public void FCMPL()
	{
		itsVisitor.visitInsn(Opcodes.FCMPL);
	}
	
	public void FCMPG()
	{
		itsVisitor.visitInsn(Opcodes.FCMPG);
	}
	
	public void DCMPL()
	{
		itsVisitor.visitInsn(Opcodes.DCMPL);
	}
	
	public void DCMPG()
	{
		itsVisitor.visitInsn(Opcodes.DCMPG);
	}
	
	public void IRETURN()
	{
		itsVisitor.visitInsn(Opcodes.IRETURN);
	}
	
	public void IRETURN(Type type)
	{
		itsVisitor.visitInsn(type.getOpcode(Opcodes.IRETURN));
	}
	
	public void LRETURN()
	{
		itsVisitor.visitInsn(Opcodes.LRETURN);
	}
	
	public void FRETURN()
	{
		itsVisitor.visitInsn(Opcodes.FRETURN);
	}
	
	public void DRETURN()
	{
		itsVisitor.visitInsn(Opcodes.DRETURN);
	}
	
	public void ARETURN()
	{
		itsVisitor.visitInsn(Opcodes.ARETURN);
	}
	
	public void RETURN()
	{
		itsVisitor.visitInsn(Opcodes.RETURN);
	}
	
	public void RETURN(Type type)
	{
		itsVisitor.visitInsn(type.getOpcode(Opcodes.IRETURN));
	}
	
	public void ATHROW()
	{
		itsVisitor.visitInsn(Opcodes.ATHROW);
	}
	
	public void MONITORENTER()
	{
		itsVisitor.visitInsn(Opcodes.MONITORENTER);
	}

	public void MONITOREXIT()
	{
		itsVisitor.visitInsn(Opcodes.MONITOREXIT);
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
	
	/**
	 * Same as IFEQ
	 */
	public void IFfalse(String label)
	{
		IFEQ(label);
	}
	
	/**
	 * Same as IFEQ
	 */
	public void IFfalse(Label label)
	{
		IFEQ(label);
	}
	
	public void IFEQ(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFEQ, getLabel(label));
	}
	
	public void IFEQ(Label label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFEQ, label);
	}
	
	/**
	 * Same as IFNE
	 */
	public void IFtrue(String label)
	{
		IFNE(label);
	}
	
	/**
	 * Same as IFNE
	 */
	public void IFtrue(Label label)
	{
		IFNE(label);
	}
	
	public void IFNE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFNE, getLabel(label));
	}
		
	public void IFNE(Label label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IFNE, label);
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
	
	public void IF_ICMPEQ(Label label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, label);
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
	
	public void IF_ACMPEQ(Label label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
	}
	
	public void IF_ACMPNE(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, getLabel(label));
	}
	
	public void GOTO(String label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.GOTO, getLabel(label));
	}
	
	public void GOTO(Label label) 
	{
		itsVisitor.visitJumpInsn(Opcodes.GOTO, label);
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


	/*
	 * Field instructions
	 */
	public void GETSTATIC(String owner, String name, String desc) 
	{
		itsVisitor.visitFieldInsn(Opcodes.GETSTATIC, owner, name, desc);
	}
	
	public void PUTSTATIC(String owner, String name, String desc) 
	{
		itsVisitor.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, desc);
	}
	
	public void GETFIELD(String owner, String name, String desc) 
	{
		itsVisitor.visitFieldInsn(Opcodes.GETFIELD, owner, name, desc);
	}
	
	public void PUTFIELD(String owner, String name, String desc) 
	{
		itsVisitor.visitFieldInsn(Opcodes.PUTFIELD, owner, name, desc);
	}
	
	/*
	 * Table switch
	 */
	
	public void TABLESWITCH(int min, int max, Label dflt, Label[] labels)
	{
		itsVisitor.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	/*
	 * Type instructions
	 */
	
	public void NEW(String type) 
	{
		itsVisitor.visitTypeInsn(Opcodes.NEW, type);
	}
	
	public void ANEWARRAY(String type) 
	{
		itsVisitor.visitTypeInsn(Opcodes.ANEWARRAY, type);
	}
	
	public void CHECKCAST(String type) 
	{
		itsVisitor.visitTypeInsn(Opcodes.CHECKCAST, type);
	}
	
	public void INSTANCEOF(String type) 
	{
		itsVisitor.visitTypeInsn(Opcodes.INSTANCEOF, type);
	}
	
	
	
	
	/**
	 * Inserts a label corresponding to the given name.
	 * The {@link LabelManager} is used to retrieve the actual label.
	 */
	public Label label(String label)
	{
		Label theLabel = getLabel(label);
		label(theLabel);
		return theLabel;
	}
	
	public void label(Label label)
	{
		itsVisitor.visitLabel(label);
	}
}
