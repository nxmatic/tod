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

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import tod.core.config.ClassSelector;
import tod.impl.database.structure.standard.PrimitiveTypeInfo;

public class BCIUtils implements Opcodes
{
	/**
	 * Return the normal Java class name corresponding to the given internal name
	 */
	public static String getClassName (String aJVMClassName)
	{
		return Type.getType("L"+aJVMClassName+";").getClassName();
	}
	
	/**
	 * Returns an array that contains the original array followed by the new item.
	 * The provided array can be null.
	 */
	public static <T> T[] addToArray(T[] aArray, T aItem)
	{
		T[] theResult;
		if (aArray == null)
		{
			theResult = (T[]) Array.newInstance(aItem.getClass(), 1);
		}
		else
		{
			theResult = (T[]) Array.newInstance(aItem.getClass(), aArray.length+1);
			System.arraycopy(aArray, 0, theResult, 0, aArray.length);
		}
		
		theResult[theResult.length-1] = aItem;
		return theResult;
	}
	
	public static boolean isInterface(int access)
	{
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}
	
	public static boolean isStatic (int access)
	{
		return (access & Opcodes.ACC_STATIC) != 0;
	}
	
	/**
	 * Generates the bytecode that pushes the given value onto the stack
	 */
	public static void pushInt (MethodVisitor aVisitor, int aValue)
	{
		switch (aValue)
		{
		case -1:
			aVisitor.visitInsn(ICONST_M1);
			return;
			
		case 0:
			aVisitor.visitInsn(ICONST_0);
			return;
			
		case 1:
			aVisitor.visitInsn(ICONST_1);
			return;
			
		case 2:
			aVisitor.visitInsn(ICONST_2);
			return;
			
		case 3:
			aVisitor.visitInsn(ICONST_3);
			return;
			
		case 4:
			aVisitor.visitInsn(ICONST_4);
			return;
			
		case 5:
			aVisitor.visitInsn(ICONST_5);
			return;
			
		}
		
		if (aValue >= Byte.MIN_VALUE && aValue <= Byte.MAX_VALUE)
			aVisitor.visitIntInsn(BIPUSH, aValue);
		else if (aValue >= Short.MIN_VALUE && aValue <= Short.MAX_VALUE)
			aVisitor.visitIntInsn(SIPUSH, aValue);
		else
			aVisitor.visitLdcInsn(new Integer(aValue));
	}
	
	/**
	 * Generates the bytecode that pushes the given value onto the stack
	 */
	public static void pushLong (MethodVisitor aVisitor, long aValue)
	{
		if (aValue == 0)
		{
			aVisitor.visitInsn(LCONST_0);
			return;
		}
		else if (aValue == 1)
		{
			aVisitor.visitInsn(LCONST_1);
			return;
		}
		
		aVisitor.visitLdcInsn(new Long(aValue));
	}
	
	/**
	 * Produces code that wrap primitive types in their corresponding objects.
	 */
	public static void wrap (MethodVisitor aVisitor, Type aType, boolean aUseJava14)
	{
		wrap (aVisitor, aType.getSort(), aUseJava14);
	}
	
	private static String getAPI(String aClassName, boolean aUseJava14)
	{
		String thePrefix = aUseJava14 ? 
				"net/sourceforge/retroweaver/runtime/java/lang/" 
				: "java/lang/";
		
		String theSuffix = aUseJava14 ? "_" : "";
		
		return thePrefix+aClassName+theSuffix;
	}
	
	public static void wrap (MethodVisitor aVisitor, int aSort, boolean aUseJava14)
	{
		
		switch (aSort)
		{
		case Type.OBJECT:
		case Type.ARRAY:
			return;
			
		case Type.BOOLEAN:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Boolean", aUseJava14), "valueOf", "(Z)Ljava/lang/Boolean;");
			return;
			
		case Type.BYTE:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Byte", aUseJava14), "valueOf", "(B)Ljava/lang/Byte;");
			return;
			
		case Type.CHAR:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Character", aUseJava14), "valueOf", "(C)Ljava/lang/Character;");
			return;
			
		case Type.DOUBLE:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Double", aUseJava14), "valueOf", "(D)Ljava/lang/Double;");
			return;
			
		case Type.FLOAT:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Float", aUseJava14), "valueOf", "(F)Ljava/lang/Float;");
			return;
			
		case Type.INT:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Integer", aUseJava14), "valueOf", "(I)Ljava/lang/Integer;");
			return;
			
		case Type.LONG:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Long", aUseJava14), "valueOf", "(J)Ljava/lang/Long;");
			return;
			
		case Type.SHORT:
			aVisitor.visitMethodInsn(INVOKESTATIC, getAPI("Short", aUseJava14), "valueOf", "(S)Ljava/lang/Short;");
			return;
			
		}
	}
	
	/**
	 * Returns the sort of data (as defined by {@link Type} that correponds to
	 * the given opcode. 
	 */
	public static int getSort(int aOpcode)
	{
		switch (aOpcode)
		{
		case AALOAD:
		case AASTORE:
		case ACONST_NULL:
		case ALOAD:
		case ANEWARRAY:
		case ARETURN:
		case ASTORE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
			return Type.OBJECT;
			
		case BALOAD:
		case BASTORE:
			return Type.BYTE;
			
		case CALOAD:
		case CASTORE:
			return Type.CHAR;
			
		case DADD:
		case DALOAD:
		case DASTORE:
		case DCMPG:
		case DCMPL:
		case DCONST_0:
		case DCONST_1:
		case DDIV:
		case DLOAD:
		case DMUL:
		case DNEG:
		case DREM:
		case DRETURN:
		case DSTORE:
		case DSUB:
			return Type.DOUBLE;
			
		case FADD:
		case FALOAD:
		case FASTORE:
		case FCMPG:
		case FCMPL:
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
		case FDIV:
		case FLOAD:
		case FMUL:
		case FNEG:
		case FREM:
		case FRETURN:
		case FSTORE:
		case FSUB:
			return Type.FLOAT;
			
		case BIPUSH:
		case IADD:
		case IALOAD:
		case IAND:
		case IASTORE:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
		case ICONST_M1:
		case IDIV:
		case IF_ICMPEQ:	
		case IF_ICMPGE:	
		case IF_ICMPGT:	
		case IF_ICMPLE:	
		case IF_ICMPLT:	
		case IF_ICMPNE:	
		case IINC:
		case ILOAD:
		case IMUL:
		case INEG:
		case IOR:
		case IREM:
		case IRETURN:
		case ISHL:
		case ISHR:
		case ISTORE:
		case ISUB:
		case IUSHR:
		case IXOR:
			return Type.INT;
			
		case LADD:
		case LALOAD:
		case LAND:
		case LASTORE:
		case LCMP:
		case LCONST_0:
		case LCONST_1:
		case LDIV:
		case LLOAD:
		case LMUL:
		case LNEG:
		case LOR:
		case LREM:
		case LRETURN:
		case LSHL:
		case LSHR:
		case LSTORE:
		case LSUB:
		case LUSHR:
		case LXOR:
			return Type.LONG;
			
		case SALOAD:
		case SASTORE:
		case SIPUSH:
			return Type.SHORT;
			
		default:
			return -1;
		}
	}
	
	public static Type getType (int aSort)
	{
		switch (aSort) 
		{
		case Type.OBJECT: return Type.getType(Object.class);
		case Type.BOOLEAN: return Type.getType(boolean.class);
		case Type.BYTE: return Type.getType(byte.class);
		case Type.CHAR: return Type.getType(char.class);
		case Type.DOUBLE: return Type.getType(double.class);
		case Type.FLOAT: return Type.getType(float.class);
		case Type.INT: return Type.getType(int.class);
		case Type.LONG: return Type.getType(long.class);
		case Type.SHORT: return Type.getType(short.class);
		default: return null;
		}
	}
	
	/**
	 * Returns the primitive type that corresponds to the given operand
	 * @param aOperand {@link Opcodes#T_BOOLEAN} etc.
	 */
	public static PrimitiveTypeInfo getPrimitiveType(int aOperand)
	{
		switch (aOperand) 
		{
		case Opcodes.T_BOOLEAN: return PrimitiveTypeInfo.BOOLEAN;
		case Opcodes.T_BYTE: return PrimitiveTypeInfo.BYTE;
		case Opcodes.T_CHAR: return PrimitiveTypeInfo.CHAR;
		case Opcodes.T_DOUBLE: return PrimitiveTypeInfo.DOUBLE;
		case Opcodes.T_FLOAT: return PrimitiveTypeInfo.FLOAT;
		case Opcodes.T_INT: return PrimitiveTypeInfo.INT;
		case Opcodes.T_LONG: return PrimitiveTypeInfo.LONG;
		case Opcodes.T_SHORT: return PrimitiveTypeInfo.SHORT;
		default: return null;
		}
	}
	
	public static boolean acceptClass (String aClassName, ClassSelector aSelector)
	{
		return aSelector.accept(getClassName(aClassName));
	}
	
	/**
	 * Generates method invocation code for the given method.
	 */
	public static void invoke(MethodVisitor mv, int aOpcode, Method aMethod)
	{
		mv.visitMethodInsn(
				aOpcode, 
				Type.getInternalName(aMethod.getDeclaringClass()), 
				aMethod.getName(), 
				Type.getMethodDescriptor(aMethod));
	}
}
