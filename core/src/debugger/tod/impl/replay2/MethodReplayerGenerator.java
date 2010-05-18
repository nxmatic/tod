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
import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.bci.asm2.MethodInfo;
import tod.impl.bci.asm2.SyntaxInsnList;
import tod.impl.bci.asm2.MethodInfo.BCIFrame;
import tod.impl.bci.asm2.MethodInfo.NewInvokeLink;
import tod.impl.database.structure.standard.StructureDatabaseUtils;
import tod2.agent.Message;
import zz.utils.SetMap;

public class MethodReplayerGenerator
{
	public static final Type TYPE_OBJECTID = Type.getType(ObjectId.class);
	public static final String CLS_REPLAYERFRAME = BCIUtils.getJvmClassName(ReplayerFrame.class);
	public static final String DSC_REPLAYERFRAME = "L"+CLS_REPLAYERFRAME+";";
	public static final String CLS_EVENTCOLLECTOR = BCIUtils.getJvmClassName(EventCollector.class);
	public static final String DSC_EVENTCOLLECTOR = "L"+CLS_EVENTCOLLECTOR+";";
	public static final String CLS_INSCOPEREPLAYERFRAME = BCIUtils.getJvmClassName(InScopeReplayerFrame.class);
	public static final String CLS_HANDLERREACHED = BCIUtils.getJvmClassName(HandlerReachedException.class);
	public static final String CLS_BEHAVIOREXITEXCEPTION = BCIUtils.getJvmClassName(BehaviorExitException.class);
	public static final String CLS_UNMONITOREDCALLEXCEPTION = BCIUtils.getJvmClassName(UnmonitoredBehaviorCallException.class);

	private final TODConfig itsConfig;
	private final IStructureDatabase itsDatabase;
	private final ReplayerGenerator itsGenerator;
	private final ClassNode itsTarget;
	
	private final ClassNode itsClassNode;
	private final MethodNode itsMethodNode;
	
	private final MethodInfo itsMethodInfo;
	
	private Label lCodeStart;
	private Label lCodeEnd;
	private Label lExitException = new Label();
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
	 * For each original NEW instruction, maps to the last instruction of the block that replaces it.
	 */
	private Map<TypeInsnNode, AbstractInsnNode> itsNewReplacementInsnsMap = new HashMap<TypeInsnNode, AbstractInsnNode>();
	

	
	private int itsSaveArgsSlots;
	
	/**
	 * Maps field keys (see {@link #getFieldKey(FieldInsnNode)}) to the corresponding cache slot. 
	 */
	private Map<String, Integer> itsFieldCacheMap = new HashMap<String, Integer>();
	
	public MethodReplayerGenerator(
			TODConfig aConfig, 
			IStructureDatabase aDatabase,
			ReplayerGenerator aGenerator,
			ClassNode aClassNode, 
			MethodNode aMethodNode)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsGenerator = aGenerator;
		itsClassNode = aClassNode;
		itsMethodNode = aMethodNode;
		
		itsArgTypes = Type.getArgumentTypes(itsMethodNode.desc);
		itsReturnType = Type.getReturnType(itsMethodNode.desc);
		itsStatic = BCIUtils.isStatic(itsMethodNode.access);
		itsConstructor = "<init>".equals(itsMethodNode.name);

		itsTarget = new ClassNode();
		itsTarget.name = ReplayerGenerator.makeReplayerClassName(itsClassNode.name, itsMethodNode.name, itsMethodNode.desc);
		itsTarget.sourceFile = itsTarget.name+".class";
		itsTarget.superName = CLS_INSCOPEREPLAYERFRAME;
		itsTarget.methods.add(itsMethodNode);
		itsTarget.version = Opcodes.V1_5;
		itsTarget.access = Opcodes.ACC_PUBLIC;
		
		itsMethodInfo = new MethodInfo(itsDatabase, itsClassNode, itsMethodNode);
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
	
	public IStructureDatabase getDatabase()
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
		// If original is static, the generated method takes no parameter, but is no longer static
		itsMethodNode.maxLocals++; 

		int theSlotsCount = itsMethodInfo.setupLocalCacheSlots(itsMethodNode.maxLocals);
		itsMethodNode.maxLocals += theSlotsCount;
		
		itsSaveArgsSlots = nextFreeVar(computeMaxSaveArgsSpace(itsMethodNode.instructions));

		itsTmpVar = nextFreeVar(2);
		itsTmpTargetVar = nextFreeVar(1);
		itsTmpValueVar = nextFreeVar(2);
		
		itsTmpTargetVars = new int[itsMethodInfo.getMaxNewInvokeNesting()+1];
//		for (int i=0;i<itsTmpTargetVars.length;i++) itsTmpTargetVars[i] = nextFreeVar(1);
		
		// Create constructor
		addConstructor();
		
		// Add OOS invoke method
		addOutOfScopeInvoke();
		
		// Modify method
		processInstructions(itsMethodNode.instructions);

		lCodeEnd = new Label();
		LabelNode nEnd = new LabelNode(lCodeEnd);
		lCodeEnd.info = nEnd;
		itsMethodNode.instructions.add(nEnd);
		
		// Setup/cleanup/handlers
		addExceptionHandling();
		itsMethodNode.instructions.insert(itsMethodInfo.getFieldCacheInitInstructions());

		// Setup infrastructure
		String[] theSignature = getInvokeMethodSignature(itsStatic, itsArgTypes, itsReturnType);
		itsMethodNode.name = theSignature[0];
		itsMethodNode.desc = theSignature[1];
		itsMethodNode.access = Opcodes.ACC_PROTECTED;
		itsMethodNode.exceptions = Collections.EMPTY_LIST;
		
		itsMethodNode.maxStack += 8;
		
		// Update debug info (local vars are shift by 1)
		for(Iterator<LocalVariableNode> theIterator = itsMethodNode.localVariables.iterator();theIterator.hasNext();)
		{
			LocalVariableNode theNode = theIterator.next();
			theNode.index++;
			if ("this".equals(theNode.name)) theNode.name = "$ref$";
		}
		itsMethodNode.localVariables.add(new LocalVariableNode("this", "L"+itsClassNode.name+";", null, nStart, nEnd, 0));
		
		// Output the modified class
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		itsTarget.accept(theWriter);
		
		byte[] theBytecode = theWriter.toByteArray();

		BCIUtils.writeClass("/home/gpothier/tmp/tod/replayer", itsTarget, theBytecode);

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
	 * Returns the type corresponding to the given sort.
	 * If the sort corresponds to object or array, returns {@link ObjectId}
	 * @param aSort
	 * @return
	 */
	private Type getTypeOrId(int aSort)
	{
		switch(aSort)
		{
		case Type.OBJECT:
		case Type.ARRAY:
			return TYPE_OBJECTID;
		
		default:
			return BCIUtils.getType(aSort);
		}
	}
	
	private void addConstructor()
	{
		MethodNode theConstructor = new MethodNode();
		theConstructor.name = "<init>";
		theConstructor.desc = "()V";
		theConstructor.exceptions = Collections.EMPTY_LIST;
		theConstructor.access = Opcodes.ACC_PUBLIC;
		theConstructor.maxStack = 1;
		theConstructor.maxLocals = 1;
		theConstructor.tryCatchBlocks = Collections.EMPTY_LIST;
		
		SList s = new SList();
		s.ALOAD(0);
		s.INVOKESPECIAL(CLS_INSCOPEREPLAYERFRAME, "<init>", "()V");
		s.RETURN();
		
		theConstructor.instructions = s;
		itsTarget.methods.add(theConstructor);
	}
	
	/**
	 * Adds a method that reads arguments from the stream before calling the actual invoke method
	 */
	private void addOutOfScopeInvoke()
	{
		MethodNode theMethod = new MethodNode();

		String[] theSignature = getInvokeMethodSignature(itsStatic, itsArgTypes, itsReturnType);
		theMethod.name = "invoke_OOS";
		theMethod.desc = "()V";
		theMethod.exceptions = Collections.EMPTY_LIST;
		theMethod.access = Opcodes.ACC_PUBLIC;
		theMethod.tryCatchBlocks = Collections.EMPTY_LIST;
		
		int theSize = 1;
		
		SList s = new SList();
		
		boolean theSendThis = !itsStatic && !itsConstructor;
		
		int theArgCount = itsArgTypes.length;
		if (theSendThis) theArgCount++;

		s.ALOAD(0);
		
		if (theArgCount > 0)
		{
			s.ALOAD(0);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "waitArgs", "()V");
			
			
			if (! itsStatic)
			{
				if (! itsConstructor) s.invokeReadRef();
				else 
				{
					s.ALOAD(0);
					s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "nextTmpId", "()"+BCIUtils.DSC_TMPOBJECTID);
				}
				theSize++;
			}
			
			for (Type theType : itsArgTypes)
			{
				s.invokeRead(theType);
				theSize += theType.getSize();
			}
		}
		else if (!itsStatic && itsConstructor)
		{
			s.ALOAD(0);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "nextTmpId", "()"+BCIUtils.DSC_TMPOBJECTID);
			theSize++;
		}
		
		s.INVOKEVIRTUAL(itsTarget.name, theSignature[0], theSignature[1]);
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
	private void addExceptionHandling()
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
		
		s.label(lExitException);
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectException", "()V");
		s.pushDefaultValue(itsReturnType);
		s.RETURN(itsReturnType);

		itsMethodNode.visitTryCatchBlock(lCodeStart, lCodeEnd, lExitException, CLS_BEHAVIOREXITEXCEPTION);

		itsMethodNode.instructions.add(s);
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

	private int nextFreeVar(int aSize)
	{
		int theVar = itsMethodNode.maxLocals;
		itsMethodNode.maxLocals += aSize;
		return theVar;
	}
	
	/**
	 * Generates the bytecode that pushes the current collector on the stack.
	 */
	private static void pushCollector(SList s)
	{
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_REPLAYERFRAME, "getCollector", "()"+DSC_EVENTCOLLECTOR);
	}

	/**
	 * Invokes one of the value() methods of {@link EventCollector}. 
	 * Assumes that the collector and the value are on the stack
	 * @param s
	 */
	private static void invokeValue(SList s, Type aType)
	{
		s.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR, "value", "("+getActualType(aType).getDescriptor()+")V");

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
			case Opcodes.FDIV:
			case Opcodes.DDIV:
			case Opcodes.IREM:
			case Opcodes.LREM:
			case Opcodes.FREM:
			case Opcodes.DREM:
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
			}
		}
	}

	private void processReturn(InsnList aInsns, InsnNode aNode)
	{
	}
	
	private void processThrow(InsnList aInsns, InsnNode aNode)
	{
		SList s = new SList();

		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectException", "()V");
		s.pushDefaultValue(itsReturnType);
		s.RETURN(itsReturnType);

		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	/**
	 * Returns the id of the behavior invoked by the given node.
	 */
	private int getBehaviorId(MethodInsnNode aNode)
	{
		IClassInfo theClass = getDatabase().getClass(Util.jvmToScreen(aNode.owner), false);
		if (theClass == null) return -1;
		IBehaviorInfo theBehavior = theClass.getBehavior(aNode.name, aNode.desc);
		if (theBehavior == null) return -1;
		return theBehavior.getId();
	}
	
	private void processInvoke(InsnList aInsns, MethodInsnNode aNode)
	{
		Type[] theArgTypes = Type.getArgumentTypes(aNode.desc);
		Type theReturnType = getTypeOrId(Type.getReturnType(aNode.desc).getSort());
		int theBehaviorId = getBehaviorId(aNode);
		if (theBehaviorId == -1)
		{
			// The behavior was not found, meaning that the class was never loaded at runtime,
			// so we are creating the replayer for code that was never executed
			
			SList s = new SList();
			s.createRTEx("The code was never executed");
			s.ATHROW();
			aInsns.insert(aNode, s);
			aInsns.remove(aNode);
			return;
		}
		
		boolean theStatic = aNode.getOpcode() == Opcodes.INVOKESTATIC;
		int theArgCount = theArgTypes.length;
		if (! theStatic) theArgCount++;

		boolean theChainingInvocation = itsMethodInfo.isChainingInvocation(aNode);
		
		boolean theExpectObjectInitialized = 
			"<init>".equals(aNode.name) 
			&& ! theChainingInvocation
			&& ! getDatabase().isInScope(aNode.owner);
		
		SList s = new SList();
		
		{
			// Save arguments
			genSaveArgs(s, theArgTypes, theStatic);
			
			// Obtain frame
			s.ALOAD(0);
			s.pushInt(theBehaviorId);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "invoke", "(I)"+DSC_REPLAYERFRAME);
			
			// Reload arguments
			genLoadArgs(s, theArgTypes, theStatic);
			
			// Invoke (w/ exception handling)
			Label lHnStart = new Label();
			Label lHnEnd = new Label();
			Label lHnException = new Label();
			Label lHnAfter = new Label();

			String[] theSignature = getInvokeMethodSignature(theStatic, theArgTypes, theReturnType);
			s.label(lHnStart);
			s.INVOKEVIRTUAL(CLS_REPLAYERFRAME, theSignature[0], theSignature[1]);
			s.GOTO(lHnAfter);
			s.label(lHnEnd);
			
			s.label(lHnException);
			s.ALOAD(0);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectException", "()V");
			s.pushDefaultValue(itsReturnType);
			s.RETURN(itsReturnType);
			s.label(lHnAfter);

			itsMethodNode.visitTryCatchBlock(lHnStart, lHnEnd, lHnException, CLS_UNMONITOREDCALLEXCEPTION);
			itsMethodNode.visitTryCatchBlock(lHnStart, lHnEnd, lHnException, CLS_BEHAVIOREXITEXCEPTION);
		}
		
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
			s.ALOAD(0);
			s.ALOAD(theVar);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "waitObjectInitialized", "("+DSC_TMPOBJECTID+")V");
		}
		else if (theChainingInvocation)
		{
			s.ALOAD(0);
			s.ALOAD(1); // Original "this" (all original locals are pushed down one slot)
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "waitConstructorTarget", "("+DSC_OBJECTID+")V");
		}

		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
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
	
	public static String[] getInvokeMethodSignature(boolean aStatic, Type[] aArgTypes, Type aReturnType)
	{
		List<Type> theArgTypes = new ArrayList<Type>();
		if (! aStatic) theArgTypes.add(ACTUALTYPE_FOR_SORT[Type.OBJECT]); // First arg is the target
		for (Type theType : aArgTypes) theArgTypes.add(getActualType(theType));
		
		return new String[] {
				"invoke"+SUFFIX_FOR_SORT[aReturnType.getSort()]+(aStatic ? "_S" : ""),
				Type.getMethodDescriptor(
						getActualType(aReturnType), 
						theArgTypes.toArray(new Type[theArgTypes.size()]))
						
		};
	}
	
	/**
	 * Returns the actual type to use for the given type (all refs are folded into ObjectId)
	 */
	public static Type getActualType(Type aType)
	{
		return ACTUALTYPE_FOR_SORT[aType.getSort()];
	}
	
	public static final Type[] ACTUALTYPE_FOR_SORT = new Type[11];
	static
	{
		ACTUALTYPE_FOR_SORT[Type.OBJECT] = TYPE_OBJECTID;
		ACTUALTYPE_FOR_SORT[Type.ARRAY] = TYPE_OBJECTID;
		ACTUALTYPE_FOR_SORT[Type.BOOLEAN] = Type.INT_TYPE;
		ACTUALTYPE_FOR_SORT[Type.BYTE] = Type.INT_TYPE;
		ACTUALTYPE_FOR_SORT[Type.CHAR] = Type.INT_TYPE;
		ACTUALTYPE_FOR_SORT[Type.DOUBLE] = Type.DOUBLE_TYPE;
		ACTUALTYPE_FOR_SORT[Type.FLOAT] = Type.FLOAT_TYPE;
		ACTUALTYPE_FOR_SORT[Type.INT] = Type.INT_TYPE;
		ACTUALTYPE_FOR_SORT[Type.LONG] = Type.LONG_TYPE;
		ACTUALTYPE_FOR_SORT[Type.SHORT] = Type.INT_TYPE;
		ACTUALTYPE_FOR_SORT[Type.VOID] = Type.VOID_TYPE;
	}

	private static final String[] SUFFIX_FOR_SORT = new String[11];
	private boolean itsStatic;
	private boolean itsConstructor;
	
	static
	{
		SUFFIX_FOR_SORT[Type.OBJECT] = "Ref";
		SUFFIX_FOR_SORT[Type.ARRAY] = "Ref";
		SUFFIX_FOR_SORT[Type.BOOLEAN] = "Int";
		SUFFIX_FOR_SORT[Type.BYTE] = "Int";
		SUFFIX_FOR_SORT[Type.CHAR] = "Int";
		SUFFIX_FOR_SORT[Type.DOUBLE] = "Double";
		SUFFIX_FOR_SORT[Type.FLOAT] = "Float";
		SUFFIX_FOR_SORT[Type.INT] = "Int";
		SUFFIX_FOR_SORT[Type.LONG] = "Long";
		SUFFIX_FOR_SORT[Type.SHORT] = "Int";
		SUFFIX_FOR_SORT[Type.VOID] = "Void";
	}
	
	private void processNew(InsnList aInsns, TypeInsnNode aNode)
	{
		SList s = new SList();
		
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "nextTmpId_skipClassloading", "()"+BCIUtils.DSC_TMPOBJECTID);
		
		itsNewReplacementInsnsMap.put(aNode, s.getLast());
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processNewArray(InsnList aInsns, AbstractInsnNode aNode, int aDimensions)
	{
		SList s = new SList();
		
		for (int i=0;i<aDimensions;i++) s.POP(); // Pop array size(s)
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectNewArray", "()"+BCIUtils.DSC_OBJECTID);
		
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
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectConstant", "()"+DSC_OBJECTID);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processInstanceOf(InsnList aInsns, TypeInsnNode aNode)
	{
		SList s = new SList();
		s.POP();
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectInstanceofOutcome", "()I");
		
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
		Integer theCacheSlot = itsMethodInfo.getCacheSlot(aNode);

		SList s = new SList();

		Label lFieldValue = new Label();
		Label lFieldValue_Same = new Label();
		Label lError = new Label();
		Label lEndIf = new Label();
		
		if (aNode.getOpcode() == Opcodes.GETSTATIC) s.ACONST_NULL(); // Push "null" target
		s.ASTORE(itsTmpTargetVar); // Store target
		
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "getNextMessageConsumingClassloading", "()B");
		s.ISTORE(itsTmpVar);
		
		s.ILOAD(itsTmpVar);
		s.pushInt(Message.FIELD_READ);
		s.IF_ICMPEQ(lFieldValue);
		s.ILOAD(itsTmpVar);
		s.pushInt(Message.FIELD_READ_SAME);
		s.IF_ICMPEQ(lFieldValue_Same);
		
		// Bad message
		s.label(lError);
			s.createRTEx("Unexpected message");
			s.ATHROW();
		
		// FIELD_READ
		s.label(lFieldValue);
			s.invokeRead(theType);
			if (theCacheSlot != null)
			{
				s.DUP(theType);
				s.ISTORE(theType, theCacheSlot);
			}
			s.GOTO(lEndIf);
		
		// FIELD_READ_SAME
		s.label(lFieldValue_Same);
			if (theCacheSlot != null)
			{
				s.ILOAD(theType, theCacheSlot);
			}
			else
			{
				s.GOTO(lError);
			}
			
		s.label(lEndIf);

		s.ISTORE(theType, itsTmpValueVar);
		
		// Register event
		pushCollector(s);
		s.DUP();
		
		s.ALOAD(itsTmpTargetVar);
		s.LDC(StructureDatabaseUtils.getFieldId(itsDatabase, aNode.owner, aNode.name, false));
		s.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR, "fieldRead", "("+DSC_OBJECTID+"I)V");
		
		s.ILOAD(theType, itsTmpValueVar);
		invokeValue(s, theType);
		
		s.ILOAD(theType, itsTmpValueVar);		
		
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
		s.LDC(StructureDatabaseUtils.getFieldId(itsDatabase, aNode.owner, aNode.name, false));
		s.INVOKEVIRTUAL(CLS_EVENTCOLLECTOR, "fieldWrite", "("+DSC_OBJECTID+"I)V");
		
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
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectArrayRead", "()V");
		s.invokeRead(theType);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processArrayLength(InsnList aInsns, InsnNode aNode)
	{
		SList s = new SList();

		s.POP(); // Pop target
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectArrayLength", "()I");
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processGetVar(InsnList aInsns, VarInsnNode aNode)
	{
		Type theType = getTypeOrId(BCIUtils.getSort(aNode.getOpcode()));
		SList s = new SList();
		s.ILOAD(theType, aNode.var+1); // Because the target is the frame
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processPutVar(InsnList aInsns, VarInsnNode aNode)
	{
		Type theType = getTypeOrId(BCIUtils.getSort(aNode.getOpcode()));
		SList s = new SList();
		s.ISTORE(theType, aNode.var+1); // Because the target is the frame
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processIinc(InsnList aInsns, IincInsnNode aNode)
	{
		SList s = new SList();
		s.IINC(aNode.var+1, aNode.incr);
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	/**
	 * We need to check if an exception is going to be thrown.
	 */
	private void processDiv(InsnList aInsns, InsnNode aNode)
	{
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()));

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
		
		case Type.DOUBLE:
			s.pushDouble(0.0);
			s.DCMPG();
			break;
			
		case Type.FLOAT:
			s.pushFloat(0f);
			s.FCMPG();
			break;
		
		default: throw new RuntimeException("Unexpected type: "+theType);
		}
		
		s.IFNE(lNormal);
		
		{
			// An arithmetic exception will occur
			s.POP(theType);
			s.POP(theType);
			
			s.ALOAD(0);
			s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "expectException", "()V");
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
		s.ALOAD(0);
		s.INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "checkCast", "()V");

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
}
