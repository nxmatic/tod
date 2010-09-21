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
package tod.impl.replay2;

import static tod.impl.bci.asm2.BCIUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import tod.Util;
import tod.core.DebugFlags;
import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.bci.asm2.MethodInfo;
import tod.impl.bci.asm2.SyntaxInsnList;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;
import tod.impl.bci.asm2.MethodInfo.NewInvokeLink;
import tod.impl.database.structure.standard.StructureDatabaseUtils;
import zz.utils.SetMap;

public abstract class MethodReplayerGenerator
{
	public static final String REPLAY_CLASS_PREFIX = "$trepl$";
	public static final String SNAPSHOT_METHOD_NAME = "$tsnap";

	private final TODConfig itsConfig;
	private final IMutableStructureDatabase itsDatabase;
	private final ClassNode itsTarget;
	
	private final IBehaviorInfo itsBehavior;
	private final ClassNode itsClassNode;
	private final MethodNode itsMethodNode;
	
	private final MethodInfo itsMethodInfo;
	
	private Label lCodeStart;
	private Label lCodeEnd;
//	private Label lExitException = new Label();
	private List itsOriginalTryCatchNodes;

	private Type[] itsArgTypes;
	private Type itsReturnType;
		
	/**
	 * A variable slot that can hold a normal or double value.
	 */
	private int itsTmpVar;
	
	/**
	 * A temporary variable used to store the target of operations.
	 */
	private int itsTmpTargetVar;
	private int itsTmpValueVar;
	
	/**
	 * Temporarily holds the target of constructor calls
	 */
	private int[] itsTmpTargetVars;
	
	/**
	 * Additional instructions that should be added at the end of the main method
	 * after instrumentation is completed.
	 */
	private InsnList itsAdditionalInstructions = new InsnList();
	
	/**
	 * For each original NEW instruction, maps to the last instruction of the block that replaces it.
	 */
	private Map<TypeInsnNode, AbstractInsnNode> itsNewReplacementInsnsMap = new HashMap<TypeInsnNode, AbstractInsnNode>();
	
	private int itsThreadReplayerSlot;
	private int itsSaveArgsSlots;
	
	/**
	 * Maps field keys (see {@link #getFieldKey(FieldInsnNode)}) to the corresponding cache slot. 
	 */
	private Map<String, Integer> itsFieldCacheMap = new HashMap<String, Integer>();
	
	public MethodReplayerGenerator(
			TODConfig aConfig, 
			IMutableStructureDatabase aDatabase,
			IBehaviorInfo aBehavior,
			ClassNode aClassNode, 
			MethodNode aMethodNode)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsBehavior = aBehavior;
		itsClassNode = aClassNode;
		itsMethodNode = aMethodNode;
		
		itsArgTypes = Type.getArgumentTypes(itsMethodNode.desc);
		itsReturnType = Type.getReturnType(itsMethodNode.desc);
		itsStatic = BCIUtils.isStatic(itsMethodNode.access);
		itsConstructor = "<init>".equals(itsMethodNode.name);

		itsTarget = new ClassNode();
		itsTarget.name = REPLAY_CLASS_PREFIX+itsBehavior.getId();
		itsTarget.sourceFile = itsTarget.name+".class";
		itsTarget.superName = CLS_OBJECT;
		itsTarget.methods.add(itsMethodNode);
		itsTarget.version = Opcodes.V1_5;
		itsTarget.access = Opcodes.ACC_PUBLIC;
		
		itsMethodInfo = new MethodInfo(itsDatabase, itsClassNode, itsMethodNode);
		
		itsThreadReplayerSlot = (Type.getArgumentsAndReturnSizes(itsMethodNode.desc) >> 2) - 1 + (itsStatic ? 0 : 1);
	}
	
	protected MethodInfo getMethodInfo()
	{
		return itsMethodInfo;
	}
	
	protected MethodNode getMethodNode()
	{
		return itsMethodNode;
	}
	
	protected int getBehaviorId()
	{
		return itsBehavior.getId();
	}

	/**
	 * Computes the maximum number of local slots needed to save invocation
	 * arguments.
	 */
	private int computeMaxSaveArgsSpace(InsnList aInsns)
	{
		int theMax = 0;
		ListIterator<AbstractInsnNode> theIterator = aInsns.iterator();
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			int theOpcode = theNode.getOpcode();
			
			switch(theOpcode)
			{
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				MethodInsnNode theMethodNode = (MethodInsnNode) theNode;
				int theSize = Type.getArgumentsAndReturnSizes(theMethodNode.desc) >> 2;
				if (theOpcode != Opcodes.INVOKESTATIC) theSize++;
				if (theSize > theMax) theMax = theSize;
				break;
			}
		}
		
		return theMax;
	}


	public TODConfig getConfig()
	{
		return itsConfig;
	}
	
	public IMutableStructureDatabase getDatabase()
	{
		return itsDatabase;
	}
	
	public byte[] generate()
	{
		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL) System.out.println("Generating replayer for: "+itsClassNode.name+"."+itsMethodNode.name);
		
		itsOriginalTryCatchNodes = itsMethodNode.tryCatchBlocks;
		itsMethodNode.tryCatchBlocks = new ArrayList();
		
		lCodeStart = new Label();
		LabelNode nStart = new LabelNode(lCodeStart);
		lCodeStart.info = nStart;
		itsMethodNode.instructions.insert(nStart);
		
		// If the original method is non-static, the generated method takes the original "this" as a parameter.
		if (! itsStatic) itsMethodNode.maxLocals++;
		itsMethodNode.maxLocals++; // For the ThreadReplayer arg.

		int theSlotsCount = itsMethodInfo.setupLocalCacheSlots(itsMethodNode.maxLocals);
		itsMethodNode.maxLocals += theSlotsCount;
		
		int theSaveArgsSpace = computeMaxSaveArgsSpace(itsMethodNode.instructions);
		itsSaveArgsSlots = nextFreeVar(theSaveArgsSpace);

		allocVars();
		itsMethodNode.maxStack = itsMethodNode.maxLocals+4;
		
		itsTmpTargetVars = new int[itsMethodInfo.getMaxNewInvokeNesting()+1];
//		for (int i=0;i<itsTmpTargetVars.length;i++) itsTmpTargetVars[i] = nextFreeVar(1);
		
		// Add OOS invoke method
		addOutOfScopeInvoke();
		addPartialReplayInvoke();
		
		// Modify method
		processInstructions(itsMethodNode.instructions);

		lCodeEnd = new Label();
		LabelNode nEnd = new LabelNode(lCodeEnd);
		lCodeEnd.info = nEnd;
		itsMethodNode.instructions.add(nEnd);
		
		// Setup/cleanup/handlers
		addSnapshotSetup(itsMethodNode.instructions);
		addExceptionHandling(itsMethodNode.instructions);
		if (DebugFlags.USE_FIELD_CACHE) itsMethodNode.instructions.insert(itsMethodInfo.getFieldCacheInitInstructions());
		itsMethodNode.instructions.add(itsAdditionalInstructions);

		// Setup infrastructure
		MethodSignature theSignature = getReplayMethodSignature(itsBehavior);
		itsMethodNode.name = theSignature.name;
		itsMethodNode.desc = theSignature.descriptor;
		itsMethodNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
		itsMethodNode.exceptions = Collections.EMPTY_LIST;
		
		// Update debug info (local vars are shifted by 1)
		for(Iterator<LocalVariableNode> theIterator = itsMethodNode.localVariables.iterator();theIterator.hasNext();)
		{
			LocalVariableNode theNode = theIterator.next();
			theNode.index = transformSlot(theNode.index);
			if ("this".equals(theNode.name)) theNode.name = "$this$";
		}
		itsMethodNode.localVariables.add(new LocalVariableNode("this", "L"+itsClassNode.name+";", null, nStart, nEnd, 0));
		
		// Output the modified class
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		itsTarget.accept(theWriter);
		
		byte[] theBytecode = theWriter.toByteArray();

		BCIUtils.writeClass("/home/gpothier/tmp/tod/replayer/"+getClassDumpSubpath(), itsTarget, theBytecode);

		// Check the methods
		try
		{
			BCIUtils.checkClass(theBytecode);
			for(MethodNode theNode : (List<MethodNode>) itsTarget.methods) 
				BCIUtils.checkMethod(itsTarget, theNode, new ReplayerVerifier(), false);
		}
		catch(Exception e)
		{
			System.err.println("Class "+itsTarget.name+" failed check.");
			e.printStackTrace();
		}
		
		return theBytecode;
	}
	
	/**
	 * Returns the subpath to use when storing classes generated by this generator.
	 */
	protected abstract String getClassDumpSubpath();
	
	protected void allocVars()
	{
		itsTmpVar = nextFreeVar(2);
		itsTmpTargetVar = nextFreeVar(1);
		itsTmpValueVar = nextFreeVar(2);
	}
	
	protected Label getCodeStartLabel()
	{
		return lCodeStart;
	}
	
	/**
	 * Returns the type corresponding to the given sort.
	 * If the sort corresponds to object or array, returns {@link ObjectId}
	 */
	private Type getTypeOrId(int aSort)
	{
		return BCIUtils.getType(aSort, TYPE_OBJECTID);
	}
	
	/**
	 * Adds a method that reads arguments from the stream before calling the actual invoke method
	 */
	private void addOutOfScopeInvoke()
	{
		MethodNode theMethod = new MethodNode();

		theMethod.name = "invoke_OOS";
		theMethod.desc = "("+DSC_THREADREPLAYER+")V";
		theMethod.exceptions = Collections.EMPTY_LIST;
		theMethod.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
		theMethod.tryCatchBlocks = Collections.EMPTY_LIST;
		
		int theSize = 1;
		
		SList s = new SList();
		
		boolean theSendThis = !itsStatic && !itsConstructor;
		
		int theArgCount = itsArgTypes.length;
		if (theSendThis) theArgCount++;

		if (theArgCount > 0)
		{
			s.ALOAD(0);
			s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "waitArgs", "("+DSC_THREADREPLAYER+")V");
			
			if (! itsStatic)
			{
				if (! itsConstructor) s.invokeReadRef(0);
				else 
				{
					s.ALOAD(0);
					s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "nextTmpId", "("+DSC_THREADREPLAYER+")"+BCIUtils.DSC_TMPOBJECTID);
				}
				theSize++;
			}
			
			for (Type theType : itsArgTypes)
			{
				s.invokeRead(theType, 0);
				theSize += theType.getSize();
			}
		}
		else if (!itsStatic && itsConstructor)
		{
			s.ALOAD(0);
			s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "nextTmpId", "("+DSC_THREADREPLAYER+")"+BCIUtils.DSC_TMPOBJECTID);
			theSize++;
		}
		
		MethodSignature theSignature = getReplayMethodSignature(itsBehavior);
		s.ALOAD(0);
		s.INVOKESTATIC(itsTarget.name, theSignature.name, theSignature.descriptor);
		if (itsReturnType.getSort() != Type.VOID) s.POP(itsReturnType);
		s.RETURN();

		theMethod.maxLocals = 1;
		theMethod.maxStack = theSize;
		
		theMethod.instructions = s;
		itsTarget.methods.add(theMethod);
	}
	
	/**
	 * Similar to {@link #addOutOfScopeInvoke()}, but passes dummy arguments,
	 * as actual values will be obtained from a snapshot
	 */
	private void addPartialReplayInvoke()
	{
		MethodNode theMethod = new MethodNode();
		
		theMethod.name = "invoke_PartialReplay";
		theMethod.desc = "("+DSC_THREADREPLAYER+")V";
		theMethod.exceptions = Collections.EMPTY_LIST;
		theMethod.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
		theMethod.tryCatchBlocks = Collections.EMPTY_LIST;
		
		int theSize = 1;
		
		SList s = new SList();
				
		if (! itsStatic)
		{
			s.ACONST_NULL();
			theSize++;
		}
		
		for (Type theType : itsArgTypes)
		{
			s.pushDefaultValue(theType);
			theSize += theType.getSize();
		}
		
		MethodSignature theSignature = getReplayMethodSignature(itsBehavior);
		s.ALOAD(0);
		s.INVOKESTATIC(itsTarget.name, theSignature.name, theSignature.descriptor);
		if (itsReturnType.getSort() != Type.VOID) s.POP(itsReturnType);
		s.RETURN();
		
		theMethod.maxLocals = 1;
		theMethod.maxStack = theSize;
		
		theMethod.instructions = s;
		itsTarget.methods.add(theMethod);
	}
	
	/**
	 * Returns a key that represents the start and end label of the try-catch block
	 */
	private static String getTryCatchBlockKey(InsnList aInstructions, TryCatchBlockNode aNode)
	{
		return ""+aInstructions.indexOf(aNode.start)+"_"+aInstructions.indexOf(aNode.end);
	}
	
	/**
	 * Replaces the exception types for every handler 
	 * (uses {@link HandlerReachedException}), and fixes the handler code as needed.
	 */
	private void addExceptionHandling(InsnList aInsns)
	{
		final Map<Label, Integer> theHandlerIds = new HashMap<Label, Integer>();
		SetMap<String, TryCatchBlockNode> theRegionData = new SetMap<String, TryCatchBlockNode>();
		
		// Assign ids to handlers and register the handlers for each region
		int theNextId = 0;
		int nHandlers = itsOriginalTryCatchNodes.size();
		for(int i=0;i<nHandlers;i++)
		{
			TryCatchBlockNode theNode = (TryCatchBlockNode) itsOriginalTryCatchNodes.get(i);
			Label theLabel = theNode.handler.getLabel();
			
			Integer theId = theHandlerIds.get(theLabel);
			if (theId == null)
			{
				theId = theNextId++;
				theHandlerIds.put(theLabel, theId);
			}
			
			String theKey = getTryCatchBlockKey(itsMethodNode.instructions, theNode);
			theRegionData.add(theKey, theNode);
		}

		SList s = new SList();

		Label lDefault = new Label();
		s.label(lDefault);
		s.createRTEx("Invalid handler id");
		s.ATHROW();
		
		// For each region we keep a single handler and create a dispatching switch statement
		// We iterate again on the original handlers, as we need to maintain the ordering
		for(int i=0;i<nHandlers;i++)
		{
			TryCatchBlockNode theOriginalNode = (TryCatchBlockNode) itsOriginalTryCatchNodes.get(i);
			String theKey = getTryCatchBlockKey(itsMethodNode.instructions, theOriginalNode);
		
			Set<TryCatchBlockNode> theSet = theRegionData.get(theKey);
			TryCatchBlockNode theFirstNode = theSet.iterator().next();
			
			Label lDispatcher = new Label();
			s.label(lDispatcher);

			s.DUP();
			s.GETFIELD(CLS_HANDLERREACHED, "exception", BCIUtils.DSC_OBJECTID);
			s.SWAP();
			s.GETFIELD(CLS_HANDLERREACHED, "handlerId", "I");
			
			int n = theSet.size();
			TryCatchBlockNode[] theNodes = theSet.toArray(new TryCatchBlockNode[n]);
			Arrays.sort(theNodes, new Comparator<TryCatchBlockNode>()
			{
				private int getValue(TryCatchBlockNode aNode)
				{
					Label theLabel = aNode.handler.getLabel();
					return theHandlerIds.get(theLabel); 
				}
				
				public int compare(TryCatchBlockNode n1, TryCatchBlockNode n2)
				{
					return getValue(n1) - getValue(n2);
				}
			});
			
			int[] theValues = new int[n];
			Label[] theLabels = new Label[n];
			
			int j=0;
			for(TryCatchBlockNode theNode : theNodes)
			{
				Label theLabel = theNode.handler.getLabel();
				theLabels[j] = theLabel;
				theValues[j] = theHandlerIds.get(theLabel); 
				j++;
			}
			
			s.LOOKUPSWITCH(lDefault, theValues, theLabels);
			
			itsMethodNode.visitTryCatchBlock(theFirstNode.start.getLabel(), theFirstNode.end.getLabel(), lDispatcher, CLS_HANDLERREACHED);
		}
		
//		s.label(lExitException);
//		s.POP();
//		s.ALOAD(itsThreadReplayerSlot);
//		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectException", "("+DSC_THREADREPLAYER+")V");
//		s.pushDefaultValue(itsReturnType);
//		s.RETURN(itsReturnType);
//
//		itsMethodNode.visitTryCatchBlock(lCodeStart, lCodeEnd, lExitException, CLS_BEHAVIOREXITEXCEPTION);

		aInsns.add(s);
	}
	
	protected void addAdditionalInstructions(InsnList aInsns)
	{
		itsAdditionalInstructions.add(aInsns);
	}
	
	private int getFieldCacheSlot(FieldInsnNode aNode)
	{
		Integer theSlot = itsFieldCacheMap.get(getFieldKey(aNode));
		return theSlot != null ? theSlot.intValue() : -1;
	}
	
	private String getFieldKey(IFieldInfo aField)
	{
		return aField.getDeclaringType().getName()+"_"+aField.getName();
	}
	
	private String getFieldKey(FieldInsnNode aNode)
	{
		return aNode.owner+"_"+aNode.name;
	}

	protected int nextFreeVar(int aSize)
	{
		int theVar = itsMethodNode.maxLocals;
		itsMethodNode.maxLocals += aSize;
		return theVar;
	}
	
	/**
	 * Generates the bytecode that pushes the current collector on the stack.
	 */
	private void pushCollector(SList s)
	{
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKEVIRTUAL(CLS_THREADREPLAYER, "getCollector", "()"+DSC_EVENTCOLLECTOR_REPLAY);
	}

	/**
	 * Invokes one of the value() methods of {@link EventCollector}. 
	 * Assumes that the collector and the value are on the stack
	 * @param s
	 */
	private static void invokeValue(SList s, Type aType)
	{
		s.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR_REPLAY, "value", "("+getActualReplayType(aType).getDescriptor()+")V");

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
			case Opcodes.ATHROW:
				processThrow(aInsns, (InsnNode) theNode);
				break;
				
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				processInvoke(aInsns, (MethodInsnNode) theNode);
				break;

			case Opcodes.NEWARRAY:
			case Opcodes.ANEWARRAY:
				processNewArray(aInsns, theNode, 1);
				break;
				
			case Opcodes.MULTIANEWARRAY:
				processNewArray(aInsns, theNode, ((MultiANewArrayInsnNode) theNode).dims);
				break;
				
			case Opcodes.NEW:
				processNew(aInsns, (TypeInsnNode) theNode);
				break;
				
			case Opcodes.GETFIELD:
			case Opcodes.GETSTATIC:
				processGetField(aInsns, (FieldInsnNode) theNode);
				break;
				
			case Opcodes.PUTFIELD:
			case Opcodes.PUTSTATIC:
				processPutField(aInsns, (FieldInsnNode) theNode);
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
				
			case Opcodes.ARRAYLENGTH:
				processArrayLength(aInsns, (InsnNode) theNode);
				break;
				
			case Opcodes.IASTORE:
			case Opcodes.LASTORE:
			case Opcodes.FASTORE:
			case Opcodes.DASTORE:
			case Opcodes.AASTORE:
			case Opcodes.BASTORE:
			case Opcodes.CASTORE:
			case Opcodes.SASTORE:
				processPutArray(aInsns, (InsnNode) theNode);
				break;
				
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:
				processGetVar(aInsns, (VarInsnNode) theNode);
				break;
				
			case Opcodes.ISTORE:
			case Opcodes.LSTORE:
			case Opcodes.FSTORE:
			case Opcodes.DSTORE:
			case Opcodes.ASTORE:
				processPutVar(aInsns, (VarInsnNode) theNode);
				break;
				
			case Opcodes.IINC:
				processIinc(aInsns, (IincInsnNode) theNode);
				break;
				
			case Opcodes.LDC:
				processLdc(aInsns, (LdcInsnNode) theNode);
				break;
				
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
				processIfAcmp(aInsns, (JumpInsnNode) theNode);
				break;
				
			case Opcodes.IDIV:
			case Opcodes.LDIV:
			case Opcodes.IREM:
			case Opcodes.LREM:
				processDiv(aInsns, (InsnNode) theNode);
				break;
				
			case Opcodes.MONITORENTER:
			case Opcodes.MONITOREXIT:
				processMonitor(aInsns, (InsnNode) theNode);
				break;
				
			case Opcodes.INSTANCEOF:
				processInstanceOf(aInsns, (TypeInsnNode) theNode);
				break;
				
			case Opcodes.CHECKCAST:
				processCheckCast(aInsns, (TypeInsnNode) theNode);
				break;
				
			case Opcodes.RET:
				processRet(aInsns, (VarInsnNode) theNode);
				break;
			}
		}
	}

	private void processThrow(InsnList aInsns, InsnNode aNode)
	{
		SList s = new SList();

		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectException", "("+DSC_THREADREPLAYER+")V");
		s.pushDefaultValue(itsReturnType);
		s.RETURN(itsReturnType);

		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	/**
	 * Returns the behavior invoked by the given node.
	 */
	private IBehaviorInfo getTargetBehavior(MethodInsnNode aNode)
	{
		IClassInfo theClass = getDatabase().getClass(Util.jvmToScreen(aNode.owner), false);
		if (theClass == null) return null;
		IBehaviorInfo theBehavior = theClass.getBehavior(aNode.name, aNode.desc);
		return theBehavior;
	}
	
	protected int getThreadReplayerSlot()
	{
		return itsThreadReplayerSlot;
	}
	
	private boolean isCalleeInScope(MethodInsnNode aNode)
	{
		return getDatabase().isInScope(aNode.owner);
	}

	private void processInvoke(InsnList aInsns, MethodInsnNode aNode)
	{
		boolean theStatic = aNode.getOpcode() == Opcodes.INVOKESTATIC;
		boolean theChainingInvocation = itsMethodInfo.isChainingInvocation(aNode);
		
		boolean theConstructor = "<init>".equals(aNode.name);
		
		boolean theExpectObjectInitialized = 
			theConstructor 
			&& ! theChainingInvocation
			&& ! getDatabase().isInScope(aNode.owner);
		
		SList s = new SList();
		
		// Add ThreadReplayer arg
		s.ALOAD(itsThreadReplayerSlot);

		MethodSignature theSignature = getDispatchMethodSignature(aNode.desc, theStatic, theConstructor);
		s.INVOKESTATIC(CLS_THREADREPLAYER, theSignature.name, theSignature.descriptor);
		insertSnapshotProbe(s, aNode, true);
		
		if (! isCalleeInScope(aNode))
		{
			if (theExpectObjectInitialized)
			{
				// Save target at the NEW site
				NewInvokeLink theNewInvokeLink = itsMethodInfo.getNewInvokeLink(aNode);
				SyntaxInsnList s2 = new SyntaxInsnList();
				int theLevel = theNewInvokeLink.getNestingLevel();
				int theVar = itsTmpTargetVars[theLevel];
				if (theVar == 0)
				{
					theVar = nextFreeVar(1);
					itsTmpTargetVars[theLevel] = theVar;
				}
				s2.DUP();
				s2.ASTORE(theVar);
				TypeInsnNode theNewInsn = theNewInvokeLink.getNewInsn();
				AbstractInsnNode theReplacementInsn = itsNewReplacementInsnsMap.get(theNewInsn);
				aInsns.insert(theReplacementInsn, s2);
	
				// Do wait
				s.ALOAD(itsThreadReplayerSlot);
				s.ALOAD(theVar);
				s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "waitObjectInitialized", "("+DSC_THREADREPLAYER+DSC_TMPOBJECTID+")V");
			}
			else if (theChainingInvocation)
			{
				s.ALOAD(itsThreadReplayerSlot);
				s.ALOAD(0); // Original "this" 
				s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "waitConstructorTarget", "("+DSC_THREADREPLAYER+DSC_OBJECTID+")V");
			}
		}
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	/**
	 * Returns the type of all the stack slots in the given frame.
	 */
	protected static Type[] getStackTypes(BCIFrame aFrame)
	{
		int theSize = aFrame.getStackSize();
		Type[] theResult = new Type[theSize];
		for(int i=0;i<theSize;i++) theResult[i] = getActualReplayType(aFrame.getStack(i).getType());
		return theResult;
	}
	
	/**
	 * Saves as many stack slots as there are items in the types array.
	 */
	protected void genSaveStack(SList s, Type[] aSlotTypes)
	{
		genSaveArgs(s, aSlotTypes, true);
	}
	
	private void genSaveArgs(SList s, Type[] aArgTypes, boolean aStatic)
	{
		int theSlot = itsSaveArgsSlots;
		for(int i=aArgTypes.length-1;i>=0;i--)
		{
			Type theType = aArgTypes[i];
			s.ISTORE(theType, theSlot);
			theSlot += theType.getSize();
		}
		if (! aStatic) s.ASTORE(theSlot);
	}
	
	/**
	 * Loads as many stack slots as there are items in the types array.
	 */
	protected void genLoadStack(SList s, Type[] aSlotTypes)
	{
		genLoadArgs(s, aSlotTypes, true);
	}
	
	private void genLoadArgs(SList s, Type[] aArgTypes, boolean aStatic)
	{
		int theSlot = itsSaveArgsSlots;
		for(int i=0;i<aArgTypes.length;i++) theSlot += aArgTypes[i].getSize();
		if (! aStatic) s.ALOAD(theSlot);
		for(int i=0;i<aArgTypes.length;i++) 
		{
			Type theType = aArgTypes[i];
			theSlot -= theType.getSize();
			s.ILOAD(theType, theSlot);
		}
	}
	
	protected void genReverseLoadStack(SList s, Type[] aArgTypes)
	{
		int theSlot = itsSaveArgsSlots;
		for(int i=aArgTypes.length-1;i>=0;i--)
		{
			Type theType = aArgTypes[i];
			s.ILOAD(theType, theSlot);
			theSlot += theType.getSize();
		}
	}
	
//	public static MethodSignature getInvokeMethodSignature(boolean aStatic, Type[] aArgTypes, Type aReturnType)
//	{
//		List<Type> theArgTypes = new ArrayList<Type>();
//		if (! aStatic) theArgTypes.add(TYPE_OBJECTID); // First arg is the target
//		for (Type theType : aArgTypes) theArgTypes.add(getActualReplayType(theType));
//		
//		return new MethodSignature(
//				"invoke"+SUFFIX_FOR_SORT[aReturnType.getSort()]+(aStatic ? "_S" : ""),
//				Type.getMethodDescriptor(
//						getActualReplayType(aReturnType), 
//						theArgTypes.toArray(new Type[theArgTypes.size()])));
//	}

	public static MethodSignature getDispatchMethodSignature(String aDescriptor, boolean aStatic, boolean aConstructor)
	{
		Type[] theArgumentTypes = Type.getArgumentTypes(aDescriptor);
		Type theReturnType = Type.getReturnType(aDescriptor);
		
		List<Type> theArgTypes = new ArrayList<Type>();
		if (! aStatic) theArgTypes.add(TYPE_OBJECTID); // First arg is the target
		for (Type theType : theArgumentTypes) theArgTypes.add(getReplayDispatchType(theType));
		theArgTypes.add(TYPE_THREADREPLAYER); 
		
		return new MethodSignature(
				"dispatch_"+getCompleteSigForType(theReturnType)+(aStatic ? "_S" : "")+(aConstructor ? "_c" : ""),
				Type.getMethodDescriptor(
						getReplayDispatchType(theReturnType), 
						theArgTypes.toArray(new Type[theArgTypes.size()])));
	}
	
	public static MethodSignature getOOSDispatchMethodSignature(IBehaviorInfo aBehavior)
	{
		String theDescriptor = aBehavior.getDescriptor();
		Type[] theArgTypes = Type.getArgumentTypes(theDescriptor);
		Type theReturnType = Type.getReturnType(theDescriptor);
		
		char[] sig = new char[theArgTypes.length];
		for(int i=0;i<sig.length;i++) sig[i] = getCompleteSigForType(theArgTypes[i]);
		
		return new MethodSignature(
				"dispatch_"+new String(sig)+"_"+getCompleteSigForType(theReturnType)
					+(aBehavior.isStatic() ? "_S" : "")+(aBehavior.isConstructor() ? "_c" : "")
					+"_OOS",
				"(I"+DSC_THREADREPLAYER+")V");
	}
	
	public static MethodSignature getReplayMethodSignature(IBehaviorInfo aBehavior)
	{
		String theDescriptor = aBehavior.getDescriptor();
		Type[] theArgumentTypes = Type.getArgumentTypes(theDescriptor);
		Type theReturnType = Type.getReturnType(theDescriptor);
		
		List<Type> theArgTypes = new ArrayList<Type>();
		if (! aBehavior.isStatic()) theArgTypes.add(TYPE_OBJECTID); 
		for (Type theType : theArgumentTypes) theArgTypes.add(getReplayDispatchType(theType));
		theArgTypes.add(TYPE_THREADREPLAYER);
		
		return new MethodSignature(
				"replay",
				Type.getMethodDescriptor(
						getReplayDispatchType(theReturnType), 
						theArgTypes.toArray(new Type[theArgTypes.size()])));
	}
	
	
	private static final String[] SUFFIX_FOR_SORT = new String[11];
	private boolean itsStatic;
	private boolean itsConstructor;
	
	static
	{
		SUFFIX_FOR_SORT[Type.OBJECT] = "Ref";
		SUFFIX_FOR_SORT[Type.ARRAY] = "Ref";
		SUFFIX_FOR_SORT[Type.BOOLEAN] = "Boolean";
		SUFFIX_FOR_SORT[Type.BYTE] = "Byte";
		SUFFIX_FOR_SORT[Type.CHAR] = "Char";
		SUFFIX_FOR_SORT[Type.DOUBLE] = "Double";
		SUFFIX_FOR_SORT[Type.FLOAT] = "Float";
		SUFFIX_FOR_SORT[Type.INT] = "Int";
		SUFFIX_FOR_SORT[Type.LONG] = "Long";
		SUFFIX_FOR_SORT[Type.SHORT] = "Short";
		SUFFIX_FOR_SORT[Type.VOID] = "Void";
	}
	
	private void processNew(InsnList aInsns, TypeInsnNode aNode)
	{
		SList s = new SList();
		
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "nextTmpId_skipClassloading", "("+DSC_THREADREPLAYER+")"+BCIUtils.DSC_TMPOBJECTID);
		
		itsNewReplacementInsnsMap.put(aNode, s.getLast());
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processNewArray(InsnList aInsns, AbstractInsnNode aNode, int aDimensions)
	{
		SList s = new SList();
		
		for (int i=0;i<aDimensions;i++) s.POP(); // Pop array size(s)
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectNewArray", "("+DSC_THREADREPLAYER+")"+BCIUtils.DSC_OBJECTID);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	/**
	 * LDC of class constant can throw an exception/cause class loading 
	 */
	private void processLdc(InsnList aInsns, LdcInsnNode aNode)
	{
		if (! (aNode.cst instanceof Type) && ! (aNode.cst instanceof String)) return;
		
		SList s = new SList();
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectConstant", "("+DSC_THREADREPLAYER+")"+DSC_OBJECTID);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processInstanceOf(InsnList aInsns, TypeInsnNode aNode)
	{
		SList s = new SList();
		s.POP();
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectInstanceofOutcome", "("+DSC_THREADREPLAYER+")I");
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	
	/**
	 * References are transformed into {@link ObjectId} so we must compare ids.
	 */
	private void processIfAcmp(InsnList aInsns, JumpInsnNode aNode)
	{
		SList s = new SList();

		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "cmpId", "("+BCIUtils.DSC_OBJECTID+BCIUtils.DSC_OBJECTID+")Z");
		switch(aNode.getOpcode())
		{
		case Opcodes.IF_ACMPEQ: s.IFtrue(aNode.label.getLabel()); break;
		case Opcodes.IF_ACMPNE: s.IFfalse(aNode.label.getLabel()); break;
		default:
			throw new RuntimeException("Not handled: "+aNode);
		}
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processGetField(InsnList aInsns, FieldInsnNode aNode)
	{
		Type theType = getTypeOrId(Type.getType(aNode.desc).getSort());

		SList s = new SList();

		if (aNode.getOpcode() == Opcodes.GETSTATIC) s.ACONST_NULL(); // Push "null" target
		s.ASTORE(itsTmpTargetVar); // Store target
		
		String theExpectMethodName = "expectAndSend"+SUFFIX_FOR_SORT[theType.getSort()]+"FieldRead";
		
		s.ALOAD(itsThreadReplayerSlot);
		s.ALOAD(itsTmpTargetVar);
		s.pushInt(StructureDatabaseUtils.getFieldId(itsDatabase, aNode.owner, aNode.name, false));
		
		if (DebugFlags.USE_FIELD_CACHE)
		{
			Integer theCacheSlot = itsMethodInfo.getCacheSlot(aNode);
			s.ILOAD(theCacheSlot);
			String theExpectMethodDesc = "("+DSC_THREADREPLAYER+DSC_OBJECTID+"I"+theType.getDescriptor()+")"+theType.getDescriptor();
			s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, theExpectMethodName, theExpectMethodDesc);
		}
		else
		{
			String theExpectMethodDesc = "("+DSC_THREADREPLAYER+DSC_OBJECTID+"I)"+theType.getDescriptor();
			s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, theExpectMethodName, theExpectMethodDesc);
		}
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processPutField(InsnList aInsns, FieldInsnNode aNode)
	{
		Type theType = getTypeOrId(Type.getType(aNode.desc).getSort());
		
		SList s = new SList();
		s.ISTORE(theType, itsTmpValueVar);
		if (aNode.getOpcode() == Opcodes.PUTSTATIC) s.ACONST_NULL(); // Push "null" target
		s.ASTORE(itsTmpTargetVar); // Store target
		
		// Register event
		pushCollector(s);
		s.DUP();
		
		s.ALOAD(itsTmpTargetVar);
		s.LDC(StructureDatabaseUtils.getFieldId(itsDatabase, aNode.owner, aNode.name, true));
		s.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR_REPLAY, "fieldWrite", "("+DSC_OBJECTID+"I)V");
		
		s.ILOAD(theType, itsTmpValueVar);
		invokeValue(s, theType);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processPutArray(InsnList aInsns, InsnNode aNode)
	{
		Type theElementType = getTypeOrId(BCIUtils.getSort(aNode.getOpcode()));
		
		SList s = new SList();
		s.POP(theElementType); // Pop value
		s.POP(); // Pop index
		s.POP(); // Pop array
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	


	private void processGetArray(InsnList aInsns, InsnNode aNode)
	{
		Type theType = getTypeOrId(BCIUtils.getSort(aNode.getOpcode()));
		
		SList s = new SList();

		s.POP(); // Pop index
		s.POP(); // Pop target
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectArrayRead", "("+DSC_THREADREPLAYER+")V");
		s.invokeRead(theType, itsThreadReplayerSlot);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processArrayLength(InsnList aInsns, InsnNode aNode)
	{
		SList s = new SList();

		s.POP(); // Pop target
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectArrayLength", "("+DSC_THREADREPLAYER+")I");
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	protected int transformSlot(int aSlot)
	{
		return aSlot < itsThreadReplayerSlot ? aSlot : aSlot+1;
	}
	
	private void processGetVar(InsnList aInsns, VarInsnNode aNode)
	{
//		Type theType = getTypeOrId(BCIUtils.getSort(aNode.getOpcode()));
//		SList s = new SList();
//		s.ILOAD(theType, aNode.var+1);
//		
//		aInsns.insert(aNode, s);
//		aInsns.remove(aNode);
		aNode.var = transformSlot(aNode.var);
	}

	private void processPutVar(InsnList aInsns, VarInsnNode aNode)
	{
//		Type theType = getTypeOrId(BCIUtils.getSort(aNode.getOpcode()));
//		SList s = new SList();
//		s.ISTORE(theType, aNode.var+1);
//		
//		aInsns.insert(aNode, s);
//		aInsns.remove(aNode);
		aNode.var = transformSlot(aNode.var);
	}
	
	private void processIinc(InsnList aInsns, IincInsnNode aNode)
	{
//		SList s = new SList();
//		s.IINC(aNode.var+1, aNode.incr);
//		
//		aInsns.insert(aNode, s);
//		aInsns.remove(aNode);
		aNode.var = transformSlot(aNode.var);
	}

	/**
	 * We need to check if an exception is going to be thrown.
	 */
	private void processDiv(InsnList aInsns, InsnNode aNode)
	{
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()), null);

		SList s = new SList();
		Label lNormal = new Label();
		s.DUP(theType);
		switch(theType.getSort()) 
		{
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.SHORT:
		case Type.INT: break;
		
		case Type.LONG:
			s.pushLong(0);
			s.LCMP();
			break;
		
		default: throw new RuntimeException("Unexpected type: "+theType);
		}
		
		s.IFNE(lNormal);
		
		{
			// An arithmetic exception will occur
			s.POP(theType);
			s.POP(theType);
			
			s.ALOAD(itsThreadReplayerSlot);
			s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "expectException", "("+DSC_THREADREPLAYER+")V");
			s.pushDefaultValue(itsReturnType);
			s.RETURN(itsReturnType);
		}
		
		s.label(lNormal);
		aInsns.insertBefore(aNode, s);
	}
	
	private void processCheckCast(InsnList aInsns, TypeInsnNode aNode)
	{
		BCIFrame theFrame = itsMethodInfo.getFrame(aNode);
		
		SList s = new SList();
		s.ALOAD(itsThreadReplayerSlot);
		s.INVOKESTATIC(CLS_INSCOPEREPLAYERFRAME, "checkCast", "("+DSC_THREADREPLAYER+")V");

		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	
	private void processMonitor(InsnList aInsns, InsnNode aNode)
	{
		SList s = new SList();
		s.POP();
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processRet(InsnList aInsns, VarInsnNode aNode)
	{
		aNode.var = transformSlot(aNode.var);
	}
	
	protected abstract void addSnapshotSetup(InsnList aInsns);
	protected abstract void insertSnapshotProbe(SList s, AbstractInsnNode aReferenceNode, boolean aSaveStack);
	
	public static class MethodSignature
	{
		public final String name;
		public final String descriptor;
		
		public MethodSignature(String aName, String aDescriptor)
		{
			name = aName;
			descriptor = aDescriptor;
		}
	}
}
