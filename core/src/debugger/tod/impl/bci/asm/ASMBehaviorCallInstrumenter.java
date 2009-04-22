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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import tod.agent.BehaviorCallType;

public class ASMBehaviorCallInstrumenter implements Opcodes
{
	private final MethodVisitor mv;
	private final ASMBehaviorInstrumenter itsInstrumenter;
	
	private final boolean itsUseJava14;
	
	private int itsMethodId;
	private boolean itsStatic;
	private Type[] itsArgTypes;
	private Type itsReturnType;

	private int itsOperationBehaviorId;
	
	/**
	 * The label that corresponds to the actual call instruction.
	 * The client should provide a label to the call instrumenter,
	 * and visit the label just prior to the actual call.
	 */
	private Label itsOriginalInstructionLabel;
	
	private int itsFirstVar;
	private int itsAfterArgumentsVar;
	private int itsTargetVar; 
	private int itsArrayVar;

	
	public ASMBehaviorCallInstrumenter(
			MethodVisitor mv, 
			ASMBehaviorInstrumenter aInstrumenter,
			int aOperationBehaviorId,
			boolean aUseJava14)
	{
		this.mv = mv;
		itsInstrumenter = aInstrumenter;
		itsOperationBehaviorId = aOperationBehaviorId;
		itsUseJava14 = aUseJava14;
	}

	public void setup(
			Label aLabel,
			int aMaxLocals,
			int aMethodId, 
			String aDesc, 
			boolean aStatic)
	{
		itsFirstVar = aMaxLocals;
		itsMethodId = aMethodId;
		itsStatic = aStatic;
		itsArgTypes = Type.getArgumentTypes(aDesc);
		itsReturnType = Type.getReturnType(aDesc);
		
		itsOriginalInstructionLabel = aLabel;
		
		itsAfterArgumentsVar = -1;
		itsTargetVar = -1;
		itsArrayVar = -1;
	}
	
	/**
	 * Generates the bytecode that stores method parameters to local variables
	 * Stack: [target], arg1, ..., argN => . 
	 */
	public void storeArgsToLocals()
	{
		int theCurrentVar = itsFirstVar;
		for (int i=itsArgTypes.length-1;i>=0;i--)
		{
			Type theType = itsArgTypes[i];
			mv.visitVarInsn(theType.getOpcode(ISTORE), theCurrentVar);
			theCurrentVar += theType.getSize();
		}
		
		itsAfterArgumentsVar = theCurrentVar; // The variable after all argument vars
		
		if (itsStatic) 
		{
			itsTargetVar = -1;
			itsArrayVar = theCurrentVar;				
		}
		else
		{
			itsTargetVar = theCurrentVar;
			itsArrayVar = itsTargetVar+1;
			mv.visitVarInsn(ASTORE, itsTargetVar);
		}
	}
	
	/**
	 * Generates the bytecode that creates the arguments array and stores
	 * it into a variable.
	 * Stack: . => .
	 */
	public void createArgsArray()
	{
		itsInstrumenter.createArgsArray(itsArgTypes, itsArrayVar, itsAfterArgumentsVar, true);
	}
	
	/**
	 * Generates the code that calls 
	 * {@link tod.core.ILogCollector#logBeforeBehaviorCall(long, long, int, int, Object, Object[])}
	 */
	public void callLogBeforeMethodCall(BehaviorCallType aCallType)
	{
		itsInstrumenter.invokeLogBeforeBehaviorCall(
				itsOriginalInstructionLabel, 
				itsMethodId, 
				aCallType,
				itsStatic ? -1 : itsTargetVar, 
				itsArrayVar);
	}
	
	/**
	 * Generates the code that calls 
	 * {@link tod.core.ILogCollector#logBeforeBehaviorCall(long, long, int, int, Object, Object[])}
	 */
	public void callLogBeforeBehaviorCallDry(BehaviorCallType aCallType)
	{
		itsInstrumenter.invokeLogBeforeBehaviorCallDry(
				itsOriginalInstructionLabel, 
				itsMethodId,
				aCallType);
	}
	
	/**
	 * Generates the code that pushes stored arguments back to the stack
	 * Stack: . => [target], arg1, ..., argN
	 */
	public void pushArgs()
	{
		// Push target & arguments back to the stack
		if (! itsStatic) mv.visitVarInsn(ALOAD, itsTargetVar);
		
		int theCurrentVar = itsAfterArgumentsVar;
		for (Type theType : itsArgTypes)
		{
			theCurrentVar -= theType.getSize();
			mv.visitVarInsn(theType.getOpcode(ILOAD), theCurrentVar);
		}
	}
	
	/**
	 * Generates the code that calls 
	 * {@link tod.core.ILogCollector#logAfterBehaviorCall(long, long, int, int, Object, Object)}
	 */
	public void callLogAfterMethodCall()
	{
		// :: [result]
		
		int theSort = itsReturnType.getSort();
		if (theSort == Type.VOID)
		{
			mv.visitInsn(ACONST_NULL);
		}
		else
		{
			mv.visitInsn(itsReturnType.getSize() == 2 ? DUP2 : DUP);
			BCIUtils.wrap(mv, itsReturnType, itsUseJava14);
		}
		
		mv.visitVarInsn(ASTORE, itsArrayVar);
		
		// :: [result]
		
		itsInstrumenter.invokeLogAfterBehaviorCall(
				itsOriginalInstructionLabel, 
				itsMethodId, 
				itsStatic ? -1 : itsTargetVar, 
				itsArrayVar);
	}

	/**
	 * Generates the code that calls 
	 * {@link tod.core.ILogCollector#logAfterBehaviorCall(long, long, int, int, Object, Object)}
	 */
	public void callLogAfterMethodCallWithException()
	{
		// :: [exception]
		
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, itsArrayVar);
		
		// :: [exception]
		
		itsInstrumenter.invokeLogAfterBehaviorCallWithException(
				itsOriginalInstructionLabel, 
				itsMethodId, 
				itsStatic ? -1 : itsTargetVar, 
				itsArrayVar);
	}
	
	/**
	 * Generates the code that calls 
	 * {@link tod.core.ILogCollector#logAfterBehaviorCall(long, long, int, int, Object, Object)}
	 */
	public void callLogAfterBehaviorCallDry()
	{
		itsInstrumenter.invokeLogAfterBehaviorCallDry();
	}
	
	
}
