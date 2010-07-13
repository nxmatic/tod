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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import tod.core.database.structure.IMutableBehaviorInfo;
import tod.impl.database.structure.standard.StructureDatabase;

/**
 * Instruments out-of-scope methods (only enveloppe is instrumented).
 * @author gpothier
 */
public class MethodInstrumenter_OutOfScope extends MethodInstrumenter
{
	private Label lExit = new Label();

	/**
	 * Temporarily holds the return value of the method
	 */
	private int itsResultVar;
	
	private int itsBootstrapVar;

	public MethodInstrumenter_OutOfScope(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior);
		itsResultVar = nextFreeVar(2); //result might need two slots.
		itsBootstrapVar = nextFreeVar(1);
		getNode().maxStack += 3; // This is the max we add to the stack
	}

	@Override
	public void proceed()
	{
		// Abstracts and natives have no body.
		if (isAbstract() || isNative()) return;
		
		// Constructors are not overridable so if a constructor is out of scope it is never monitored.
		// Same for statics and privates
		// NOT for finals (they can override something) nor clinit (called automatically)
		if ((isConstructor() || isStatic() || isPrivate()) && ! isStaticInitializer()) return;
		
		// Temp optimizations (ObjectIdentity uses a WeakHashMap which uses refs)
		String theClassName = getClassNode().name;
		
		if (StructureDatabase.isSkipped(theClassName)) return;
		
		if (BCIUtils.CLS_OBJECT.equals(theClassName)) 
		{
//			if ("equals".equals(getNode().name)) return;
//			if ("toString".equals(getNode().name)) return;
			if ("finalize".equals(getNode().name)) return;
			if ("wait".equals(getNode().name)) return;
			System.out.println("Instrumenting: "+theClassName+"."+getNode().name+getNode().desc);
		}
		
		SyntaxInsnList s = new SyntaxInsnList();
		
		Label lStart = new Label();
		Label lEnd = new Label();
		Label lFinally = new Label();

		// Insert entry instructions
		{
			// Store bootstrap field
			s.GETSTATIC(BCIUtils.CLS_OBJECT, ClassInstrumenter.BOOTSTRAP_FLAG, "Z");
			s.DUP();
			s.ISTORE(itsBootstrapVar);
			
			s.IFfalse(lStart);
			
			// Store ThreadData object
			s.INVOKESTATIC(BCIUtils.CLS_EVENTCOLLECTOR_AGENT, "_getThreadData", "()"+BCIUtils.DSC_THREADDATA);
//			s.DUP();
			s.ASTORE(getThreadDataVar());

			// Send event
//			s.INVOKEVIRTUAL(
//					BCIUtils.CLS_THREADDATA, 
//					isStaticInitializer() ? "evOutOfScopeClinitEnter" : "evOutOfScopeBehaviorEnter", 
//					"()V");
			
			s.GOTO(lStart);
		}
		
		// Insert exit instructions (every return statement is replaced by a GOTO to this block)
		{
			s.label(lExit);
			Type theReturnType = getReturnType();
			Label lReturn = new Label();

			s.ILOAD(itsBootstrapVar);
			s.IFfalse(lReturn);
			
//			// Send event (the method returns a flag that indicates if the result must be sent)
//			s.ALOAD(getThreadDataVar());
//			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evOutOfScopeBehaviorExit_Normal", "()Z");				
//
//			// Send return value if needed
//			if (theReturnType.getSort() != Type.VOID) 
//			{
//				s.IFfalse(lReturn);
//
//				s.ISTORE(theReturnType, itsResultVar); // We can't use DUP in case of long or double, so we just store the value
//				sendValue(s, itsResultVar, theReturnType);
//				s.ILOAD(theReturnType, itsResultVar);
//			}
//			else
//			{
//				s.POP();
//			}
			
			s.label(lReturn);
			s.RETURN(theReturnType);
		}
		
		// Insert finally instructions
		{
			s.label(lFinally);
			Label lThrow = new Label();
			
			s.ILOAD(itsBootstrapVar);
			s.IFfalse(lThrow);
			
//			s.ALOAD(getThreadDataVar());
//			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evOutOfScopeBehaviorExit_Exception", "()V");

			s.label(lThrow);
			s.ATHROW();
		}

		s.label(lStart);
		
		processInstructions(getNode().instructions);
		
		getNode().instructions.insert(s);
		getNode().visitLabel(lEnd);
		getNode().visitTryCatchBlock(lStart, lEnd, lFinally, null);
	}
	
	private void processInstructions(InsnList aInsns)
	{
		ListIterator<AbstractInsnNode> theIterator = aInsns.iterator();
		while(theIterator.hasNext()) 
		{
			AbstractInsnNode theNode = theIterator.next();
			int theOpcode = theNode.getOpcode();
			if (theOpcode >= Opcodes.IRETURN && theOpcode <= Opcodes.RETURN)
			{
				SyntaxInsnList s = new SyntaxInsnList();
				
				s.GOTO(lExit);
				
				aInsns.insert(theNode, s);
				aInsns.remove(theNode);
			}
		}
	}

}
