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
	public static final boolean ECHO = false;
	public static final String REPLAYER_NAME_PREFIX = "$tod$replayer$";
	
	private final TODConfig itsConfig;
	private final IStructureDatabase itsDatabase;
	private final TmpIdManager itsTmpIdManager;
	private final BufferStream itsBuffer;
	
	private int itsMessageCount = 0;
	private byte itsCurrentMessage = -1;
	private byte itsLastMessage = -1;
	
	/**
	 * The cached replayer classes, indexed by behavior id.
	 */
	private List<Class<InScopeReplayerFrame>> itsReplayers =
		new ArrayList<Class<InScopeReplayerFrame>>();

	private Stack<ReplayerFrame> itsStack = new ArrayStack<ReplayerFrame>();
	
	private final IntDeltaReceiver itsBehIdReceiver = new IntDeltaReceiver();
	private final LongDeltaReceiver itsObjIdReceiver = new LongDeltaReceiver();
	
	
	/**
	 * The monitoring modes of each behavior, indexed by behavior id.
	 * The mode is updated whenever we receive a {@link Message#TRACEDMETHODS_VERSION} message.
	 */
	private final TByteArrayList itsMonitoringModes = new TByteArrayList();
	
	private List<PrimitiveMultiStack> itsMultiStacksPool = new ArrayList<PrimitiveMultiStack>();
	
	private int itsCurrentMonitoringModeVersion = 0;

	public ThreadReplayer(
			TODConfig aConfig, 
			IStructureDatabase aDatabase, 
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsTmpIdManager = aTmpIdManager;
		itsBuffer = aBuffer;
		
		pushFrame(createUnmonitoredReplayer(null));
	}
	
	public TmpIdManager getTmpIdManager()
	{
		return itsTmpIdManager;
	}
	
	public IStructureDatabase getDatabase()
	{
		return itsDatabase;
	}
	
	public final void echo(String aText, Object... aArgs)
	{
		Utils.printlnIndented(itsStack.size()*2, aText, aArgs);
	}
	
	public PrimitiveMultiStack getPMS()
	{
		if (itsMultiStacksPool.isEmpty()) return new PrimitiveMultiStack();
		else
		{
			PrimitiveMultiStack theStack = itsMultiStacksPool.remove(itsMultiStacksPool.size()-1);
			theStack.clear();
			return theStack;
		}
	}
	
	public void releasePMS(PrimitiveMultiStack aMultiStack)
	{
		itsMultiStacksPool.add(aMultiStack);
	}
	
	public byte peekNextMessage()
	{
		return itsBuffer.peek();
	}
	
	public boolean isNextMessageStateless()
	{
		byte theMessage = peekNextMessage();
		switch(theMessage)
		{
		case Message.TRACEDMETHODS_VERSION:
		case Message.REGISTER_REFOBJECT:
		case Message.REGISTER_REFOBJECT_DELTA:
		case Message.REGISTER_OBJECT: 
		case Message.REGISTER_OBJECT_DELTA: 
		case Message.REGISTER_THREAD: 
		case Message.REGISTER_CLASS: 
		case Message.REGISTER_CLASSLOADER: 
		case Message.SYNC:
			return true;
		
		default:
			return false;
		}		
	}
	
	/**
	 * Processes available state-independant messages.
	 */
	public void processStatelessMessages()
	{
		while(isNextMessageStateless()) processNextMessage(false);
	}

	public void replay()
	{
		itsCurrentMessage = -1;
		while(itsBuffer.remaining() > 0) processNextMessage(true);
	}
	
	/**
	 * Reads and processes the next message from the buffer.
	 * @param aAllowFrameMessages Whether state-dependant messages are allowed
	 */
	private void processNextMessage(boolean aAllowFrameMessages)
	{
		itsLastMessage = itsCurrentMessage;
		itsCurrentMessage = itsBuffer.get();
		itsMessageCount++;
		if (ThreadReplayer.ECHO) echo("Message: %s [#%d @%d]", Message._NAMES[itsCurrentMessage], itsMessageCount, itsBuffer.position());
		
		boolean theProcessed = true;
		
		// Filter state-independant messages
		switch(itsCurrentMessage)
		{
		case Message.TRACEDMETHODS_VERSION:
			processTracedMethodsVersion(itsBuffer.getInt());
			break;
			
		case Message.REGISTER_REFOBJECT:
			processRegisterRefObject(itsObjIdReceiver.receiveFull(itsBuffer), itsBuffer);
			break;
			
		case Message.REGISTER_REFOBJECT_DELTA:
			processRegisterRefObject(itsObjIdReceiver.receiveDelta(itsBuffer), itsBuffer);
			break;
			
		case Message.REGISTER_OBJECT: processRegisterObject(itsBuffer); break;
		case Message.REGISTER_OBJECT_DELTA: throw new UnsupportedOperationException();
		case Message.REGISTER_THREAD: processRegisterThread(itsBuffer); break;
		case Message.REGISTER_CLASS: processRegisterClass(itsBuffer); break;
		case Message.REGISTER_CLASSLOADER: processRegisterClassLoader(itsBuffer); break;
		case Message.SYNC: processSync(itsBuffer); break;
		
		default:
			theProcessed = false;
		}
		if (theProcessed) return;
		if (! aAllowFrameMessages) throw new IllegalStateException();

		peekFrame().processMessage(itsCurrentMessage, itsBuffer);
	}
	
	/**
	 * Publicly accessible version of {@link #processNextMessage(boolean)} that
	 * only allows state-independant messages.
	 */
	public void processNextMessage()
	{
		processNextMessage(false);
	}
	
	public IntDeltaReceiver getBehIdReceiver()
	{
		return itsBehIdReceiver;
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
	
	private ReplayerFrame peekFrame()
	{
		return itsStack.peek();
	}
	
	private ReplayerFrame popFrame()
	{
		if (ThreadReplayer.ECHO) echo("Pop");
		ReplayerFrame theFrame = itsStack.pop();
		theFrame.dispose(this);
		return theFrame;
	}
	
	public void pushFrame(ReplayerFrame aFrame)
	{
		if (ThreadReplayer.ECHO) echo("Push: %s", aFrame);
		itsStack.push(aFrame);
	}
	
	/**
	 * Called by the current replayer to indicate that it returned
	 */
	public void returnNormal(InScopeReplayerFrame aFrame)
	{
		ReplayerFrame theFrame = popFrame();
		if (theFrame != aFrame) throw new IllegalStateException();
		peekFrame().transferResult(aFrame);
	}
	
	/**
	 * Called by the current replayer to indicate that it returned
	 */
	public void returnNormal(BufferStream aStream)
	{
		popFrame();
		peekFrame().transferResult(aStream);
	}
	
	/**
	 * Called by enveloppe replayers, as they don't know if a result will be send or not.
	 */
	public void returnNormal()
	{
		popFrame();
	}
	
	public void returnClassloader()
	{
		popFrame();
		peekFrame().classloaderReturned();
	}
	
	/**
	 * Called by the current replayer to indicate that it returned with an exception
	 */
	public void returnException()
	{
		popFrame();
		peekFrame().expectException();
	}
	
	/**
	 * Called when an unmonitored call failed before the method started executing.
	 * @param aException 
	 */
	public void failedCall(byte aMessage, BufferStream aBuffer, ExceptionInfo aException)
	{
		ReplayerFrame theReplayer = popFrame();
		if (! (theReplayer instanceof UnmonitoredReplayerFrame)) throw new IllegalStateException();
		InScopeReplayerFrame theCurrentReplayer = (InScopeReplayerFrame) peekFrame();
		theCurrentReplayer.evExceptionGenerated(aException.behaviorId, aException.bytecodeIndex, aException.exception);
		theCurrentReplayer.processMessage(aMessage, aBuffer);
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
	
	public InScopeReplayerFrame createInScopeReplayer(ReplayerFrame aParent, int aBehaviorId)
	{
		try
		{
			Class<InScopeReplayerFrame> theClass = getReplayerClass(aBehaviorId);
			InScopeReplayerFrame theReplayer = theClass.newInstance();
			theReplayer.setup(this, aParent instanceof InScopeReplayerFrame);
			return theReplayer;
		}
		catch (Exception e)
		{
			IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
			throw new RuntimeException("Exception while creating replayer for "+theBehavior, e);
		}
	}
	
	public EnveloppeReplayerFrame createEnveloppeReplayer(ReplayerFrame aParent)
	{
		EnveloppeReplayerFrame theReplayer = new EnveloppeReplayerFrame();
		theReplayer.setup(this, aParent instanceof InScopeReplayerFrame);
		return theReplayer;
	}
	
	public UnmonitoredReplayerFrame createUnmonitoredReplayer(ReplayerFrame aParent)
	{
		UnmonitoredReplayerFrame theReplayer = new UnmonitoredReplayerFrame();
		theReplayer.setup(this, aParent instanceof InScopeReplayerFrame);
		return theReplayer;
	}
	
	public ClassloaderWrapperReplayerFrame createClassloaderReplayer(ReplayerFrame aParent)
	{
		ClassloaderWrapperReplayerFrame theReplayer = new ClassloaderWrapperReplayerFrame();
		theReplayer.setup(this, aParent instanceof InScopeReplayerFrame);
		return theReplayer;
	}
	
	/**
	 * Returns the replayer class used to replay the given behavior.
	 */
	private Class<InScopeReplayerFrame> getReplayerClass(int aBehaviorId)
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
				theReplayerClass = theLoader.loadClass(theReplayerName).asSubclass(InScopeReplayerFrame.class);
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
