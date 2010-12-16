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

import static tod.impl.bci.asm2.BCIUtils.*;

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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import tod.Util;
import tod.core.DebugFlags;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;
import tod.impl.bci.asm2.MethodInfo.NewInvokeLink;

/**
 * Instruments in-scope methods.
 * @author gpothier
 */
public class MethodInstrumenter_InScope extends MethodInstrumenter
{
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
	
	private WrapperDef[] itsWrapperDefs = {
			new ClassLoader_loadClass_WrapperDef(),
			new SimpleWrapperDef("Object_equals", "("+DSC_OBJECT+DSC_OBJECT+")Z", null, "equals", "("+DSC_OBJECT+")Z"),
			new SimpleWrapperDef("Object_hashCode", "("+DSC_OBJECT+")I", null, "hashCode", "()I"),
			new SimpleWrapperDef("Object_clone", "("+DSC_OBJECT+")"+DSC_OBJECT, null, "clone", "()"+DSC_OBJECT),
			new SimpleWrapperDef("String_length", "("+DSC_STRING+")I", CLS_STRING, "length", "()I"),
			new SimpleWrapperDef("String_charAt", "("+DSC_STRING+"I)C", CLS_STRING, "charAt", "(I)C"),
			new SimpleWrapperDef("String_compareTo", "("+DSC_STRING+DSC_STRING+")I", CLS_STRING, "compareTo", "("+DSC_STRING+")I"),
			new SimpleWrapperDef("String_compareToIgnoreCase", "("+DSC_STRING+DSC_STRING+")I", CLS_STRING, "compareToIgnoreCase", "("+DSC_STRING+")I"),
	};
	
	public MethodInstrumenter_InScope(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior);

		// At least for now...
		if (BCIUtils.CLS_OBJECT.equals(getClassNode().name)) throw new RuntimeException("java.lang.Object cannot be in scope!");
		
		itsTmpValueVar = nextFreeVar(2);
		
		getNode().maxStack += 3; // This is the max we add to the stack
	}
	
	@Override
	public void proceed()
	{
		// Abstracts and natives have no body.
		if (isAbstract() || isNative()) return;
		
		itsMethodInfo = new MethodInfo(getDatabase(), getClassNode().name, getNode());
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
		// TODO: See how we do the dynamic scoping
		s.GETSTATIC(BCIUtils.CLS_OBJECT, ClassInstrumenter.BOOTSTRAP_FLAG, "Z");
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
			if (DebugFlags.USE_FIELD_CACHE) s.add(itsMethodInfo.getFieldCacheInitInstructions());
			
			s.INVOKESTATIC(BCIUtils.CLS_EVENTCOLLECTOR_AGENT, "_getThreadData", "()"+BCIUtils.DSC_THREADDATA); // ThD
			s.DUP();
			s.ASTORE(getThreadDataVar());
			
			// Send event
			s.pushInt(getBehavior().getId()); // ThD, BId 
			s.INVOKEVIRTUAL(
					BCIUtils.CLS_THREADDATA, 
					isStaticInitializer() ? "evInScopeClinitEnter" : "evInScopeBehaviorEnter", 
					"(I)Z");
			
			//Check if we must send args
			Label lAfterSendArgs = new Label();
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
//			s.ALOAD(getThreadDataVar());
//			s.pushInt(theArgCount); 
//			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "sendBehaviorEnterArgs", "(I)V");

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
	private IBehaviorInfo getCalledBehavior(MethodInsnNode aNode)
	{
		IClassInfo theClass = getDatabase().getClass(Util.jvmToScreen(aNode.owner), false);
		if (theClass == null) return null;
		else return theClass.getBehavior(aNode.name, aNode.desc);
	}
	
	private WrapperDef getWrapperFor(MethodInsnNode aNode)
	{
		if (aNode.getOpcode() == Opcodes.INVOKESPECIAL) return null; // That would be a super call, don't reroute it.
		for (WrapperDef theDef : itsWrapperDefs) if (theDef.accept(getDatabase(), aNode)) return theDef;
		return null;
	}
	
	private void processInvoke(MethodInsnNode aNode)
	{
		BCIFrame theNextFrame = itsMethodInfo.getFrame(aNode.getNext());
		getDatabase().registerSnapshotSignature(BCIUtils.getSnapshotSig(theNextFrame, true)); // For the after call
		getDatabase().registerSnapshotSignature(BCIUtils.getSnapshotSig(theNextFrame, false)); // For the after throws
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		boolean theStatic = aNode.getOpcode() == Opcodes.INVOKESTATIC;
		boolean theTouchy = MethodInstrumenter_OutOfScope.isTouchy(aNode.owner, aNode.name, theStatic);
		
		Label lBeginTry = new Label();
		Label lEndTry = new Label();
		Label lHandler = new Label();
		Label lAfter = new Label();

		// Touchy methods have no enveloppe instrumentation, so we have to add it here.
		if (theTouchy)
		{
			// Send event
			s.ALOAD(getThreadDataVar());
			s.pushInt(getCalledBehavior(aNode).getId());
			s.INVOKEVIRTUAL(
					BCIUtils.CLS_THREADDATA, 
					"<clinit>".equals(aNode.name) ? "evOutOfScopeClinitEnter" : "evOutOfScopeBehaviorEnter", 
					"(I)V");

			s.label(lBeginTry);
		}

		// Insert method call
		WrapperDef theWrapperDef = getWrapperFor(aNode);
		if (theWrapperDef != null)
		{
			s.INVOKESTATIC(CLS_INTRINSICS, theWrapperDef.getWrapperName(), theWrapperDef.getWrapperDesc());
		}
		else
		{
			MethodInsnNode theClone = (MethodInsnNode) aNode.clone(null);
			s.add(theClone);				
		}

		if (theTouchy)
		{
			s.label(lEndTry);
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evOutOfScopeBehaviorExit_Normal", "()Z");		
			
			Type theReturnType = Type.getReturnType(aNode.desc);
			
			// Send return value if needed
			if (theReturnType.getSort() != Type.VOID) 
			{
				s.IFfalse(lAfter);

				s.ISTORE(theReturnType, itsTmpValueVar); // We can't use DUP in case of long or double, so we just store the value
				sendValue(s, itsTmpValueVar, theReturnType);
				s.ILOAD(theReturnType, itsTmpValueVar);
			}
			else
			{
				s.POP();
			}

			s.GOTO(lAfter);
			
			// Handler
			s.label(lHandler);
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evOutOfScopeBehaviorExit_Exception", "()V");
			s.ATHROW();

			getNode().visitTryCatchBlock(lBeginTry, lEndTry, lHandler, null);
		}
		
		s.label(lAfter);
		
		if (BCIUtils.isConstructorCall(aNode)) 
		{
			// Check if called method is monitored
			if (! isCalleeInScope(aNode))
			{
				boolean theChaining = itsMethodInfo.isChainingInvocation(aNode);
				if (theChaining)
				{
					s.ALOAD(getThreadDataVar());
					s.ALOAD(0);
					s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "sendConstructorTarget", "("+BCIUtils.DSC_OBJECT+")V");				
				}
				else
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
			}
		}
		
		replace(aNode, s);
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

	private static final boolean NO_FIELD_CAPTURE = false;
	
	static
	{
	    if (NO_FIELD_CAPTURE) 
	        System.err.println("*** WARNING: MethodInstrumenter_InScope.NO_FIELD_CAPTURE = true ***");
	}
	
	private void processGetField(FieldInsnNode aNode)
	{
	    if (NO_FIELD_CAPTURE) return;
		if (DebugFlags.USE_FIELD_CACHE) processGetField_Cache(aNode);
		else processGetField_NoCache(aNode);
	}
	
	private void processGetField_NoCache(FieldInsnNode aNode)
	{
		SyntaxInsnList s = new SyntaxInsnList();
		Type theType = Type.getType(aNode.desc);

		s.ISTORE(theType, itsTmpValueVar);
		
		s.ALOAD(getThreadDataVar()); 
		s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evFieldRead", "()V"); 
		sendValue(s, itsTmpValueVar, theType);
		
		s.ILOAD(theType, itsTmpValueVar);
		
		insertAfter(aNode, s);
	}
	
	private void processGetField_Cache(FieldInsnNode aNode)
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
		if (! DebugFlags.USE_FIELD_CACHE) return;
			
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
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()), BCIUtils.TYPE_OBJECT);

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

	private static abstract class WrapperDef
	{
		private final String itsWrapperName;
		private final String itsWrapperDesc;
		
		public WrapperDef(String aWrapperName, String aWrapperDesc)
		{
			itsWrapperName = aWrapperName;
			itsWrapperDesc = aWrapperDesc;
		}
		
		public String getWrapperName()
		{
			return itsWrapperName;
		}
		
		public String getWrapperDesc()
		{
			return itsWrapperDesc;
		}
		
		public abstract boolean accept(IStructureDatabase aDatabase, MethodInsnNode aNode);
	}
	
	/**
	 * A wrapper def for a single method
	 * @author gpothier
	 */
	private static class SimpleWrapperDef extends WrapperDef
	{
		private final String itsClassName;
		private final String itsMethodName;
		private final String itsDesc;
		
		public SimpleWrapperDef(
				String aWrapperName,
				String aWrapperDesc,
				String aClassName,
				String aMethodName,
				String aDesc)
		{
			super(aWrapperName, aWrapperDesc);
			itsClassName = aClassName;
			itsMethodName = aMethodName;
			itsDesc = aDesc;
		}

		@Override
		public boolean accept(IStructureDatabase aDatabase, MethodInsnNode aNode)
		{
			if (itsClassName != null && ! itsClassName.equals(aNode.owner)) return false;
			if (itsMethodName != null && ! itsMethodName.equals(aNode.name)) return false;
			if (itsDesc != null && ! itsDesc.equals(aNode.desc)) return false;
			return true;
		}
	}
	
	private static class ClassLoader_loadClass_WrapperDef extends WrapperDef
	{
		public ClassLoader_loadClass_WrapperDef()
		{
			super("ClassLoader_loadClass", "("+DSC_CLASSLOADER+DSC_STRING+")"+DSC_CLASS);
		}

		@Override
		public boolean accept(IStructureDatabase aDatabase, MethodInsnNode aNode)
		{
			return aNode.name.equals("loadClass")
				&& isClassLoader(aDatabase, aNode.owner)
				&& aNode.desc.equals("("+DSC_STRING+")"+DSC_CLASS);
		}
		
		private static boolean isClassLoader(IClassInfo aClass)
		{
			while(aClass != null)
			{
				if (CLS_CLASSLOADER.equals(aClass.getName())) return true;
				aClass = aClass.getSupertype();
			}
			return false;
		}
		
		private static boolean isClassLoader(IStructureDatabase aDatabase, String aClassName)
		{
			IClassInfo theClass = aDatabase.getClass(Util.jvmToScreen(aClassName), false);
			return isClassLoader(theClass);
		}
		

	}
}
