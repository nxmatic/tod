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

import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import tod.Util;
import tod.core.bci.IInstrumenter.InstrumentedClass;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import zz.utils.Utils;

/**
 * Handles the instrumentation of a single class
 * @author gpothier
 */
public class ClassInstrumenter
{
	/**
	 * Enables a few safety checks for debugging the instrumentation 
	 */
	private static final boolean ENABLE_CHECKS = true;
	
	
	private final ASMInstrumenter2 itsInstrumenter;
	private final String itsName;
	private final byte[] itsOriginal;
	private final ClassNode itsNode;
	private final boolean itsUseJava14;
	
	private boolean itsInterface;
	private IMutableClassInfo itsClassInfo;
	private IClassInfo itsSuperclass;
	private IClassInfo[] itsInterfaces;
	
	private boolean itsModified = false;
	
	public ClassInstrumenter(ASMInstrumenter2 aInstrumenter, String aName, byte[] aBytecode, boolean aUseJava14)
	{
		itsInstrumenter = aInstrumenter;
		itsName = aName;
		itsOriginal = aBytecode;
		itsUseJava14 = aUseJava14;

		itsNode = new ClassNode();
		ClassReader theReader = new ClassReader(aBytecode);
		theReader.accept(itsNode, 0);
		
		if (! itsName.equals(itsNode.name)) Utils.rtex("Internal error - %s != %s", itsName, itsNode.name);
		
		itsClassInfo = getDatabase().getNewClass(Util.jvmToScreen(aName));
		itsSuperclass = itsInterface || itsNode.superName == null ? 
				null
				: getDatabase().getNewClass(Util.jvmToScreen(itsNode.superName));
		
		IClassInfo[] theInterfaces = new IClassInfo[itsNode.interfaces != null ? itsNode.interfaces.size() : 0];
		if (itsNode.interfaces != null) for (int i = 0; i < itsNode.interfaces.size(); i++)
		{
			String theInterface = (String) itsNode.interfaces.get(i);
			theInterfaces[i] = getDatabase().getNewClass(Util.jvmToScreen(theInterface));
		}
	}
	
	private IMutableStructureDatabase getDatabase()
	{
		return itsInstrumenter.getStructureDatabase();
	}
	
	public InstrumentedClass proceed()
	{
		// Get classes and interfaces from the structure database
		itsInterface = BCIUtils.isInterface(itsNode.access);
		itsClassInfo = getDatabase().getNewClass(Util.jvmToScreen(itsNode.name));
		itsSuperclass = itsInterface || itsNode.superName == null ? 
				null
				: getDatabase().getNewClass(Util.jvmToScreen(itsNode.superName));
		
		IClassInfo[] theInterfaces = new IClassInfo[itsNode.interfaces != null ? itsNode.interfaces.size() : 0];
		if (itsNode.interfaces != null) for (int i = 0; i < itsNode.interfaces.size(); i++)
		{
			String theInterface = (String) itsNode.interfaces.get(i);
			theInterfaces[i] = getDatabase().getNewClass(Util.jvmToScreen(theInterface));
		}
		
		// Process each method
		for(MethodNode theNode : (List<MethodNode>) itsNode.methods) processMethod(theNode);
		
		// Output the modified class
		if (! itsModified) return null;
		
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		itsNode.accept(theWriter);
		return new InstrumentedClass(theWriter.toByteArray(), null);
	}
	
	private void processMethod(MethodNode aNode)
	{
		IMutableBehaviorInfo theBehavior = itsClassInfo.getNewBehavior(
				aNode.name,
				aNode.desc, 
				BCIUtils.isStatic(aNode.access));
		
		if (itsInstrumenter.isInScope(itsName)) new MethodInstrumenter_InScope(aNode, theBehavior).proceed();
		else new MethodInstrumenter_OutOfScope(aNode, theBehavior).proceed();
	}
}
