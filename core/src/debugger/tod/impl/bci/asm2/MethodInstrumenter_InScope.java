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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.SourceValue;

import tod.Util;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.ITypeInfo;
import tod.impl.bci.asm2.Analysis.SourceFrame;
import zz.utils.ArrayStack;
import zz.utils.ListMap;
import zz.utils.Stack;
import zz.utils.Utils;

/**
 * Instruments in-scope methods.
 * @author gpothier
 */
public class MethodInstrumenter_InScope extends MethodInstrumenter
{
	private LabelManager itsLabelManager = new LabelManager();

	/**
	 * A boolean that indicates if the method was called from in-scope code.
	 */
	private int itsFromScopeVar;
	
	/**
	 * Temporarily holds the return value of called methods
	 */
	private int itsTmpValueVar;
	
	/**
	 * For constructors, the invocation instructions that corresponds to constructor chaining.
	 */
	private MethodInsnNode itsChainingInvocation;

	/**
	 * Maps field access instructions to the local variable slot that holds the cached value.
	 */
	private Map<FieldInsnNode, Integer> itsCachedFieldAccesses = new HashMap<FieldInsnNode, Integer>();
	
	/**
	 * Instructions that initializes each slot of the field cache.
	 */
	private InsnList itsFieldCacheInit;
	
	public MethodInstrumenter_InScope(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior, true);

		// At least for now...
		if (CLS_OBJECT.equals(getClassNode().name)) throw new RuntimeException("java.lang.Object cannot be in scope!");
		
		itsFromScopeVar = nextFreeVar(1);
		itsTmpValueVar = nextFreeVar(2);
		getNode().maxStack += 3; // This is the max we add to the stack
	}
	
	private void throwRTEx(String aMessage)
	{
		Utils.rtex("Error in %s.%s%s: %s", getClassNode().name, getNode().name, getNode().desc, aMessage);	
	}

	@Override
	public void proceed()
	{
		// Abstracts and natives have no body.
		if (isAbstract() || isNative()) return;
		
		itsChainingInvocation = findChainingInvocation();
		if (! CLS_OBJECT.equals(getClassNode().name) && isConstructor() && itsChainingInvocation == null) 
			throwRTEx("Should have constructor chaining");
		
		setupFieldCaches();

		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);

		// Insert entry instructions
		{
			// Set ThreadData var to null for verifier to work
			s.ACONST_NULL();
			s.ASTORE(getThreadDataVar());
			
			s.add(itsFieldCacheInit);
			
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
				s.INVOKESTATIC(CLS_EVENTCOLLECTOR, "_getThreadData", "()"+DSC_THREADDATA); // ThD
				s.DUP(); // ThD ThD
				s.DUP(); // ThD ThD ThD
				s.ASTORE(getThreadDataVar()); // ThD ThD

				// Store inScope 
				s.INVOKEVIRTUAL(CLS_THREADDATA, "isInScope", "()Z"); // ThD, inScope
				s.ISTORE(itsFromScopeVar); // ThD
				
				// Send event
				s.pushInt(getBehavior().getId()); // ThD, BId 
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evInScopeBehaviorEnter", "(I)V");
				
				//Check if we must send args
				s.ILOAD(itsFromScopeVar);
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
		getNode().visitInsn(Opcodes.NOP);
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

	private boolean hasAload0Only(SourceValue aValue)
	{
		if (aValue.insns.size() != 1) return false;
		return isALOAD0((AbstractInsnNode) aValue.insns.iterator().next()); 
	}
	
	/**
	 * For constructors, looks for the invoke instruction that corresponds to constructor
	 * chaining, if any (the only case there is none is for java.lang.Object);
	 */
	private MethodInsnNode findChainingInvocation()
	{
		if (! isConstructor()) return null;

		ListIterator<AbstractInsnNode> theIterator = getNode().instructions.iterator();
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			
			if (isConstructorCall(theNode))
			{
				SourceFrame theFrame = getFrame(theNode);
				int theArgCount = Type.getArgumentTypes(((MethodInsnNode) theNode).desc).length;
				
				// Check if the target of the call is "this"
				SourceValue theThis = theFrame.getStack(theFrame.getStackSize()-theArgCount-1);
				if (hasAload0Only(theThis)) return (MethodInsnNode) theNode;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the TOD field referred to by the given instruction.
	 */
	private IFieldInfo getField(FieldInsnNode aNode)
	{
		IMutableClassInfo theOwner = getDatabase().getNewClass(aNode.owner);
		ITypeInfo theType = getDatabase().getNewType(aNode.desc);

		return theOwner.getNewField(aNode.name, theType, aNode.getOpcode() == Opcodes.GETSTATIC);
	}
	
	/**
	 * Whether the given field access instructions accesses a field on self (this).
	 */
	private boolean isSelfFieldAccess(FieldInsnNode aNode)
	{
		if (isStatic()) return false; //Not an error: a static method can get fields of some object.
		
		SourceValue theTarget = getFrame(aNode).getStack(0);

		for(AbstractInsnNode theNode : (Set<AbstractInsnNode>) theTarget.insns)
		{
			if (theNode instanceof VarInsnNode)
			{
				VarInsnNode theVarInsnNode = (VarInsnNode) theNode;
				if (theVarInsnNode.var == 0) return true;
			}
		}
		
		return false;
	}
	
	/**
	 * For each field access on the current object or static field access,
	 * check if the access might execute more than once (because of several
	 * access locations or of loops). If so, reserve a local variable to hold the
	 * last observed value of the field so that we can optimize the Get Field event.  
	 */
	private void setupFieldCaches()
	{
		Set<AbstractInsnNode> theVisitedJumps = new HashSet<AbstractInsnNode>();
		
		// Maps each field to the instructions that read that field on self.
		// Note: the same instruction can appear twice, which is actually what ultimately
		// triggers the field to be cached.
		ListMap<IFieldInfo, FieldInsnNode> theAccessMap = new ListMap<IFieldInfo, FieldInsnNode>();

		// A list of paths to process (denoted by the first instruction of the path) 
		Stack<AbstractInsnNode> theWorkList = new ArrayStack<AbstractInsnNode>();
		theWorkList.push(getNode().instructions.getFirst());
		
		// Build the access maps
		while(! theWorkList.isEmpty()) 
		{
			AbstractInsnNode theNode = theWorkList.pop();
			
			// If this flag is true the next instruction is pushed onto
			// the working list at the end of the iteration
			boolean theContinue = false;
			
			if (theNode instanceof FieldInsnNode)
			{
				FieldInsnNode theFieldInsnNode = (FieldInsnNode) theNode;
				
				if (theNode.getOpcode() == Opcodes.GETFIELD)
				{
					if (isSelfFieldAccess(theFieldInsnNode))
					{
						IFieldInfo theField = getField(theFieldInsnNode);
						theAccessMap.add(theField, theFieldInsnNode);
					}
				}
				else if (theNode.getOpcode() == Opcodes.GETSTATIC)
				{
					IFieldInfo theField = getField(theFieldInsnNode);
					theAccessMap.add(theField, theFieldInsnNode);
				}
				
				theContinue = true;
			}
			else if (theNode instanceof JumpInsnNode)
			{
				JumpInsnNode theJumpInsnNode = (JumpInsnNode) theNode;
				if (theVisitedJumps.add(theNode)) 
				{
					theWorkList.push(theJumpInsnNode.label);
					if (theNode.getOpcode() != Opcodes.GOTO) theContinue = true;
				}
			}
			else if (theNode instanceof TableSwitchInsnNode)
			{
				TableSwitchInsnNode theTableSwitchInsnNode = (TableSwitchInsnNode) theNode;
				if (theVisitedJumps.add(theNode))
				{
					theWorkList.push(theTableSwitchInsnNode.dflt);
					theWorkList.pushAll(theTableSwitchInsnNode.labels);
				}
			}
			else if (theNode instanceof LookupSwitchInsnNode)
			{
				LookupSwitchInsnNode theLookupSwitchInsnNode = (LookupSwitchInsnNode) theNode;
				if (theVisitedJumps.add(theNode))
				{
					theWorkList.push(theLookupSwitchInsnNode.dflt);
					theWorkList.pushAll(theLookupSwitchInsnNode.labels);
				}
			}
			else if ((theNode.getOpcode() >= Opcodes.IRETURN && theNode.getOpcode() <= Opcodes.RETURN)
				|| theNode.getOpcode() == Opcodes.RET)
			{
				// Don't continue
			}
			else
			{
				theContinue = true;
			}
			
			if (theContinue && theNode.getNext() != null) theWorkList.push(theNode.getNext()); 
		}

		// Set up the final structure
		SyntaxInsnList s = new SyntaxInsnList(null);
		Iterator<Map.Entry<IFieldInfo, List<FieldInsnNode>>> theIterator = theAccessMap.entrySet().iterator();
		while(theIterator.hasNext())
		{
			Map.Entry<IFieldInfo, List<FieldInsnNode>> theEntry = theIterator.next();
			if (theEntry.getValue().size() >= 2)
			{
				String theDesc = theEntry.getValue().get(0).desc;
				Type theType = Type.getType(theDesc);
				int theSlot = nextFreeVar(theType.getSize());
				
				// Register instruction in the map
				for(FieldInsnNode theNode : theEntry.getValue()) 
					itsCachedFieldAccesses.put(theNode, theSlot);
				
				// Create initializing instruction.
				s.pushDefaultValue(theType);
				s.ISTORE(theType, theSlot);
			}
		}
		
		itsFieldCacheInit = s;
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
		// We are going to create three paths, each using a clone of the original instruction:
		// - Trace disabled
		// - Trace enabled, monitored call
		// - Trace enabled, unmonitored call
		MethodInsnNode theTraceDisabledClone = (MethodInsnNode) aNode.clone(null);
		
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
		Label lTrDisabled = new Label(); // Trace disabled path
		Label lEnd = new Label(); 
		
		s.ILOAD(getTraceEnabledVar());
		s.IFfalse(lTrDisabled);
		{
			// Trace enabled
			Label lCheckChaining = new Label();
			
			// For constructor calls scope is resolved statically
			if (isConstructorCall(aNode)) 
				processInvoke_TraceEnabled_Constructor(s, aNode, lCheckChaining);
			else 
				processInvoke_TraceEnabled_Method(s, aNode, lCheckChaining);
			
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
	
	private void processInvoke_TraceEnabled_Method(
			SyntaxInsnList s, 
			MethodInsnNode aNode, 
			Label lCheckChaining)
	{
		Type theReturnType = Type.getReturnType(aNode.desc);
		
		MethodInsnNode theMonitoredClone = (MethodInsnNode) aNode.clone(null);
		MethodInsnNode theUnmonitoredClone = (MethodInsnNode) aNode.clone(null);
		Label lUnmonitored = new Label(); // Unmonitored path

		// Check if called method is monitored
		s.pushInt(getBehaviorId(aNode));
		s.INVOKESTATIC(CLS_TRACEDMETHODS, "traceUnmonitored", "(I)Z");
		s.IFtrue(lUnmonitored);

		// Monitored call
		s.add(theMonitoredClone);				
		s.GOTO(lCheckChaining);
		
		s.label(lUnmonitored);
		
		// Unmonitored call
		Label lHnStart = new Label();
		Label lHnEnd = new Label();
		Label lHandler = new Label(); // Exception handler
		
		// before call
		s.ALOAD(getThreadDataVar());
		s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorCall", "()V");
		
		// call
		s.label(lHnStart);
		s.add(theUnmonitoredClone);
		s.label(lHnEnd);
		
		// after call
		s.ALOAD(getThreadDataVar());

		if (theReturnType.getSort() != Type.VOID) 
		{
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorResultNonVoid", "()V");
			s.ISTORE(theReturnType, itsTmpValueVar);
			sendValue(s, itsTmpValueVar, theReturnType);
			s.ILOAD(theReturnType, itsTmpValueVar);
		}
		else
		{
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorResultVoid", "()V");
		}
		
		s.GOTO(lCheckChaining);
		
		// Exception handler
		s.label(lHandler);
		
		s.ALOAD(getThreadDataVar());
		s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorException", "()V");
		s.ATHROW();
		
		getNode().visitTryCatchBlock(lHnStart, lHnEnd, lHandler, null);
	}

	private void processInvoke_TraceEnabled_Constructor(
			SyntaxInsnList s, 
			MethodInsnNode aNode, 
			Label lCheckChaining)
	{
		Type theReturnType = Type.getReturnType(aNode.desc);
		
		MethodInsnNode theMonitoredClone = (MethodInsnNode) aNode.clone(null);
		MethodInsnNode theUnmonitoredClone = (MethodInsnNode) aNode.clone(null);

		// Check if called method is monitored
		if (isCalleeInScope(aNode))
		{
			// Monitored call
			s.add(theMonitoredClone);				
			s.GOTO(lCheckChaining);
		}
		
		if (!isCalleeInScope(aNode))
		{
			// Unmonitored call
			Label lHnStart = new Label();
			Label lHnEnd = new Label();
			Label lHandler = new Label(); // Exception handler
			
			// before call
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorCall", "()V");
			
			// call
			s.label(lHnStart);
			s.add(theUnmonitoredClone);
			s.label(lHnEnd);
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evUnmonitoredBehaviorResultVoid", "()V");
	
			// Send object initialized
			if (!isChainingInvocation(aNode))
			{
				s.DUP(); // Note that this relies on standard compiler behavior (NEW followed by DUP)
				s.ALOAD(getThreadDataVar());
				s.SWAP();
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
		Label lSameValue = new Label();
		Label lEndIf = new Label();
		
		Type theType = Type.getType(aNode.desc);

		s.ILOAD(getTraceEnabledVar());
		s.IFfalse(l);
		{
			s.ISTORE(theType, itsTmpValueVar);
			
			s.ALOAD(getThreadDataVar()); 

			
			Integer theCacheSlot = itsCachedFieldAccesses.get(aNode);
			if (theCacheSlot != null)
			{
				// Check if value is the same as before
				s.ILOAD(theType, itsTmpValueVar);
				s.ILOAD(theType, theCacheSlot);
				
				switch(theType.getSort())
				{
		        case Type.BOOLEAN:
		        case Type.BYTE:
		        case Type.CHAR:
		        case Type.SHORT:
		        case Type.INT:
		        	s.IF_ICMPEQ(lSameValue);
		        	break;
		        	
		        case Type.FLOAT:
		        	s.FCMPL();
		        	s.IFEQ(lSameValue);
		        	break;
		        	
		        case Type.LONG:
		        	s.LCMP();
		        	s.IFEQ(lSameValue);
		        	break;
		        	
		        case Type.DOUBLE:
		        	s.DCMPL();
		        	s.IFEQ(lSameValue);
		        	break;

		        case Type.OBJECT:
		        case Type.ARRAY:
		        	s.IF_ACMPEQ(lSameValue);
		        	break;
		        	
		        default:
		            throw new RuntimeException("Not handled: "+theType);
				}

			}
			
			// Value not equal to cached value
			{
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evFieldRead", "()V"); 
				sendValue(s, itsTmpValueVar, theType);
			}
			
			if (theCacheSlot != null)
			{
				// Update cache
				s.ILOAD(theType, itsTmpValueVar);
				s.ISTORE(theType, theCacheSlot);

				s.GOTO(lEndIf);
				
				s.label(lSameValue);
				
				// Same value as cached
				{
					s.INVOKEVIRTUAL(CLS_THREADDATA, "evFieldRead_Same", "()V");
				}
				
				s.label(lEndIf);
			}
			
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
