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

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import tod.Util;
import tod.core.config.TODConfig;
import tod.core.database.structure.Access;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;
import tod.core.database.structure.IBehaviorInfo.HasTrace;
import tod.impl.bci.asm.ASMInstrumenter.CodeRange;
import tod.impl.bci.asm.ASMInstrumenter.RangeManager;
import tod.impl.bci.asm.ProbesManager.TmpProbeInfo;
import tod.impl.database.structure.standard.TagMap;
import tod2.agent.BehaviorCallType;

/**
 * Provides all the methods that perform the insertion
 * of operations logging.
 * @author gpothier
 */
public class ASMBehaviorInstrumenter implements Opcodes
{
	private static final String CLS_EVENTCOLLECTOR = "java/tod/EventCollector";
	private static final String DSC_EVENTCOLLECTOR = "L"+CLS_EVENTCOLLECTOR+";";
	private static final String CLS_AGENTREADY = "java/tod/AgentReady";
	private static final String CLS_EXCEPTIONGENERATEDRECEIVER = "java/tod/ExceptionGeneratedReceiver";
	private static final String CLS_TRACEDMETHODS = "java/tod/TracedMethods";
	private static final String CLS_BEHAVIORCALLTYPE = "java/tod/_BehaviorCallType";
	private static final String DSC_BEHAVIORCALLTYPE = "L"+CLS_BEHAVIORCALLTYPE+";";
	
	private final IMutableStructureDatabase itsStructureDatabase;
	private final ProbesManager itsProbesManager;
	private final IMutableBehaviorInfo itsBehavior;
	private final ASMBehaviorCallInstrumenter itsBehaviorCallInstrumenter;
	private final MethodVisitor mv;
	private final ASMMethodInfo itsMethodInfo;
	private final ASMDebuggerConfig itsConfig;
	
	private final boolean itsUseJava14;
	private final InstrumentationSpec itsSpec;
	
	/**
	 * Index of the variable that stores the real return point of the method.
	 * The stored value is a probe id (int)
	 */
	private int itsReturnLocationVar;
	
	/**
	 * Index of the variable that stores the event interpreter.
	 */
	private int itsCollectorVar;
	
	/**
	 * This variable is a flag that indicates if trace capture is enabled.
	 */
	private int itsCaptureEnabledVar;
	
	private int itsFirstFreeVar;
	
	private Label itsReturnHookLabel;
	private Label itsFinallyHookLabel;
	private Label itsCodeStartLabel;

	/**
	 * A list of code ranges that corresponds to instrumentation instructions
	 * added by TOD.
	 */
	private final RangeManager itsInstrumentationRanges;
	
	/**
	 * This set contains the classes whose loading has already been forced.
	 */
	private final Set<String> itsLoadedClasses = new HashSet<String>();
	
	public ASMBehaviorInstrumenter(
			ASMDebuggerConfig aConfig,
			MethodVisitor mv,
			IMutableBehaviorInfo aBehavior,
			ASMMethodInfo aMethodInfo,
			boolean aUseJava14,
			InstrumentationSpec aSpec)
	{
		itsConfig = aConfig;
		this.mv = mv;
		itsBehavior = aBehavior;
		
		itsInstrumentationRanges = new RangeManager(mv);
		
		// TODO: _getMutableDatabase is a workaround for a jdk compiler bug
		itsStructureDatabase = itsBehavior._getMutableDatabase();
		itsProbesManager = new ProbesManager(itsStructureDatabase);
		itsMethodInfo = aMethodInfo;
		itsBehaviorCallInstrumenter = new ASMBehaviorCallInstrumenter(
				mv, 
				this, 
				itsBehavior.getId(),
				aUseJava14);
		
		itsFirstFreeVar = itsMethodInfo.getMaxLocals();
		
		// Allocate space for return var
		itsReturnLocationVar = itsFirstFreeVar;
		itsFirstFreeVar += 1;
		
		// Allocate space for interpreter var
		itsCollectorVar = itsFirstFreeVar;
		itsFirstFreeVar += 1;
		
		// Allocate space for trace capture var
		itsCaptureEnabledVar = itsFirstFreeVar;
		itsFirstFreeVar += 1;
		
		itsUseJava14 = aUseJava14;
		itsSpec = aSpec;
	}
	
	/**
	 * Fills the provided tag map with the instrumentation tags
	 */
	public void fillTagMap(TagMap aTagMap)
	{
		for (CodeRange theRange : itsInstrumentationRanges.getRanges())
		{
			aTagMap.putTagRange(
					BytecodeTagType.ROLE, 
					BytecodeRole.TOD_CODE, 
					theRange.start.getOffset(),
					theRange.end.getOffset());
		}
	}
	
	/**
	 * Updates the probes used in the behavior:
	 * <li> Resolve bytecode indexes
	 * <li> Include advice source id information (if tagmap is specified).
	 */
	public void updateProbes(TagMap aTagMap)
	{
		int theBehaviorId = itsBehavior.getId();
		for (TmpProbeInfo theProbe : itsProbesManager.getProbes())
		{
			int theBytecodeIndex = theProbe.label.getOffset();
			
			Integer theAdviceSourceId = aTagMap != null ? 
					aTagMap.getTag(BytecodeTagType.ADVICE_SOURCE_ID, theBytecodeIndex) 
					: null;
					
			BytecodeRole theRole = aTagMap != null ? 
					aTagMap.getTag(BytecodeTagType.ROLE, theBytecodeIndex)
					: null;
			
			itsStructureDatabase.setProbe(
					theProbe.id, 
					theBehaviorId, 
					theBytecodeIndex, 
					theRole,
					theAdviceSourceId != null ? theAdviceSourceId : -1);
		}
	}
	
	/**
	 * Creates a new probe and generates an instruction that pushes its id
	 * on the stack.
	 */
	public void pushProbeId(Label aLabel)
	{
		int theId = itsProbesManager.createProbe(aLabel);
		BCIUtils.pushInt(mv, theId);
	}
	

	
	/**
	 * <li>Identifiable objects' id initialization</li>
	 * <li>Log behavior enter</li>
	 * <li>Insert return hooks at the beginning of the method body:
	 * 		<li>Log behavior exit</li>
	 * </li>
	 */
	public void insertEntryHooks()
	{
		itsInstrumentationRanges.start();
		
		itsReturnHookLabel = new Label();
		itsFinallyHookLabel = new Label();
		itsCodeStartLabel = new Label();

		// Obtain the event interpreter and store it into the interpreter var.
		mv.visitFieldInsn(
				GETSTATIC, 
				CLS_EVENTCOLLECTOR, 
				"INSTANCE", 
				DSC_EVENTCOLLECTOR);
		mv.visitVarInsn(ASTORE, itsCollectorVar);
		
		// Init the CreatedInScope field (only for constructors)
		if (itsSpec.hasCreatedInScope() 
				&& "<init>".equals(itsMethodInfo.getName()))
		{
			initCreatedInScope();
		}
		
		// Store the capture enabled flag
		// We need to use the same value of the flag during the whole execution of the method.
		mv.visitFieldInsn(GETSTATIC, CLS_AGENTREADY, "CAPTURE_ENABLED", "Z");
		mv.visitVarInsn(ISTORE, itsCaptureEnabledVar);
		
		if (itsSpec.hasCreatedInScope() && ! itsMethodInfo.isStatic())
		{
			// If the current instance is not created in scope, disable capture
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, itsMethodInfo.getOwner(), LogBCIVisitor.FIELD_CREATEDINSCOPE, "I");

			mv.visitInsn(ICONST_1);
			Label theInScopeLabel = new Label();
			mv.visitJumpInsn(IF_ICMPEQ, theInScopeLabel);
			
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, itsCaptureEnabledVar);
			
			mv.visitLabel(theInScopeLabel);
		}

		if (! itsSpec.traceEnveloppe())
		{
			itsInstrumentationRanges.end();
			return;
		}
		
		// Call logBehaviorEnter
		// We suppose that if a class is instrumented all its descendants
		// are also instrumented, so we can't miss a super call
		if (itsSpec.traceEntry())
		{
			behaviorEnter("<init>".equals(itsMethodInfo.getName()) ?
					BehaviorCallType.INSTANTIATION
					: BehaviorCallType.METHOD_CALL);
		}
		
		mv.visitJumpInsn(GOTO, itsCodeStartLabel);
		
		// -- Return hook
		mv.visitLabel(itsReturnHookLabel);

		// Call logBehaviorExit
		if (itsSpec.traceExit())
		{
			behaviorExit();
		}

		// Insert RETURN
		Type theReturnType = Type.getReturnType(itsMethodInfo.getDescriptor());
		mv.visitInsn(theReturnType.getOpcode(IRETURN));
		
		// -- Finally hook
		mv.visitLabel(itsFinallyHookLabel);
		
		// Call logBehaviorExitWithException
		if (itsSpec.traceExit())
		{
			behaviorExitWithException();
		}
		
		mv.visitMethodInsn(
				INVOKESTATIC, 
				CLS_EXCEPTIONGENERATEDRECEIVER, 
				"ignoreNextException", 
				"()V");
		
		mv.visitInsn(ATHROW);

		mv.visitLabel(itsCodeStartLabel);
		
		itsInstrumentationRanges.end();
	}
	
	public void endHooks()
	{
		if (itsSpec.traceEnveloppe())
		{
			Label theCodeEndLabel = new Label();
			mv.visitLabel(theCodeEndLabel);
			mv.visitTryCatchBlock(itsCodeStartLabel, theCodeEndLabel, itsFinallyHookLabel, null);
		}
	}

	public void initCreatedInScope()
	{
		// ->this
		mv.visitVarInsn(ALOAD, 0);
		
		mv.visitFieldInsn(GETFIELD, itsMethodInfo.getOwner(), LogBCIVisitor.FIELD_CREATEDINSCOPE, "I");

		Label theInitialized = new Label();
		mv.visitJumpInsn(IFNE, theInitialized);
		
		// ->this
		mv.visitVarInsn(ALOAD, 0);
		
		// ->this, target collector
		mv.visitVarInsn(ALOAD, itsCollectorVar);

		// ->this, result
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"getCurrentCalledBehavior", 
				"()I");

		// ->this, result, our bid
		mv.visitLdcInsn(itsBehavior.getId());
		
		Label theSame = new Label();
		Label theEndCmp = new Label();
		
		mv.visitJumpInsn(IF_ICMPEQ, theSame);
		
		// different
		
		mv.visitInsn(ICONST_M1);
		
		mv.visitJumpInsn(GOTO, theEndCmp);
		mv.visitLabel(theSame);
		
		// same
		
		mv.visitInsn(ICONST_1);
		
		mv.visitLabel(theEndCmp);
		
		// ->this, val
		mv.visitFieldInsn(PUTFIELD, itsMethodInfo.getOwner(), LogBCIVisitor.FIELD_CREATEDINSCOPE, "I");
		
		mv.visitLabel(theInitialized);
	}
	
	public void doReturn(int aOpcode)
	{
		// Store location of this return into the variable
		Label l = new Label();
		mv.visitLabel(l);
		
		pushProbeId(l);
		mv.visitVarInsn(ISTORE, itsReturnLocationVar);
		
		mv.visitJumpInsn(GOTO, itsReturnHookLabel);
	}
	
	private void checkCaptureEnabled(Label aLabel)
	{
//		mv.visitFieldInsn(GETSTATIC, Type.getInternalName(AgentReady.class), "CAPTURE_ENABLED", "Z");
		mv.visitVarInsn(ILOAD, itsCaptureEnabledVar);
		mv.visitJumpInsn(IFEQ, aLabel);
	}
	
	private Label checkCaptureEnabled()
	{
		Label l = new Label();
		checkCaptureEnabled(l);
		return l;
	}
	
	public void behaviorEnter(BehaviorCallType aCallType)
	{
		Label l = checkCaptureEnabled();
		
		// Create arguments array
		int theArrayVar = itsFirstFreeVar;
		int theFirstArgVar = itsMethodInfo.isStatic() ? 0 : 1;
		Type[] theArgumentTypes = Type.getArgumentTypes(itsMethodInfo.getDescriptor());
		
		createArgsArray(theArgumentTypes, theArrayVar, theFirstArgVar, false);

		invokeLogBehaviorEnter(
				itsBehavior.getId(),
				"<clinit>".equals(itsBehavior.getName()),
				aCallType,
				itsMethodInfo.isStatic() ? -1 : 0,
				theArrayVar);
		
		mv.visitLabel(l);
	}
	
	public void behaviorExit()
	{
		Label l = checkCaptureEnabled();
		
		Type theReturnType = Type.getReturnType(itsMethodInfo.getDescriptor());

		// Wrap and store return value
		if (theReturnType.getSort() == Type.VOID)
		{
			mv.visitInsn(ACONST_NULL);
		}
		else
		{
			mv.visitInsn(theReturnType.getSize() == 2 ? DUP2 : DUP);
			BCIUtils.wrap(mv, theReturnType, itsUseJava14);
		}
		
		mv.visitVarInsn(ASTORE, itsFirstFreeVar);
		
		invokeLogBehaviorExit(
				itsBehavior.getId(), 
				"<clinit>".equals(itsBehavior.getName()),
				itsFirstFreeVar);
		
		mv.visitLabel(l);
	}
	
	public void behaviorExitWithException()
	{
		Label l = checkCaptureEnabled();
		
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, itsFirstFreeVar);
		
		invokeLogBehaviorExitWithException(itsBehavior.getId(), itsFirstFreeVar);
		
		mv.visitLabel(l);
	}
	
	/**
	 * Determines if the given behavior is traced.
	 */
	private HasTrace hasTrace(IBehaviorInfo aCalledBehavior)
	{
//		// If the target class is in scope, the method is traced.
//		String theClassName = aCalledBehavior.getType().getName();
//		if (itsConfig.isInScope(theClassName)) return HasTrace.YES;
		
		// Otherwise:
		// A class that has trace might inherit methods from
		// a non-traced superclass. Therefore we cannot be sure that
		// the called method is indeed traced. On the other hand if the
		// owner class has no trace, we are sure that the called method
		// is also traced, if the filters respect our requirement that
		// a traced class' subclasses must also be traced.
		return aCalledBehavior.hasTrace();
	}
	
	private void forceLoad(String aClass)
	{
		if (! itsConfig.getTODConfig().get(TODConfig.BCI_PRELOAD_CLASSES)) return;
		
		if (! itsLoadedClasses.add(aClass)) return;
		if (itsMethodInfo.getOwner().equals(aClass)) return;

		if (itsUseJava14)
		{
			mv.visitFieldInsn(GETSTATIC, itsMethodInfo.getOwner(), LogBCIVisitor.getClassFieldName(aClass), "Z");
			
			Label theEndLabel = new Label();
			mv.visitJumpInsn(IFNE, theEndLabel);
			
			mv.visitLdcInsn(aClass.replace('/', '.'));
			mv.visitMethodInsn(INVOKESTATIC, "tod/agent/AgentUtils", "loadClass", "(Ljava/lang/String;)V");
			mv.visitInsn(ICONST_1);
			mv.visitFieldInsn(PUTSTATIC, itsMethodInfo.getOwner(), LogBCIVisitor.getClassFieldName(aClass), "Z");
			
			mv.visitLabel(theEndLabel);
		}
		else
		{
			mv.visitLdcInsn(Type.getObjectType(aClass));
			mv.visitInsn(POP);
		}
	}
	
	public void methodCall(
			int aOpcode,
			String aOwner, 
			String aName,
			String aDesc,
			BehaviorCallType aCallType)
	{
		boolean theStatic = aOpcode == INVOKESTATIC;
		
		IMutableClassInfo theOwner = itsStructureDatabase.getNewClass(Util.jvmToScreen(aOwner));
		IMutableBehaviorInfo theCalledBehavior = theOwner.getNewBehavior(aName, aDesc, theStatic);

		Label theOriginalCallLabel = new Label();
				
		itsBehaviorCallInstrumenter.setup(
				theOriginalCallLabel,
				itsFirstFreeVar,
				theCalledBehavior.getId(),
				aDesc,
				theStatic);
		
		HasTrace theHasTrace = hasTrace(theCalledBehavior);
		
		Label theElse = new Label();
		Label theEndif = new Label();
		
		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();

		boolean theOriginalDone = false; // We must have exactly one original method call not tagged as TOD_CODE

		if (theHasTrace == HasTrace.UNKNOWN)
		{
			forceLoad(aOwner);
			
			// Runtime check for trace info
			BCIUtils.pushInt(mv, theCalledBehavior.getId());
			
			mv.visitMethodInsn(
					INVOKESTATIC, 
					CLS_TRACEDMETHODS, 
					"isTraced", 
					"(I)Z");
			
			mv.visitJumpInsn(IFEQ, theElse);
		}
		
		if (theHasTrace == HasTrace.UNKNOWN || theHasTrace == HasTrace.YES)
		{
			// Handle before method call
			itsBehaviorCallInstrumenter.callLogBeforeBehaviorCallDry(aCallType);
			
			itsInstrumentationRanges.end();
			theOriginalDone = true;
			
			mv.visitLabel(theOriginalCallLabel);
			
			// Do the original call
			mv.visitMethodInsn(aOpcode, aOwner, aName, aDesc);
			
			itsInstrumentationRanges.start();
			
			// Handle after method call
			itsBehaviorCallInstrumenter.callLogAfterBehaviorCallDry();						
		}
		
		if (theHasTrace == HasTrace.UNKNOWN)
		{
			mv.visitJumpInsn(GOTO, theEndif);
			mv.visitLabel(theElse);
		}
		
		if (theHasTrace == HasTrace.UNKNOWN || theHasTrace == HasTrace.NO)
		{
			// Handle before method call
			itsBehaviorCallInstrumenter.storeArgsToLocals();
			itsBehaviorCallInstrumenter.createArgsArray();
			itsBehaviorCallInstrumenter.callLogBeforeMethodCall(aCallType);
			itsBehaviorCallInstrumenter.pushArgs();

			Label theBefore = new Label();
			Label theAfter = new Label();
			Label theHandler = new Label();
			Label theFinish = new Label();
			
			mv.visitLabel(theBefore);
			
			if (! theOriginalDone) 
			{
				itsInstrumentationRanges.end();
				mv.visitLabel(theOriginalCallLabel);
			}
			
			// Do the original call
			mv.visitMethodInsn(aOpcode, aOwner, aName, aDesc);
			
			if (! theOriginalDone) itsInstrumentationRanges.start();
			
			mv.visitLabel(theAfter);
			
			mv.visitJumpInsn(GOTO, theFinish);
			
			mv.visitLabel(theHandler);
			
			itsBehaviorCallInstrumenter.callLogAfterMethodCallWithException();
			mv.visitInsn(ATHROW);

			mv.visitLabel(theFinish);
			
			mv.visitTryCatchBlock(theBefore, theAfter, theHandler, null);

			// Handle after method call
			itsBehaviorCallInstrumenter.callLogAfterMethodCall();
		}

		if (theHasTrace == HasTrace.UNKNOWN)
		{
			mv.visitLabel(theEndif);
		}
		
		// Handle capture enable condition
		Label theEnd = new Label();
		mv.visitJumpInsn(GOTO, theEnd);
		
		mv.visitLabel(theCaptureEnd);
		// Original instruction
		mv.visitMethodInsn(aOpcode, aOwner, aName, aDesc);

		mv.visitLabel(theEnd);
		
		itsInstrumentationRanges.end();
	}
	
	private IFieldInfo getVirtualField(IMutableClassInfo aOwner, String aName, ITypeInfo aType, boolean aStatic)
	{
		IMutableClassInfo theClass = aOwner;
		while(theClass != null)
		{
			IFieldInfo theField = theClass.getField(aName);
			if (theField != null)
			{
				if (theClass == aOwner) return theField;
				else if (theField.getAccess() != Access.PRIVATE) return theField;
			}
			theClass = (IMutableClassInfo) theClass.getSupertype();
		}
		
		return aOwner.getNewField(aName, aType, aStatic);
	}
	
	public void fieldWrite(
			int aOpcode, 
			String aOwner, 
			String aName, 
			String aDesc)
	{
		boolean theStatic = aOpcode == PUTSTATIC;

		IMutableClassInfo theOwner = itsStructureDatabase.getNewClass(Util.jvmToScreen(aOwner));

		ITypeInfo theType = itsStructureDatabase.getNewType(aDesc);
		IFieldInfo theField = getVirtualField(theOwner, aName, theType, theStatic);
		
		Type theASMType = Type.getType(aDesc);
		
		Label theOriginalInstructionLabel = new Label();

		int theCurrentVar = itsFirstFreeVar;
		int theValueVar;
		int theTargetVar;
		
		if (theStatic)
		{
			theTargetVar = -1;
			theValueVar = theCurrentVar++;
		}
		else
		{
			theTargetVar = theCurrentVar++;
			theValueVar = theCurrentVar++;
		}
		
		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();
		
		// :: [target], value
	
		// Store parameters
		
		mv.visitVarInsn(theASMType.getOpcode(ISTORE), theValueVar);
		if (! theStatic) mv.visitVarInsn(ASTORE, theTargetVar);
		
		// Call log method
		invokeLogFieldWrite(theOriginalInstructionLabel, theField.getId(), theTargetVar, theASMType, theValueVar);
		
		// Push parameters back to stack
		if (! theStatic) mv.visitVarInsn(ALOAD, theTargetVar);
		mv.visitVarInsn(theASMType.getOpcode(ILOAD), theValueVar);
		
		mv.visitLabel(theCaptureEnd);
		
		itsInstrumentationRanges.end();

		// Do the original operation
		mv.visitLabel(theOriginalInstructionLabel);
		mv.visitFieldInsn(aOpcode, aOwner, aName, aDesc);
	}
	
	public void variableWrite(
			int aOpcode, 
			int aVar)
	{
		int theSort = BCIUtils.getSort(aOpcode);
		
		Label theOriginalInstructionLabel = new Label();

		// :: value
	
		// Perform store
		mv.visitLabel(theOriginalInstructionLabel);
		mv.visitVarInsn(aOpcode, aVar);
		
		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();
		
		// Call log method
		invokeLogLocalVariableWrite(
				theOriginalInstructionLabel, 
				aVar, 
				BCIUtils.getType(theSort), 
				aVar);
		
		mv.visitLabel(theCaptureEnd);
		
		itsInstrumentationRanges.end();
	}
	
	public void variableInc(
			int aVar, 
			int aIncrement)
	{
		Label theOriginalInstructionLabel = new Label();

		// :: value
	
		// Perform store
		mv.visitLabel(theOriginalInstructionLabel);
		mv.visitIincInsn(aVar, aIncrement);
		
		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();
		
		// Call log method
		invokeLogLocalVariableWrite(
				theOriginalInstructionLabel, 
				aVar, 
				BCIUtils.getType(Type.INT), 
				aVar);
		
		mv.visitLabel(theCaptureEnd);
		
		itsInstrumentationRanges.end();
	}

	public void newArray(NewArrayClosure aClosure, int aBaseTypeId)
	{
		Label theOriginalInstructionLabel = new Label();

		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();

		// :: size
		
		int theCurrentVar = itsFirstFreeVar;
		int theSizeVar;
		int theTargetVar;
		
		theSizeVar = theCurrentVar++;
		theTargetVar = theCurrentVar++;
		
		// Store size
		mv.visitVarInsn(ISTORE, theSizeVar);
		
		// Reload size
		mv.visitVarInsn(ILOAD, theSizeVar);
	
		itsInstrumentationRanges.end();
		
		// Perform new array
		mv.visitLabel(theOriginalInstructionLabel);
		aClosure.proceed(mv);
		
		itsInstrumentationRanges.start();
		
		// :: array
		
		// Store target
		mv.visitVarInsn(ASTORE, theTargetVar);
		
		// Reload target
		mv.visitVarInsn(ALOAD, theTargetVar);

		// Call log method (if no exception occurred)
		invokeLogNewArray(
				theOriginalInstructionLabel, 
				theTargetVar, 
				aBaseTypeId, 
				theSizeVar);
		
		// Handle capture enable condition
		Label theEnd = new Label();
		mv.visitJumpInsn(GOTO, theEnd);
		
		mv.visitLabel(theCaptureEnd);
		// Original instruction
		aClosure.proceed(mv);

		mv.visitLabel(theEnd);
		
		itsInstrumentationRanges.end();
	}
	
	public void arrayWrite(int aOpcode)
	{
		int theSort = BCIUtils.getSort(aOpcode);
		Type theType = BCIUtils.getType(theSort);
		
		Label theOriginalInstructionLabel = new Label();
		
		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();
		
		// :: array ref, index, value
		
		int theCurrentVar = itsFirstFreeVar;
		int theValueVar;
		int theIndexVar;
		int theTargetVar;
		
		theTargetVar = theCurrentVar++;
		theIndexVar = theCurrentVar++;
		theValueVar = theCurrentVar++;
		
		// Store parameters
		
		mv.visitVarInsn(theType.getOpcode(ISTORE), theValueVar);
		mv.visitVarInsn(ISTORE, theIndexVar);
		mv.visitVarInsn(ASTORE, theTargetVar);
		
		// Reload parameters
		
		mv.visitVarInsn(ALOAD, theTargetVar);
		mv.visitVarInsn(ILOAD, theIndexVar);
		mv.visitVarInsn(theType.getOpcode(ILOAD), theValueVar);
		
		itsInstrumentationRanges.end();
		
		// Perform store
		mv.visitLabel(theOriginalInstructionLabel);
		mv.visitInsn(aOpcode);
		
		itsInstrumentationRanges.start();
		
		// Call log method (if no exception occurred)
		invokeLogArrayWrite(
				theOriginalInstructionLabel, 
				theTargetVar, 
				theIndexVar, 
				theType, 
				theValueVar);
	
		// Handle capture enable condition
		Label theEnd = new Label();
		mv.visitJumpInsn(GOTO, theEnd);
		
		mv.visitLabel(theCaptureEnd);
		// Original instruction
		mv.visitInsn(aOpcode);

		mv.visitLabel(theEnd);
		
		itsInstrumentationRanges.end();
	}
	
	public void instanceOf(String aDesc, ITypeInfo aType)
	{
		Label theOriginalInstructionLabel = new Label();
		
		itsInstrumentationRanges.start();
		
		Label theCaptureEnd = checkCaptureEnabled();
		
		// :: object
		
		int theCurrentVar = itsFirstFreeVar;
		int theObjectVar = theCurrentVar++;
		int theResultVar = theCurrentVar++;
		
		// Store parameters
		
		mv.visitVarInsn(ASTORE, theObjectVar);
		
		// Reload parameters
		mv.visitVarInsn(ALOAD, theObjectVar);
		
		itsInstrumentationRanges.end();
		
		// Perform original instanceof
		mv.visitLabel(theOriginalInstructionLabel);
		mv.visitTypeInsn(INSTANCEOF, aDesc);
		
		itsInstrumentationRanges.start();
		
		mv.visitVarInsn(ISTORE, theResultVar);
		
		// Call log method (if no exception occurred)
		invokeLogInstanceOf(theOriginalInstructionLabel, theObjectVar, aType, theResultVar);
		
		// Reload result
		mv.visitVarInsn(ILOAD, theResultVar);
		
		// Handle capture enable condition
		Label theEnd = new Label();
		mv.visitJumpInsn(GOTO, theEnd);
		
		mv.visitLabel(theCaptureEnd);
		// Original instruction
		mv.visitTypeInsn(INSTANCEOF, aDesc);

		mv.visitLabel(theEnd);

		itsInstrumentationRanges.end();
	}
	

	/**
	 * Pushes standard method log args onto the stack:
	 * <li>Target collector</li>
	 * <li>Timestamp</li>
	 * <li>Thread id</li>
	 */
	public void pushStdLogArgs()
	{
		// ->target collector
		mv.visitVarInsn(ALOAD, itsCollectorVar);
	}

	/**
	 * Pushes standard method log args onto the stack:
	 * <li>Target collector</li>
	 * <li>Timestamp</li>
	 * <li>Thread id</li>
	 */
	public void pushDryLogArgs()
	{
		// ->target collector
		mv.visitVarInsn(ALOAD, itsCollectorVar);
	}
	
	public void invokeLogBeforeBehaviorCall(
			Label aOriginalInstructionLabel, 
			int aMethodId,
			BehaviorCallType aCallType,
			int aTargetVar,
			int aArgsArrayVar)
	{
		pushStdLogArgs();
	
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);
		
		// ->call type
		mv.visitFieldInsn(
				GETSTATIC, 
				CLS_BEHAVIORCALLTYPE,
				aCallType.name(), 
				DSC_BEHAVIORCALLTYPE);
	
		// ->target
		if (aTargetVar < 0) mv.visitInsn(ACONST_NULL);
		else mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->arguments
		mv.visitVarInsn(ALOAD, aArgsArrayVar);
	
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logBeforeBehaviorCall", 
				"(II"+DSC_BEHAVIORCALLTYPE+"Ljava/lang/Object;[Ljava/lang/Object;)V");
	}

	public void invokeLogBeforeBehaviorCallDry(
			Label aOriginalInstructionLabel, 
			int aMethodId,
			BehaviorCallType aCallType)
	{
		pushDryLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);

		// ->call type
		mv.visitFieldInsn(
				GETSTATIC, 
				CLS_BEHAVIORCALLTYPE,
				aCallType.name(), 
				DSC_BEHAVIORCALLTYPE);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logBeforeBehaviorCallDry", 
				"(II"+DSC_BEHAVIORCALLTYPE+")V");
	}
	
	public void invokeLogAfterBehaviorCall(
			Label aOriginalInstructionLabel, 
			int aMethodId,
			int aTargetVar,
			int aResultVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);
		
		// ->target
		if (aTargetVar < 0) mv.visitInsn(ACONST_NULL);
		else mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->result
		mv.visitVarInsn(ALOAD, aResultVar);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logAfterBehaviorCall", 
				"(IILjava/lang/Object;Ljava/lang/Object;)V");
	}

	public void invokeLogAfterBehaviorCallDry()
	{
		pushDryLogArgs();
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logAfterBehaviorCallDry", 
				"()V");
	}
	
	public void invokeLogAfterBehaviorCallWithException(
			Label aOriginalInstructionLabel, 
			int aMethodId,
			int aTargetVar,
			int aExceptionVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);
		
		// ->target
		if (aTargetVar < 0) mv.visitInsn(ACONST_NULL);
		else mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->result
		mv.visitVarInsn(ALOAD, aExceptionVar);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logAfterBehaviorCallWithException", 
				"(IILjava/lang/Object;Ljava/lang/Object;)V");
	}
	
	public void invokeLogFieldWrite(
			Label aOriginalInstructionLabel, 
			int aFieldId,
			int aTargetVar,
			Type theType,
			int aValueVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->field id
		BCIUtils.pushInt(mv, aFieldId);
		
		// ->target
		if (aTargetVar < 0) mv.visitInsn(ACONST_NULL);
		else mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->value
		mv.visitVarInsn(theType.getOpcode(ILOAD), aValueVar);
		BCIUtils.wrap(mv, theType, itsUseJava14);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logFieldWrite", 
				"(IILjava/lang/Object;Ljava/lang/Object;)V");
	}

	public void invokeLogNewArray(
			Label aOriginalInstructionLabel, 
			int aTargetVar,
			int aBaseTypeId,
			int aSizeVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->target
		mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->type id
		BCIUtils.pushInt(mv, aBaseTypeId);
		
		// ->size
		mv.visitVarInsn(ILOAD, aSizeVar);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logNewArray", 
				"(ILjava/lang/Object;II)V");
	}
	
	public void invokeLogArrayWrite(
			Label aOriginalInstructionLabel, 
			int aTargetVar,
			int aIndexVar,
			Type theType,
			int aValueVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->target
		mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->index
		mv.visitVarInsn(ILOAD, aIndexVar);
		
		// ->value
		mv.visitVarInsn(theType.getOpcode(ILOAD), aValueVar);
		BCIUtils.wrap(mv, theType, itsUseJava14);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logArrayWrite", 
				"(ILjava/lang/Object;ILjava/lang/Object;)V");
	}
	
	public void invokeLogInstanceOf(
			Label aOriginalInstructionLabel, 
			int aObjectVar,
			ITypeInfo aType,
			int aResultVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->object
		mv.visitVarInsn(ALOAD, aObjectVar);
		
		// ->type id
		BCIUtils.pushInt(mv, aType.getId());
		
		// ->result
		mv.visitVarInsn(ILOAD, aResultVar);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logInstanceOf",
				"(ILjava/lang/Object;II)V");
	}
	
	public void invokeLogLocalVariableWrite(
			Label aOriginalInstructionLabel, 
			int aVariableId,
			Type theType,
			int aValueVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		pushProbeId(aOriginalInstructionLabel);
		
		// ->variable id
		BCIUtils.pushInt(mv, aVariableId);
		
		// ->value
		mv.visitVarInsn(theType.getOpcode(ILOAD), aValueVar);
		BCIUtils.wrap(mv, theType, itsUseJava14);
		
		mv.visitMethodInsn(
				INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logLocalVariableWrite", 
				"(IILjava/lang/Object;)V");
	}

	public void invokeLogBehaviorEnter (
			int aMethodId,
			boolean aClInit,
			BehaviorCallType aCallType,
			int aTargetVar, 
			int aArgsArrayVar)
	{
		pushStdLogArgs();
	
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);
		
		// ->call type
		mv.visitFieldInsn(
				GETSTATIC, 
				CLS_BEHAVIORCALLTYPE,
				aCallType.name(), 
				DSC_BEHAVIORCALLTYPE);
	
		// ->target
		if (LogBCIVisitor.ENABLE_VERIFY || aTargetVar < 0) mv.visitInsn(ACONST_NULL);
		else mv.visitVarInsn(ALOAD, aTargetVar);
		
		// ->arguments
		mv.visitVarInsn(ALOAD, aArgsArrayVar);
		
		mv.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				aClInit ? "logClInitEnter" : "logBehaviorEnter", 
				"(I"+DSC_BEHAVIORCALLTYPE+"Ljava/lang/Object;[Ljava/lang/Object;)V");	
	}

	public void invokeLogBehaviorExit (
			int aMethodId, 
			boolean aClInit,
			int aResultVar)
	{
		pushStdLogArgs();
		
		// ->operation location
		mv.visitVarInsn(ILOAD, itsReturnLocationVar);
		
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);
		
		// ->result
		mv.visitVarInsn(ALOAD, aResultVar);

		mv.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				aClInit ? "logClInitExit" : "logBehaviorExit", 
				"(IILjava/lang/Object;)V");	
	}

	private void invokeLogBehaviorExitWithException (int aMethodId, int aExceptionVar)
	{
		pushStdLogArgs();
		
		// ->method id
		BCIUtils.pushInt(mv, aMethodId);
		
		// ->exception
		mv.visitVarInsn(ALOAD, aExceptionVar);
		
		mv.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL, 
				CLS_EVENTCOLLECTOR, 
				"logBehaviorExitWithException", 
				"(ILjava/lang/Object;)V");	
	}
	
	/**
	 * Generates the bytecode that creates the arguments array and stores
	 * it into a variable.
	 * Stack: . => .
	 */
	public void createArgsArray(Type[] aArgTypes, int aArrayVar, int aFirstArgVar, boolean aReverse)
	{
		if (aArgTypes.length > 0)
		{
			mv.visitIntInsn(BIPUSH, aArgTypes.length);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			
			// :: array
			mv.visitVarInsn(ASTORE, aArrayVar);
			
			// ::
			
			int theCurrentVar = aFirstArgVar;
			short theIndex = 0;
			for (Type theType : aArgTypes)
			{
				if (aReverse) theCurrentVar -= theType.getSize();
				
				mv.visitVarInsn(ALOAD, aArrayVar); // :: array
				BCIUtils.pushInt(mv, theIndex++); // :: array, index
				mv.visitVarInsn(theType.getOpcode(ILOAD), theCurrentVar); // :: array, index, val
				BCIUtils.wrap(mv, theType, itsUseJava14);
				mv.visitInsn(AASTORE); // ::
				
				if (! aReverse) theCurrentVar += theType.getSize();
			}
		}
		else
		{
			mv.visitInsn(ACONST_NULL); // :: null
			mv.visitVarInsn(ASTORE, aArrayVar); // ::
		}
	}

	/**
	 * Creates a {@link NewArrayClosure} for the NEWARRAY opcode.
	 */
	public static NewArrayClosure createNewArrayClosure(final int aOperand)
	{
		return new NewArrayClosure()
		{
			@Override
			public void proceed(MethodVisitor mv)
			{
				mv.visitIntInsn(NEWARRAY, aOperand);
			}
		};
	}
	
	/**
	 * Creates a {@link NewArrayClosure} for the ANEWARRAY opcode.
	 */
	public static NewArrayClosure createNewArrayClosure(final String aDesc)
	{
		return new NewArrayClosure()
		{
			@Override
			public void proceed(MethodVisitor mv)
			{
				mv.visitTypeInsn(ANEWARRAY, aDesc);
			}
		};
	}
	
	/**
	 * A closure for generating the bytecode for NEWARRAY instructions.
	 * @author gpothier
	 */
	public static abstract class NewArrayClosure
	{
		public abstract void proceed(MethodVisitor mv);
	}

}
