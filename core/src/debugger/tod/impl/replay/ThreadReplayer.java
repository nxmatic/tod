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

import gnu.trove.TByteArrayList;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import tod.agent.Message;
import tod.agent.ValueType;
import tod.agent.io._ByteBuffer;
import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IStructureDatabase.BehaviorMonitoringModeChange;
import zz.utils.ArrayStack;
import zz.utils.Stack;
import zz.utils.Utils;

public class ThreadReplayer
{
	// States
	static final int S_UNDEFINED = 0; 
	static final int S_INSCOPE = 1; 
	static final int S_CALLING_MONITORED = 2; 
	static final int S_CALLING_UNMONITORED = 3; 
	static final int S_OUTOFSCOPE = 4; 

	public static final String REPLAYER_NAME_PREFIX = "$todgen$";
	
	private TODConfig itsConfig;
	private IMutableStructureDatabase itsDatabase;
	
	private int itsState = S_UNDEFINED;

	/**
	 * The cached replayer classes, indexed by behavior id.
	 */
	private List<Class<InScopeMethodReplayer>> itsReplayers =
		new ArrayList<Class<InScopeMethodReplayer>>();

	private Stack<MethodReplayer> itsStack = new ArrayStack<MethodReplayer>();
	
	private final IntDeltaReceiver itsBehIdReceiver = new IntDeltaReceiver();
	private final LongDeltaReceiver itsObjIdReceiver = new LongDeltaReceiver();
	
	private final TmpIdManager itsTmpIdManager = null; // TODO: get one
	
	/**
	 * The monitoring modes of each behavior, indexed by behavior id.
	 * The mode is updated whenever we receive a {@link Message#TRACEDMETHODS_VERSION} message.
	 */
	private final TByteArrayList itsMonitoringModes = new TByteArrayList();
	
	private int itsCurrentMonitoringModeVersion = 0;

	public TmpIdManager getTmpIdManager()
	{
		return itsTmpIdManager;
	}
	
	public IMutableStructureDatabase getDatabase()
	{
		return itsDatabase;
	}
	
	public void replay(_ByteBuffer aBuffer)
	{
		loop:
		while(true)
		{
			byte theMessage = aBuffer.get();
			
			// Filter state-independant messages
			switch(theMessage)
			{
			case Message.TRACEDMETHODS_VERSION:
				processTracedMethodsVersion(aBuffer.getInt());
				continue loop;
			}

			// Dispatch the message according to the current state
			switch(itsState)
			{
			case S_INSCOPE: process_InScope(theMessage, aBuffer); break;
			case S_CALLING_MONITORED: process_CallingMonitored(theMessage, aBuffer); break;
			case S_CALLING_UNMONITORED: process_CallingUnmonitored(theMessage, aBuffer); break;
			case S_OUTOFSCOPE: process_OutOfScope(theMessage, aBuffer); break;
			default: throw new RuntimeException("Not handled: "+itsState);
			}
		}
	}
	
	/**
	 * Process the next event considering we are in the {@link #S_INSCOPE} state.
	 */
	private void process_InScope(byte aMessage, _ByteBuffer aBuffer)
	{
		InScopeMethodReplayer theCurrentReplayer = (InScopeMethodReplayer) peekReplayer();
		
		switch(aMessage)
		{
		case Message.ARRAY_READ: 
		case Message.CONSTANT: 
		case Message.FIELD_READ: 
		case Message.FIELD_READ_SAME: 
		case Message.NEW_ARRAY: 
		case Message.OBJECT_INITIALIZED: 
		case Message.EXCEPTION: 
		case Message.HANDLER_REACHED: 
			theCurrentReplayer.processMessage(aMessage, aBuffer);
			break;
			
		default: throw new IllegalStateException(""+aMessage);
		}
		
		switch(theCurrentReplayer.getState())
		{
		case InScopeMethodReplayer.S_FINISHED_NORMAL:
		{
			popReplayer();
			MethodReplayer theParentReplayer = peekReplayer();
			if (theParentReplayer == null) break;
			
			theParentReplayer.transferResult(theCurrentReplayer);
			break;
		}
			
		case InScopeMethodReplayer.S_FINISHED_EXCEPTION:
		{
			popReplayer();
			MethodReplayer theParentReplayer = peekReplayer();
			if (theParentReplayer == null) break;
			
			theParentReplayer.expectException();
			break;
		}
			
		case InScopeMethodReplayer.S_CALLING_MONITORED:
			itsState = S_CALLING_MONITORED;
			break;
			
		case InScopeMethodReplayer.S_CALLING_UNMONITORED:
			itsState = S_CALLING_UNMONITORED;			
			break;
		}
	}
	
	/**
	 * Process the next event considering we are in the {@link #S_CALLING_MONITORED} state.
	 */
	private void process_CallingMonitored(byte aMessage, _ByteBuffer aBuffer)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION: processExceptionMessage(aBuffer); break;
		
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveFull(aBuffer)); 
			itsState = S_INSCOPE;
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveDelta(aBuffer)); 
			itsState = S_INSCOPE;
			break;
			
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
			pushReplayer(new OutOfScopeMethodReplayer());
			itsState = S_OUTOFSCOPE;
			break;
			
		default: throw new RuntimeException("Command not handled: "+aMessage);
		}
	}
	
	/**
	 * Process the next event considering we are in the {@link #S_CALLING_UNMONITORED} state.
	 */
	private void process_CallingUnmonitored(byte aMessage, _ByteBuffer aBuffer)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION: processExceptionMessage(aBuffer); break;
		
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveFull(aBuffer)); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveDelta(aBuffer)); 
			break;
			
		default: throw new RuntimeException("Command not handled: "+aMessage);
		}
	}

	private void process_OutOfScope(byte aMessage, _ByteBuffer aBuffer)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION: processExceptionMessage(aBuffer); break;
		
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveFull(aBuffer)); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveDelta(aBuffer)); 
			break;
			
		default: throw new RuntimeException("Command not handled: "+aMessage);
		}
	}

	public ObjectId readValue(_ByteBuffer aBuffer)
	{
		byte theType = aBuffer.get();
		switch(theType)
		{
		case ValueType.OBJECT_ID: return new ObjectId(itsObjIdReceiver.receiveFull(aBuffer));
		case ValueType.OBJECT_ID_DELTA: return new ObjectId(itsObjIdReceiver.receiveDelta(aBuffer));
		case ValueType.NULL: return null;
		default: throw new RuntimeException("Not handled: "+theType); 
		}
	}
	
	private MethodReplayer peekReplayer()
	{
		return itsStack.peek();
	}
	
	private MethodReplayer popReplayer()
	{
		return itsStack.pop();
	}
	
	private void pushReplayer(MethodReplayer aReplayer)
	{
		itsStack.push(aReplayer);
	}
	
	private void processInScopeBehaviorEnter(int aBehaviorId)
	{
		MethodReplayer theCurrentReplayer = peekReplayer();
		InScopeMethodReplayer theNextReplayer = createReplayer(aBehaviorId);
		pushReplayer(theNextReplayer);
		
		if (theCurrentReplayer == null)
		{
			theNextReplayer.startFromOutOfScope();
		}
		else
		{
			switch(itsState)
			{
			case S_CALLING_UNMONITORED:
				theNextReplayer.startFromOutOfScope();
				break;
				
			case S_CALLING_MONITORED:
				theNextReplayer.startFromScope((InScopeMethodReplayer) theCurrentReplayer);
				break;
				
			default:
				throw new IllegalStateException(""+itsState);
			
			}
		}
	}
	
	private void processTracedMethodsVersion(int aVersion)
	{
		for(int i=itsCurrentMonitoringModeVersion;i<aVersion;i++)
		{
			BehaviorMonitoringModeChange theChange = itsDatabase.getBehaviorMonitoringModeChange(i);
			while (itsMonitoringModes.size() <= theChange.behaviorId) itsMonitoringModes.add((byte) 0);
			itsMonitoringModes.set(theChange.behaviorId, (byte) theChange.mode);
		}
		
		itsCurrentMonitoringModeVersion = aVersion;
	}
	
	public int getBehaviorMonitoringMode(int aBehaviorId)
	{
		if (aBehaviorId >= itsMonitoringModes.size()) return 0;
		else return itsMonitoringModes.getQuick(aBehaviorId);
	}
	
	private InScopeMethodReplayer createReplayer(int aBehaviorId)
	{
		try
		{
			Class<InScopeMethodReplayer> theClass = getReplayerClass(aBehaviorId);
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
	private Class<InScopeMethodReplayer> getReplayerClass(int aBehaviorId)
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
				theReplayerClass = theLoader.loadClass(theReplayerName).asSubclass(InScopeMethodReplayer.class);
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
