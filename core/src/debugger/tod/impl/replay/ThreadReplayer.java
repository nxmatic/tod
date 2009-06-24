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

import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IStructureDatabase.BehaviorMonitoringModeChange;
import tod.impl.server.BufferStream;
import tod2.agent.Message;
import tod2.agent.ValueType;
import zz.utils.ArrayStack;
import zz.utils.Stack;
import zz.utils.Utils;

public class ThreadReplayer
{
	// States
	static final int S_UNDEFINED = 0; 
	static final int S_INSCOPE = 1; 
	static final int S_CALLING_MONITORED = 2; 
	static final int S_OUTOFSCOPE = 3; 
	static final int S_INVOKE_PENDING = 4; 
	static final int S_HOLD = 5; 
	static final int S_OOS_RESULT = 6; 
	
	private static final String[] _STATES = 
	{
		"S_UNDEFINED", 
		"S_INSCOPE",
		"S_CALLING_MONITORED", 
		"S_OUTOFSCOPE",
		"S_INVOKE_PENDING", 
		"S_HOLD",
		"S_OOS_RESULT",
	};

	public static final String REPLAYER_NAME_PREFIX = "$tod$replayer$";
	
	private final TODConfig itsConfig;
	private final IStructureDatabase itsDatabase;
	private final TmpIdManager itsTmpIdManager;
	
	private int itsState = S_OUTOFSCOPE;
	
	private int itsMessageCount = 0;
	private byte itsLastMessage = -1;
	private ExceptionInfo itsLastException = null;
	
	private byte itsNextMessage = -1;
	
	/**
	 * The cached replayer classes, indexed by behavior id.
	 */
	private List<Class<InScopeMethodReplayer>> itsReplayers =
		new ArrayList<Class<InScopeMethodReplayer>>();

	private Stack<MethodReplayer> itsStack = new ArrayStack<MethodReplayer>();
	
	private final IntDeltaReceiver itsBehIdReceiver = new IntDeltaReceiver();
	private final LongDeltaReceiver itsObjIdReceiver = new LongDeltaReceiver();
	
	
	/**
	 * The monitoring modes of each behavior, indexed by behavior id.
	 * The mode is updated whenever we receive a {@link Message#TRACEDMETHODS_VERSION} message.
	 */
	private final TByteArrayList itsMonitoringModes = new TByteArrayList();
	
	private int itsCurrentMonitoringModeVersion = 0;

	public ThreadReplayer(TODConfig aConfig, IStructureDatabase aDatabase, TmpIdManager aTmpIdManager)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsTmpIdManager = aTmpIdManager;
	}

	public TmpIdManager getTmpIdManager()
	{
		return itsTmpIdManager;
	}
	
	public IStructureDatabase getDatabase()
	{
		return itsDatabase;
	}
	
	public void echo(String aText, Object... aArgs)
	{
		Utils.printlnIndented(itsStack.size()*2, aText, aArgs);
	}
	
	private void setState(int aState)
	{
		itsState = aState;
		echo("State: "+_STATES[aState]);
	}

	private int getState()
	{
		return itsState;
	}

	public byte peekNextMessage()
	{
		return itsNextMessage;
	}

	public void replay(BufferStream aBuffer)
	{
		byte theMessage = -1;
		if (aBuffer.remaining() > 0)
		{
			itsNextMessage = aBuffer.get();
		}
		
		while(aBuffer.remaining() > 0 || itsNextMessage >= 0)
		{
			itsLastMessage = theMessage;
			theMessage = itsNextMessage;
			itsMessageCount++;
			itsNextMessage = aBuffer.remaining() > 0 ? aBuffer.get() : -1;
			echo("Message: %s [#%d @%d]", Message._NAMES[theMessage], itsMessageCount, aBuffer.position());
			
			// Process held state
			while(true)
			{
				boolean theChanged = false;
				
				switch(getState())
				{
				case S_INVOKE_PENDING:
					if (theMessage != Message.TRACEDMETHODS_VERSION)
					{
						InScopeMethodReplayer theReplayer = (InScopeMethodReplayer) peekReplayer();
						theReplayer.proceedInvoke();
						setState(S_INSCOPE);
						postprocess();
						theChanged = true;
					}
					break;
					
				case S_HOLD:
					if (theMessage != Message.EXCEPTION)
					{
						InScopeMethodReplayer theReplayer = (InScopeMethodReplayer) peekReplayer();
						theReplayer.proceed();
						setState(S_INSCOPE);
						postprocess();
						theChanged = true;
					}
					else
					{
						setState(S_INSCOPE);
					}
					break;
				}
				
				if (! theChanged) break;
			} 
			
			// Process message
			boolean theProcessed = true;
			
			// Filter state-independant messages
			switch(theMessage)
			{
			case Message.TRACEDMETHODS_VERSION:
				processTracedMethodsVersion(aBuffer.getInt());
				if (getState() == S_INVOKE_PENDING)
				{
					InScopeMethodReplayer theReplayer = (InScopeMethodReplayer) peekReplayer();
					theReplayer.proceedInvoke();
					setState(S_INSCOPE);
					postprocess();
				}
				break;
				
			case Message.REGISTER_REFOBJECT:
				processRegisterRefObject(itsObjIdReceiver.receiveFull(aBuffer), aBuffer);
				break;
				
			case Message.REGISTER_REFOBJECT_DELTA:
				processRegisterRefObject(itsObjIdReceiver.receiveDelta(aBuffer), aBuffer);
				break;
				
			case Message.REGISTER_OBJECT: processRegisterObject(aBuffer); break;
			case Message.REGISTER_OBJECT_DELTA: throw new UnsupportedOperationException();
			case Message.REGISTER_THREAD: processRegisterThread(aBuffer); break;
			case Message.REGISTER_CLASS: processRegisterClass(aBuffer); break;
			case Message.REGISTER_CLASSLOADER: processRegisterClassLoader(aBuffer); break;
			case Message.SYNC: processSync(aBuffer); break;
			
			default:
				theProcessed = false;
			}
			if (theProcessed) continue;

			// Dispatch the message according to the current state
			switch(getState())
			{
			case S_INSCOPE: process_InScope(theMessage, aBuffer); break;
			case S_CALLING_MONITORED: process_CallingMonitored(theMessage, aBuffer); break;
			case S_OUTOFSCOPE: process_OutOfScope(theMessage, aBuffer); break;
			case S_OOS_RESULT: process_OutOfScope_Result(theMessage, aBuffer); break;
			default: throw new RuntimeException("Not handled: "+getState());
			}
			
			postprocess();
		}
	}
	
	/**
	 * Process the next event considering we are in the {@link #S_INSCOPE} state.
	 */
	private void process_InScope(byte aMessage, BufferStream aBuffer)
	{
		InScopeMethodReplayer theCurrentReplayer = (InScopeMethodReplayer) peekReplayer();
		
		switch(aMessage)
		{
		case Message.BEHAVIOR_ENTER_ARGS: 
		case Message.ARRAY_READ: 
		case Message.ARRAY_LENGTH: 
		case Message.CONSTANT: 
		case Message.FIELD_READ: 
		case Message.FIELD_READ_SAME: 
		case Message.NEW_ARRAY: 
		case Message.OBJECT_INITIALIZED: 
		case Message.CONSTRUCTOR_TARGET: 
		case Message.EXCEPTION: 
		case Message.HANDLER_REACHED: 
		case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION:
			// Let the current replayer process the message
			theCurrentReplayer.processMessage(aMessage, aBuffer);
			break;
			
//		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT:
//		{
//			MethodReplayer theParentReplayer = peekReplayer();
//			theParentReplayer.transferResult(aBuffer);
//			break;
//		}
//		

		default: throw new IllegalStateException("State: INSCOPE, got "+Message._NAMES[aMessage]);
		}
	}
	
	private void postprocess()
	{
		InScopeMethodReplayer theLastReplayer = null;
		while (getState() == S_INSCOPE)
		{
			InScopeMethodReplayer theCurrentReplayer = (InScopeMethodReplayer) peekReplayer();

			switch(theCurrentReplayer.getState())
			{
			case InScopeMethodReplayer.S_FINISHED_NORMAL:
			{
				popReplayer();
				MethodReplayer theParentReplayer = peekReplayer();
				setState(getStateForReplayer(theParentReplayer));
				if (theParentReplayer == null) break;
				
				theParentReplayer.transferResult(theCurrentReplayer);
				break;
			}
				
			case InScopeMethodReplayer.S_FINISHED_EXCEPTION:
			{
				popReplayer();
				MethodReplayer theParentReplayer = peekReplayer();
				setState(getStateForReplayer(theParentReplayer));
				if (theParentReplayer == null) break;
				
				theParentReplayer.expectException();
				break;
			}
				
			case InScopeMethodReplayer.S_CALLING_MONITORED:
				setState(S_CALLING_MONITORED);
				break;
				
			case InScopeMethodReplayer.S_CALLING_UNMONITORED:
				pushReplayer(new UnmonitoredMethodReplayer());
				setState(S_OUTOFSCOPE);			
				break;
				
			case InScopeMethodReplayer.S_INVOKE_PENDING:
				setState(S_INVOKE_PENDING);
				return;
				
			case InScopeMethodReplayer.S_HOLD:
				setState(S_HOLD);
				return;
				
			default: return;
			}
			
			if (peekReplayer() instanceof ClinitWrapperMethodReplayer) popReplayer();
			
			if (theLastReplayer == theCurrentReplayer) return;
			theLastReplayer = theCurrentReplayer;
		}
	}
	
	private int getStateForReplayer(MethodReplayer aReplayer)
	{
		if (aReplayer instanceof InScopeMethodReplayer) return S_INSCOPE;
		else if (aReplayer instanceof ClinitWrapperMethodReplayer)
		{
			ClinitWrapperMethodReplayer theReplayer = (ClinitWrapperMethodReplayer) aReplayer;
			return theReplayer.isFromScope() ? S_INSCOPE : S_OUTOFSCOPE;
		}
		else return S_OUTOFSCOPE;
	}
	
	/**
	 * Process the next event considering we are in the {@link #S_CALLING_MONITORED} state.
	 */
	private void process_CallingMonitored(byte aMessage, BufferStream aBuffer)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION: peekReplayer().processMessage(aMessage, aBuffer); break;
		
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveFull(aBuffer)); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveDelta(aBuffer)); 
			break;
			
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
			pushReplayer(new OutOfScopeMethodReplayer());
			setState(S_OUTOFSCOPE);
			break;
			
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
	}
	
	private void process_OutOfScope(byte aMessage, BufferStream aBuffer)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION:
		{
			itsLastException = readExceptionInfo(aBuffer);
			// TODO: register the exception
			break;
		}
		
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveFull(aBuffer)); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			processInScopeBehaviorEnter(itsBehIdReceiver.receiveDelta(aBuffer)); 
			break;
			
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
			pushReplayer(new OutOfScopeMethodReplayer());
			break;
			
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL: 
		{
			popReplayer();
			MethodReplayer theParentReplayer = peekReplayer();
			if (theParentReplayer instanceof InScopeMethodReplayer) setState(S_OOS_RESULT);
			else setState(getStateForReplayer(theParentReplayer));
			break;
		}

		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION: 
		{
			popReplayer();
			MethodReplayer theParentReplayer = peekReplayer();
			setState(getStateForReplayer(theParentReplayer));
			break;
		}
			
		case Message.UNMONITORED_BEHAVIOR_CALL_RESULT:
		{
			MethodReplayer theReplayer = popReplayer();
			if (! (theReplayer instanceof UnmonitoredMethodReplayer)) throw new IllegalStateException();
			InScopeMethodReplayer theParentReplayer = (InScopeMethodReplayer) peekReplayer();
			theParentReplayer.transferResult(aBuffer);
			setState(S_INSCOPE);
			break;
		}
		
		case Message.UNMONITORED_BEHAVIOR_CALL_EXCEPTION:
		{
			MethodReplayer theReplayer = popReplayer();
			if (! (theReplayer instanceof UnmonitoredMethodReplayer)) throw new IllegalStateException();
			setState(S_INSCOPE);
			break;
		}
					
		case Message.HANDLER_REACHED:
		case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION:
		{
			// An unmonitored invocation failed (eg. NPE)
			if (itsLastMessage != Message.EXCEPTION) throw new IllegalStateException();
			
			MethodReplayer theReplayer = popReplayer();
			if (! (theReplayer instanceof UnmonitoredMethodReplayer)) throw new IllegalStateException();
			setState(S_INSCOPE);
			InScopeMethodReplayer theCurrentReplayer = (InScopeMethodReplayer) peekReplayer();
			theCurrentReplayer.evExceptionGenerated(itsLastException.behaviorId, itsLastException.bytecodeIndex, itsLastException.exception);
			theCurrentReplayer.processMessage(aMessage, aBuffer);
			break;
		}
		
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
	}

	private void process_OutOfScope_Result(byte aMessage, BufferStream aBuffer)
	{
		switch(aMessage)
		{
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT:
		{
			InScopeMethodReplayer theParentReplayer = (InScopeMethodReplayer) peekReplayer();
			theParentReplayer.transferResult(aBuffer);
			setState(S_INSCOPE);
			break;
		}
		
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
	}
	
	public ObjectId readValue(BufferStream aBuffer)
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
		
		IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
		if (theBehavior.isStaticInit())
		{
			theCurrentReplayer = new ClinitWrapperMethodReplayer(getState() == S_CALLING_MONITORED);
			pushReplayer(theCurrentReplayer);
		}

		InScopeMethodReplayer theNextReplayer = createReplayer(aBehaviorId);
		echo("Entering "+theBehavior);
		pushReplayer(theNextReplayer);
		
		if (theCurrentReplayer == null)
		{
			theNextReplayer.startFromOutOfScope();
		}
		else
		{
			switch(getState())
			{
			case S_OUTOFSCOPE:
				theNextReplayer.startFromOutOfScope();
				break;
				
			case S_CALLING_MONITORED:
				theNextReplayer.startFromScope(
						theCurrentReplayer instanceof InScopeMethodReplayer ? (InScopeMethodReplayer) theCurrentReplayer : null);
				break;
				
			default:
				throw new IllegalStateException(""+getState());
			
			}
		}
		
		setState(S_INSCOPE);
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
	
	private void processRegisterObject(BufferStream aBuffer)
	{
		int theDataSize = aBuffer.getInt();
		long theId = aBuffer.getLong();
		boolean theIndexable = aBuffer.get() != 0;
		
		byte[] theData = new byte[theDataSize];
		aBuffer.get(theData, 0, theDataSize);
		
		//TODO: register object
	}
	
	private void processRegisterRefObject(long aId, BufferStream aBuffer)
	{
		int theClassId = aBuffer.getInt();
		
		// TODO: register object
	}
	
	private void processRegisterThread(BufferStream aBuffer)
	{
		long theId = aBuffer.getLong();
		String theName = aBuffer.getString();
		
		// TODO: register
	}
	
	private void processRegisterClass(BufferStream aBuffer)
	{
		int theClassId = aBuffer.getInt();
		long theLoaderId = aBuffer.getLong();
		String theName = aBuffer.getString();
		
		// TODO: register
	}
	
	private void processRegisterClassLoader(BufferStream aBuffer)
	{
		long theLoaderId = aBuffer.getLong();
		long theLoaderClassId = aBuffer.getLong();
		
		// TODO: register
	}
	
	private void processSync(BufferStream aBuffer)
	{
		long theTimestamp = aBuffer.getLong();
		
		// TODO: register
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
			InScopeMethodReplayer theReplayer = theClass.newInstance();
			theReplayer.setup(this);
			return theReplayer;
		}
		catch (Exception e)
		{
			IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
			throw new RuntimeException("Exception while creating replayer for "+theBehavior, e);
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
					theClassNode, 
					theMethodNode);
			
			byte[] theReplayerBytecode = theGenerator.generate();
			
			String theReplayerName = makeReplayerClassName(
					theClassNode.name, 
					theMethodName, 
					theMethodDesc).replace('/', '.');
			
			ClassLoader theLoader = new ReplayerClassLoader(
					getClass().getClassLoader(), 
					theReplayerName, 
					theReplayerBytecode);
			try
			{
				theReplayerClass = theLoader.loadClass(theReplayerName).asSubclass(InScopeMethodReplayer.class);
			}
			catch (ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			
			theBehavior = LocationUtils.getBehavior(itsDatabase, theClass, theMethodName, theMethodDesc, false);
			Utils.listSet(itsReplayers, theBehavior.getId(), theReplayerClass);
		}
		
		return Utils.listGet(itsReplayers, aBehaviorId);
	}
	
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
	
	public ExceptionInfo readExceptionInfo(BufferStream aBuffer)
	{
		String theMethodName = aBuffer.getString();
		String theMethodSignature = aBuffer.getString();
		String theDeclaringClassSignature = aBuffer.getString();
		short theBytecodeIndex = aBuffer.getShort();
		ObjectId theException = readValue(aBuffer);
		
		String theClassName;
		try
		{
			theClassName = Type.getType(theDeclaringClassSignature).getClassName();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Bad declaring class signature: "+theDeclaringClassSignature, e);
		}
		
		int theBehaviorId = getDatabase().getBehaviorId(theClassName, theMethodName, theMethodSignature);

		return new ExceptionInfo(
				theMethodName, 
				theMethodSignature, 
				theDeclaringClassSignature,
				theBehaviorId,
				theBytecodeIndex, 
				theException);
	}
	
	private static class ReplayerClassLoader extends ClassLoader
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
	
	public static class ExceptionInfo
	{
		public final String methodName;
		public final String methodSignature;
		public final String declaringClassSignature;
		public final int behaviorId;
		public final short bytecodeIndex;
		public final ObjectId exception;
		
		public ExceptionInfo(
				String aMethodName,
				String aMethodSignature,
				String aDeclaringClassSignature,
				int aBehaviorId,
				short aBytecodeIndex,
				ObjectId aException)
		{
			methodName = aMethodName;
			methodSignature = aMethodSignature;
			declaringClassSignature = aDeclaringClassSignature;
			behaviorId = aBehaviorId;
			bytecodeIndex = aBytecodeIndex;
			exception = aException;
		}
	}
}
