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

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import tod.Util;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.impl.bci.asm2.MethodInfo.NewInvokeLink;
import tod.impl.replay2.SList;

/**
 * Instruments in-scope methods.
 * @author gpothier
 */
public class MethodInstrumenter_InScope extends MethodInstrumenter
{
	/**
	 * A boolean that indicates if the method was called from in-scope code.
	 */
	private int itsFromScopeVar;
	
	/**
	 * Temporarily holds the return value of called methods
	 */
	private int itsTmpValueVar;
	
	/**
	 * Temporarily holds the target of constructor calls
	 */
	private int[] itsTmpTargetVars;
	
	private MethodInfo itsMethodInfo;
	
	private Label lExit = new Label();
	
	/**
	 * Contains the classes whose loading has already been forced.
	 */
	private final Set<String> itsLoadedClasses = new HashSet<String>();


	public MethodInstrumenter_InScope(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior);

		// At least for now...
		if (BCIUtils.CLS_OBJECT.equals(getClassNode().name)) throw new RuntimeException("java.lang.Object cannot be in scope!");
		
		itsFromScopeVar = nextFreeVar(1);
		itsTmpValueVar = nextFreeVar(2);
		
		getNode().maxStack += 3; // This is the max we add to the stack
		
		// Mark classes that are known to be already loaded as loaded
		itsLoadedClasses.add(getClassNode().name);
		itsLoadedClasses.add("java/lang/Object");
		itsLoadedClasses.add("java/lang/String");
		itsLoadedClasses.add("java/lang/StringBuffer");
		itsLoadedClasses.add("java/lang/StringBuilder");
		itsLoadedClasses.add("java/lang/System");
		itsLoadedClasses.add("java/lang/Class");
	}
	
	@Override
	public void proceed()
	{
		// Abstracts and natives have no body.
		if (isAbstract() || isNative()) return;
		
		itsMethodInfo = new MethodInfo(getDatabase(), getClassNode(), getNode());
		int theSlotsCount = itsMethodInfo.setupLocalCacheSlots(getNode().maxLocals);
		getNode().maxLocals += theSlotsCount;			
		
		itsTmpTargetVars = new int[itsMethodInfo.getMaxNewInvokeNesting()+1];
		for (int i=0;i<itsTmpTargetVars.length;i++) itsTmpTargetVars[i] = nextFreeVar(1);
		
		// Save original handlers
		TryCatchBlockNode[] theHandlers = (TryCatchBlockNode[]) getNode().tryCatchBlocks.toArray(new TryCatchBlockNode[getNode().tryCatchBlocks.size()]);

		SyntaxInsnList s = new SyntaxInsnList();
		
		Label lStart = new Label();
		Label lFinally = new Label();
		Label lEnd = new Label();
		Label lActive = new Label();
		
		// Create activation checking code
		s.pushInt(getBehavior().getId()); 
		s.INVOKESTATIC(BCIUtils.CLS_TRACEDMETHODS, "traceFull", "(I)Z");
		s.IFtrue(lActive);

		{
			// Tracing not active
			MethodNode theClone = BCIUtils.cloneMethod(getNode());
			s.add(theClone.instructions);
			getNode().tryCatchBlocks.addAll(theClone.tryCatchBlocks);
			getNode().localVariables.addAll(theClone.localVariables);
		}


		s.label(lActive);
			
		// Insert entry instructions
		{
			// Set ThreadData var to null for verifier to work
			s.ACONST_NULL();
			s.ASTORE(getThreadDataVar());
			
			s.add(itsMethodInfo.getFieldCacheInitInstructions());
			
			// Store ThreadData object
			s.INVOKESTATIC(BCIUtils.CLS_EVENTCOLLECTOR_AGENT, "_getThreadData", "()"+BCIUtils.DSC_THREADDATA); // ThD
			s.DUP(); // ThD ThD
			s.DUP(); // ThD ThD ThD
			s.ASTORE(getThreadDataVar()); // ThD ThD

			// Store inScope 
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "isInScope", "()Z"); // ThD, inScope
			s.ISTORE(itsFromScopeVar); // ThD
			
			// Send event
			s.pushInt(getBehavior().getId()); // ThD, BId 
			s.INVOKEVIRTUAL(
					BCIUtils.CLS_THREADDATA, 
					isStaticInitializer() ? "evInScopeClinitEnter" : "evInScopeBehaviorEnter", 
					"(I)V");
			
			//Check if we must send args
			Label lAfterSendArgs = new Label();
			s.ILOAD(itsFromScopeVar);
			s.IFtrue(lAfterSendArgs);
			sendEnterArgs(s);
			s.label(lAfterSendArgs);
			
			s.GOTO(lStart);
		}
		
		// Insert exit instructions (every return statement is replaced by a GOTO to this block)
		{
			s.label(lExit);
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evInScopeBehaviorExit_Normal", "()V");

			s.RETURN(Type.getReturnType(getNode().desc));
		}
		
		// Insert finally instructions
		{
			s.label(lFinally);
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evInScopeBehaviorExit_Exception", "()V");

			s.ATHROW();
		}

		s.label(lStart);
		
		processInstructions();
		processHandlers(theHandlers);
		
		getNode().instructions.insert(s);
		getNode().visitLabel(lEnd);
		getNode().visitInsn(Opcodes.NOP);
		getNode().visitTryCatchBlock(lStart, lEnd, lFinally, null);
	}
	
	@Override
	protected int getTraceEnabledVar()
	{
		throw new UnsupportedOperationException();
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

		if (theArgCount > 0)
		{
			s.ALOAD(getThreadDataVar());
			s.pushInt(theArgCount); 
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "sendBehaviorEnterArgs", "(I)V");

			int theArgIndex = 0;
			if (theSendThis) sendValue_Ref(s, theArgIndex++);
			else if (isConstructor()) theArgIndex++;
			
			for(Type theType : getArgTypes())
			{
				sendValue(s, theArgIndex, theType);
				theArgIndex += theType.getSize();
			}
		}			
	}
	
	/**
	 * Assigns an id to each handler (based on the order of the try-catch nodes)
	 * and add corresponding event emission.
	 */
	private void processHandlers(TryCatchBlockNode[] aHandlers)
	{
		Set<Label> theProcessedLabels = new HashSet<Label>();
		int theId = 0;
		for(TryCatchBlockNode theNode : aHandlers)
		{
			Label theLabel = theNode.handler.getLabel();
			if (theProcessedLabels.add(theLabel)) 
			{
				// Add event emission code
				SyntaxInsnList s = new SyntaxInsnList();
				s.ALOAD(getThreadDataVar());
				s.pushInt(theId);
				s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evHandlerReached", "(I)V");
				
				getNode().instructions.insert(theNode.handler, s);
				
				// Update id
				theId++;
			}
		}
	}
	
	private InsnList getInstructions()
	{
		return getNode().instructions;
	}
	
	private void replace(AbstractInsnNode aNode, InsnList aList)
	{
		InsnList theInstructions = getInstructions();
		theInstructions.insert(aNode, aList);
		theInstructions.remove(aNode);
	}
	
	private void insertAfter(AbstractInsnNode aNode, InsnList aList)
	{
		getInstructions().insert(aNode, aList);
	}
	
	private void insertBefore(AbstractInsnNode aNode, InsnList aList)
	{
		getInstructions().insertBefore(aNode, aList);
	}
	
	private void processInstructions()
	{
		ListIterator<AbstractInsnNode> theIterator = getInstructions().iterator();
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
				processReturn((InsnNode) theNode);
				break;
				
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				processInvoke((MethodInsnNode) theNode);
				break;

			case Opcodes.NEWARRAY:
			case Opcodes.ANEWARRAY:
			case Opcodes.MULTIANEWARRAY:
				processNewArray(theNode);
				break;
				
			case Opcodes.GETFIELD:
			case Opcodes.GETSTATIC:
				processGetField((FieldInsnNode) theNode);
				break;
				
//			case Opcodes.PUTFIELD:
//			case Opcodes.PUTSTATIC:
//				processPutField(aInsns, (FieldInsnNode) theNode);
//				break;
				
			case Opcodes.IALOAD:
			case Opcodes.LALOAD:
			case Opcodes.FALOAD:
			case Opcodes.DALOAD:
			case Opcodes.AALOAD:
			case Opcodes.BALOAD:
			case Opcodes.CALOAD:
			case Opcodes.SALOAD:
				processGetArray((InsnNode) theNode);
				break;
				
			case Opcodes.ARRAYLENGTH:
				processArrayLength((InsnNode) theNode);
				break;
				
			case Opcodes.LDC:
				processLdc((LdcInsnNode) theNode);
				break;
				
			case Opcodes.INSTANCEOF:
				processInstanceOf((TypeInsnNode) theNode);
				break;
			}
		}
	}
	
	private void processReturn(InsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		s.GOTO(lExit);
		
		replace(aNode, s);
	}

	private boolean isCalleeInScope(MethodInsnNode aNode)
	{
		return getDatabase().isInScope(aNode.owner);
	}
	
	/**
	 * Returns the id of the behavior invoked by the given node.
	 */
	private int getBehaviorId(MethodInsnNode aNode)
	{
		IMutableClassInfo theClass = getDatabase().getNewClass(Util.jvmToScreen(aNode.owner));
		IMutableBehaviorInfo theBehavior = theClass.getNewBehavior(
				aNode.name, 
				aNode.desc, 
				aNode.getOpcode() == Opcodes.INVOKESTATIC ? Opcodes.ACC_STATIC : 0);
		return theBehavior.getId();
	}
	
	private void forceLoad(String aClass, SyntaxInsnList s)
	{
		if (! itsLoadedClasses.add(aClass)) return;

		s.LDC(Type.getObjectType(aClass));
		s.POP();
		
		if ((getClassNode().version & 0xffff) < 49) getClassNode().version = 49; // Needed for LDC <class>
	}
	

	
	private void processInvoke(MethodInsnNode aNode)
	{
		getDatabase().registerSnapshotLocalsSignature(BCIUtils.getLocalsSig(itsMethodInfo.getFrame(aNode)));
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		// Force the loading of the class so that its scope is known
		forceLoad(aNode.owner, s);
		
		Label lCheckChaining = new Label();
		
		// For constructor calls scope is resolved statically
		if (BCIUtils.isConstructorCall(aNode)) 
			processInvoke_TraceEnabled_Constructor(s, aNode, lCheckChaining);
		else 
			processInvoke_TraceEnabled_Method(s, aNode, lCheckChaining);
		
		s.label(lCheckChaining);
		
		if (itsMethodInfo.isChainingInvocation(aNode))
		{
			// Send deferred target
			s.ALOAD(getThreadDataVar());
			s.ALOAD(0);
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "sendConstructorTarget", "("+BCIUtils.DSC_OBJECT+")V");
		}

		replace(aNode, s);
	}

	/** Copied from MethodNode */
	private LabelNode getLabelNode(final Label l)
	{
		if (!(l.info instanceof LabelNode))
		{
			l.info = new LabelNode(l);
		}
		return (LabelNode) l.info;
	}
	
	/** Copied from MethodNode */
	private void visitTryCatchBlock(
			MethodNode node,
			final Label start,
			final Label end,
			final Label handler,
			final String type)
	{
		node.tryCatchBlocks.add(0, new TryCatchBlockNode(
				getLabelNode(start),
				getLabelNode(end),
				getLabelNode(handler),
				type));
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
		s.INVOKESTATIC(BCIUtils.CLS_TRACEDMETHODS, "traceUnmonitored", "(I)Z");
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
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorCall", "()V");
		
		// call
		s.label(lHnStart);
		s.add(theUnmonitoredClone);
		s.label(lHnEnd);
		
		// after call
		s.ALOAD(getThreadDataVar());

		if (theReturnType.getSort() != Type.VOID) 
		{
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorResultNonVoid", "()V");
			s.ISTORE(theReturnType, itsTmpValueVar);
			sendValue(s, itsTmpValueVar, theReturnType);
			s.ILOAD(theReturnType, itsTmpValueVar);
		}
		else
		{
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorResultVoid", "()V");
		}
		
		s.GOTO(lCheckChaining);
		
		// Exception handler
		s.label(lHandler);
		
		s.ALOAD(getThreadDataVar());
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorException", "()V");
		s.ATHROW();
		
		visitTryCatchBlock(getNode(), lHnStart, lHnEnd, lHandler, null);
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
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorCall", "()V");
			
			// call
			s.label(lHnStart);
			s.add(theUnmonitoredClone);
			s.label(lHnEnd);
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorResultVoid", "()V");
	
			// Send object initialized
			if (! itsMethodInfo.isChainingInvocation(aNode))
			{
				// Save target
				NewInvokeLink theNewInvokeLink = itsMethodInfo.getNewInvokeLink(aNode);
				SyntaxInsnList s2 = new SyntaxInsnList();
				s2.DUP();
				s2.ASTORE(itsTmpTargetVars[theNewInvokeLink.getNestingLevel()]);
				insertAfter(theNewInvokeLink.getNewInsn(), s2);

				// Do send
				s.ALOAD(getThreadDataVar());
				s.ALOAD(itsTmpTargetVars[theNewInvokeLink.getNestingLevel()]);
				s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evObjectInitialized", "("+BCIUtils.DSC_OBJECT+")V");
			}
			
			s.GOTO(lCheckChaining);
			
			// Exception handler
			s.label(lHandler);
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evUnmonitoredBehaviorException", "()V");
			s.ATHROW();
			
			visitTryCatchBlock(getNode(), lHnStart, lHnEnd, lHandler, null);
		}
		
	}
	
	private void processNewArray(AbstractInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		s.DUP();
		s.ALOAD(getThreadDataVar());
		s.SWAP();
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evNewArray", "("+BCIUtils.DSC_OBJECT+")V");
		
		insertAfter(aNode, s);
	}

	private void processGetField(FieldInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		Label lSameValue = new Label();
		Label lEndIf = new Label();
		
		Type theType = Type.getType(aNode.desc);

		s.ISTORE(theType, itsTmpValueVar);
		
		s.ALOAD(getThreadDataVar()); 

		
		Integer theCacheSlot = itsMethodInfo.getCacheSlot(aNode);
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
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evFieldRead", "()V"); 
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
				s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evFieldRead_Same", "()V");
			}
			
			s.label(lEndIf);
		}
		
		s.ILOAD(theType, itsTmpValueVar);
		
		insertAfter(aNode, s);
	}

	private void processPutField(FieldInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		Type theType = Type.getType(aNode.desc);
		
		Integer theCacheSlot = itsMethodInfo.getCacheSlot(aNode);
		if (theCacheSlot != null)
		{
			// Store the value in the cache
			s.DUP(theType);
			s.ISTORE(theType, theCacheSlot);
		}
			
		insertBefore(aNode, s);
	}
	
	private void processGetArray(InsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()));

		s.ALOAD(getThreadDataVar()); 
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evArrayRead", "()V"); 
		
		s.ISTORE(theType, itsTmpValueVar);
		sendValue(s, itsTmpValueVar, theType);
		s.ILOAD(theType, itsTmpValueVar);
		
		insertAfter(aNode, s);
	}

	private void processArrayLength(InsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();

		s.DUP();
		s.ALOAD(getThreadDataVar());
		s.SWAP();
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evArrayLength", "(I)V"); 
		
		insertAfter(aNode, s);
	}
	
	/**
	 * LDC of class constant can throw an exception
	 */
	private void processLdc(LdcInsnNode aNode)
	{
		if (! (aNode.cst instanceof Type) && ! (aNode.cst instanceof String)) return;
		
		SyntaxInsnList s = new SyntaxInsnList();

		s.DUP();
		s.ALOAD(getThreadDataVar());
		s.SWAP();
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evCst", "("+BCIUtils.DSC_OBJECT+")V");
		
		insertAfter(aNode, s);
	}
	
	private void processInstanceOf(TypeInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();

		s.DUP();
		s.ALOAD(getThreadDataVar());
		s.SWAP();
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evInstanceOfOutcome", "(I)V");
		
		insertAfter(aNode, s);
	}
}
