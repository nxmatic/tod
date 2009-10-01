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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.python.modules.newmodule;

import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.replay2.InScopeReplayerFrame.Factory;
import zz.utils.Utils;

public class ReplayerGenerator
{
	private final TODConfig itsConfig;
	private final IStructureDatabase itsDatabase;
	private final ReplayerLoader itsLoader;

	
	/**
	 * The cached replayer class factories, indexed by behavior id.
	 */
	private List<InScopeReplayerFrame.Factory> itsInScopeFrameFactories = new ArrayList<InScopeReplayerFrame.Factory>();

	public ReplayerGenerator(ReplayerLoader aLoader, TODConfig aConfig, IStructureDatabase aDatabase)
	{
		itsLoader = aLoader;
		itsConfig = aConfig;
		itsDatabase = aDatabase;
	}

	
	public static final String REPLAYER_NAME_PREFIX = "$tod$replayer2$";

	/**
	 * Returns the JVM name of the replayer class for the given method.
	 */
	public static String makeReplayerClassName(String aJvmClassName, String aJvmMethodName, String aDesc)
	{
		String theName = aJvmClassName+"_"+aJvmMethodName+"_"+aDesc;
		StringBuilder theBuilder = new StringBuilder(theName.length());
		for (int i=0;i<theName.length();i++)
		{
			char c = theName.charAt(i);
			switch(c)
			{
			case '/':
			case '(':
			case ')':
			case '<':
			case '>':
			case '[':
			case ';':
				c = '_';
				break;
			}
			theBuilder.append(c);
		}
		return REPLAYER_NAME_PREFIX+theBuilder.toString();
	}
	

	
	/**
	 * Returns the replayer class used to replay the given behavior.
	 */
	public InScopeReplayerFrame.Factory getReplayerFactory(int aBehaviorId)
	{
		InScopeReplayerFrame.Factory theFactory = Utils.listGet(itsInScopeFrameFactories, aBehaviorId);
		if (theFactory != null) return theFactory;

		// Replayer class for this behavior not found 
		// Create replayers for all the behaviors in the class.
		IBehaviorInfo theBehavior = itsDatabase.getBehavior(aBehaviorId, true);
		IClassInfo theClass = theBehavior.getDeclaringType();

		byte[] theClassBytecode = theClass.getOriginalBytecode();
		ClassNode theClassNode = new ClassNode();
		ClassReader theReader = new ClassReader(theClassBytecode);
		theReader.accept(theClassNode, 0);

		for (MethodNode theMethodNode : (List<MethodNode>) theClassNode.methods)
		{
			// Get info about the method before transforming, as the generator modifies it.
			String theMethodName = theMethodNode.name;
			String theMethodDesc = theMethodNode.desc;
			
			MethodReplayerGenerator theGenerator = new MethodReplayerGenerator(
					itsConfig, 
					itsDatabase, 
					this,
					theClassNode, 
					theMethodNode);
			
			byte[] theReplayerBytecode = theGenerator.generate();
			
			String theFrameClassJVMName = makeReplayerClassName(
					theClassNode.name, 
					theMethodName, 
					theMethodDesc);
			
			String theFrameClassName = theFrameClassJVMName.replace('/', '.');
			
			itsLoader.addClass(theFrameClassName, theReplayerBytecode);
						
			theFactory = createFactory(theFrameClassJVMName);
			theFactory.setSignature(theMethodName, theMethodNode.access, theMethodDesc);
			theBehavior = LocationUtils.getBehavior(itsDatabase, theClass, theMethodName, theMethodDesc, false);
			Utils.listSet(itsInScopeFrameFactories, theBehavior.getId(), theFactory);
		}
		
		return Utils.listGet(itsInScopeFrameFactories, aBehaviorId);
	}
	
	private InScopeReplayerFrame.Factory createFactory(String aFrameClassJVMName) 
	{
		ClassNode classNode = new ClassNode();
		classNode.name = aFrameClassJVMName+"_Factory";
		classNode.sourceFile = classNode.name+".class";
		classNode.superName = BCIUtils.getJvmClassName(InScopeReplayerFrame.Factory.class);
		classNode.version = Opcodes.V1_5;
		classNode.access = Opcodes.ACC_PUBLIC;

		// Generate constructor
		MethodNode theConstructor = new MethodNode();
		theConstructor.name = "<init>";
		theConstructor.desc = "()V";
		theConstructor.exceptions = Collections.EMPTY_LIST;
		theConstructor.access = Opcodes.ACC_PUBLIC;
		theConstructor.maxStack = 1;
		theConstructor.maxLocals = 1;
		theConstructor.tryCatchBlocks = Collections.EMPTY_LIST;
		
		SList s = new SList();
		s.ALOAD(0);
		s.INVOKESPECIAL(classNode.superName, "<init>", "()V");
		s.RETURN();
		
		theConstructor.instructions = s;
		classNode.methods.add(theConstructor);
		
		// Generate create method
		MethodNode theMethod = new MethodNode();
		theMethod.name = "create0";
		theMethod.desc = "()L"+BCIUtils.getJvmClassName(InScopeReplayerFrame.class)+";";
		theMethod.exceptions = Collections.EMPTY_LIST;
		theMethod.access = Opcodes.ACC_PUBLIC;
		theMethod.maxStack = 1;
		theMethod.maxLocals = 1;
		theMethod.tryCatchBlocks = Collections.EMPTY_LIST;

		s = new SList();
		s.NEW(aFrameClassJVMName);
		s.DUP();
		s.INVOKESPECIAL(aFrameClassJVMName, "<init>", "()V");
		s.ARETURN();

		theMethod.instructions = s;
		classNode.methods.add(theMethod);
		
		// Generate bytecode
		ClassWriter theWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(theWriter);
		
		byte[] theBytecode = theWriter.toByteArray();

		String theFactoryClassName = classNode.name.replace('/', '.');
		itsLoader.addClass(theFactoryClassName, theBytecode);

		try
		{
			Class cls = itsLoader.loadClass(theFactoryClassName);
			return (Factory) cls.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public InScopeReplayerFrame createInScopeFrame(int aBehaviorId)
	{
		IBehaviorInfo theBehavior = itsDatabase.getBehavior(aBehaviorId, true);
		if (ThreadReplayer.ECHO) System.out.println("ReplayerGenerator.createInScopeFrame(): "+theBehavior);
		InScopeReplayerFrame.Factory theFactory = getReplayerFactory(aBehaviorId);
		return theFactory.create();
	}

	public UnmonitoredReplayerFrame createUnmonitoredFrame()
	{
		return new UnmonitoredReplayerFrame();
	}
	
	public EnveloppeReplayerFrame createEnveloppeFrame()
	{
		return new EnveloppeReplayerFrame();
	}
	
	public ClassloaderWrapperReplayerFrame createClassloaderWrapperFrame()
	{
		return new ClassloaderWrapperReplayerFrame();
	}
}
