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
package tod.impl.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import tod.agent.Message;
import tod.agent.io._ByteBuffer;
import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import zz.utils.ArrayStack;
import zz.utils.Stack;
import zz.utils.Utils;

public class ThreadReplayer
{
	public static final String REPLAYER_NAME_PREFIX = "$todgen$";
	
	private TODConfig itsConfig;
	private IMutableStructureDatabase itsDatabase;

	/**
	 * The cached replayer classes, indexed by behavior id.
	 */
	private List<Class<AbstractMethodReplayer>> itsReplayers =
		new ArrayList<Class<AbstractMethodReplayer>>();

	private Stack<AbstractMethodReplayer> itsStack = new ArrayStack<AbstractMethodReplayer>();
	
	private final IntDeltaReceiver itsBehIdReceiver = new IntDeltaReceiver();

	public void replay(_ByteBuffer aBuffer)
	{
		while(true)
		{
			byte theMessage = aBuffer.get();
			switch(theMessage)
			{
			case Message.ARRAY_READ: peekReplayer().evArrayRead(aBuffer); break;
			case Message.CONSTANT: peekReplayer().evCst(aBuffer); break;
			case Message.FIELD_READ: peekReplayer().evFieldRead(aBuffer); break;
			case Message.FIELD_READ_SAME: peekReplayer().evFieldRead_Same(); break;
			
			case Message.EXCEPTION: processExceptionMessage(aBuffer); break;
			case Message.HANDLER_REACHED: processHandlerReachedMessage(aBuffer); break;
			case Message.NEW_ARRAY: processNewArrayMessage(aBuffer); break;
			case Message.OBJECT_INITIALIZED: processObjectInitializedMessage(aBuffer); break;
			
			case Message.INSCOPE_BEHAVIOR_ENTER: 
				processInScopeBehaviorEnter(itsBehIdReceiver.receiveFull(aBuffer)); 
				break;
				
			case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
				processInScopeBehaviorEnter(itsBehIdReceiver.receiveDelta(aBuffer)); 
				break;
			
			default: throw new RuntimeException("Command not handled: "+theMessage);
			}
		}
	}
	
	private AbstractMethodReplayer peekReplayer()
	{
		return itsStack.peek();
	}
	
	private AbstractMethodReplayer popReplayer()
	{
		return itsStack.pop();
	}
	
	private void pushReplayer(AbstractMethodReplayer aReplayer)
	{
		itsStack.push(aReplayer);
	}
	
	private void processExceptionMessage(_ByteBuffer aBuffer)
	{
		String theMethodName = aBuffer.getString();
		String theMethodSignature = aBuffer.getString();
		String theDeclaringClassSignature = aBuffer.getString();
		short theBytecodeIndex = aBuffer.getShort();
		// value
	}
	
	private void processHandlerReachedMessage(_ByteBuffer aBuffer)
	{
		int theLocation = aBuffer.getInt();
		peekReplayer().evHandlerReached(theLocation);
	}
	
	private void processNewArrayMessage(_ByteBuffer aBuffer)
	{
		// value
	}
	
	private void processObjectInitializedMessage(_ByteBuffer aBuffer)
	{
		// value
	}
	
	private void processInScopeBehaviorEnter(int aBehaviorId)
	{
		AbstractMethodReplayer theCurrentReplayer = peekReplayer();
		AbstractMethodReplayer theNextReplayer = createReplayer(aBehaviorId);
		pushReplayer(theNextReplayer);
		
		if (theCurrentReplayer == null)
		{
			
		}
		else
		{
			
		}
	}
	
	private AbstractMethodReplayer createReplayer(int aBehaviorId)
	{
		try
		{
			Class<AbstractMethodReplayer> theClass = getReplayerClass(aBehaviorId);
			return theClass.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns the replayer class used to replay the given behavior.
	 */
	private Class<AbstractMethodReplayer> getReplayerClass(int aBehaviorId)
	{
		Class theReplayerClass = Utils.listGet(itsReplayers, aBehaviorId);
		if (theReplayerClass != null) return theReplayerClass;

		// Replayer class for this behavior not found 
		// Create replayers for all the behaviors in the class.
		IBehaviorInfo theBehavior = itsDatabase.getBehavior(aBehaviorId, true);
		IClassInfo theClass = theBehavior.getDeclaringType();

		byte[] theClassBytecode = theClass.getBytecode();
		ClassNode theClassNode = new ClassNode();
		ClassReader theReader = new ClassReader(theClassBytecode);
		theReader.accept(theClassNode, 0);

		for (MethodNode theMethodNode : (List<MethodNode>) theClassNode.methods)
		{
			MethodReplayerGenerator theGenerator = new MethodReplayerGenerator(itsConfig, itsDatabase, theClassNode, theMethodNode);
			byte[] theReplayerBytecode = theGenerator.generate();
			String theReplayerName = makeReplayerClassName(theClassNode.name, theMethodNode.name, theMethodNode.desc);
			
			ClassLoader theLoader = new ReplayerClassLoader(getClass().getClassLoader(), theReplayerName, theReplayerBytecode);
			try
			{
				theReplayerClass = theLoader.loadClass(theReplayerName).asSubclass(AbstractMethodReplayer.class);
			}
			catch (ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			
			theBehavior = LocationUtils.getBehavior(itsDatabase, theClass, theMethodNode.name, theMethodNode.desc, false);
			Utils.listSet(itsReplayers, theBehavior.getId(), theReplayerClass);
		}
		
		return Utils.listGet(itsReplayers, aBehaviorId);
	}
	
	/**
	 * Returns the JVM name of the replayer class for the given method.
	 */
	public static String makeReplayerClassName(String aJvmClassName, String aJvmMethodName, String aDesc)
	{
		String theName = REPLAYER_NAME_PREFIX+aJvmClassName+"_"+aJvmMethodName+"_"+aDesc;
		char[] r = new char[theName.length()];
		for (int i=0;i<r.length;i++)
		{
			char c = theName.charAt(i);
			switch(c)
			{
			case '/':
			case '(':
			case ')':
			case '[':
			case ';':
				c = '_';
				break;
			}
			r[i] = c;
		}
		return new String(r);
	}
	
	private final class ReplayerClassLoader extends ClassLoader
	{
		private final ClassLoader itsParent;
		private final String itsClassName;
		private final byte[] itsBytecode;

		public ReplayerClassLoader(ClassLoader aParent, String aClassName, byte[] aBytecode)
		{
			itsParent = aParent;
			itsClassName = aClassName;
			itsBytecode = aBytecode;
		}

		public Class loadClass(String name) throws ClassNotFoundException
		{
			if (itsClassName.equals(name)) return super.defineClass(itsClassName, itsBytecode, 0, itsBytecode.length);
			else return itsParent.loadClass(name);
		}
	}
}
