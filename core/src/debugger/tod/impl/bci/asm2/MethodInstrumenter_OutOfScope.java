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

	public static boolean isTouchy(String aClassName, String aMethodName, boolean aStatic)
	{
		if (BCIUtils.CLS_OBJECT.equals(aClassName)) 
		{
			if ("finalize".equals(aMethodName)) return true;
			if ("wait".equals(aMethodName)) return true; 
			if ("notify".equals(aMethodName)) return true; 
			if ("notifyAll".equals(aMethodName)) return true; 
			if ("getClass".equals(aMethodName)) return true; 
		}

		if (aClassName.startsWith("java/lang/Math")
				|| aClassName.startsWith("java/lang/Object")
				|| aClassName.startsWith("java/lang/String$1")
				|| aClassName.startsWith("java/lang/String")
				)
		{
			if ("<init>".equals(aMethodName)) return true;	
			if (aStatic) return true;
		}
		
		if (aClassName.startsWith("java/util/Arrays"))
		{
			if (aStatic) return true;
		}
		
		if ("java/lang/System".equals(aClassName))
		{
			if ("nanoTime".equals(aMethodName)) return true;
			if ("currentTimeMillis".equals(aMethodName)) return true;
			if ("arraycopy".equals(aMethodName)) return true;
			if ("identityHashCode".equals(aMethodName)) return true;
		}

		if (BCIUtils.CLS_THREAD.equals(aClassName))
		{
			if ("currentThread".equals(aMethodName)) return true;
			if ("sleep".equals(aMethodName)) return true;
		}

		return false;
	}
	
	@Override
	public void proceed()
	{
		// Abstracts and natives have no body.
		if (isAbstract() || isNative()) return;
		
		// Constructors are not overridable so if a constructor is out of scope it is never monitored.
		// Same for statics and privates
		// NOT for finals (they can override something) nor clinit (called automatically)
//		if ((isConstructor() || isStatic() || isPrivate()) && ! isStaticInitializer()) return;
	
		// Yes, but we need enveloppe instrumentation anyway...
		// Just leaving privates, because scope is at the class granularity.
		if ((isPrivate()) && ! isStaticInitializer()) return;
		
		// Temp optimizations (ObjectIdentity uses a WeakHashMap which uses refs)
		String theClassName = getClassNode().name;
		
		if (StructureDatabase.isSkipped(theClassName)) return;
		if (isTouchy(theClassName, getNode().name, isStatic())) return;
		if (theClassName.contains("tod")) System.out.println("TOD?: "+theClassName);
		
		// For some reason, we need to do this to avoid a JVM crash
		// Note: it might be possible to reduce the set of classes for which we shouldn't store the ThreadData
		boolean theStoreThreadData = false;
//		if (theClassName.startsWith("java/")) theStoreThreadData = false;
		
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
			if (theStoreThreadData) 
			{
				s.DUP();
				s.ASTORE(getThreadDataVar());
			}
			
			// Send event
			s.pushInt(getBehavior().getId());
			s.INVOKEVIRTUAL(
					BCIUtils.CLS_THREADDATA, 
					isStaticInitializer() ? "evOutOfScopeClinitEnter" : "evOutOfScopeBehaviorEnter", 
					"(I)V");
			
			s.GOTO(lStart);
		}
		
		// Insert exit instructions (every return statement is replaced by a GOTO to this block)
		{
			s.label(lExit);
			Type theReturnType = getReturnType();
			Label lReturn = new Label();

			s.ILOAD(itsBootstrapVar);
			s.IFfalse(lReturn);
			
			// Send event (the method returns a flag that indicates if the result must be sent)
			if (theStoreThreadData) s.ALOAD(getThreadDataVar());
			else 
			{
				s.INVOKESTATIC(BCIUtils.CLS_EVENTCOLLECTOR_AGENT, "_getThreadData", "()"+BCIUtils.DSC_THREADDATA);
				
				// Store for sendValue
				s.DUP();
				s.ASTORE(getThreadDataVar());
			}
			
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evOutOfScopeBehaviorExit_Normal", "()Z");				

			// Send return value if needed
			if (theReturnType.getSort() != Type.VOID) 
			{
				s.IFfalse(lReturn);

				s.ISTORE(theReturnType, itsResultVar); // We can't use DUP in case of long or double, so we just store the value
				sendValue(s, itsResultVar, theReturnType);
				s.ILOAD(theReturnType, itsResultVar);
			}
			else
			{
				s.POP();
			}
			
			s.label(lReturn);
			s.RETURN(theReturnType);
		}
		
		// Insert finally instructions
		{
			s.label(lFinally);
			Label lThrow = new Label();
			
			s.ILOAD(itsBootstrapVar);
			s.IFfalse(lThrow);
			
			if (theStoreThreadData) s.ALOAD(getThreadDataVar());
			else s.INVOKESTATIC(BCIUtils.CLS_EVENTCOLLECTOR_AGENT, "_getThreadData", "()"+BCIUtils.DSC_THREADDATA);

			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evOutOfScopeBehaviorExit_Exception", "()V");

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
