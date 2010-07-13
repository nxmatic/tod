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
import tod2.agent.Message;

/**
 * Special processing for {@link ClassLoader#loadClassInternal} and similar methods. 
 * Simply wraps them with {@link Message#CLASSLOADER_ENTER} and {@link Message#CLASSLOADER_EXIT}.
 * @author gpothier
 */
public class MethodInstrumenter_ClassLoader extends MethodInstrumenter
{
	private Label lExit = new Label();
	
	public MethodInstrumenter_ClassLoader(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		super(aClassInstrumenter, aNode, aBehavior);
		getNode().maxStack += 2; // This is the max we add to the stack
	}

	@Override
	public void proceed()
	{
		SyntaxInsnList s = new SyntaxInsnList();
		
		Label lStart = new Label();
		Label lEnd = new Label();
		Label lFinally = new Label();

		// Insert entry instructions
		{
			// Set ThreadData var to null for verifier to work
			s.ACONST_NULL();
			s.ASTORE(getThreadDataVar());

			// Store the monitoring mode for the behavior in a local
//			s.INVOKESTATIC(BCIUtils.CLS_TRACEDMETHODS, "traceEnabled", "()Z");
			s.pushInt(1); // TODO: check how we implement dynamic scoping
			s.DUP();
			s.ISTORE(getTraceEnabledVar());
			
			// Check monitoring mode
			s.IFfalse(lStart);
			
			// Monitoring enabled
			{
				// Store ThreadData object
				s.INVOKESTATIC(BCIUtils.CLS_EVENTCOLLECTOR_AGENT, "_getThreadData", "()"+BCIUtils.DSC_THREADDATA);
				s.DUP();
				s.ASTORE(getThreadDataVar());
				
				// Send event
				s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evClassLoaderEnter", "()V");
				
				s.GOTO(lStart);
			}
		}
		
		// Insert exit instructions (every return statement is replaced by a GOTO to this block)
		{
			s.label(lExit);
			
			// Check monitoring mode
			Label lReturn = new Label();
			s.ILOAD(getTraceEnabledVar());
			s.IFfalse(lReturn);

			// Monitoring active, send event
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evClassLoaderExit", "()V");				

			s.label(lReturn);
			s.RETURN(Type.getReturnType(getNode().desc));
		}
		
		// Insert finally instructions
		{
			s.label(lFinally);
			
			// Check monitoring mode
			Label lThrow = new Label();
			s.ILOAD(getTraceEnabledVar());
			s.IFfalse(lThrow);
			
			s.ALOAD(getThreadDataVar());
			s.INVOKEVIRTUAL(BCIUtils.CLS_THREADDATA, "evClassLoaderExit", "()V");

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
