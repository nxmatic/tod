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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import tod.core.database.structure.ObjectId;
import zz.utils.tree.SimpleTreeBuilder;
import zz.utils.tree.SimpleTreeNode;

/**
 * A verifier for the generated replayer classes.
 * Avoids using a ClassLoader because our hierarchy is simple.
 * @author gpothier
 */
public class ReplayerVerifier extends SimpleVerifier
{
	private static final Type TYPE_CONCRETEREPLAYERFRAME = Type.getType("L$$CONCRETE_REPLAYER;");
	private static final Type TYPE_FACTORY = Type.getType("L$$FACTORY;");
	private static final SimpleTreeBuilder<Type> b = new SimpleTreeBuilder<Type>();
	private static final SimpleTreeNode<Type> ROOT = b.getTree().getRoot();
	
	static
	{
		b.root(Type.getType(Object.class), 
				b.node(Type.getType(ReplayerFrame.class), 
						b.node(Type.getType(InScopeReplayerFrame.class), 
								b.leaf(TYPE_CONCRETEREPLAYERFRAME))),
				b.node(Type.getType(Throwable.class), 
						b.node(Type.getType(Exception.class),
								b.node(Type.getType(RuntimeException.class), 
										b.leaf(Type.getType(HandlerReachedException.class))))),
				b.node(Type.getType(ObjectId.class),
						b.leaf(Type.getType(TmpObjectId.class))),
				b.leaf(Type.getType(String.class)),
				b.leaf(Type.getType(EventCollector.class)));
	}
	
	
	
	private boolean isConcreteFrame(Type aType)
	{
		String theName = aType.getClassName();
		return theName.startsWith("$tod$replayer2$") && ! theName.endsWith("_Factory");
	}
	
	private boolean isFactory(Type aType)
	{
		String theName = aType.getClassName();
		return theName.startsWith("$tod$replayer2$") && theName.endsWith("_Factory");
	}
	
	private Type getCanonicalType(Type t)
	{
		if (isConcreteFrame(t)) return TYPE_CONCRETEREPLAYERFRAME;
		if (isFactory(t)) return TYPE_FACTORY;
		return t;
	}
	
	private boolean sameType(Type t1, Type t2)
	{
		t1 = getCanonicalType(t1);
		t2 = getCanonicalType(t2);
		return t1.equals(t2);
	}
	
	private SimpleTreeNode<Type> searchNode(SimpleTreeNode<Type> aNode, Type aType)
	{
		if (sameType(aNode.pValue().get(), aType)) return aNode;
		if (! aNode.isLeaf()) for(SimpleTreeNode<Type> theChild : aNode.pChildren())
		{
			SimpleTreeNode<Type> theMatch = searchNode(theChild, aType);
			if (theMatch != null) return theMatch;
		}
		return null;
	}
	
	@Override
	protected boolean isAssignableFrom(Type t, Type u)
	{
		SimpleTreeNode<Type> nt = searchNode(ROOT, t);
		if (nt == null) throw new RuntimeException("Not handled: "+t);
		if (searchNode(ROOT, u) == null) throw new RuntimeException("Not handled: "+u);
		return searchNode(nt, u) != null;
	}

	@Override
	protected boolean isInterface(Type t)
	{
		if (searchNode(ROOT, t) == null) throw new RuntimeException("Not handled: "+t);
		return false;
	}

	@Override
	protected Type getSuperClass(Type t)
	{
		SimpleTreeNode<Type> nt = searchNode(ROOT, t);
		SimpleTreeNode<Type> theParent = nt.getParent();
		return theParent != null ? theParent.pValue().get() : null;
	}

}
