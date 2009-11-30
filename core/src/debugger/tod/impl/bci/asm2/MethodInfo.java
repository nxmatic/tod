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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.objectweb.asm.tree.analysis.Value;

import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import zz.utils.ArrayStack;
import zz.utils.ListMap;
import zz.utils.Stack;
import zz.utils.Utils;

/**
 * Aggregates analysis information about a method that is needed by both the
 * instrumenter and the replayer.
 * @author gpothier
 */
public class MethodInfo
{
	private final IStructureDatabase itsDatabase;
	private final ClassNode itsClassNode;
	private final MethodNode itsMethodNode;
	
	/**
	 * Maps instructions to frames.
	 */
	private Map<AbstractInsnNode, BCIFrame> itsFramesMap;
	
	/**
	 * Maps each field to the instructions that read that field on self.
	 * Note: the same instruction can appear twice, which is actually what ultimately
	 * triggers the field to be cached.
	 */
	private ListMap<IFieldInfo, FieldInsnNode> itsAccessMap = new ListMap<IFieldInfo, FieldInsnNode>();

	/**
	 * Maps field access instructions to the local variable slot that holds the cached value.
	 */
	private Map<FieldInsnNode, Integer> itsCachedFieldAccesses = new HashMap<FieldInsnNode, Integer>();
	
	/**
	 * Instructions that initializes each slot of the field cache.
	 */
	private InsnList itsFieldCacheInit;
	
	/**
	 * For constructors, the invocation instructions that corresponds to constructor chaining.
	 */
	private MethodInsnNode itsChainingInvocation;
	
	/**
	 * Maps non-chanining constructor invocation to the corresponding
	 * {@link NewInvokeLink}, which indicates the corresponding NEW instruction and nesting level. 
	 */
	private Map<MethodInsnNode, NewInvokeLink> itsNewInvokeLinks;
	
	private int itsMaxNewInvokeNesting = 0;

	public MethodInfo(IStructureDatabase aDatabase, ClassNode aClassNode, MethodNode aMethodNode)
	{
		itsDatabase = aDatabase;
		itsClassNode = aClassNode;
		itsMethodNode = aMethodNode;
		setupFrames();
		mapSelfAccesses();
		setupChainingInvocation();
		setupNewInvokeLinks();
	}

	public ClassNode getClassNode()
	{
		return itsClassNode;
	}
	
	public MethodNode getMethodNode()
	{
		return itsMethodNode;
	}
	
	public IStructureDatabase getDatabase()
	{
		return itsDatabase;
	}
	
	/**
	 * Gets the frame corresponding to the specified node.
	 * @param aNode A node that is part of the original method body.
	 */
	public BCIFrame getFrame(AbstractInsnNode aNode)
	{
		return itsFramesMap.get(aNode);
	}
	
	/**
	 * Determines if the given node is an invocation that corresponds to constructor chaining.
	 */
	public boolean isChainingInvocation(MethodInsnNode aNode)
	{
		return aNode == itsChainingInvocation;
	}
	
	/**
	 * Determines if the given node corresponds to the initial constructor call
	 * (vs. constructor chaining).
	 */
	public boolean isObjectInitialization(MethodInsnNode aNode)
	{
		if (! BCIUtils.isConstructorCall(aNode)) return false;
		else return !isChainingInvocation(aNode);
	}

	public NewInvokeLink getNewInvokeLink(MethodInsnNode aNode)
	{
		return itsNewInvokeLinks.get(aNode);
	}
	
	public int getMaxNewInvokeNesting()
	{
		return itsMaxNewInvokeNesting;
	}

	/**
	 * Allocates local variable slots for field caches.
	 * @param aFirstFreeSlot The first free local slot
	 * @return The number of slots used for caches
	 */
	public int setupLocalCacheSlots(int aFirstFreeSlot)
	{
		int theSlotCount = 0;
		
		// Set up the final structure
		SyntaxInsnList s = new SyntaxInsnList();
		Iterator<Map.Entry<IFieldInfo, List<FieldInsnNode>>> theIterator = itsAccessMap.entrySet().iterator();
		while(theIterator.hasNext())
		{
			Map.Entry<IFieldInfo, List<FieldInsnNode>> theEntry = theIterator.next();
			if (theEntry.getValue().size() >= 2)
			{
				String theDesc = theEntry.getValue().get(0).desc;
				Type theType = Type.getType(theDesc);
				int theSlot = aFirstFreeSlot+theSlotCount;
				theSlotCount += theType.getSize();
				
				// Register instruction in the map
				for(FieldInsnNode theNode : theEntry.getValue()) 
					itsCachedFieldAccesses.put(theNode, theSlot);
				
				// Create initializing instruction.
				s.pushDefaultValue(theType);
				s.ISTORE(theType, theSlot);
			}
		}
		
		itsFieldCacheInit = s;
		
		return theSlotCount;
	}

	/**
	 * Returns a list of all cached fields.
	 */
	public List<IFieldInfo> getCachedFields()
	{
		List<IFieldInfo> theResult = new ArrayList<IFieldInfo>();
		
		Iterator<Map.Entry<IFieldInfo, List<FieldInsnNode>>> theIterator = itsAccessMap.entrySet().iterator();
		while(theIterator.hasNext())
		{
			Map.Entry<IFieldInfo, List<FieldInsnNode>> theEntry = theIterator.next();
			if (theEntry.getValue().size() >= 2) theResult.add(theEntry.getKey());
		}
		
		return theResult;
	}
	

	/**
	 * Returns a block of code that initializes the cache for each field.
	 * {@link #setupLocalCacheSlots(int)} must have been called before
	 */
	public InsnList getFieldCacheInitInstructions()
	{
		return itsFieldCacheInit;
	}
	
	/**
	 * Returns the local variable slot to use as a cache for the field accessed
	 * by the given instruction
	 * {@link #setupLocalCacheSlots(int)} must have been called before
	 */
	public Integer getCacheSlot(FieldInsnNode aNode)
	{
		return itsCachedFieldAccesses.get(aNode);
	}
	
	
	private void setupFrames()
	{
		itsFramesMap = new HashMap<AbstractInsnNode, BCIFrame>();
		BCIFrame[] theFrames = analyze_nocflow(getClassNode().name, getMethodNode());
		
		int i = 0; // Instruction rank
		ListIterator<AbstractInsnNode> theIterator = getMethodNode().instructions.iterator();
		while(theIterator.hasNext()) itsFramesMap.put(theIterator.next(), theFrames[i++]);
	}
	
	public static Node[] analyze_cflow(String aClassName, MethodNode aNode)
	{
		SourceInterpreter theInterpreter = new SourceInterpreter();
		Analyzer theAnalyzer = new Analyzer(theInterpreter)
		{
			@Override
			protected Frame newFrame(int nLocals, int nStack)
			{
				return new Node(nLocals, nStack);
			}

			@Override
			protected Frame newFrame(Frame src)
			{
				return new Node(src);
			}

			@Override
			protected void newControlFlowEdge(int aInsn, int aSuccessor)
			{
				Node thePred = (Node) getFrames()[aInsn];
				Node theSucc = (Node) getFrames()[aSuccessor];
				thePred.addSuccessor(theSucc);
				theSucc.addPredecessor(thePred);
			}
		};
		
		try
		{
			theAnalyzer.analyze(aClassName, aNode);
		}
		catch (AnalyzerException e)
		{
			throw new RuntimeException(e);
		}
		
		return (Node[]) theAnalyzer.getFrames();
	}
	
	public static BCIFrame[] analyze_nocflow(String aClassName, MethodNode aNode)
	{
		BCIInterpreter theInterpreter = new BCIInterpreter();
		Analyzer theAnalyzer = new Analyzer(theInterpreter)
		{
			@Override
			protected Frame newFrame(int nLocals, int nStack)
			{
				return new BCIFrame(nLocals, nStack);
			}

			@Override
			protected Frame newFrame(Frame src)
			{
				return new BCIFrame(src);
			}
		};
		
		try
		{
			theAnalyzer.analyze(aClassName, aNode);
		}
		catch (AnalyzerException e)
		{
			throw new RuntimeException(e);
		}
		
		Frame[] theFrames = theAnalyzer.getFrames();
		BCIFrame[] theResult = new BCIFrame[theFrames.length];
		for(int i=0;i<theFrames.length;i++) theResult[i] = (BCIFrame) theFrames[i];
		return theResult;
	}
	
	/**
	 * Whether the given field access instructions accesses a field on self (this).
	 */
	private boolean isSelfFieldAccess(FieldInsnNode aNode)
	{
		if (BCIUtils.isStatic(getMethodNode().access)) return false; //Not an error: a static method can get fields of some object.
		
		BCIValue theTarget = getFrame(aNode).getStack(0);

		for(AbstractInsnNode theNode : theTarget.getInsns())
		{
			if (theNode instanceof VarInsnNode)
			{
				VarInsnNode theVarInsnNode = (VarInsnNode) theNode;
				if (theVarInsnNode.var == 0) return true;
			}
		}
		
		return false;
	}
	
	private IFieldInfo getField(FieldInsnNode aNode)
	{
		if (getDatabase() instanceof IMutableStructureDatabase)
		{
			IMutableStructureDatabase theDatabase = (IMutableStructureDatabase) getDatabase();
			IMutableClassInfo theOwner = theDatabase.getNewClass(aNode.owner);
			ITypeInfo theType = theDatabase.getNewType(aNode.desc);

			return theOwner.getNewField(aNode.name, theType, aNode.getOpcode() == Opcodes.GETSTATIC);
		}
		else
		{
			IClassInfo theOwner = getDatabase().getClass(aNode.owner, true);
			return theOwner.getField(aNode.name);
		}
	}
	
	


	/**
	 * For each field access on the current object or static field access,
	 * check if the access might execute more than once (because of several
	 * access locations or of loops). If so, reserve a local variable to hold the
	 * last observed value of the field so that we can optimize the Get Field event.  
	 */
	private void mapSelfAccesses()
	{
		Set<AbstractInsnNode> theVisitedJumps = new HashSet<AbstractInsnNode>();
		

		// A list of paths to process (denoted by the first instruction of the path) 
		Stack<AbstractInsnNode> theWorkList = new ArrayStack<AbstractInsnNode>();
		theWorkList.push(getMethodNode().instructions.getFirst());
		
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
						itsAccessMap.add(theField, theFieldInsnNode);
					}
				}
				else if (theNode.getOpcode() == Opcodes.GETSTATIC)
				{
					IFieldInfo theField = getField(theFieldInsnNode);
					itsAccessMap.add(theField, theFieldInsnNode);
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

	}
	
	public boolean isConstructor()
	{
		return "<init>".equals(getMethodNode().name);
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
	
	private boolean hasAload0Only(BCIValue aValue)
	{
		if (aValue.getInsns().size() != 1) return false;
		return isALOAD0(aValue.getInsns().iterator().next()); 
	}
	
	private static boolean isNEW(AbstractInsnNode aNode)
	{
		return aNode.getOpcode() == Opcodes.NEW;
	}
	
	private static boolean isDUP(AbstractInsnNode aNode)
	{
		return aNode.getOpcode() == Opcodes.DUP;
	}
	
	private TypeInsnNode getNewInsn(BCIValue aValue)
	{
		if (aValue.getInsns().size() != 1) return null;
		AbstractInsnNode theNode = aValue.getInsns().iterator().next();
		if (isNEW(theNode)) return (TypeInsnNode) theNode;
		else if (isDUP(theNode))
		{
			BCIFrame theFrame = getFrame(theNode);
			BCIValue theSource = theFrame.getStack(theFrame.getStackSize()-1);
			return getNewInsn(theSource);
		}
		else return null;
	}
	
	/**
	 * Counts the number of NEW instructions between the given instructions, excluded.
	 */
	private int countNews(AbstractInsnNode aStart, AbstractInsnNode aEnd)
	{
		int theCount = 0;
		if (aStart != aEnd)
		{
			while ((aStart = aStart.getNext()) != aEnd) if (isNEW(aStart)) theCount++;
		}
		return theCount;
	}
	
	/**
	 * For constructors, looks for the invoke instruction that corresponds to constructor
	 * chaining, if any (the only case there is none is for java.lang.Object);
	 */
	private void setupChainingInvocation()
	{
		if (! isConstructor()) return;

		ListIterator<AbstractInsnNode> theIterator = getMethodNode().instructions.iterator();
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			
			if (BCIUtils.isConstructorCall(theNode))
			{
				BCIFrame theFrame = getFrame(theNode);
				int theArgCount = Type.getArgumentTypes(((MethodInsnNode) theNode).desc).length;
				
				// Check if the target of the call is "this"
				BCIValue theThis = theFrame.getStack(theFrame.getStackSize()-theArgCount-1);
				if (hasAload0Only(theThis)) itsChainingInvocation = (MethodInsnNode) theNode;
			}
		}
		
		if (! BCIUtils.CLS_OBJECT.equals(getClassNode().name) && isConstructor() && itsChainingInvocation == null) 
			throwRTEx("Should have constructor chaining");

	}
	
	/**
	 * For each non-chaining constructor invocation, determines which NEW opcode produced the target,
	 * as well as nesting level (eg. NEW NEW INVOKE INVOKE, first NEW is level 0, second NEW is level 1). 
	 */
	private void setupNewInvokeLinks()
	{
		itsNewInvokeLinks = new HashMap<MethodInsnNode, NewInvokeLink>();
		
		ListIterator<AbstractInsnNode> theIterator = getMethodNode().instructions.iterator();
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			
			if (BCIUtils.isConstructorCall(theNode) && ! isChainingInvocation((MethodInsnNode) theNode))
			{
				MethodInsnNode theInvokeNode = (MethodInsnNode) theNode;
				BCIFrame theFrame = getFrame(theNode);
				int theArgCount = Type.getArgumentTypes(theInvokeNode.desc).length;
				
				// Check if the target of the call is "this"
				BCIValue theThis = theFrame.getStack(theFrame.getStackSize()-theArgCount-1);
				TypeInsnNode theNew = getNewInsn(theThis);
				
				if (theNew != null)
				{
					int theNesting = countNews(theNew, theNode);
					itsMaxNewInvokeNesting = Math.max(itsMaxNewInvokeNesting, theNesting);
					NewInvokeLink theLink = new NewInvokeLink(theNew, theNesting);
					itsNewInvokeLinks.put(theInvokeNode, theLink);
				}
			}
		}
	}
	
	private void throwRTEx(String aMessage)
	{
		Utils.rtex(
				"Error in %s.%s%s: %s", 
				getClassNode().name, 
				getMethodNode().name, 
				getMethodNode().desc, 
				aMessage);	
	}

	/**
	 * Blends {@link SourceInterpreter} and {@link BasicInterpreter}
	 * @author gpothier
	 */
	public static class BCIInterpreter implements Interpreter
	{
		private final BasicInterpreter itsBasicInterpreter = new BasicInterpreter();
		private final SourceInterpreter itsSourceInterpreter = new SourceInterpreter();
		
		private BasicValue b(Value v)
		{
			return v != null ? ((BCIValue) v).getBasicValue() : null;
		}
		
		private SourceValue s(Value v)
		{
			return v != null ? ((BCIValue) v).getSourceValue() : null;
		}
		
		private List<BasicValue> b(List<BCIValue> aValues)
		{
			List<BasicValue> theResult = new ArrayList<BasicValue>(aValues.size());
			for (BCIValue theValue : aValues) theResult.add(b(theValue));
			return theResult;
		}
		
		private List<SourceValue> s(List<BCIValue> aValues)
		{
			List<SourceValue> theResult = new ArrayList<SourceValue>(aValues.size());
			for (BCIValue theValue : aValues) theResult.add(s(theValue));
			return theResult;
		}
		
		private BCIValue create(Value b, Value s)
		{
			return new BCIValue((BasicValue) b, (SourceValue) s);
		}
		
		public Value binaryOperation(AbstractInsnNode aInsn, Value aValue1, Value aValue2)
				throws AnalyzerException
		{
			return create(
					itsBasicInterpreter.binaryOperation(aInsn, b(aValue1), b(aValue2)),
					itsSourceInterpreter.binaryOperation(aInsn, s(aValue1), s(aValue2)));
		}
		
		public Value copyOperation(AbstractInsnNode aInsn, Value aValue) throws AnalyzerException
		{
			return create(
					itsBasicInterpreter.copyOperation(aInsn, b(aValue)),
					itsSourceInterpreter.copyOperation(aInsn, s(aValue)));
		}
		
		public Value merge(Value aV1, Value aV2)
		{
			Value b = itsBasicInterpreter.merge(b(aV1), b(aV2));
			Value s = itsSourceInterpreter.merge(s(aV1), s(aV2));
			if (b == b(aV1) && s == s(aV1)) return aV1;
			else return create(b, s);
		}
		
		public Value naryOperation(AbstractInsnNode aInsn, List aValues) throws AnalyzerException
		{
			return create(
					itsBasicInterpreter.naryOperation(aInsn, b(aValues)),
					itsSourceInterpreter.naryOperation(aInsn, s(aValues)));
		}
		
		public Value newOperation(AbstractInsnNode aInsn) throws AnalyzerException
		{
			return create(
					itsBasicInterpreter.newOperation(aInsn),
					itsSourceInterpreter.newOperation(aInsn));
		}
		
		public Value newValue(Type aType)
		{
			if (aType != null && aType.getSort() == Type.VOID) return null;
			
			return create(
					itsBasicInterpreter.newValue(aType),
					itsSourceInterpreter.newValue(aType));
		}
		
		public Value ternaryOperation(
				AbstractInsnNode aInsn,
				Value aV1,
				Value aV2,
				Value aV3) throws AnalyzerException
		{
			return create(
					itsBasicInterpreter.ternaryOperation(aInsn, b(aV1), b(aV2), b(aV3)),
					itsSourceInterpreter.ternaryOperation(aInsn, s(aV1), s(aV2), s(aV3)));
		}
		
		public Value unaryOperation(AbstractInsnNode aInsn, Value aValue) throws AnalyzerException
		{
			return create(
					itsBasicInterpreter.unaryOperation(aInsn, b(aValue)),
					itsSourceInterpreter.unaryOperation(aInsn, s(aValue)));
		}

		public void returnOperation(AbstractInsnNode aInsn, Value aValue, Value aExpected)
		{
		}
	}
	
	/**
	 * A {@link Frame} that contains {@link SourceValue}s.
	 * @author gpothier
	 */
	public static class BCIFrame extends Frame
	{
		public BCIFrame(Frame aSrc)
		{
			super(aSrc);
		}

		public BCIFrame(int aLocals, int aStack)
		{
			super(aLocals, aStack);
		}

		@Override
		public BCIValue getLocal(int aI) throws IndexOutOfBoundsException
		{
			return (BCIValue) super.getLocal(aI);
		}

		@Override
		public BCIValue getStack(int aI) throws IndexOutOfBoundsException
		{
			return (BCIValue) super.getStack(aI);
		}
	}
	
	/**
	 * Combines the information of {@link SourceValue} and {@link BasicValue}
	 * @author gpothier
	 */
	public static class BCIValue implements Value
	{
		private final BasicValue itsBasicValue;
		private final SourceValue itsSourceValue;
		
		public BCIValue(BasicValue aBasicValue, SourceValue aSourceValue)
		{
			itsBasicValue = aBasicValue;
			itsSourceValue = aSourceValue;
			
			if (itsBasicValue != null && itsSourceValue != null && itsBasicValue.getSize() != itsSourceValue.getSize()) 
				throw new RuntimeException("Size mismatch");
		}
		
		public BasicValue getBasicValue()
		{
			return itsBasicValue;
		}
		
		public SourceValue getSourceValue()
		{
			return itsSourceValue;
		}

		public int getSize()
		{
			return itsBasicValue.getSize();
		}
		
		public Type getType()
		{
			return itsBasicValue.getType();
		}
		
		public Set<AbstractInsnNode> getInsns()
		{
			return itsSourceValue.insns;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((itsBasicValue == null) ? 0 : itsBasicValue.hashCode());
			result = prime * result + ((itsSourceValue == null) ? 0 : itsSourceValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			BCIValue other = (BCIValue) obj;
			if (itsBasicValue == null)
			{
				if (other.itsBasicValue != null) return false;
			}
			else if (!itsBasicValue.equals(other.itsBasicValue)) return false;
			if (itsSourceValue == null)
			{
				if (other.itsSourceValue != null) return false;
			}
			else if (!itsSourceValue.equals(other.itsSourceValue)) return false;
			return true;
		}
	}
	
	public static class Node extends Frame 
	{
		private Set<Node> itsSuccessors = new HashSet<Node>();
		private Set<Node> itsPredecessors = new HashSet<Node>();

		public Node(int nLocals, int nStack)
		{
			super(nLocals, nStack);
		}

		public Node(Frame src)
		{
			super(src);
		}
		
		private void addSuccessor(Node aNode)
		{
			itsSuccessors.add(aNode);
		}
		
		private void addPredecessor(Node aNode)
		{
			itsPredecessors.add(aNode);
		}
		
		public Iterable<Node> getSuccessors()
		{
			return itsSuccessors;
		}
		
		public Iterable<Node> getPredecessors()
		{
			return itsPredecessors;
		}
	}

	public static class NewInvokeLink
	{
		private TypeInsnNode itsNewInsn;
		private int itsNestingLevel;

		public NewInvokeLink(TypeInsnNode aNewInsn, int aNestingLevel)
		{
			itsNewInsn = aNewInsn;
			itsNestingLevel = aNestingLevel;
		}

		public TypeInsnNode getNewInsn()
		{
			return itsNewInsn;
		}

		public int getNestingLevel()
		{
			return itsNestingLevel;
		}
	}
}
