/*
 * Created on Apr 30, 2009
 */
package java.tod;

import java.tod.io._IO;
import java.tod.transport.IOThread;
import java.tod.transport.ObjectEncoder;
import java.tod.transport.IOThread.StringPacket;
import java.tod.transport.IOThread.ThreadPacket;
import java.tod.util.BitStack;
import java.tod.util.IntDeltaSender;
import java.tod.util.LongDeltaSender;
import java.tod.util._StringBuilder;

import tod2.access.TODAccessor;
import tod2.agent.AgentDebugFlags;
import tod2.agent.Command;
import tod2.agent.Message;
import tod2.agent.ValueType;
import tod2.agent.io._ByteBuffer;
import tod2.agent.io._GrowingByteBuffer;

/**
 * Per-thread data managed by the {@link EventCollector}.
 * Contains information about the thread, a stack that permits to
 * track whether the thread is currently in instrumented code,
 * and methods to build the packets to be sent to the database.
 * @author gpothier
 */
public final class ThreadData 
{
	/**
	 * Internal thread id.
	 * These are different than JVM thread ids, which can potentially
	 * use the whole 64 bits range. Internal ids are sequential.
	 */
	private final int itsId;
	
	private boolean itsIgnoringExceptions = false;
	private boolean itsProcessingExceptions = false;
	
	/**
	 * This flag permits to avoid reentrancy.
	 */
	private boolean itsInCflow = false;

	/**
	 * The top of this stack indicates if the thread is executing in-scope or
	 * out-of-scope code.
	 */
	private BitStack itsScopeStack = new BitStack();
	
	/**
	 * Size of the buffer store.
	 */
	private static final int BUFFER_SIZE = 4096;
	
	/**
	 * Size of the largest message that can be built between calls to {@link #commitBuffer()}.
	 * When the buffer grows larger than {@link #BUFFER_SIZE}-{@link #MAX_MESSAGE_SIZE}, its content
	 * is transferred to the packet buffer.
	 */
	private static final int MAX_MESSAGE_SIZE = 256;
	
	private static final byte TRUE = (byte) 1;
	private static final byte FALSE = (byte) 0;
	
	/**
	 * The IO thread that actually sends packets. 
	 */
	private final IOThread itsIOThread;
	
	/**
	 * In-construction packet. Contains the backing array for {@link #itsBuffer}
	 */
	private ThreadPacket itsPacket;
	
	/**
	 * Buffer to prepare (small) packets
	 */
	private _ByteBuffer itsBuffer;
	
	private RegisteredObjectsStack itsRegisteredObjectsStack = new RegisteredObjectsStack();
	private RegisteredRefObjectsStack itsRegisteredRefObjectsStack = new RegisteredRefObjectsStack();
	
	private int itsLastSentLTimestamp = 0;
	private int itsLastTracedMethodsVersion = 0;

	private int[] itsEvCount = new int[Message.MSG_COUNT];
	private long[] itsEvData = new long[Message.MSG_COUNT];
	private int itsLastBufferPos = -1;
	private int itsExpectedArgsCount = -1;
	private int itsAccountToCharge = -1;
	
	private int itsMessageCount = 0;
	
	private LongDeltaSender itsObjIdSender = new LongDeltaSender();
	private IntDeltaSender itsBehIdSender = new IntDeltaSender();
	
	public ThreadData(int aId, IOThread aIOThread)
	{
		itsId = aId;
		itsIOThread = aIOThread;
		resetBuffer();
		itsIOThread.registerThreadData(this);
		pushOutOfScope(); // Always start out of scope.
	}
	
	/**
	 * Reentrancy control.
	 */
	private boolean enter()
	{
		if (itsInCflow) return true;
		itsInCflow = true;
		return false;
	}
	
	private void exit()
	{
		itsInCflow = false;
	}
	
	private void resetBuffer()
	{
		byte[] theData = new byte[BUFFER_SIZE]; 
		
		itsPacket = new ThreadPacket();
		itsPacket.set(getId(), theData, ThreadPacket.RECYCLE_QUEUE_STANDARD);
		
		itsBuffer = _ByteBuffer.wrap(theData);
	}
	
	public int getId()
	{
		return itsId;
	}
	
	private long getObjectId(Object aObject)
	{
		try
		{
			return TODAccessor.getObjectId(aObject);
		}
		catch (Throwable t)
		{
			_StringBuilder b = new _StringBuilder();
			b.append("Cannot get id of instance of ");
			b.append(aObject.getClass().getName());
			b.append(": ");
			b.append(t.getMessage());
			_IO.err(b.toString());
			return 0;
		}
	}
	
	/**
	 * Returns true if the thread is currently executing instrumented code.
	 */
	public boolean isInScope()
	{
		return itsScopeStack.peek();
	}
	
	private void pushInScope()
	{
		itsScopeStack.push(true);
		if (AgentDebugFlags.EVENT_LOG) echoPushScope(true);
	}
	
	private void pushOutOfScope()
	{
		itsScopeStack.push(false);
		if (AgentDebugFlags.EVENT_LOG) echoPushScope(false);
	}
	
	private boolean popScope()
	{
		boolean thePop = itsScopeStack.pop();
		if (AgentDebugFlags.EVENT_LOG) echoPopScope(thePop);
		return thePop;
	}
	
	public void sendMessageType(_ByteBuffer aBuffer, byte aType) 
	{
		aBuffer.put(aType);
	}
	
	public void echoMessageType(byte aMessage, long aArg)
	{
		echoMessageType(aMessage, aArg, -1);
	}
	
	public void echoMessageType_NoIncCount(byte aMessage, long aArg)
	{
		echoMessageType_NoIncCount(aMessage, aArg, -1);
	}
	
	public void echoMessageType(byte aMessage, long aArg1, long aArg2)
	{
		itsMessageCount++;
		
		_StringBuilder theBuilder = new _StringBuilder();
		theBuilder.append(getId());
		theBuilder.append(" #");
		theBuilder.append(itsMessageCount);
		theBuilder.append(": ");
		theBuilder.append(Message._NAMES[aMessage]);
		if (aArg1 >= 0 || aArg2 >= 0)
		{
			theBuilder.append(" (");
			if (aArg1 >= 0) theBuilder.append(aArg1);
			if (aArg1 >= 0 && aArg2 >= 0) theBuilder.append(' ');
			if (aArg2 >= 0) theBuilder.append(aArg2);
			theBuilder.append(")");
		}
		
		_IO.out(theBuilder.toString());		
	}
	
	public void echoMessageType_NoIncCount(byte aMessage, long aArg1, long aArg2)
	{
		_StringBuilder theBuilder = new _StringBuilder();
		theBuilder.append(getId());
		theBuilder.append(": ");
		theBuilder.append(Message._NAMES[aMessage]);
		if (aArg1 >= 0 || aArg2 >= 0)
		{
			theBuilder.append(" (");
			if (aArg1 >= 0) theBuilder.append(aArg1);
			if (aArg1 >= 0 && aArg2 >= 0) theBuilder.append(' ');
			if (aArg2 >= 0) theBuilder.append(aArg2);
			theBuilder.append(")");
		}
		_IO.out(theBuilder.toString());		
	}
	
	public void echoPushScope(boolean aInScope)
	{
		_StringBuilder theBuilder = new _StringBuilder();
		theBuilder.append(getId());
		theBuilder.append(" >>>");
		theBuilder.append(aInScope ? "In" : "Out");
		_IO.out(theBuilder.toString());
	}

	public void echoPopScope(boolean aInScope)
	{
		_StringBuilder theBuilder = new _StringBuilder();
		theBuilder.append(getId());
		theBuilder.append(" <<<");
		theBuilder.append(aInScope ? "In" : "Out");
		_IO.out(theBuilder.toString());
	}
	
	private static void sendValueType(_ByteBuffer aBuffer, byte aType) 
	{
		aBuffer.put(aType);
	}
	
	private static void sendCommand(_ByteBuffer aBuffer, byte aCommand) 
	{
		aBuffer.put(aCommand);
	}
	
	private void msgStart(int aAccount, int aExpectedArgsCount)
	{
		if (!AgentDebugFlags.COLLECT_PROFILE) return;
		if (itsExpectedArgsCount != -1) 
		{
			_IO.err("Illegal state in msgStart");
		}
		itsLastBufferPos = itsBuffer.position();
		itsAccountToCharge = aAccount;
		itsExpectedArgsCount = aExpectedArgsCount;
	}
	
	private void msgStop()
	{
		if (!AgentDebugFlags.COLLECT_PROFILE) return;
		if (itsExpectedArgsCount == -1) 
		{
			_IO.err("Illegal state in msgStop");
		}
		if (itsExpectedArgsCount-- == 0)
		{
			itsEvData[itsAccountToCharge] += itsBuffer.position() - itsLastBufferPos;
			itsEvCount[itsAccountToCharge] ++;
			itsAccountToCharge = -1;
		}
	}
	
	public void printStats()
	{
		if (! AgentDebugFlags.COLLECT_PROFILE) return;
		_StringBuilder b = new _StringBuilder();
		
		long theTotalData = 0;
		
		for(int i = Message.FIELD_READ;i<Message.MSG_COUNT;i++)
		{
			if (i >= 20 && itsEvCount[i] == 0 && itsEvData[i] == 0) continue;
			b.append("[ThreadData] ");
			b.append(Message._NAMES[i]);
			b.append(": ");
			b.append(itsEvCount[i]);
			b.append(" - ");
			b.append(itsEvData[i]);
			b.append("\n");
			
			theTotalData += itsEvData[i];
		}
		
		b.append("[ThreadData] Total: ");
		b.append(theTotalData);
		b.append("\n");
		
		b.append("[ThreadData] Object ids: ");
		b.append(itsObjIdSender.toString());
		b.append("\n");
		
		b.append("[ThreadData] Object ids cache access: ");
		b.append(ObjectIdentity.itsObjIdCacheAccess);
		b.append(" - hits: ");
		b.append(ObjectIdentity.itsObjIdCacheHit);
		if (ObjectIdentity.itsObjIdCacheAccess != 0)
		{
			b.append(" - ");
			b.append(ObjectIdentity.itsObjIdCacheHit*100/ObjectIdentity.itsObjIdCacheAccess);
			b.append("%");
		}
		b.append("\n");
		
		b.append("[ThreadData] Behavior ids: ");
		b.append(itsBehIdSender.toString());
		b.append("\n");

		_IO.out(b.toString());
	}
	
	
	/**
	 * Flushes the buffer if it reached a certain size.
	 */
	private void commitBuffer() 
	{
		if (itsBuffer.remaining() <= MAX_MESSAGE_SIZE) flushBuffer();
	}
	
	/**
	 * Copy the temporary buffer to the output stream, and resets the buffer.
	 */
	public void flushBuffer()
	{
		ThreadPacket thePacket = itsPacket;
		thePacket.offset = 0;
		thePacket.length = itsBuffer.position();
		
		itsIOThread.pushPacket(thePacket);
		
		thePacket = itsIOThread.getFreeThreadPacket(ThreadPacket.RECYCLE_QUEUE_STANDARD);
		if (thePacket != null)
		{
			thePacket.threadId = getId();
			itsBuffer.clear(thePacket.data);
			itsPacket = thePacket;
		}
		else resetBuffer();
	}
	
	/**
	 * Send a sync event if the current timestamp has changed since the last sync.
	 */
	private void checkTimestamp()
	{
		int theLTimestamp = Timestamper.lt;
		if (theLTimestamp != itsLastSentLTimestamp)
		{
			sendSync(Timestamper.t);
			itsLastSentLTimestamp = theLTimestamp;
		}
	}
	
	/**
	 * Checks if the current version of the {@link TracedMethods} structure
	 * is the same as the last observed; if not, send a message to inform the database.
	 */
	public void checkTracedMethodsVersion()
	{
		int theCurrentVersion = TracedMethods.version;
		if (itsLastTracedMethodsVersion != theCurrentVersion)
		{
			itsLastTracedMethodsVersion = theCurrentVersion;
			
			msgStart(Message.TRACEDMETHODS_VERSION, 0);
			if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.TRACEDMETHODS_VERSION, theCurrentVersion); 
			sendMessageType(itsBuffer, Message.TRACEDMETHODS_VERSION);
			itsBuffer.putInt(theCurrentVersion);
			msgStop();
			
			commitBuffer();
		}
	}
	
	/**
	 * Sends a synchronization message.
	 */
	private void sendSync(long aTimestamp)
	{
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.SYNC, aTimestamp); 
		sendMessageType(itsBuffer, Message.SYNC);
		itsBuffer.putLong(aTimestamp);
		commitBuffer();
	}
	
	/**
	 * A field has just been read. Value expected next
	 */
	public void evFieldRead()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.FIELD_READ, 1);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.FIELD_READ, -1); 
		sendMessageType(itsBuffer, Message.FIELD_READ);
		msgStop();
		
		exit();
	}
	
	/**
	 * A field has just been read, and the value is the same as in the previous read.
	 */
	public void evFieldRead_Same()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();

		msgStart(Message.FIELD_READ_SAME, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.FIELD_READ_SAME, -1); 
		sendMessageType(itsBuffer, Message.FIELD_READ_SAME);
		msgStop();
		
		exit();
	}
	
	/**
	 * An array slot has just been read. Value expected next.
	 */
	public void evArrayRead()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();

		msgStart(Message.ARRAY_READ, 1);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.ARRAY_READ, -1); 
		sendMessageType(itsBuffer, Message.ARRAY_READ);
		msgStop();
		
		exit();
	}
	
	public void evArrayLength(int aLength)
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();

		msgStart(Message.ARRAY_LENGTH, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.ARRAY_LENGTH, aLength); 
		sendMessageType(itsBuffer, Message.ARRAY_LENGTH);
		itsBuffer.putInt(aLength);
		msgStop();
		
		exit();
	}
	
	public void evNewArray(Object aValue)
	{
		if (enter()) return;
		
		commitBuffer();

		msgStart(Message.NEW_ARRAY, 0);
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.NEW_ARRAY, -1); 
		sendMessageType(itsBuffer, Message.NEW_ARRAY);
		sendValue(itsBuffer, aValue);
		msgStop();
		
		sendRegisteredObjects();
		
		exit();
	}
	
	public void evCst(Object aValue)
	{
		if (enter()) return;
		
		commitBuffer();

		msgStart(Message.CONSTANT, 0);
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.CONSTANT, -1); 
		sendMessageType(itsBuffer, Message.CONSTANT);
		sendValue(itsBuffer, aValue);
		msgStop();
		
		sendRegisteredObjects();
		
		exit();
	}
	
	public void evInstanceOfOutcome(int aOutcome)
	{
		if (enter()) return;
		
		commitBuffer();

		msgStart(Message.INSTANCEOF_OUTCOME, 0);
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.INSTANCEOF_OUTCOME, -1); 
		sendMessageType(itsBuffer, Message.INSTANCEOF_OUTCOME);
		itsBuffer.put((byte) aOutcome);
		msgStop();
		
		sendRegisteredObjects();
		
		exit();
	}
	

	public void evObjectInitialized(Object aValue)
	{
		if (enter()) return;
				
		sendRegisteredObjects();
		commitBuffer();

		msgStart(Message.OBJECT_INITIALIZED, 0);

		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.OBJECT_INITIALIZED, -1); 
		sendMessageType(itsBuffer, Message.OBJECT_INITIALIZED);
		sendValue(itsBuffer, aValue);

		msgStop();
		
		exit();
	}
	
	public void evExceptionGenerated(
			String aMethodName,
			String aMethodSignature,
			String aMethodDeclaringClassSignature, 
			int aBytecodeIndex,
			Throwable aException) 
	{
		if (enter()) return;
		
		// Exception messages can be larger than other messages so check we have enough room
		int theMsgLen = aMethodName.length()*2 
				+ aMethodSignature.length()*2
				+ aMethodDeclaringClassSignature.length()*2
				+ 256;
		
		if (itsBuffer.remaining() <= theMsgLen) flushBuffer();
		
		msgStart(Message.EXCEPTION, 0);

		checkTimestamp();
//		int p0 = itsBuffer.position();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.EXCEPTION, -1); 
		sendMessageType(itsBuffer, Message.EXCEPTION);

		itsBuffer.putString(aMethodName);
		itsBuffer.putString(aMethodSignature);
		itsBuffer.putString(aMethodDeclaringClassSignature);
		itsBuffer.putShort((short) aBytecodeIndex);
		sendValue(itsBuffer, aException);
//		int p1 = itsBuffer.position();
		
//		_StringBuilder theBuilder = new _StringBuilder();
//		theBuilder.append("Exception data: ");
//		theBuilder.append(p0);
//		theBuilder.append(" ");
//		theBuilder.append(p1);
//		_IO.out(theBuilder.toString());
		
		msgStop();
		
		commitBuffer();

		// There might be harmless recursive exceptions here, ignore
		// Note: this is only for exception events, other events don't need this.
		ExceptionGeneratedReceiver.setIgnoreExceptions(true);
		sendRegisteredObjects();
		ExceptionGeneratedReceiver.setIgnoreExceptions(false);
		
		exit();
	}
	
	public void evHandlerReached(int aLocation)
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.HANDLER_REACHED, 0);
		
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.HANDLER_REACHED, aLocation); 
		sendMessageType(itsBuffer, Message.HANDLER_REACHED);
		itsBuffer.putInt(aLocation);
		
		msgStop();
		
		exit();
	}
	
	public void evInScopeBehaviorEnter(int aBehaviorId)
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		checkTracedMethodsVersion();
		commitBuffer();
		
		msgStart(Message.INSCOPE_BEHAVIOR_ENTER, 0);

		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.INSCOPE_BEHAVIOR_ENTER, aBehaviorId); 
		itsBehIdSender.send(itsBuffer, aBehaviorId, Message.INSCOPE_BEHAVIOR_ENTER_DELTA, Message.INSCOPE_BEHAVIOR_ENTER);

		msgStop();
		pushInScope();
		exit();
	}

	public void evInScopeClinitEnter(int aBehaviorId)
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		checkTracedMethodsVersion();
		commitBuffer();
		
		msgStart(Message.INSCOPE_CLINIT_ENTER, 0);
		
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.INSCOPE_CLINIT_ENTER, aBehaviorId);
		sendMessageType(itsBuffer, Message.INSCOPE_CLINIT_ENTER);
		itsBuffer.putInt(aBehaviorId);
		
		msgStop();
		pushInScope();
		exit();
	}
	
	public void evClassLoaderEnter()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.CLASSLOADER_ENTER, 0);
		
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.CLASSLOADER_ENTER, -1);
		sendMessageType(itsBuffer, Message.CLASSLOADER_ENTER);

		msgStop();
		pushOutOfScope();
		exit();
	}
	
	public void evClassLoaderExit()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		checkTracedMethodsVersion();
		commitBuffer();
		
		msgStart(Message.CLASSLOADER_EXIT, 0);
		
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.CLASSLOADER_EXIT, -1);
		sendMessageType(itsBuffer, Message.CLASSLOADER_EXIT);
		
		msgStop();
		if (popScope()) throw new TODError("Unexpected scope state");
		exit();
	}
	
	/**
	 * Indicates that behavior enter arguments are going to be sent next.
	 * Must be sent right after a behavior enter, if needed.
	 * Use the sendValue_Xxxx methods to send the arguments.
	 * For constructors, the target (or this) argument is sent
	 * after the constructor chaining is finished (see {@link #sendConstructorTarget(Object)}).
	 */
	public void sendBehaviorEnterArgs(int aCount)
	{
		if (enter()) return;
		
		msgStart(Message.BEHAVIOR_ENTER_ARGS, aCount);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.BEHAVIOR_ENTER_ARGS, -1); 
		sendMessageType(itsBuffer, Message.BEHAVIOR_ENTER_ARGS);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Boolean(boolean aValue)
	{
		if (enter()) return;
		
		itsBuffer.put(aValue ? TRUE : FALSE);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Byte(byte aValue)
	{
		if (enter()) return;
		
		itsBuffer.put(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Char(char aValue)
	{
		if (enter()) return;
		
		itsBuffer.putChar(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Short(short aValue)
	{
		if (enter()) return;
		
		itsBuffer.putShort(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Int(int aValue)
	{
		if (enter()) return;
		
		itsBuffer.putInt(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Long(long aValue)
	{
		if (enter()) return;
		
		itsBuffer.putLong(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Float(float aValue)
	{
		if (enter()) return;
		
		itsBuffer.putFloat(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Double(double aValue)
	{
		if (enter()) return;
		
		itsBuffer.putDouble(aValue);
		msgStop();
		
		exit();
	}
	
	public void sendValue_Ref(Object aValue)
	{
		if (enter()) return;
		
		sendValue(itsBuffer, aValue);
		msgStop();
		
		exit();
	}
	
	public void sendConstructorTarget(Object aTarget)
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();

		msgStart(Message.CONSTRUCTOR_TARGET, 0);
		
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.CONSTRUCTOR_TARGET, -1); 
		sendMessageType(itsBuffer, Message.CONSTRUCTOR_TARGET);
		sendValue(itsBuffer, aTarget);
		
		msgStop();
		exit();
	}
	
	public void evInScopeBehaviorExit_Normal()
	{
		if (enter()) return;
		
		msgStart(Message.INSCOPE_BEHAVIOR_EXIT_NORMAL, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType_NoIncCount(Message.INSCOPE_BEHAVIOR_EXIT_NORMAL, -1); 
		msgStop();
		
		if (! popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	
	public void evInScopeBehaviorExit_Exception()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION, -1); 
		sendMessageType(itsBuffer, Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION);
		msgStop();
		
		if (! popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	
	/**
	 * Entering into an out-of-scope behavior (which has envelope only instrumentation).
	 */
	public void evOutOfScopeBehaviorEnter()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		checkTracedMethodsVersion();
		commitBuffer();
		
		msgStart(Message.OUTOFSCOPE_BEHAVIOR_ENTER, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.OUTOFSCOPE_BEHAVIOR_ENTER, -1); 
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_ENTER);
		msgStop();
		
		pushOutOfScope();
		
		exit();
	}
	
	public void evOutOfScopeClinitEnter()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		checkTracedMethodsVersion();
		commitBuffer();
		
		msgStart(Message.OUTOFSCOPE_CLINIT_ENTER, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.OUTOFSCOPE_CLINIT_ENTER, -1); 
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_CLINIT_ENTER);
		msgStop();
		
		pushOutOfScope();
		
		exit();
	}
	
	/**
	 * Exiting normally from an out-of-scope non-void behavior (which has envelope only instrumentation).
	 */
	public void evOutOfScopeBehaviorExit_Normal()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL, -1); 
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL);
		msgStop();
		
		if (popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	
	/**
	 * Indicates that a behavior return value is going to be sent next.
	 */
	public void sendOutOfScopeBehaviorResult()
	{
		if (enter()) return;
		
		msgStart(Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT, 1);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT, -1); 
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT);
		msgStop();
		
		exit();
	}
	
	/**
	 * Exiting with an exception from an out-of-scope behavior (which has enveloppe only instrumentation).
	 */
	public void evOutOfScopeBehaviorExit_Exception()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION, -1); 
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION);
		msgStop();
		
		if (popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	

	/**
	 * Before an unmonitored behavior call.
	 */
	public void evUnmonitoredBehaviorCall()
	{
		if (enter()) return;
		
		checkTracedMethodsVersion();

		msgStart(Message.UNMONITORED_BEHAVIOR_CALL, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType_NoIncCount(Message.UNMONITORED_BEHAVIOR_CALL, -1); 
		msgStop();
		pushOutOfScope();
		
		exit();
	}
	
	/**
	 * After an unmonitored behavior call.
	 * The result value should be sent immediately after.
	 */
	public void evUnmonitoredBehaviorResultNonVoid()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.UNMONITORED_BEHAVIOR_CALL_RESULT, 1);
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.UNMONITORED_BEHAVIOR_CALL_RESULT, -1); 
		sendMessageType(itsBuffer, Message.UNMONITORED_BEHAVIOR_CALL_RESULT);
		msgStop();
		
		if (popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	
	/**
	 * After an unmonitored behavior call.
	 */
	public void evUnmonitoredBehaviorResultVoid()
	{
		if (enter()) return;
		
		sendRegisteredObjects();
		commitBuffer();
		
		msgStart(Message.UNMONITORED_BEHAVIOR_CALL_RESULT, 0);
		checkTimestamp();
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.UNMONITORED_BEHAVIOR_CALL_RESULT, -1); 
		sendMessageType(itsBuffer, Message.UNMONITORED_BEHAVIOR_CALL_RESULT);
		msgStop();
		
		if (popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	
	/**
	 * An unmonitored behavior call threw an exception.
	 */
	public void evUnmonitoredBehaviorException()
	{
		if (enter()) return;
		
		msgStart(Message.UNMONITORED_BEHAVIOR_CALL_EXCEPTION, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType_NoIncCount(Message.UNMONITORED_BEHAVIOR_CALL_EXCEPTION, -1); 
		msgStop();
		if (popScope()) throw new TODError("Unexpected scope state");
		
		exit();
	}
	
	/**
	 * Determines if the given object should be sent by value.
	 */
	private boolean shouldSendByValue(Object aObject)
	{
		return (aObject instanceof String) || (aObject instanceof Throwable) 
			|| (aObject instanceof Number) || (aObject instanceof Boolean);
	}

	/**
	 * Determines if the given object is indexable (see ILogCollector.register)
	 */
	private boolean isIndexable(Object aObject)
	{
		return (aObject instanceof String);
	}
	
	public void sendThread(
			long aJVMThreadId,
			char[] aName) 
	{
		if (enter()) return;
		
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.REGISTER_THREAD, aJVMThreadId); 
		sendMessageType(itsBuffer, Message.REGISTER_THREAD);

		itsBuffer.putLong(aJVMThreadId);
		itsBuffer.putInt(aName.length);
		itsBuffer.putChars(aName, 0, aName.length);

		commitBuffer();

		sendRegisteredObjects();
		
		exit();
	}

	public void sendClear() 
	{
		if (enter()) return;
		
		sendCommand(itsBuffer, Command.DBCMD_CLEAR);
		commitBuffer();
		
		exit();
	}

	public void sendFlush() 
	{
		if (enter()) return;
		
		sendCommand(itsBuffer, Command.DBCMD_FLUSH);
		commitBuffer();
		
		exit();
	}
	
	public void sendEnd() 
	{
		if (enter()) return;		
		
		sendCommand(itsBuffer, Command.DBCMD_END);
		commitBuffer();
		
		exit();
	}
	
	public void sendEvCaptureEnabled(boolean aValue)
	{
		if (enter()) return;
		
		sendCommand(itsBuffer, Command.DBEV_CAPTURE_ENABLED);
		itsBuffer.put(aValue ? (byte) 1 : (byte) 0);
		commitBuffer();
		
		exit();
	}
	
	private void sendValue(_ByteBuffer aBuffer, Object aValue) 
	{
		if (aValue == null) sendValueType(aBuffer, ValueType.NULL);
		else if (shouldSendByValue(aValue)) sendObjectByValue(aBuffer, aValue);
		else sendObjectByRef(aBuffer, aValue);
	}
	
	/**
	 * Sends an object by value. This method checks if the object already had an
	 * id. If it didn't, it is placed on the registered objects stack so that
	 * its value is sent when {@link #sendRegisteredObjects()} is called. In any
	 * case, the id of the object is sent.
	 */
	private void sendObjectByValue(_ByteBuffer aBuffer, Object aObject) 
	{
		long theObjectId = getObjectId(aObject);
		assert theObjectId != 0;
		
		if (theObjectId < 0)
		{
			// First time this object appears, register it.
			theObjectId = -theObjectId;
			itsRegisteredObjectsStack.push(theObjectId, aObject);
		}

		// Send object id
		itsObjIdSender.send(aBuffer, theObjectId, ValueType.OBJECT_ID_DELTA, ValueType.OBJECT_ID);
	}

	/**
	 * Sends an object by reference. This method checks if the object already had an
	 * id. If it didn't, it is placed on the registered refs stack so that
	 * its type is sent when {@link #sendRegisteredObjects()} is called. In any
	 * case, the id of the object is sent.
	 */
	private void sendObjectByRef(_ByteBuffer aBuffer, Object aObject) 
	{
		long theObjectId = getObjectId(aObject);
		assert theObjectId != 0;
		
		if (theObjectId < 0)
		{
			// First time this object appears, register its type
			theObjectId = -theObjectId;
			itsRegisteredRefObjectsStack.push(aObject, theObjectId);
		}

		itsObjIdSender.send(aBuffer, theObjectId, ValueType.OBJECT_ID_DELTA, ValueType.OBJECT_ID);
	}

	/**
	 * Sends all pending registered objects.
	 */
	private void sendRegisteredObjects() 
	{
		while (!itsRegisteredObjectsStack.isEmpty())
		{
			ObjectEntry theEntry = itsRegisteredObjectsStack.pop();
			sendRegisteredObject(theEntry.id, theEntry.object);
		}
		
		while (!itsRegisteredRefObjectsStack.isEmpty())
		{
			RefObjectEntry theEntry = itsRegisteredRefObjectsStack.pop();
			sendRegisteredRefObject(theEntry.object, theEntry.id);
		}

	}

	private void sendRegisteredObject(long aId, Object aObject) 
	{
		if (aObject.getClass() == String.class)
		{
			// Special case for strings, for speed
			itsIOThread.pushPacket(new StringPacket(aId, (String) aObject));
			return;
		}
		
		ThreadPacket thePacket = itsIOThread.getFreeThreadPacket(ThreadPacket.RECYCLE_QUEUE_OTHER);
		if (thePacket == null) 
		{
			thePacket = new ThreadPacket();
			thePacket.data = new byte[128];
			thePacket.recycleQueue = ThreadPacket.RECYCLE_QUEUE_OTHER;
		}
		
		thePacket.threadId = getId();
		
		final int HEADER_SIZE = 14; // msg type (1), size (4), id (8), indexable (1)
		
		// We use a different buffer here because the packet might be huge.
		_GrowingByteBuffer theBuffer = _GrowingByteBuffer.wrap(thePacket.data);
		theBuffer.position(HEADER_SIZE); // Leave room for header
		
		ObjectEncoder.encode(ObjectValueFactory.convert(aObject), theBuffer);
		
		int thePacketSize = theBuffer.position(); 
		int theDataSize = thePacketSize-HEADER_SIZE; 
	
		theBuffer.position(0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType_NoIncCount(Message.REGISTER_OBJECT, aId, theDataSize); 
		sendMessageType(theBuffer, Message.REGISTER_OBJECT);
		theBuffer.putInt(theDataSize); 
		
		theBuffer.putLong(aId);
		theBuffer.put(isIndexable(aObject) ? (byte) 1 : (byte) 0);
		
		thePacket.offset = 0;
		thePacket.length = thePacketSize;
		thePacket.data = theBuffer.array();
		itsIOThread.pushPacket(thePacket);
		
		if (AgentDebugFlags.COLLECT_PROFILE)
		{
			itsEvCount[Message.REGISTER_OBJECT]++;
			itsEvData[Message.REGISTER_OBJECT] += thePacketSize;
		}
	}
	
	private void sendRegisteredRefObject(Object aObject, long aId) 
	{
		Class theClass = aObject.getClass();
		
		// That must stay before we start using the buffer
		if (theClass == Class.class)
		{
			// We have to register it explicitly now otherwise the system thinks it
			// is already registered because it has an id.
			Class<?> theClassValue = (Class<?>) aObject;
			sendRegisterClass(TODAccessor.getClassId(theClassValue), theClassValue);
		}
		int theClassId = getClassId(theClass); 
		
		msgStart(Message.REGISTER_REFOBJECT, 0);
		
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.REGISTER_REFOBJECT, aId); 
		itsObjIdSender.send(itsBuffer, aId, Message.REGISTER_REFOBJECT_DELTA, Message.REGISTER_REFOBJECT);
		itsBuffer.putInt(theClassId);
		
		msgStop();
		
		commitBuffer();

	}
	
	private int getClassId(Class<?> aClass) 
	{
		int theId = TODAccessor.getClassId(aClass);
		assert theId != 0;
		
		if (theId < 0)
		{
			theId = -theId;
			sendRegisterClass(theId, aClass);
		}
		
		return theId;
	}
	
	private void sendRegisterClass(int aClassId, Class<?> aClass) 
	{
		// That must stay before we start using the buffer
		long theLoaderId = getClassLoaderId(aClass.getClassLoader());
		
		msgStart(Message.REGISTER_CLASS, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.REGISTER_CLASS, aClassId); 
		sendMessageType(itsBuffer, Message.REGISTER_CLASS);
		
		itsBuffer.putInt(aClassId);
		itsBuffer.putLong(theLoaderId);
		
		String theName = aClass.getName();
		itsBuffer.putString(theName);
		
		msgStop();
		commitBuffer();
	}
	
	private long getClassLoaderId(ClassLoader aLoader) 
	{
		if (aLoader == null) return 0;
		
		long theId = getObjectId(aLoader);
		assert theId != 0;
		
		if (theId < 0)
		{
			theId = -theId;
			sendRegisterClassLoader(theId, aLoader);
		}
		
		return theId;
	}
	
	private void sendRegisterClassLoader(long aLoaderId, ClassLoader aLoader) 
	{
		// That must stay before we start using the buffer
		long theLoaderClassId = getClassId(aLoader.getClass());
		
		msgStart(Message.REGISTER_CLASSLOADER, 0);
		if (AgentDebugFlags.EVENT_LOG) echoMessageType(Message.REGISTER_CLASSLOADER, aLoaderId); 
		sendMessageType(itsBuffer, Message.REGISTER_CLASSLOADER);
		
		itsBuffer.putLong(aLoaderId);
		itsBuffer.putLong(theLoaderClassId);
		
		msgStop();
		
		commitBuffer();
	}
	
	public boolean isIgnoringExceptions()
	{
		return itsIgnoringExceptions;
	}

	public void setIgnoringExceptions(boolean aIgnoringExceptions)
	{
		itsIgnoringExceptions = aIgnoringExceptions;
	}

	public boolean isProcessingExceptions()
	{
		return itsProcessingExceptions;
	}

	public void setProcessingExceptions(boolean aProcessingExceptions)
	{
		itsProcessingExceptions = aProcessingExceptions;
	}

	/**
	 * A stack of objects pending to be sent.
	 * 
	 * @author gpothier
	 */
	private static class RegisteredObjectsStack
	{
		/**
		 * List of registered objects that must be sent. Note: There is space
		 * for a hard-coded number of entries that should "be enough for
		 * everybody".
		 */
		private final ObjectEntry[] itsObjects = new ObjectEntry[1024];

		/**
		 * Number of entries in {@link #itsObjects}.
		 */
		private int itsSize = 0;

		public RegisteredObjectsStack()
		{
			for (int i = 0; i < itsObjects.length; i++)
			{
				itsObjects[i] = new ObjectEntry();
			}
		}

		public void push(long aId, Object aObject)
		{
			//TODO remove this 
			if (itsSize>= itsObjects.length) {
				_IO.out("---------TOD---------WARNING");
				for (int theI = 0; theI < itsObjects.length; theI++)
					_IO.out(itsObjects[theI].object.getClass() +" ");
			}
			
			itsObjects[itsSize++].set(aId, aObject);
		}

		public boolean isEmpty()
		{
			return itsSize == 0;
		}

		public ObjectEntry pop()
		{
			return itsObjects[--itsSize];
		}
	}

	private static class ObjectEntry
	{
		public long id;
		public Object object;

		public void set(long aId, Object aObject)
		{
			id = aId;
			object = aObject;
		}
	}

	/**
	 * A stack of newly created objects whose type must be registered
	 * @author gpothier
	 */
	private static class RegisteredRefObjectsStack
	{
		/**
		 * List of registered objects that must be sent.
		 */
		private final RefObjectEntry[] itsObjects = new RefObjectEntry[1024];

		/**
		 * Number of entries in {@link #itsObjects}.
		 */
		private int itsSize = 0;

		public RegisteredRefObjectsStack()
		{
			for (int i = 0; i < itsObjects.length; i++)
			{
				itsObjects[i] = new RefObjectEntry();
			}
		}

		public void push(Object aObject, long aId)
		{
			itsObjects[itsSize++].set(aObject, aId);
		}

		public boolean isEmpty()
		{
			return itsSize == 0;
		}

		public RefObjectEntry pop()
		{
			return itsObjects[--itsSize];
		}
	}

	private static class RefObjectEntry
	{
		public Object object;
		public long id;

		public void set(Object aObject, long aId)
		{
			object = aObject;
			id = aId;
		}
	}

}