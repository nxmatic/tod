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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import tod.core.database.structure.IMutableBehaviorInfo;

/**
 * Instruments out-of-scope methods (only enveloppe is instrumented).
 * @author gpothier
 */
public class MethodInstrumenter_OutOfScope extends MethodInstrumenter
{
	private LabelManager itsLabelManager = new LabelManager();
	
	/**
	 * Temporarily holds the return value of the method
	 */
	private int itsResultVar;

	public MethodInstrumenter_OutOfScope(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior, false);
		itsResultVar = nextFreeVar(2); //result might need two slots.
		getNode().maxStack += 3; // This is the max we add to the stack
	}

	@Override
	public void proceed()
	{
		// Abstracts and natives have no body.
		if (isAbstract() || isNative()) return;
		
		// Constructors are not overridable so if a constructor is out of scope it is never monitored
		// Same for statics and privates
		// NOT for finals (they can override something)
		if (isConstructor() || isStaticInitializer() || isStatic() || isPrivate()) return;
		
		// Temp optimizations (ObjectIdentity uses a WeakHashMap which uses refs)
		if (getClassNode().name.startsWith("java/lang/ref/")) return;
		if (getClassNode().name.startsWith("java/lang/ThreadLocal")) return;
		if (getClassNode().name.indexOf("ClassLoader") >= 0) return;
		
		if (CLS_OBJECT.equals(getClassNode().name)) 
		{
//			if ("equals".equals(getNode().name)) return;
//			if ("toString".equals(getNode().name)) return;
			if ("finalize".equals(getNode().name)) return;
			if ("wait".equals(getNode().name)) return;
			System.out.println("Instrumenting: "+getClassNode().name+"."+getNode().name+getNode().desc);
		}
		
		SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);

		// Insert entry instructions
		{
			// Set ThreadData var to null for verifier to work
			s.ACONST_NULL();
			s.ASTORE(getThreadDataVar());

			// Store the monitoring mode for the behavior in a local
			s.pushInt(getBehavior().getId()); 
			s.INVOKESTATIC(CLS_TRACEDMETHODS, "traceEnveloppe", "(I)Z");
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
				s.INVOKEVIRTUAL(CLS_THREADDATA, "evOutOfScopeBehaviorEnter", "()V");
				
				s.GOTO("start");
			}
		}
		
		// Insert exit instructions (every return statement is replaced by a GOTO to this block)
		{
			s.label("exit");
			
			// Check monitoring mode
			s.ILOAD(getTraceEnabledVar());
			s.IFfalse("return");

			// Monitoring active, send event
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evOutOfScopeBehaviorExit_Normal", "()V");				

			// Send return value if needed
			Type theReturnType = getReturnType();
			if (theReturnType.getSort() != Type.VOID) 
			{
				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "isInScope", "()Z");
				s.IFfalse("return");

				s.ALOAD(getThreadDataVar());
				s.INVOKEVIRTUAL(CLS_THREADDATA, "sendOutOfScopeBehaviorResult", "()V");
				s.ISTORE(theReturnType, itsResultVar); // We can't use DUP in case of long or double, so we just store the value
				sendValue(s, itsResultVar, theReturnType);
				s.ILOAD(theReturnType, itsResultVar);
			}
			
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
			s.INVOKEVIRTUAL(CLS_THREADDATA, "evOutOfScopeBehaviorExit_Exception", "()V");

			s.label("throw");
			s.ATHROW();
		}

		s.label("start");
		
		processInstructions(getNode().instructions);
		
		getNode().instructions.insert(s);
		getNode().visitLabel(s.getLabel("end"));
		getNode().visitTryCatchBlock(s.getLabel("start"), s.getLabel("end"), s.getLabel("finally"), null);
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
				SyntaxInsnList s = new SyntaxInsnList(itsLabelManager);
				
				s.GOTO("exit");
				
				aInsns.insert(theNode, s);
				aInsns.remove(theNode);
			}
		}
	}

}
