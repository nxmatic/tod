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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import tod.core.database.structure.IMutableBehaviorInfo;

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
	
	public MethodInstrumenter_InScope(MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aNode, aBehavior);
		itsFromScopeVar = nextFreeVar(1);
		
		itsLocationsManager = new LocationsManager(getDatabase());
	}

	@Override
	public void proceed()
	{
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);

		// Insert entry instructions
		{
			// Store the monitoring mode for the behavior in a local
			s.pushInt(getBehavior().getId()); 
			s.INVOKESTATIC(CLS_TRACEDMETHODS, "traceFull", "(I)Z");
			s.DUP();
			s.ISTORE(getTraceEnabledVar());
			
			// Check monitoring mode
			s.IFEQ("start");
			
			// Monitoring enabled
			{
				// Store ThreadData object
				s.INVOKESTATIC(CLS_EVENTCOLLECTOR, "_getThreadData", "()"+DSC_EVENTCOLLECTOR);
				s.DUP();
				s.ASTORE(getThreadDataVar());
				
				// Send event
				s.pushInt(getBehavior().getId()); 
				s.INVOKEVIRTUAL(CLS_THREADDATA, "sendBehaviorEnter_InScope", "(I)V");
				
				//Check if we must send args
				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "isInScope", "()Z");
				s.DUP();
				s.ISTORE(itsFromScopeVar);
				
				s.IFNE("afterSendArgs");
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
			s.IFEQ("return");
			
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
			s.IFEQ("throw");
			
			s.DUP();
			s.ALOAD(getThreadDataVar());
			s.SWAP();
			s.INVOKEVIRTUAL(CLS_THREADDATA, "sendBehaviorExit_Exception", "("+DSC_THROWABLE+")V");

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

			case Opcodes.NEW:
				processNew(aInsns, (TypeInsnNode) theNode);
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
		
		s.pushInt(itsLocationsManager.createLocation(aInsns, aNode));
		s.GOTO("exit");
		
		aInsns.insert(aNode, s);
		aInsns.remove(aNode);
	}
	
	private void processInvoke(InsnList aInsns, MethodInsnNode aNode)
	{
		
	}

	private void processNew(InsnList aInsns, TypeInsnNode aNode)
	{
	}

	private void processGetField(InsnList aInsns, FieldInsnNode aNode)
	{
	}

	private void processGetArray(InsnList aInsns, InsnNode aNode)
	{
	}

}
