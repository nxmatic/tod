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
package tod.impl.replay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import tod.core.config.TODConfig;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.bci.asm2.SyntaxInsnList;

public class MethodReplayerGenerator
{
	private final TODConfig itsConfig;
	private final ClassNode itsTarget;
	
	private final ClassNode itsClassNode;
	private final MethodNode itsMethodNode;
	
	private Type[] itsArgTypes;
	private Type itsReturnType;
	
	/**
	 * A variable slot that can hold a normal or double value.
	 */
	private int itsTmpVar;
	
	private Set<String> itsCreatedFields = new HashSet<String>();
	
	/**
	 * Maps instructions to block numbers. The instruction is the first one of the block.
	 */
	private Map<AbstractInsnNode, BlockData> itsBlocksMap = new HashMap<AbstractInsnNode, BlockData>();
	
	public MethodReplayerGenerator(TODConfig aConfig, ClassNode aClassNode, MethodNode aMethodNode)
	{
		itsConfig = aConfig;
		itsClassNode = aClassNode;
		itsMethodNode = aMethodNode;
		
		itsArgTypes = Type.getArgumentTypes(itsMethodNode.desc);
		itsReturnType = Type.getReturnType(itsMethodNode.desc);
		
		itsTarget = new ClassNode();
		itsTarget.name = makeTargetName();
		itsTarget.superName = AbstractMethodReplayer.class.getName().replace('.', '/');
		itsTarget.methods.add(itsMethodNode);
		
		itsMethodNode.name = "proceed";
		itsMethodNode.desc = "()V";
		itsMethodNode.access = Opcodes.ACC_PROTECTED;
		
		itsTmpVar = nextFreeVar(2);
	}
	
	private String makeTargetName()
	{
		String theName = "$todgen$"+itsClassNode.name+"_"+itsMethodNode.name+"_"+itsMethodNode.desc;
		char[] r = new char[theName.length()];
		for (int i=0;i<r.length;i++)
		{
			char c = theName.charAt(i);
			switch(c)
			{
			case '/':
			case '(':
			case ')':
			case '[':
			case ';':
				c = '_';
				break;
			}
			r[i] = c;
		}
		return new String(r);
	}

	public TODConfig getConfig()
	{
		return itsConfig;
	}
	
	public byte[] generate()
	{
		processInstructions(itsMethodNode.instructions);
		
		// Output the modified class
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		itsTarget.accept(theWriter);
		
		byte[] theBytecode = theWriter.toByteArray();

		try
		{
			BCIUtils.checkClass(theBytecode);
			for(MethodNode theNode : (List<MethodNode>) itsTarget.methods) BCIUtils.checkMethod(itsTarget, theNode);
		}
		catch(Exception e)
		{
			BCIUtils.writeClass(getConfig().get(TODConfig.CLASS_CACHE_PATH)+"/gen", itsTarget, theBytecode);
			System.err.println("Class "+itsTarget.name+" failed check. Writing out bytecode.");
			e.printStackTrace();
		}
		
		return theBytecode;
	}

	/**
	 * Allocates a block id for the given instruction
	 */
	private int getBlockId(AbstractInsnNode aNode)
	{
		BlockData theData = itsBlocksMap.get(aNode);
		if (theData == null)
		{
			Label theLabel;
			if (aNode.getPrevious() instanceof LabelNode) theLabel = ((LabelNode) aNode.getPrevious()).getLabel();
			else theLabel = new Label();
			
			int theId = itsBlocksMap.size(); 
			
			theData = new BlockData(theLabel, theId);
			itsBlocksMap.put(aNode, theData);
		}
		return theData.id;
	}
	
	/**
	 * Generates the switch statement that jumps to a specific block
	 */
	private InsnList generateBlockSwitch()
	{
		SyntaxInsnList s = new SyntaxInsnList(null);
		Label theDefault = new Label();
		
		int theCount = itsBlocksMap.size();
		
		// Sort blocks by id
		BlockData[] theDatas = new BlockData[theCount];
		Iterator<BlockData> theIterator = itsBlocksMap.values().iterator();
		for (int i=0;i<theCount;i++) theDatas[i] = theIterator.next();
		Arrays.sort(theDatas);
		
		// Obtain labels
		Label[] theLabels = new Label[theCount];
		for(int i=0;i<theCount;i++) theLabels[i] = theDatas[i].label;
		
		// Generate tableswitch
		s.TABLESWITCH(0, theCount-1, theDefault, theLabels);
		
		// Generate default case
		s.label(theDefault);
		s.ATHROW(); xxxx
		
	}
	
	private int nextFreeVar(int aSize)
	{
		int theVar = itsMethodNode.maxLocals;
		itsMethodNode.maxLocals += aSize;
		return theVar;
	}
	
	/**
	 * Returns the name of the field that holds the value for the specified variable, and ensures that the field is created.
	 * There is one field per (slot, type) combination. 
	 */
	private String getFieldForVar(int aSlot, Type aType)
	{
		String theName;
		
		switch(aType.getSort())
		{
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
        case Type.INT:
        	theName = "vInt_"+aSlot;
        	break;
        	
        case Type.FLOAT:
        	theName = "vFloat_"+aSlot;
        	break;
        	
        case Type.LONG:
        	theName = "vLong_"+aSlot;
        	break;
        	
        case Type.DOUBLE:
        	theName = "vDouble_"+aSlot;
        	break;

        case Type.OBJECT:
        case Type.ARRAY:
        	theName = "vRef_"+aSlot;
        	break;
        	
        default:
            throw new RuntimeException("Not handled: "+aType);
		}
		
		if (itsCreatedFields.add(theName))
		{
			itsTarget.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, theName, aType.getDescriptor(), null, null));
		}
		
		return theName;
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
			}
		}
	}

	private void processReturn(InsnList aInsns, InsnNode aNode)
	{
	}
	
	private void processInvoke(InsnList aInsns, MethodInsnNode aNode)
	{
	}

	private void processNewArray(InsnList aInsns, AbstractInsnNode aNode)
	{
	}

	private void processGetField(InsnList aInsns, FieldInsnNode aNode)
	{
	}

	private void processGetArray(InsnList aInsns, InsnNode aNode)
	{
	}

	private void processGetVar(InsnList aInsns, VarInsnNode aNode)
	{
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()));
		String theField = getFieldForVar(aNode.var, theType);
		
		SyntaxInsnList s = new SyntaxInsnList(null);
		s.ALOAD(0);
		s.GETFIELD(itsTarget.name, theField, theType.getDescriptor());
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}

	private void processPutVar(InsnList aInsns, VarInsnNode aNode)
	{
		Type theType = BCIUtils.getType(BCIUtils.getSort(aNode.getOpcode()));
		String theField = getFieldForVar(aNode.var, theType);
		
		SyntaxInsnList s = new SyntaxInsnList(null);
		s.ISTORE(theType, itsTmpVar); // Can't use SWAP because the arg my be 2-slots wide
		s.ALOAD(0);
		s.ILOAD(theType, itsTmpVar);
		s.PUTFIELD(itsTarget.name, theField, theType.getDescriptor());
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private static final class BlockData implements Comparable<BlockData>
	{
		public final Label label;
		public final int id;
		
		public BlockData(Label aLabel, int aId)
		{
			label = aLabel;
			id = aId;
		}

		public int compareTo(BlockData o)
		{
			return id-o.id;
		}
	}
}
