/*
 * Created on Apr 30, 2009
 */
package java.tod;

import java.tod.io._IO;
import java.tod.transport.IOThread;
import java.tod.transport.ObjectEncoder;
import java.tod.transport.ThreadPacket;
import java.tod.util.BitStack;
import java.tod.util.IntDeltaSender;
import java.tod.util.LongDeltaSender;
import java.tod.util._StringBuilder;

import tod.access.TODAccessor;
import tod.agent.AgentDebugFlags;
import tod.agent.Command;
import tod.agent.Message;
import tod.agent.ValueType;
import tod.agent.io._ByteBuffer;
import tod.agent.io._GrowingByteBuffer;

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
	
	/**
	 * When this flag is true the next exception generated event
	 * is ignored. This permits to avoid reporting EG events that
	 * are caused by the instrumentation.
	 */
	private boolean itsIgnoreNextException = false;

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
	

	private int[] itsEvCount = new int[Message.SYNC];
	private long[] itsEvData = new long[Message.SYNC];
	private int itsLastBufferPos = -1;
	private int itsExpectedArgsCount = -1;
	private int itsAccountToCharge = -1;
	
	private LongDeltaSender itsObjIdSender = new LongDeltaSender();
	private IntDeltaSender itsBehIdSender = new IntDeltaSender();
	
	public ThreadData(int aId, IOThread aIOThread)
	{
		itsId = aId;
		itsIOThread = aIOThread;
		resetBuffer();
		itsIOThread.registerThreadData(this);
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
		return TODAccessor.getObjectId(aObject);
	}
	
	/**
	 * Sets the ignore next exception flag.
	 */
	public void ignoreNextException()
	{
		itsIgnoreNextException = true;
	}
	
	/**
	 * Checks if the ignore next exception flag is set, and resets it.
	 */
	public boolean checkIgnoreNextException()
	{
		boolean theIgnoreNext = itsIgnoreNextException;
		itsIgnoreNextException = false;
		return theIgnoreNext;
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
	}
	
	private void pushOutOfScope()
	{
		itsScopeStack.push(false);
	}
	
	private boolean popScope()
	{
		return itsScopeStack.pop();
	}
	
	private static void sendMessageType(_ByteBuffer aBuffer, byte aType) 
	{
		aBuffer.put(aType);
	}

	private static void sendValueType(_ByteBuffer aBuffer, byte aType) 
	{
		aBuffer.put(aType);
	}
	
	private static void sendCommand(_ByteBuffer aBuffer, byte aCommand) 
	{
		aBuffer.put(aCommand);
	}
	
	private static void sendString(_ByteBuffer aBuffer, String aString)
	{
		int theSize = aString.length();
		aBuffer.putInt(theSize);
		for(int i=0;i<theSize;i++) aBuffer.putChar(aString.charAt(i));
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
	    
	    for(int i = Message.FIELD_READ;i<Message.SYNC;i++)
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
		
		thePacket = itsIOThread.getFreePacket(ThreadPacket.RECYCLE_QUEUE_STANDARD);
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
	 * Sends a synchronization message.
	 */
	public void sendSync(long aTimestamp)
	{
		sendMessageType(itsBuffer, Message.SYNC);
		itsBuffer.putLong(aTimestamp);
		commitBuffer();
	}
	
	/**
	 * A field has just been read. Value expected next
	 */
	public void evFieldRead()
	{
		sendRegisteredObjects();
		
		msgStart(Message.FIELD_READ, 1);
		sendMessageType(itsBuffer, Message.FIELD_READ);
		msgStop();
	}
	
	/**
	 * A field has just been read, and the value is the same as in the previous read.
	 */
	public void evFieldRead_Same()
	{
		sendRegisteredObjects();
		
	    msgStart(Message.FIELD_READ_SAME, 0);
		sendMessageType(itsBuffer, Message.FIELD_READ_SAME);
		msgStop();
		
        commitBuffer();
	}
	
	/**
	 * An array slot has just been read. Value expected next.
	 */
	public void evArrayRead()
	{
		sendRegisteredObjects();
		
	    msgStart(Message.ARRAY_READ, 1);
		sendMessageType(itsBuffer, Message.ARRAY_READ);
		msgStop();
	}
	
	public void evNew(Object aValue)
	{
		sendRegisteredObjects();
		
		msgStart(Message.NEW, 0);
	    
		checkTimestamp();
		sendMessageType(itsBuffer, Message.NEW);
		sendValue(itsBuffer, aValue);
		
		msgStop();
		
		commitBuffer();
		
		// Objects that are sent by value must not be serialized now as they
		// are not yet initialized
		if (! shouldSendByValue(aValue)) sendRegisteredObjects();
	}
	
	public void evObjectInitialized(Object aValue)
	{
		sendRegisteredObjects();

		msgStart(Message.OBJECT_INITIALIZED, 0);

		checkTimestamp();
		sendMessageType(itsBuffer, Message.NEW);
		sendValue(itsBuffer, aValue);

		msgStop();
		
		commitBuffer();
	}
	
	public void evExceptionGenerated(
			String aMethodName,
			String aMethodSignature,
			String aMethodDeclaringClassSignature, 
			int aBytecodeIndex,
			Throwable aException) 
	{
		msgStart(Message.EXCEPTION, 0);

		checkTimestamp();
		sendMessageType(itsBuffer, Message.EXCEPTION);

		sendString(itsBuffer, aMethodName);
		sendString(itsBuffer, aMethodSignature);
		sendString(itsBuffer, aMethodDeclaringClassSignature);
		itsBuffer.putShort((short) aBytecodeIndex);
		sendValue(itsBuffer, aException);

		msgStop();
		
		commitBuffer();

		// There might be harmless recursive exceptions here, ignore
		// Note: this is only for exception events, other events don't need this.
		ExceptionGeneratedReceiver.setIgnoreExceptions(true);
		sendRegisteredObjects();
		ExceptionGeneratedReceiver.setIgnoreExceptions(false);
	}
	
	public void evHandlerReached(int aLocation)
	{
		sendRegisteredObjects();
		
	    msgStart(Message.HANDLER_REACHED, 0);
		
		checkTimestamp();
		sendMessageType(itsBuffer, Message.HANDLER_REACHED);
		itsBuffer.putInt(aLocation);
		
		msgStop();
		
		commitBuffer();
	}
	
	public void evInScopeBehaviorEnter(int aBehaviorId)
	{
		sendRegisteredObjects();
		
		msgStart(Message.INSCOPE_BEHAVIOR_ENTER, 0);

		checkTimestamp();
		itsBehIdSender.send(itsBuffer, aBehaviorId, Message.INSCOPE_BEHAVIOR_ENTER_DELTA, Message.INSCOPE_BEHAVIOR_ENTER);

		msgStop();
		
		commitBuffer();
		
		pushInScope();
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
	    msgStart(Message.BEHAVIOR_ENTER_ARGS, aCount);
	    
		sendMessageType(itsBuffer, Message.BEHAVIOR_ENTER_ARGS);
		itsBuffer.putInt(aCount);
		
		msgStop();
	}
	
	public void sendValue_Boolean(boolean aValue)
	{
		itsBuffer.put(aValue ? TRUE : FALSE);
		msgStop();
	}
	
	public void sendValue_Byte(byte aValue)
	{
		itsBuffer.put(aValue);
        msgStop();
	}
	
	public void sendValue_Char(char aValue)
	{
		itsBuffer.putChar(aValue);
        msgStop();
	}
	
	public void sendValue_Short(short aValue)
	{
		itsBuffer.putShort(aValue);
        msgStop();
	}
	
	public void sendValue_Int(int aValue)
	{
		itsBuffer.putInt(aValue);
        msgStop();
	}
	
	public void sendValue_Long(long aValue)
	{
		itsBuffer.putLong(aValue);
        msgStop();
	}
	
	public void sendValue_Float(float aValue)
	{
		itsBuffer.putFloat(aValue);
        msgStop();
	}
	
	public void sendValue_Double(double aValue)
	{
		itsBuffer.putDouble(aValue);
        msgStop();
	}
	
	public void sendValue_Ref(Object aValue)
	{
		sendValue(itsBuffer, aValue);
        msgStop();
	}
	
	public void sendConstructorTarget(Object aTarget)
	{
		sendRegisteredObjects();

		msgStart(Message.CONSTRUCTOR_TARGET, 0);
		
		sendMessageType(itsBuffer, Message.CONSTRUCTOR_TARGET);
		sendValue(itsBuffer, aTarget);
		
		msgStop();
		
		commitBuffer();
	}
	
	public void evInScopeBehaviorExit_Normal()
	{
	    msgStart(Message.INSCOPE_BEHAVIOR_EXIT_NORMAL, 0);
		msgStop();
		
		popScope();
	}
	
	public void evInScopeBehaviorExit_Exception()
	{
		sendRegisteredObjects();
		
	    msgStart(Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION, 0);
		sendMessageType(itsBuffer, Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION);
		msgStop();
		
		commitBuffer();
		
		popScope();
	}
	
	/**
	 * Entering into an out-of-scope behavior (which has enveloppe only instrumentation).
	 */
	public void evOutOfScopeBehaviorEnter()
	{
		sendRegisteredObjects();
		
	    msgStart(Message.OUTOFSCOPE_BEHAVIOR_ENTER, 0);
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_ENTER);
		msgStop();
		
		commitBuffer();
		
		pushOutOfScope();
	}
	
	/**
	 * Exiting normally from an out-of-scope non-void behavior (which has enveloppe only instrumentation).
	 */
	public void evOutOfScopeBehaviorExit_Normal()
	{
		sendRegisteredObjects();
		
	    msgStart(Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL, 0);
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL);
		msgStop();
		
		commitBuffer();

		popScope();
	}
	
	/**
	 * Indicates that a behavior return value is going to be sent next.
	 */
	public void sendOutOfScopeBehaviorResult()
	{
	    msgStart(Message.BEHAVIOR_ENTER_ARGS, 1);
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT);
		msgStop();
	}
	
	/**
	 * Exiting with an exception from an out-of-scope behavior (which has enveloppe only instrumentation).
	 */
	public void evOutOfScopeBehaviorExit_Exception()
	{
		sendRegisteredObjects();
		
	    msgStart(Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION, 0);
		sendMessageType(itsBuffer, Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION);
		msgStop();
		
		commitBuffer();
		
		popScope();
	}
	

	/**
	 * Before an unmonitored behavior call.
	 */
	public void evUnmonitoredBehaviorCall()
	{
	    msgStart(Message.UNMONITORED_BEHAVIOR_CALL, 0);
		msgStop();
		pushOutOfScope();
	}
	
	/**
	 * After an unmonitored behavior call.
	 * The result value should be sent immediately after.
	 */
	public void evUnmonitoredBehaviorResultNonVoid()
	{
		sendRegisteredObjects();
		
		msgStart(Message.UNMONITORED_BEHAVIOR_CALL_RESULT, 1);
		checkTimestamp();
		sendMessageType(itsBuffer, Message.UNMONITORED_BEHAVIOR_CALL_RESULT);
		msgStop();
		
		commitBuffer();
		
		if (popScope()) throw new Error("Unexpected scope state");
	}
	
	/**
	 * After an unmonitored behavior call.
	 */
	public void evUnmonitoredBehaviorResultVoid()
	{
		sendRegisteredObjects();
		
		msgStart(Message.UNMONITORED_BEHAVIOR_CALL_RESULT, 0);
		checkTimestamp();
		sendMessageType(itsBuffer, Message.UNMONITORED_BEHAVIOR_CALL_RESULT);
		msgStop();
		
		commitBuffer();
		
		if (popScope()) throw new Error("Unexpected scope state");
	}
	
	/**
	 * An unmonitored behavior call threw an exception.
	 */
	public void evUnmonitoredBehaviorException()
	{
	    msgStart(Message.UNMONITORED_BEHAVIOR_CALL_EXCEPTION, 0);
		msgStop();
		if (popScope()) throw new Error("Unexpected scope state");
	}
	
	/**
	 * Determines if the given object should be sent by value.
	 */
	private boolean shouldSendByValue(Object aObject)
	{
		return (aObject instanceof String) || (aObject instanceof Throwable);
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
			String aName) 
	{
		sendMessageType(itsBuffer, Message.REGISTER_THREAD);

		itsBuffer.putLong(aJVMThreadId);
		sendString(itsBuffer, aName);

		commitBuffer();

		sendRegisteredObjects();
	}

	public void sendClear() 
	{
		sendCommand(itsBuffer, Command.DBCMD_CLEAR);
		commitBuffer();
	}

	public void sendFlush() 
	{
		sendCommand(itsBuffer, Command.DBCMD_FLUSH);
		commitBuffer();
	}
	
	public void sendEnd() 
	{
		sendCommand(itsBuffer, Command.DBCMD_END);
		commitBuffer();
	}
	
	public void sendEvCaptureEnabled(boolean aValue)
	{
		sendCommand(itsBuffer, Command.DBEV_CAPTURE_ENABLED);
		itsBuffer.put(aValue ? (byte) 1 : (byte) 0);
		commitBuffer();
	}
	
	private void sendValue(_ByteBuffer aBuffer, Object aValue) 
	{
		if (aValue == null)
		{
			sendValueType(aBuffer, ValueType.NULL);
		}
		else if (aValue instanceof Boolean)
		{
			Boolean theBoolean = (Boolean) aValue;
			sendValueType(aBuffer, ValueType.BOOLEAN);
			aBuffer.put(theBoolean.booleanValue() ? TRUE : FALSE);
		}
		else if (aValue instanceof Byte)
		{
			Byte theByte = (Byte) aValue;
			sendValueType(aBuffer, ValueType.BYTE);
			aBuffer.put(theByte.byteValue());
		}
		else if (aValue instanceof Character)
		{
			Character theCharacter = (Character) aValue;
			sendValueType(aBuffer, ValueType.CHAR);
			aBuffer.putChar(theCharacter.charValue());
		}
		else if (aValue instanceof Integer)
		{
			Integer theInteger = (Integer) aValue;
			sendValueType(aBuffer, ValueType.INT);
			aBuffer.putInt(theInteger.intValue());
		}
		else if (aValue instanceof Long)
		{
			Long theLong = (Long) aValue;
			sendValueType(aBuffer, ValueType.LONG);
			aBuffer.putLong(theLong.longValue());
		}
		else if (aValue instanceof Float)
		{
			Float theFloat = (Float) aValue;
			sendValueType(aBuffer, ValueType.FLOAT);
			aBuffer.putFloat(theFloat.floatValue());
		}
		else if (aValue instanceof Double)
		{
			Double theDouble = (Double) aValue;
			sendValueType(aBuffer, ValueType.DOUBLE);
			aBuffer.putDouble(theDouble.doubleValue());
		}
		else if (shouldSendByValue(aValue))
		{
			sendObjectByValue(aBuffer, aValue);
		}
		else
		{
			sendObjectByRef(aBuffer, aValue);
		}
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
			
			// add the time stamp for flushing purpose in ObjectDatabase
			itsRegisteredObjectsStack.push(theObjectId, aObject);
			// _IO.out("Registering: "+aObject+", id: "+theObjectId);
		}

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
		ThreadPacket thePacket = itsIOThread.getFreePacket(ThreadPacket.RECYCLE_QUEUE_OTHER);
		if (thePacket == null) 
		{
			thePacket = new ThreadPacket();
			thePacket.data = new byte[128];
			thePacket.recycleQueue = ThreadPacket.RECYCLE_QUEUE_OTHER;
		}
		
		thePacket.threadId = getId();
		
		// We use a different buffer here because the packet might be huge.
		_GrowingByteBuffer theBuffer = _GrowingByteBuffer.wrap(thePacket.data);
		theBuffer.position(14); // Header placeholder
		
		ObjectEncoder.encode(ObjectValueFactory.convert(aObject), theBuffer);
		
		int thePacketSize = theBuffer.position(); 
		int theSize = thePacketSize-5; // 5: event type + size 
	
		theBuffer.position(0);
		sendMessageType(theBuffer, Message.REGISTER_OBJECT);
		theBuffer.putInt(theSize); 
		
		theBuffer.putLong(aId);
		theBuffer.put(isIndexable(aObject) ? (byte) 1 : (byte) 0);
		
		thePacket.offset = 0;
		thePacket.length = thePacketSize;
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
		sendMessageType(itsBuffer, Message.REGISTER_CLASSLOADER);
		
		itsBuffer.putLong(aLoaderId);
		itsBuffer.putLong(theLoaderClassId);
		
		msgStop();
		
		commitBuffer();
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