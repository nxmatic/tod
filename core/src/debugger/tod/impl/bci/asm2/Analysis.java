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

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;

public class Analysis
{
	public static Node[] analyze_cflow(String aClassName, MethodNode aNode)
	{
		SourceInterpreter theInterpreter = new SourceInterpreter();
		Analyzer theAnalyzer = new Analyzer(theInterpreter)
		{
			protected Frame newFrame(int nLocals, int nStack)
			{
				return new Node(nLocals, nStack);
			}

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
	
	public static Frame[] analyze_nocflow(String aClassName, MethodNode aNode)
	{
		SourceInterpreter theInterpreter = new SourceInterpreter();
		Analyzer theAnalyzer = new Analyzer(theInterpreter);
		
		try
		{
			theAnalyzer.analyze(aClassName, aNode);
		}
		catch (AnalyzerException e)
		{
			throw new RuntimeException(e);
		}
		
		return theAnalyzer.getFrames();
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


}
