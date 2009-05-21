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

import java.util.ListIterator;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import tod.Util;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;

/**
 * Instruments in-scope methods.
 * @author gpothier
 */
public class MethodInstrumenter_InScope extends MethodInstrumenter
{
	private LabelManager itsLabelManager = new LabelManager();
	private LocationsManager itsLocationsManager;

	/**
	 * A boolean that indicates if the method was called from in-scope code.
	 */
	private int itsFromScopeVar;
	
	/**
	 * Temporarily holds the return value of called methods
	 */
	private int itsTmpValueVar;
	
	/**
	 * Stores the target of out-of-scope constructor calls.
	 */
	private int itsCtorTargetVar;
	
	/**
	 * For constructors, the invocation instructions that corresponds to constructor chaining.
	 */
	private MethodInsnNode itsChainingInvocation;
	
	public MethodInstrumenter_InScope(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior);
		itsFromScopeVar = nextFreeVar(1);
		itsTmpValueVar = nextFreeVar(2);
		itsCtorTargetVar = nextFreeVar(1);
		getNode().maxStack += 3; // This is the max we add to the stack
		
		itsLocationsManager = new LocationsManager(getDatabase());
		itsChainingInvocation = findChainingInvocation();
		if (! CLS_OBJECT.equals(getClassNode().name) && isConstructor() && itsChainingInvocation == null) 
			throw new RuntimeException("Should have constructor chaining: "+aNode);
		
		// At least for now...
		if (CLS_OBJECT.equals(getClassNode().name)) throw new RuntimeException("java.lang.Object cannot be in scope!");
	}

	@Override
	public void proceed()
	{
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);

		// Insert entry instructions
		{
			// Set ThreadData var to null for verifier to work
			s.ACONST_NULL();
			s.ASTORE(getThreadDataVar());
			
			// Store the monitoring mode for the behavior in a local
			s.pushInt(getBehavior().getId()); 
			s.INVOKESTATIC(CLS_TRACEDMETHODS, "traceFull", "(I)Z");
			s.DUP();
			s.ISTORE(getTraceEnabledVar());
			
			// Check monitoring mode
			s.IFfalse("start");
			
			// Monitoring enabled
			{
				// Store ThreadData object
				s.INVOKESTATIC(CLS_EVENTCOLLECTOR, "_getThreadData", "()"+DSC_THREADDATA);
				s.DUP();
				s.ASTORE(getThreadDataVar());
				
				// Send event
				s.pushInt(getBehavior().getId()); 
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evInScopeBehaviorEnter", "(I)V");
				
				//Check if we must send args
				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "isInScope", "()Z");
				s.DUP();
				s.ISTORE(itsFromScopeVar);
				
				s.IFtrue("afterSendArgs");
				sendEnterArgs(s);
				s.label("afterSendArgs");
				
				s.GOTO("start");
			}
		}
		
		// Insert exit instructions (every return statement is replaced by a GOTO to this block)
		{
			s.label("exit");
			
			// Check monitoring mode
			s.ILOAD(getTraceEnabledVar());
			s.IFfalse("return");
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evInScopeBehaviorExit_Normal", "()V");
			
			s.label("return");
			s.RETURN(Type.getReturnType(getNode().desc));
		}
		
		// Insert finally instructions
		{
			s.label("finally");
			
			// Check monitoring mode
			s.ILOAD(getTraceEnabledVar());
			s.IFfalse("throw");
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evInScopeBehaviorExit_Exception", "()V");

			s.label("throw");
			s.ATHROW();
		}

		s.label("start");
		
		processInstructions(getNode().instructions);
		
		getNode().instructions.insert(s);
		getNode().visitLabel(s.getLabel("end"));
		getNode().visitTryCatchBlock(s.getLabel("start"), s.getLabel("end"), s.getLabel("finally"), null);
	}

	/**
	 * Sends the arguments to the method.
	 */
	private void sendEnterArgs(SyntaxInsnList s)
	{
		// For non-static methods, we add the this argument, except
		// for constructors, where we send the this after chaining is done.
		boolean theSendThis = !isStatic() && !isConstructor();
		
		int theArgCount = getArgTypes().length;
		if (theSendThis) theArgCount++;

		s.ALOAD(getThreadDataVar());
		s.pushInt(theArgCount); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendBehaviorEnterArgs", "(I)V");

		int theArgIndex = 0;
		if (theSendThis) sendValue_Ref(s, theArgIndex++);
		else if (isConstructor()) theArgIndex++;
		
		for(Type theType : getArgTypes())
		{
			sendValue(s, theArgIndex, theType);
			theArgIndex += theType.getSize();
		}
	}
	
	private static boolean isALOAD0(AbstractInsnNode aNode)
	{
		if (aNode.getOpcode() == Opcodes.ALOAD)
		{
			VarInsnNode theVarNode = (VarInsnNode) aNode;
			if (theVarNode.var == 0) return true;
		}
		return false;
	}
	
	private static boolean isConstructorCall(AbstractInsnNode aNode)
	{
		if (aNode.getOpcode() == Opcodes.INVOKESPECIAL)
		{
			MethodInsnNode theMethodNode = (MethodInsnNode) aNode;
			if ("<init>".equals(theMethodNode.name)) return true;
		}
		return false;
	}
	
	/**
	 * For constructors, looks for the invoke instruction that corresponds to constructor
	 * chaining, if any (the only case there is none is for java.lang.Object);
	 */
	private MethodInsnNode findChainingInvocation()
	{
		if (! isConstructor()) return null;

		VarInsnNode theAload0 = null;
		
		Frame[] theFrames = getFrames();
		
		ListIterator<AbstractInsnNode> theIterator = getNode().instructions.iterator();
		int i = 0; // Instruction rank
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			
			// Verify that "this" is not accessed before chaining
			if (isALOAD0(theNode))
			{
				if (theAload0 == null) theAload0 = (VarInsnNode) theNode;
				else throw new RuntimeException("This loaded more than once before chaining");
			}
			
			if (isConstructorCall(theNode))
			{
				Frame theFrame = theFrames[i];
				int sz = Type.getArgumentsAndReturnSizes(((MethodInsnNode) theNode).desc);
				int theArgSize = sz >> 2;
				
				// Check if the target of the call is "this"
				SourceValue theThis = (SourceValue) theFrame.getStack(theArgSize-1);
				if (theThis.insns.contains(theAload0)) return (MethodInsnNode) theNode;
			}
			
			i++;
		}
		
		return null;
	}
	
	private void processInstructions(InsnList aInsns)
	{
		ListIterator<AbstractInsnNode> theIterator = aInsns.iterator();
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			int theOpcode = theNode.getOpcode();
			
			
			switch(theOpcode)
			{
			case Opcodes.IRETURN:
			case Opcodes.LRETURN:
			case Opcodes.FRETURN:
			case Opcodes.DRETURN:
			case Opcodes.ARETURN:
			case Opcodes.RETURN:
				processReturn(aInsns, (InsnNode) theNode);
				break;
				
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				processInvoke(aInsns, (MethodInsnNode) theNode);
				break;

			case Opcodes.NEWARRAY:
			case Opcodes.ANEWARRAY:
			case Opcodes.MULTIANEWARRAY:
				processNewArray(aInsns, theNode);
				break;
				
			case Opcodes.GETFIELD:
			case Opcodes.GETSTATIC:
				processGetField(aInsns, (FieldInsnNode) theNode);
				break;
				
			case Opcodes.IALOAD:
			case Opcodes.LALOAD:
			case Opcodes.FALOAD:
			case Opcodes.DALOAD:
			case Opcodes.AALOAD:
			case Opcodes.BALOAD:
			case Opcodes.CALOAD:
			case Opcodes.SALOAD:
				processGetArray(aInsns, (InsnNode) theNode);
				break;
			}
		}
	}
	
	private void processReturn(InsnList aInsns, InsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
		
		s.GOTO("exit");
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	/**
	 * Determines if the given node is an invocation that corresponds to constructor chaining.
	 */
	private boolean isChainingInvocation(MethodInsnNode aNode)
	{
		return aNode == itsChainingInvocation;
	}
	
	/**
	 * Determines if the given node corresponds to the initial constructor call
	 * (vs. constructor chaining).
	 */
	private boolean isObjectInitialization(MethodInsnNode aNode)
	{
		if (! isConstructorCall(aNode)) return false;
		else return !isChainingInvocation(aNode);
	}
	
	private boolean isCalleeInScope(MethodInsnNode aNode)
	{
		return getClassInstrumenter().getInstrumenter().isInScope(aNode.owner);
	}
	
	/**
	 * Returns the id of the behavior invoked by the given node.
	 */
	private int getBehaviorId(MethodInsnNode aNode)
	{
		IMutableClassInfo theClass = getDatabase().getNewClass(Util.jvmToScreen(aNode.owner));
		IMutableBehaviorInfo theBehavior = theClass.getNewBehavior(aNode.name, aNode.desc, aNode.getOpcode() == Opcodes.INVOKESTATIC);
		return theBehavior.getId();
	}
	
	private void processInvoke(InsnList aInsns, MethodInsnNode aNode)
	{
		Type theReturnType = Type.getReturnType(aNode.desc);
		
		// We are going to create three paths:
		// - Trace disabled
		// - Trace enabled, monitored call
		// - Trace enabled, unmonitored call
		// The original instruction is used for the trace disabled path
		MethodInsnNode theMonitoredClone = (MethodInsnNode) aNode.clone(null);
		MethodInsnNode theUnmonitoredClone = (MethodInsnNode) aNode.clone(null);
		MethodInsnNode theTraceDisabledClone = (MethodInsnNode) aNode.clone(null);
		
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
		Label lTrDisabled = new Label(); // Trace disabled path
		Label lUnmonitored = new Label(); // Unmonitored path
		Label lEnd = new Label(); 
		
		s.ILOAD(getTraceEnabledVar());
		s.IFfalse(lTrDisabled);
		{
			// Trace enabled
			Label lCheckChaining = new Label();
			
			// For constructor calls scope is resolved statically
			boolean theCtor = isConstructorCall(aNode); 
			
			// Check if called method is monitored
			if (! theCtor)	s.pushInt(getBehaviorId(aNode));
			if (! theCtor)	s.INVOKESTATIC(CLS_TRACEDMETHODS, "traceUnmonitored", "(I)Z");
			if (! theCtor)	s.IFtrue(lUnmonitored);
			if (! theCtor || isCalleeInScope(aNode))
			{
				// Monitored call
				s.add(theMonitoredClone);				
				s.GOTO(lCheckChaining);
			}
			
			if (! theCtor)	s.label(lUnmonitored);
			
			if (! theCtor || !isCalleeInScope(aNode))
			{
				// Unmonitored call
				Label lHnStart = new Label();
				Label lHnEnd = new Label();
				Label lHandler = new Label(); // Exception handler
				
				// before call
				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorCall", "()V");
				
				if (theCtor)
				{
					// Prepare for sending object initialized
					s.DUP();
					s.ASTORE(itsCtorTargetVar);
				}

				// call
				s.label(lHnStart);
				s.add(theUnmonitoredClone);
				s.label(lHnEnd);
				
				// after call
				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorResult", "()V");
		
				if (theReturnType.getSort() != Type.VOID) 
				{
					s.ISTORE(theReturnType, itsTmpValueVar);
					sendValue(s, itsTmpValueVar, theReturnType);
					s.ILOAD(theReturnType, itsTmpValueVar);
				}
				
				if (theCtor)
				{
					// Send object initialized
					s.ALOAD(getThreadDataVar());
					s.ALOAD(itsCtorTargetVar);
					s.INVOKEVIRTUAL(CLS_THREADDATA, "evObjectInitialized", "("+DSC_OBJECT+")V");
				}
				
				s.GOTO(lCheckChaining);
				
				// Exception handler
				s.label(lHandler);
				
				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorException", "()V");
				s.ATHROW();
				
				getNode().visitTryCatchBlock(lHnStart, lHnEnd, lHandler, null);
			}
			
			s.label(lCheckChaining);
			
			if (isChainingInvocation(aNode))
			{
				// Send deferred target
				s.ALOAD(getThreadDataVar());
				s.ALOAD(0);
				s.INVOKEVIRTUAL(CLS_THREADDATA, "sendConstructorTarget", "("+DSC_OBJECT+")V");
			}

			s.GOTO(lEnd);
		}
		s.label(lTrDisabled);
		{
			// trace is disabled
			s.add(theTraceDisabledClone);
		}

		s.label(lEnd);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processNewArray(InsnList aInsns, AbstractInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
		Label l = new Label();

		s.ILOAD(getTraceEnabledVar());
		s.IFfalse(l);
		{
			s.DUP();
			s.ALOAD(getThreadDataVar());
			s.SWAP();
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evNew", "("+DSC_OBJECT+")V");
		}
		
		s.label(l);
		
		aInsns.insert(aNode, s);
	}

	private void processGetField(InsnList aInsns, FieldInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
		Label l = new Label();
		Type theType = Type.getType(aNode.desc);

		s.ILOAD(getTraceEnabledVar());
		s.IFfalse(l);
		{
			s.ALOAD(getThreadDataVar()); 
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evFieldRead", "()V"); 
			
			s.ISTORE(theType, itsTmpValueVar);
			sendValue(s, itsTmpValueVar, theType);
			s.ILOAD(theType, itsTmpValueVar);
		}
		
		s.label(l);
		
		aInsns.insert(aNode, s);
	}

	private void processGetArray(InsnList aInsns, InsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
		Label l = new Label();
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()));

		s.ILOAD(getTraceEnabledVar());
		s.IFfalse(l);
		{
			s.ALOAD(getThreadDataVar()); 
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evArrayRead", "()V"); 
			
			s.ISTORE(theType, itsTmpValueVar);
			sendValue(s, itsTmpValueVar, theType);
			s.ILOAD(theType, itsTmpValueVar);
		}
		
		s.label(l);
		
		aInsns.insert(aNode, s);
	}

}
