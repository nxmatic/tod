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

import java.tod.ThreadData;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableStructureDatabase;

public abstract class MethodInstrumenter
{
	protected static final String CLS_EVENTCOLLECTOR = "java/tod/EventCollector";
	protected static final String DSC_EVENTCOLLECTOR = "L"+CLS_EVENTCOLLECTOR+";";
	protected static final String CLS_AGENTREADY = "java/tod/AgentReady";
	protected static final String CLS_EXCEPTIONGENERATEDRECEIVER = "java/tod/ExceptionGeneratedReceiver";
	protected static final String CLS_TRACEDMETHODS = "java/tod/TracedMethods";
	protected static final String CLS_THREADDATA = "java/tod/ThreadData";
	protected static final String CLS_OBJECT = "java/lang/Object";
	protected static final String DSC_OBJECT = "L"+CLS_OBJECT+";";
	protected static final String CLS_THROWABLE = "java/lang/Throwable";
	protected static final String DSC_THROWABLE = "L"+CLS_THROWABLE+";";
	
	private final ClassInstrumenter itsClassInstrumenter;
	private final MethodNode itsNode;
	private final IMutableBehaviorInfo itsBehavior;
	
	private final boolean itsStatic;
	private final boolean itsConstructor;
	private final Type[] itsArgTypes;
	private final Type itsReturnType;


	
	private int itsNextFreeVar;
	
	/**
	 * The {@link ThreadData} of the current thread.
	 */
	private final int itsThreadDataVar;
	
	/**
	 * A boolean that indicates if trace is enabled
	 */
	private int itsTraceEnabledVar;
	
	/**
	 * The cached stack frames resulting from the analysis of the method
	 */
	private Frame[] itsFrames;
	
	public MethodInstrumenter(ClassInstrumenter aClassInstrumenter, MethodNode aNode, IMutableBehaviorInfo aBehavior)
	{
		itsClassInstrumenter = aClassInstrumenter;
		itsNode = aNode;
		itsBehavior = aBehavior;
		
		itsStatic = BCIUtils.isStatic(getNode().access);
		itsConstructor = "<init>".equals(getNode().name);
		assert itsStatic != itsConstructor;
		
		itsArgTypes = Type.getArgumentTypes(getNode().desc);
		itsReturnType = Type.getReturnType(getNode().desc);
		
		itsNextFreeVar = getNode().maxLocals;
		itsThreadDataVar = nextFreeVar(1);
		itsTraceEnabledVar = nextFreeVar(1);
	}
	
	public ClassInstrumenter getClassInstrumenter()
	{
		return itsClassInstrumenter;
	}
	
	public ClassNode getClassNode()
	{
		return getClassInstrumenter().getNode();
	}
	
	protected boolean isStatic()
	{
		return itsStatic;
	}
	
	protected boolean isConstructor()
	{
		return itsConstructor;
	}
	
	protected Type[] getArgTypes()
	{
		return itsArgTypes;
	}
	
	protected Type getReturnType()
	{
		return itsReturnType;
	}

	protected int nextFreeVar(int aSize)
	{
		int theVar = itsNextFreeVar;
		itsNextFreeVar += aSize;
		return theVar;
	}
	
	protected int getThreadDataVar()
	{
		return itsThreadDataVar;
	}
	
	protected int getTraceEnabledVar()
	{
		return itsTraceEnabledVar;
	}
	
	public MethodNode getNode()
	{
		return itsNode;
	}
	
	public IMutableBehaviorInfo getBehavior()
	{
		return itsBehavior;
	}
	
	public IMutableStructureDatabase getDatabase()
	{
		return getBehavior().getDatabase();
	}

	public abstract void proceed();
	
	/**
	 * Sends a value of a given type through {@link ThreadData}
	 * @param aIndex The local variable slot to take the value from.
	 * @param aType The type of the value
	 */
	protected void sendValue(SyntaxInsnList s, int aIndex, Type aType)
	{
		switch(aType.getSort())
		{
		case Type.BOOLEAN: sendValue_Boolean(s, aIndex); break;
		case Type.BYTE: sendValue_Byte(s, aIndex); break;
		case Type.CHAR: sendValue_Char(s, aIndex); break;
		case Type.DOUBLE: sendValue_Double(s, aIndex); break;
		case Type.FLOAT: sendValue_Float(s, aIndex); break;
		case Type.INT: sendValue_Int(s, aIndex); break;
		case Type.LONG: sendValue_Long(s, aIndex); break;
		case Type.OBJECT: sendValue_Ref(s, aIndex); break;
		case Type.SHORT: sendValue_Short(s, aIndex); break;
		default: throw new Error("Unknown sort: "+aType);
		}
	}
	
	protected void sendValue_Boolean(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.ILOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Boolean", "(Z)V");
	}
	
	protected void sendValue_Byte(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.ILOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Byte", "(B)V");
	}
	
	protected void sendValue_Char(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.ILOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Char", "(C)V");
	}
	
	protected void sendValue_Short(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.ILOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Short", "(S)V");
	}
	
	protected void sendValue_Int(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.ILOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Int", "(I)V");
	}
	
	protected void sendValue_Long(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.LLOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Long", "(J)V");
	}
	
	protected void sendValue_Float(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.FLOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Float", "(F)V");
	}
	
	protected void sendValue_Double(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.DLOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Double", "(D)V");
	}
	
	protected void sendValue_Ref(SyntaxInsnList s, int aIndex)
	{
		s.ALOAD(itsThreadDataVar);
		s.ALOAD(aIndex); 
		s.INVOKEVIRTUAL(CLS_THREADDATA, "sendValue_Ref", "("+DSC_OBJECT+")V");
	}
	
	/**
	 * Returns the stack frames resulting from the analysis of the method
	 * using the {@link SourceInterpreter}.
	 */
	protected Frame[] getFrames()
	{
		if (itsFrames == null) itsFrames = Analysis.analyze_nocflow(getClassNode().name, getNode());
		return itsFrames;
	}

}
