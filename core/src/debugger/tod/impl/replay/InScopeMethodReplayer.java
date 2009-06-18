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

import gnu.trove.TDoubleStack;
import gnu.trove.TFloatStack;
import gnu.trove.TIntStack;
import gnu.trove.TLongStack;

import org.objectweb.asm.Type;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.replay.ThreadReplayer.ExceptionInfo;
import tod2.agent.Message;
import tod2.agent.MonitoringMode;
import tod2.agent.io._ByteBuffer;
import zz.utils.ArrayStack;
import zz.utils.primitive.IntArray;

public abstract class InScopeMethodReplayer extends MethodReplayer
{
	private final String itsName;
	private final int itsAccess;
	
	private final Type[] itsArgTypes;
	private final Type itsReturnType;
	
	private int itsNextBlock = 0;
	
	/**
	 * The type for expected values (for field reads and behavior calls).
	 */
	private Type itsExpectedType = null;
	
	/**
	 * Cache slot for the currently expected field
	 */
	private int itsExpectedFieldCacheSlot = -1;
	
	private boolean itsExpectObjectInitialized = false;
	
	private ObjectId itsRefValue;
	private boolean itsBooleanValue;
	private byte itsByteValue;
	private char itsCharValue;
	private double itsDoubleValue;
	private float itsFloatValue;
	private int itsIntValue;
	private long itsLongValue;
	private short itsShortValue;

	private final ObjectId[] itsRefCache;
	private final boolean[] itsBooleanCache;
	private final byte[] itsByteCache;
	private final char[] itsCharCache;
	private final double[] itsDoubleCache;
	private final float[] itsFloatCache;
	private final int[] itsIntCache;
	private final long[] itsLongCache;
	private final short[] itsShortCache;
	
	private final ArrayStack<ObjectId> itsRefStack = new ArrayStack<ObjectId>();
	private final TIntStack itsIntStack = new TIntStack();
	private final TDoubleStack itsDoubleStack = new TDoubleStack();
	private final TFloatStack itsFloatStack = new TFloatStack();
	private final TLongStack itsLongStack = new TLongStack();
	
	/**
	 * Block id corresponding to each handler id. Indexed by handler id.
	 */
	private final IntArray itsHandlerBlocks = new IntArray();
	
	/**
	 * Data for pending invocation
	 */
	private InvocationData itsInvokeData;
	
	/**
	 * @param aCacheCounts "encoded" cache counts.
	 */
	protected InScopeMethodReplayer(
			String aName, 
			int aAccess, 
			String aDescriptor, 
			String aCacheCounts,
			String aHandlerBlocks)
	{
		itsName = aName;
		itsAccess = aAccess;
		
		itsArgTypes = Type.getArgumentTypes(aDescriptor);
		itsReturnType = Type.getReturnType(aDescriptor);
		
		itsRefCache = new ObjectId[aCacheCounts.charAt(Type.OBJECT)];
		itsBooleanCache = new boolean[aCacheCounts.charAt(Type.BOOLEAN)];
		itsByteCache = new byte[aCacheCounts.charAt(Type.BYTE)];
		itsCharCache = new char[aCacheCounts.charAt(Type.CHAR)];
		itsDoubleCache = new double[aCacheCounts.charAt(Type.DOUBLE)];
		itsFloatCache = new float[aCacheCounts.charAt(Type.FLOAT)];
		itsIntCache = new int[aCacheCounts.charAt(Type.INT)];
		itsLongCache = new long[aCacheCounts.charAt(Type.LONG)];
		itsShortCache = new short[aCacheCounts.charAt(Type.SHORT)];

		for(int i=0;i<aHandlerBlocks.length();i++) itsHandlerBlocks.set(i, aHandlerBlocks.charAt(i));
	}
	
	/**
	 * Writes the current value (one of the vXxxx fields) into the indicated cache slot
	 * of the given type.
	 */
	private void storeFieldCache(Type aType, int aCacheSlot)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT: 
		case Type.ARRAY:
			itsRefCache[aCacheSlot] = vRef(); 
			break;
			
		case Type.BOOLEAN: itsBooleanCache[aCacheSlot] = vBoolean(); break;
		case Type.BYTE: itsByteCache[aCacheSlot] = vByte(); break;
		case Type.CHAR: itsCharCache[aCacheSlot] = vChar(); break;
		case Type.DOUBLE: itsDoubleCache[aCacheSlot] = vDouble(); break;
		case Type.FLOAT: itsFloatCache[aCacheSlot] = vFloat(); break;
		case Type.INT: itsIntCache[aCacheSlot] = vInt(); break;
		case Type.LONG: itsLongCache[aCacheSlot] = vLong(); break;
		case Type.SHORT: itsShortCache[aCacheSlot] = vShort(); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}
	}

	/**
	 * Takes the value in the specified field cache for the specified type,
	 * and writes it as current value
	 * @param aType
	 * @param aCacheSlot
	 */
	private void loadFieldCache(Type aType, int aCacheSlot)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT:
		case Type.ARRAY:
			vRef(itsRefCache[aCacheSlot]); 
			break;
			
		case Type.BOOLEAN: vBoolean(itsBooleanCache[aCacheSlot]); break;
		case Type.BYTE: vByte(itsByteCache[aCacheSlot]); break;
		case Type.CHAR: vChar(itsCharCache[aCacheSlot]); break;
		case Type.DOUBLE: vDouble(itsDoubleCache[aCacheSlot]); break;
		case Type.FLOAT: vFloat(itsFloatCache[aCacheSlot]); break;
		case Type.INT: vInt(itsIntCache[aCacheSlot]); break;
		case Type.LONG: vLong(itsLongCache[aCacheSlot]); break;
		case Type.SHORT: vShort(itsShortCache[aCacheSlot]); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}
	}
	
	/**
	 * Resets all state info and proceed to current block
	 */
	private void next()
	{
		itsExpectedType = null;
		itsExpectedFieldCacheSlot = -1;
		setState(S_UNDEFINED);
		int theNextBlock = itsNextBlock;
		itsNextBlock = -1;
		
		getThreadReplayer().echo("Proceed: %d", theNextBlock);
		proceed(theNextBlock);
	}
	
	private void transferArg(InScopeMethodReplayer aParent, Type aType, int aSlot)
	{
		switch(aType.getSort())
		{
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
		case Type.INT: 
			lIntSet(aSlot, aParent.sIntPop()); 
			break;
			
		case Type.OBJECT:
		case Type.ARRAY:
			lRefSet(aSlot, aParent.sRefPop()); 
			break;
			
		case Type.DOUBLE: lDoubleSet(aSlot, aParent.sDoublePop()); break;
		case Type.FLOAT: lFloatSet(aSlot, aParent.sFloatPop()); break;
		case Type.LONG: lLongSet(aSlot, aParent.sLongPop()); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}	
	}
	
	@Override
	public void transferResult(InScopeMethodReplayer aSource)
	{
		if (itsExpectObjectInitialized) throw new IllegalStateException();
		
		Type theType = aSource.itsReturnType;
		if (! theType.equals(itsExpectedType)) throw new IllegalStateException();
		
		switch(theType.getSort())
		{
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
		case Type.INT: 
			sIntPush(aSource.sIntPop());
			break;
			
		case Type.OBJECT:
		case Type.ARRAY:
			sRefPush(aSource.sRefPop()); 
			break;
			
		case Type.DOUBLE: sDoublePush(aSource.sDoublePop()); break;
		case Type.FLOAT: sFloatPush(aSource.sFloatPop()); break;
		case Type.LONG: sLongPush(aSource.sLongPop()); break;

		case Type.VOID: break;
		
		default: throw new RuntimeException("Unknown type: "+theType);
		}	
		
		next();
	}
	
	@Override
	public void transferResult(_ByteBuffer aBuffer)
	{
		switch(itsExpectedType.getSort())
		{
		case Type.OBJECT:
		case Type.ARRAY:
			sRefPush(getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Type.BOOLEAN: sIntPush(aBuffer.get()); break;
		case Type.BYTE: sIntPush(aBuffer.get()); break;
		case Type.CHAR: sIntPush(aBuffer.getChar()); break;
		case Type.DOUBLE: sDoublePush(aBuffer.getDouble()); break;
		case Type.FLOAT: sFloatPush(aBuffer.getFloat()); break;
		case Type.INT: sIntPush(aBuffer.getInt()); break;
		case Type.LONG: sLongPush(aBuffer.getLong()); break;
		case Type.SHORT: sIntPush(aBuffer.getShort()); break;
		
		case Type.VOID: break;
		
		default: throw new RuntimeException("Unknown type: "+itsExpectedType);
		}	
		
		if (itsExpectObjectInitialized)
		{
			if (itsExpectedType.getSort() != Type.VOID) throw new IllegalStateException();
			itsExpectObjectInitialized = false;
			getThreadReplayer().echo("expectObjectInitialized");
			setState(S_WAIT_OBJECTINITIALIZED);
		}
		else
		{
			next();
		}
	}
	
	/**
	 * Starts the replay of the method, taking arguments from the parent replayer.
	 */
	public void startFromScope(InScopeMethodReplayer aParent)
	{
		allowedState(S_INITIALIZED);
		
		boolean theStatic = BCIUtils.isStatic(itsAccess);
		boolean theConstructor = "<init>".equals(itsName);
		
		int theSlot = 0;
		
		// We go in reverse arg order, so find out the last slot index
		if (! theStatic) theSlot++;
		for(Type theType : itsArgTypes) theSlot += theType.getSize();

		// Pop args from parent and set them as locals
		for(int i=itsArgTypes.length-1;i>=0;i--)
		{
			Type theType = itsArgTypes[i];
			theSlot -= theType.getSize();
			transferArg(aParent, theType, theSlot);
		}
		
		// Check how to proceed for the target argument (this)
		if (! theStatic)
		{
			assert theSlot == 1;
			theSlot = 0;
			
			if (theConstructor) lRefSet(0, nextTmpId()); // Target is deferred
			else lRefSet(0, aParent.sRefPop());
		}

		itsNextBlock = 0;
		setState(S_STARTED);
		next();
	}
	
	/**
	 * Starts the replay of the method, expecting explicit args.
	 */
	public void startFromOutOfScope()
	{
		allowedState(S_INITIALIZED);
		setState(S_WAIT_ARGS);
	}
	
	/**
	 * Reads a value corresponding to the specified type from the stream, and set it 
	 * as current value.
	 */
	private void readArg(_ByteBuffer aBuffer, Type aType, int aSlot)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT:
		case Type.ARRAY:
			lRefSet(aSlot, getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Type.BOOLEAN: lIntSet(aSlot, aBuffer.get()); break;
		case Type.SHORT: lIntSet(aSlot, aBuffer.getShort()); break;
		case Type.BYTE: lIntSet(aSlot, aBuffer.get()); break;
		case Type.CHAR: lIntSet(aSlot, aBuffer.getChar()); break;
		case Type.DOUBLE: lDoubleSet(aSlot, aBuffer.getDouble()); break;
		case Type.FLOAT: lFloatSet(aSlot, aBuffer.getFloat()); break;
		case Type.INT: lIntSet(aSlot, aBuffer.getInt()); break;
		case Type.LONG: lLongSet(aSlot, aBuffer.getLong()); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}
	}
	
	@Override
	public void processMessage(byte aMessage, _ByteBuffer aBuffer)
	{
		switch(aMessage)
		{
		case Message.BEHAVIOR_ENTER_ARGS: args(aBuffer); break;
		case Message.ARRAY_READ: evArrayRead(aBuffer); break;
		case Message.CONSTANT: evCst(aBuffer); break;
		case Message.FIELD_READ: evFieldRead(aBuffer); break;
		case Message.FIELD_READ_SAME: evFieldRead_Same(); break;
		case Message.NEW_ARRAY: evNewArray(aBuffer); break;
		case Message.OBJECT_INITIALIZED: evObjectInitialized(getThreadReplayer().readValue(aBuffer)); break;
		case Message.CONSTRUCTOR_TARGET: evConstructorTarget(getThreadReplayer().readValue(aBuffer)); break;
		case Message.EXCEPTION: evExceptionGenerated(aBuffer); break;
		case Message.HANDLER_REACHED: evHandlerReached(aBuffer); break;
		case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION: evExitException(aBuffer); break;
			
		default: throw new IllegalStateException(""+aMessage);
		}
	}
	
	public void proceed()
	{
		allowedState(S_HOLD);
		next();
	}
	
	/**
	 * Read the method arguments from the buffer
	 */
	public void args(_ByteBuffer aBuffer)
	{
		allowedState(S_WAIT_ARGS);
		
		boolean theStatic = BCIUtils.isStatic(itsAccess);
		boolean theConstructor = "<init>".equals(itsName);
		
		int theSlot = 0;
		
		if (! theStatic && ! theConstructor)
		{
			readArg(aBuffer, BCIUtils.getType(Type.OBJECT), theSlot);
			theSlot++;
		}
		
		for(int i=0;i<itsArgTypes.length;i++)
		{
			Type theType = itsArgTypes[i];
			readArg(aBuffer, theType, theSlot);
			theSlot += theType.getSize();
		}
		
		itsNextBlock = 0;
		setState(S_STARTED);
		next();
	}
	
	
	public void evFieldRead(_ByteBuffer aBuffer)
	{
		allowedState(S_WAIT_FIELD);
		
		readValue(itsExpectedType, aBuffer);
		if (itsExpectedFieldCacheSlot >= 0) storeFieldCache(itsExpectedType, itsExpectedFieldCacheSlot);

		next();
	}
	
	public void evFieldRead_Same()
	{
		allowedState(S_WAIT_FIELD);
		if (itsExpectedFieldCacheSlot < 0) throw new IllegalStateException();
		
		loadFieldCache(itsExpectedType, itsExpectedFieldCacheSlot);

		next();
	}
	
	public void evArrayRead(_ByteBuffer aBuffer)
	{
		allowedState(S_WAIT_ARRAY);
		readValue(itsExpectedType, aBuffer);
		next();
	}
	
	public void evCst(_ByteBuffer aBuffer)
	{
		allowedState(S_WAIT_CST);
		readValue(BCIUtils.getType(Type.OBJECT), aBuffer);
		next();
	}
	
	public void evNewArray(_ByteBuffer aBuffer)
	{
		allowedState(S_WAIT_NEWARRAY);
		readValue(BCIUtils.getType(Type.OBJECT), aBuffer);
		next();
	}
	
	public void evObjectInitialized(ObjectId aObject)
	{
		allowedState(S_WAIT_OBJECTINITIALIZED);
		ObjectId theTmpId = sRefPeek();
		getThreadReplayer().getTmpIdManager().associate(theTmpId.getId(), aObject.getId());
		next();
	}
	
	public void evConstructorTarget(ObjectId aObject)
	{
		allowedState(S_WAIT_CONSTRUCTORTARGET);
		ObjectId theTmpId = lRefGet(0);
		getThreadReplayer().getTmpIdManager().associate(theTmpId.getId(), aObject.getId());
		next();
	}
	
	private void evExceptionGenerated(_ByteBuffer aBuffer)
	{
		ExceptionInfo theExceptionInfo = getThreadReplayer().readExceptionInfo(aBuffer);
		
		evExceptionGenerated(
				theExceptionInfo.behaviorId, 
				theExceptionInfo.bytecodeIndex, 
				theExceptionInfo.exception);
	}
	

	public void evExceptionGenerated(int aBehaviorId, int aBytecodeIndex, ObjectId aException)
	{
		setState(S_EXCEPTION_THROWN);
		vRef(aException);
	}
	
	public void evHandlerReached(_ByteBuffer aBuffer)
	{
		allowedState(S_EXCEPTION_THROWN);
		int theHandlerId = aBuffer.getInt();
		itsNextBlock = itsHandlerBlocks.get(theHandlerId);
		next();
	}
	
	public void evExitException(_ByteBuffer aBuffer)
	{
		allowedStates(S_EXCEPTION_THROWN);
		setState(S_FINISHED_EXCEPTION);
	}
	
	public void constructorTarget(long aId)
	{
		ObjectId theTmpId = lRefGet(0);
		getThreadReplayer().getTmpIdManager().associate(theTmpId.getId(), aId);
		next();
	}

	/**
	 * Sets the expected type.
	 * @param aSort The sort corresponding to the type (see {@link Type}).
	 */
	protected void expect(int aSort)
	{
		if (itsExpectedType != null) throw new IllegalStateException();
		itsExpectedType = BCIUtils.getType(aSort);
	}
	
	protected void expectField(int aSort, int aNextBlock, int aCacheSlot)
	{
		getThreadReplayer().echo("expectField");
		setState(S_WAIT_FIELD);
		itsNextBlock = aNextBlock;
		itsExpectedFieldCacheSlot = aCacheSlot;
		expect(aSort);
	}
	
	protected void expectArray(int aSort, int aNextBlock)
	{
		getThreadReplayer().echo("expectArray");
		setState(S_WAIT_ARRAY);
		itsNextBlock = aNextBlock;
		expect(aSort);
	}
	
	protected void expectClassCst(int aNextBlock)
	{
		getThreadReplayer().echo("expectClassCst");
		setState(S_WAIT_CST);
		itsNextBlock = aNextBlock;
	}
	
	protected void expectNewArray(int aNextBlock)
	{
		getThreadReplayer().echo("expectNewArray");
		setState(S_WAIT_NEWARRAY);
		itsNextBlock = aNextBlock;
	}
	
	protected void expectConstructorTarget(int aNextBlock)
	{
		getThreadReplayer().echo("expectConstructorTarget");
		setState(S_WAIT_CONSTRUCTORTARGET);
		itsNextBlock = aNextBlock;
	}
	
	@Override
	public void expectException()
	{
		getThreadReplayer().echo("expectException");
		setState(S_WAIT_EXCEPTION);
		itsNextBlock = -1;
	}
	
	/**
	 * Hold execution until the next message is received.
	 * If it is not an exception, then proceed.
	 */
	public void hold(int aNextBlock)
	{
		getThreadReplayer().echo("Hold");
		setState(S_HOLD);
		itsNextBlock = aNextBlock;
	}
	
	protected void processReturn()
	{
		setState(S_FINISHED_NORMAL);
		itsNextBlock = -1;
	}
	
	/**
	 * Reads a value corresponding to the specified type from the stream, and set it 
	 * as current value.
	 */
	private void readValue(Type aType, _ByteBuffer aBuffer)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT:
		case Type.ARRAY:
			vRef(getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Type.BOOLEAN: vBoolean(aBuffer.get() != 0); break;
		case Type.BYTE: vByte(aBuffer.get()); break;
		case Type.CHAR: vChar(aBuffer.getChar()); break;
		case Type.DOUBLE: vDouble(aBuffer.getDouble()); break;
		case Type.FLOAT: vFloat(aBuffer.getFloat()); break;
		case Type.INT: vInt(aBuffer.getInt()); break;
		case Type.LONG: vLong(aBuffer.getLong()); break;
		case Type.SHORT: vShort(aBuffer.getShort()); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}
	}
	
	// Get current value
	protected ObjectId vRef() { return itsRefValue; }
	protected boolean vBoolean() { return itsBooleanValue; }
	protected byte vByte() { return itsByteValue; }
	protected char vChar() { return itsCharValue; }
	protected double vDouble() { return itsDoubleValue; }
	protected float vFloat() { return itsFloatValue; }
	protected int vInt() { return itsIntValue; }
	protected long vLong() { return itsLongValue; }
	protected short vShort() { return itsShortValue; }

	// Set current value
	protected void vRef(ObjectId v) { itsRefValue = v; }
	protected void vBoolean(boolean v) { itsBooleanValue = v; }
	protected void vByte(byte v) { itsByteValue = v; }
	protected void vChar(char v) { itsCharValue = v; }
	protected void vDouble(double v) { itsDoubleValue = v; }
	protected void vFloat(float v) { itsFloatValue = v; }
	protected void vInt(int v) { itsIntValue = v; }
	protected void vLong(long v) { itsLongValue = v; }
	protected void vShort(short v) { itsShortValue = v; }
	
	// Push to operand stack
	protected void sRefPush(ObjectId v) { itsRefStack.push(v); }
	protected void sIntPush(int v) { itsIntStack.push(v); }
	protected void sDoublePush(double v) { itsDoubleStack.push(v); }
	protected void sFloatPush(float v) { itsFloatStack.push(v); }
	protected void sLongPush(long v) { itsLongStack.push(v); }
	
	// Pop from operand stack
	protected ObjectId sRefPop() { return itsRefStack.pop(); }
	protected int sIntPop() { return itsIntStack.pop(); }
	protected double sDoublePop() { return itsDoubleStack.pop(); }
	protected float sFloatPop() { return itsFloatStack.pop(); }
	protected long sLongPop() { return itsLongStack.pop(); }
	
	// Peek operand stack
	protected ObjectId sRefPeek() { return itsRefStack.peek(); }
	
	// Set local variable slot
	protected abstract void lRefSet(int aSlot, ObjectId v);
	protected abstract void lIntSet(int aSlot, int v);
	protected abstract void lDoubleSet(int aSlot, double v);
	protected abstract void lFloatSet(int aSlot, float v);
	protected abstract void lLongSet(int aSlot, long v);
	
	// Get local variable slot
	protected abstract ObjectId lRefGet(int aSlot);
	
	protected ObjectId nextTmpId()
	{
		return new ObjectId(getThreadReplayer().getTmpIdManager().nextId());
	}
	
	@Override
	protected void allowedState(int aState)
	{
		super.allowedState(aState);
		if (itsInvokeData != null && aState != S_INVOKE_PENDING) throw new IllegalStateException();
	}

	
	/**
	 * Processes an invocation
	 * Actually saves the data for later processing by {@link #proceedInvoke()}.
	 * This is to allow a {@link Message#TRACEDMETHODS_VERSION} message to
	 * be processed before the invocation is actually processed
	 */
	protected void invoke(int aBehaviorId, int aNextBlock, boolean aExpectObjectInitialized)
	{
		itsInvokeData = new InvocationData(aBehaviorId, aNextBlock, aExpectObjectInitialized);
		setState(S_INVOKE_PENDING);
	}
	
	public void proceedInvoke()
	{
		allowedState(S_INVOKE_PENDING);
		proceedInvoke(itsInvokeData.behaviorId, itsInvokeData.nextBlock, itsInvokeData.expectObjectInitialized);
		itsInvokeData = null;
	}
	
	private void proceedInvoke(int aBehaviorId, int aNextBlock, boolean aExpectObjectInitialized)
	{
		int theMode = getThreadReplayer().getBehaviorMonitoringMode(aBehaviorId);
		IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
		itsExpectedType = Type.getType(theBehavior.getReturnType().getJvmName());
		
		switch(theMode)
		{
		case MonitoringMode.FULL:
		case MonitoringMode.ENVELOPPE:
			getThreadReplayer().echo("invoke (monitored): "+theBehavior+" ["+aExpectObjectInitialized+"]");
			setState(S_CALLING_MONITORED);
			break;
			
		case MonitoringMode.NONE:
			getThreadReplayer().echo("invoke (unmonitored): "+theBehavior+" ["+aExpectObjectInitialized+"]");
			setState(S_CALLING_UNMONITORED);
			break;
			
		default: throw new RuntimeException("Mode not handled: "+theMode); 
		}
		
		itsNextBlock = aNextBlock;
		itsExpectObjectInitialized = aExpectObjectInitialized;
	}

	/**
	 * This method is generated by {@link MethodReplayerGenerator}.
	 * It consists of a big switch statement that dispatches execution based on the block id.
	 */
	protected abstract void proceed(int aBlockId);
	
	protected static boolean cmpId(ObjectId id1, ObjectId id2)
	{
		if (id1 == null && id2 == null) return true;
		if (id1 == null || id2 == null) return false;
		return id1.getId() == id2.getId();
	}
	
	private static final class InvocationData
	{
		public final int behaviorId;
		public final int nextBlock;
		public final boolean expectObjectInitialized;

		public InvocationData(int aBehaviorId, int aNextBlock, boolean aExpectObjectInitialized)
		{
			behaviorId = aBehaviorId;
			nextBlock = aNextBlock;
			expectObjectInitialized = aExpectObjectInitialized;
		}
	}
}
