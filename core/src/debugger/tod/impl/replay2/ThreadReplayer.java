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
import java.util.List;

import org.objectweb.asm.Type;

import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.server.BufferStream;
import tod.impl.server.BufferStream.EndOfStreamException;
import tod2.agent.Message;
import tod2.agent.ValueType;
import zz.utils.Utils;
import zz.utils.primitive.ByteArray;

public abstract class ThreadReplayer
{
	static
	{
		System.out.println("ThreadReplayer loaded by: "+ThreadReplayer.class.getClassLoader());
	}
	public static final boolean ECHO = true;
	public static boolean ECHO_FORREAL = false;
	
	/**
	 * Minimum number of messages between snapshots.
	 * Usually snapshots are taken (roughly) after each SYNC, but if there are too few
	 * messages since the previous SYNC, the snapshot is deferred.
	 */
	protected static final int MIN_MESSAGES_BETWEEN_SNAPSHOTS = 100000;


	private final int itsThreadId;
	private final TODConfig itsConfig;
	private final IStructureDatabase itsDatabase;
	
	private int itsMessageCount = 0;

	private BufferStream itsStream;
	private final TmpIdManager itsTmpIdManager;
	private final IntDeltaReceiver itsBehIdReceiver = new IntDeltaReceiver();
	private final LongDeltaReceiver itsObjIdReceiver = new LongDeltaReceiver();
	
	/**
	 * The monitoring modes of each behavior, indexed by behavior id.
	 * The mode is updated whenever we receive a {@link Message#TRACEDMETHODS_VERSION} message.
	 */
	private final ByteArray itsMonitoringModes = new ByteArray();
	
	private final List<Type> itsBehaviorReturnTypes = new ArrayList<Type>();

	private ExceptionInfo itsLastException;
	
	private final EventCollector itsCollector;
	private final ReplayerLoader itsLoader;
	
	public ThreadReplayer(
			ReplayerLoader aLoader,
			int aThreadId,
			TODConfig aConfig, 
			IStructureDatabase aDatabase, 
			EventCollector aCollector,
			TmpIdManager aTmpIdManager,
			BufferStream aBuffer)
	{
		itsLoader = aLoader;
		itsThreadId = aThreadId;
		itsConfig = aConfig;
		itsDatabase = aDatabase;
		itsCollector = aCollector;
		itsTmpIdManager = aTmpIdManager;
		itsStream = aBuffer;
	}
	
	public BufferStream getStream()
	{
		return itsStream;
	}
	
	public IStructureDatabase getDatabase()
	{
		return itsDatabase;
	}
	
	public EventCollector getCollector()
	{
		return itsCollector;
	}
	
	public final void echo(String aText, Object... aArgs)
	{
//		Utils.printlnIndented(itsStack.size()*2, aText, aArgs);
		Utils.println(aText, aArgs);
	}
	
	public abstract void replay();
	
	public abstract LocalsSnapshot createSnapshot(int aProbeId);
	
	public abstract int getSnapshotSeq();
	
	/**
	 * Only for partial replay. Should be called by snapshot probes.
	 * Throws an {@link EndOfStreamException} when the execution should be stopped at a probe.
	 */
	public abstract void checkSnapshotKill();
	public abstract LocalsSnapshot getSnapshotForResume();
	public abstract void registerSnapshot(LocalsSnapshot aSnapshot);
	public abstract int getStartProbe();
	
	public LocalsSnapshot createSnapshot(
			int aProbeId,
			int aIntValuesCount, 
			int aLongValuesCount, 
			int aFloatValuesCount, 
			int aDoubleValuesCount, 
			int aRefValuesCount)
	{
		LocalsSnapshot theSnapshot = createSnapshot(aProbeId);
		theSnapshot.alloc(aIntValuesCount, aLongValuesCount, aFloatValuesCount, aDoubleValuesCount, aRefValuesCount);
		return theSnapshot;
	}


	public byte getNextMessage()
	{
		processStatelessMessages();
		return nextMessage();
	}
	
	private byte nextMessage()
	{
		byte theMessage = itsStream.get();
		if (ECHO) 
		{
			if (theMessage != Message.REGISTER_OBJECT) itsMessageCount++;
			if (ECHO_FORREAL) echo("Message (%d): [#%d @%d] %s", itsThreadId, itsMessageCount, itsStream.position(), Message._NAMES[theMessage]);
			if (itsMessageCount == -1)
			{
				ECHO_FORREAL = true;
				System.out.println("ThreadReplayer.nextMessage()");
			}
		}
		return theMessage;
	}
	
	public byte peekNextMessage()
	{
		processStatelessMessages();
		return itsStream.peek();
	}
	
	public byte peekNextMessageConsumingClassloading()
	{
		while(true)
		{
			byte theMessage = peekNextMessage();
			switch(theMessage)
			{
			case Message.CLASSLOADER_ENTER:
				getNextMessage();
				replay_ClassLoader_loop();
				break;
				
			case Message.INSCOPE_CLINIT_ENTER_FROM_SCOPE:
			case Message.INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE:
			{
				getNextMessage();
				int theBehaviorId = readInt();
				dispatch_inscope(theBehaviorId, this);
				break;
			}
				
			case Message.OUTOFSCOPE_CLINIT_ENTER:
			{
				getNextMessage();
				replay_OOS_loop();
				break;
			}
			
			default: 
				return theMessage;
			}
		}
	}
	

	
	/**
	 * Whether the next message will be an exception.
	 * This method only peeks the next message.
	 */
	public boolean isExceptionNext()
	{
		return peekNextMessage() == Message.EXCEPTION;
	}
	
	private void processStatelessMessages()
	{
		while(true)
		{
			byte theMessage = itsStream.peek();
			
			switch(theMessage)
			{
			case Message.REGISTER_REFOBJECT:
				nextMessage();
				processRegisterRefObject(itsObjIdReceiver.receiveFull(itsStream), itsStream);
				break;
				
			case Message.REGISTER_REFOBJECT_DELTA:
				nextMessage();
				processRegisterRefObject(itsObjIdReceiver.receiveDelta(itsStream), itsStream);
				break;
				
			case Message.REGISTER_OBJECT: 
				nextMessage();
				processRegisterObject(itsStream); 
				break;
				
			case Message.REGISTER_OBJECT_DELTA: 
				nextMessage();
				throw new UnsupportedOperationException();
				
			case Message.REGISTER_THREAD: 
				nextMessage();
				processRegisterThread(itsStream); 
				break;
				
			case Message.REGISTER_CLASS: 
				nextMessage();
				processRegisterClass(itsStream); 
				break;
				
			case Message.REGISTER_CLASSLOADER: 
				nextMessage();
				processRegisterClassLoader(itsStream); 
				break;
				
			case Message.SYNC: 
				nextMessage();
				processSync(itsStream); 
				break;
			
			default:
				return;
			}
		}
	}
	
	/**
	 * Returns the current monitoring mode for the given method
	 * @return One of the constants in {@link MonitoringMode}.
	 */
	public int getBehaviorMonitoringMode(int aBehaviorId)
	{
		return itsMonitoringModes.get(aBehaviorId);
	}
	
	public Type getBehaviorReturnType(int aBehaviorId)
	{
		Type theType = Utils.listGet(itsBehaviorReturnTypes, aBehaviorId);
		if (theType == null)
		{
			IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
			String theSignature = theBehavior.getDescriptor();
			theType = Type.getReturnType(theSignature);
//			theType = MethodReplayerGenerator.getActualType(theType);
			Utils.listSet(itsBehaviorReturnTypes, aBehaviorId, theType);
		}
		
		return theType;
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
		
		if ("DestroyJavaVM".equals(theName))
		{
			// Temporary hack: the destroyer thread is not recorded until the end, so just skip it
			getStream().skipAll();
			throw new SkipThreadException();
		}
		
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
	
	protected void processSync(BufferStream aBuffer)
	{
		long theTimestamp = aBuffer.getLong();
		itsCollector.sync(theTimestamp);
		processSync(theTimestamp);
	}
	
	protected void processSync(long aTimestamp)
	{
	}
	
	public IntDeltaReceiver getBehIdReceiver()
	{
		return itsBehIdReceiver;
	}
	
	public LongDeltaReceiver getObjIdReceiver()
	{
		return itsObjIdReceiver;
	}
	
	public TmpIdManager getTmpIdManager()
	{
		return itsTmpIdManager;
	}
	
	public ObjectId readRef()
	{
		byte theType = itsStream.get();
		switch(theType)
		{
		case ValueType.OBJECT_ID: return new ObjectId(itsObjIdReceiver.receiveFull(itsStream));
		case ValueType.OBJECT_ID_DELTA: return new ObjectId(itsObjIdReceiver.receiveDelta(itsStream));
		case ValueType.NULL: return null;
		default: throw new RuntimeException("Not handled: "+theType); 
		}
	}
	
	public ExceptionInfo readExceptionInfo()
	{
		String theMethodName = itsStream.getString();
		String theMethodSignature = itsStream.getString();
		String theDeclaringClassSignature = itsStream.getString();
		short theBytecodeIndex = itsStream.getShort();
		ObjectId theException = readRef();
		
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

		itsLastException = new ExceptionInfo(
				theMethodName, 
				theMethodSignature, 
				theDeclaringClassSignature,
				theBehaviorId,
				theBytecodeIndex, 
				theException);
		
		return itsLastException;
	}
	
	public int readInt()
	{
		return getStream().getInt();
	}
	
	public boolean readBoolean()
	{
		return getStream().get() != 0;
	}
	
	public byte readByte()
	{
		return getStream().get();
	}
	
	public char readChar()
	{
		return getStream().getChar();
	}
	
	public short readShort()
	{
		return getStream().getShort();
	}
	
	public float readFloat()
	{
		return getStream().getFloat();
	}
	
	public long readLong()
	{
		return getStream().getLong();
	}
	
	public double readDouble()
	{
		return getStream().getDouble();
	}
	
	public ExceptionInfo getLastException()
	{
		return itsLastException;
	}
	
	/**
	 * Starts the replay of the thread.
	 */
	public void replay_main()
	{
		try
		{
			while(true)
			{
				byte m = getNextMessage();
				
				boolean theContinue = replay_OOS(m);
				if (! theContinue) break;
			}
		}
		catch(EndOfStreamException e)
		{
		}
	}
	
	/**
	 * Common loop for out-of-scope methods.
	 * Returns normally if the replayed methods returns normally, otherwise throws an exception.
	 */
	public void replay_OOS_loop()
	{
		try
		{
			while(true)
			{
				byte m = getNextMessage();
				
				boolean theContinue = replay_OOS(m);
				if (! theContinue) break;
			}
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	private boolean replay_OOS(byte aMessage)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION:
			processException();
			break;
		
		case Message.INSCOPE_BEHAVIOR_ENTER_FROM_SCOPE: 
		case Message.INSCOPE_BEHAVIOR_ENTER_FROM_OUTOFSCOPE: 
			dispatch_inscope(getBehIdReceiver().receiveFull(getStream()), this); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_SCOPE: 
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_OUTOFSCOPE: 
			dispatch_inscope(getBehIdReceiver().receiveDelta(getStream()), this); 
			break;
			
		case Message.INSCOPE_CLINIT_ENTER_FROM_SCOPE: 
		case Message.INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE: 
			dispatch_inscope(getStream().getInt(), this); 
			break;
		
//		case Message.OUTOFSCOPE_BEHAVIOR_ENTER: dispatch_OOS_loop(); break;
//		case Message.OUTOFSCOPE_CLINIT_ENTER: dispatch_OOS_loop(); break;
		case Message.CLASSLOADER_ENTER: replay_ClassLoader_loop(); break;
		
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL:
			return false;

		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION:
			throw new BehaviorExitException();
			
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
	
		return true;
	}
	
	/**
	 * Loop for classloader code.
	 * Returns normally if the replayed methods returns normally, otherwise throws an exception.
	 */
	public void replay_ClassLoader_loop()
	{
		try
		{
			while(true)
			{
				byte m = getNextMessage();
				
				boolean theContinue = replay_ClassLoader(m);
				if (! theContinue) break;
			}
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException_ClassLoader(this);
			throw new RuntimeException("This line should never be executed.");
		}

	}
	
	private boolean replay_ClassLoader(byte aMessage)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION:
			processException();
			break;
		
		case Message.INSCOPE_BEHAVIOR_ENTER_FROM_SCOPE: 
		case Message.INSCOPE_BEHAVIOR_ENTER_FROM_OUTOFSCOPE: 
			dispatch_inscope(getBehIdReceiver().receiveFull(getStream()), this); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_SCOPE: 
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_OUTOFSCOPE: 
			dispatch_inscope(getBehIdReceiver().receiveDelta(getStream()), this); 
			break;
			
		case Message.INSCOPE_CLINIT_ENTER_FROM_SCOPE: 
		case Message.INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE: 
			dispatch_inscope(getStream().getInt(), this); 
			break;
		
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER: replay_OOS_loop(); break;
		case Message.OUTOFSCOPE_CLINIT_ENTER: replay_OOS_loop(); break;
		case Message.CLASSLOADER_ENTER: replay_ClassLoader_loop(); break;
		
		case Message.CLASSLOADER_EXIT_NORMAL:
		case Message.CLASSLOADER_EXIT_EXCEPTION:
			return false;
			
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
	
		return true;
	}
	
	public static void dispatch_inscope(int aBehaviorId, ThreadReplayer aReplayer)
	{
		throw new Error("This code is supposed to be replaced by the ReplayerLoader.");
	}

	public void dispatch_OOS_V()
	{
		try
		{
			replay_OOS_loop();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public int dispatch_OOS_I()
	{
		try
		{
			replay_OOS_loop();
			return getStream().getInt();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public boolean dispatch_OOS_Z()
	{
		try
		{
			replay_OOS_loop();
			return getStream().get() != 0;
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public byte dispatch_OOS_B()
	{
		try
		{
			replay_OOS_loop();
			return getStream().get();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public short dispatch_OOS_S()
	{
		try
		{
			replay_OOS_loop();
			return getStream().getShort();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public char dispatch_OOS_C()
	{
		try
		{
			replay_OOS_loop();
			return getStream().getChar();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public long dispatch_OOS_J()
	{
		try
		{
			replay_OOS_loop();
			return getStream().getLong();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public float dispatch_OOS_F()
	{
		try
		{
			replay_OOS_loop();
			return getStream().getFloat();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public double dispatch_OOS_D()
	{
		try
		{
			replay_OOS_loop();
			return getStream().getDouble();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public ObjectId dispatch_OOS_L()
	{
		try
		{
			replay_OOS_loop();
			return readRef();
		}
		catch (BehaviorExitException e)
		{
			InScopeReplayerFrame.expectException(this);
			throw new RuntimeException("This line should never be executed.");
		}
	}
	
	public ObjectId processException()
	{
		ExceptionInfo theInfo = readExceptionInfo();
		// TODO: register exception
		return theInfo.exception;
	}

	public static Exception createRtEx(int aArg, String aMessage)
	{
		return new RuntimeException(aMessage+": "+aArg);
	}
	
	public static Exception createRtEx(String aMessage)
	{
		return new RuntimeException(aMessage);
	}
	
	public static Exception createUnsupportedEx()
	{
		return new UnsupportedOperationException();
	}

	public static Exception createUnsupportedEx(String aMessage)
	{
		return new UnsupportedOperationException(aMessage);
	}
	
	/**
	 * Expects a behavior enter message (skipping classloading).
	 * Returns the behavior id if the message is for an in scope behavior,
	 * -1 otherwise.
	 */
	public int getDispatchTarget()
	{
		byte theMessage = peekNextMessageConsumingClassloading();
		getNextMessage();

		switch(theMessage)
		{
		case Message.INSCOPE_BEHAVIOR_ENTER_FROM_SCOPE: 
		case Message.INSCOPE_BEHAVIOR_ENTER_FROM_OUTOFSCOPE: 
			return getBehIdReceiver().receiveFull(getStream()); 
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_SCOPE: 
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA_FROM_OUTOFSCOPE: 
			return getBehIdReceiver().receiveDelta(getStream()); 
			
		case Message.INSCOPE_CLINIT_ENTER_FROM_SCOPE: 
		case Message.INSCOPE_CLINIT_ENTER_FROM_OUTOFSCOPE: 
			return getStream().getInt(); 
		
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
			return -1;
			
		case Message.OUTOFSCOPE_CLINIT_ENTER:
			return -1;
			
		default:
			throw new RuntimeException("Unexpected message: "+Message._NAMES[theMessage]);
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
