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

import tod.agent.io._ByteBuffer;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.bci.asm2.BCIUtils;
import zz.utils.ArrayStack;

public abstract class AbstractMethodReplayer
{
	// States
	private static int s = 0;
	static final int S_UNDEFINED = s++; 
	static final int S_INITIALIZED = s++; // The replayer is ready to start
	static final int S_WAIT_ARGS = s++; // Waiting for method args
	static final int S_STARTED = s++; // The replayer has started replaying
	static final int S_WAIT_FIELD = s++; // Waiting for a field value
	static final int S_WAIT_ARRAY = s++; // Waiting for an array slot value
	static final int S_WAIT_NEWARRAY = s++; // Waiting for a new array ref
	static final int S_WAIT_CST = s++; // Waiting for a class constant (LDC)
	static final int S_FINISHED_NORMAL = s++; // Execution finished normally
	static final int S_FINISHED_EXCEPTION = s++; // Execution finished because an exception was thrown
	
	private final Type[] itsArgTypes;
	private final Type itsReturnType;
	
	private TmpIdManager itsTmpIdManager;
	
	private int itsNextBlock = 0;
	private int itsState = S_INITIALIZED;
	
	/**
	 * The type for expected values (for field reads and behavior calls).
	 */
	private Type itsExpectedType = null;
	
	/**
	 * Cache slot for the currently expected field
	 */
	private int itsExpectedFieldCacheSlot = -1;
	
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
	 * Indicates if the replayer is processing an out-of-scope call
	 */
	private boolean itsCallingOutOfScope = false;
	
	/**
	 * @param aCacheCounts "encoded" cache counts.
	 */
	protected AbstractMethodReplayer(String aDescriptor, String aCacheCounts)
	{
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
	}
	
	/**
	 * Finishes the setup of this replayer (we don't add these args
	 * to the constructor to simplify generated code). 
	 */
	public void setup(TmpIdManager aTmpIdManager)
	{
		itsTmpIdManager = aTmpIdManager;
	}
	
	/**
	 * Whether the execution of the method has terminated (normally).
	 */
	public boolean hasFinishedNormally()
	{
		return itsState == S_FINISHED_NORMAL;
	}
	
	/**
	 * Whether the execution of the method has terminated (by throwing an exception).
	 */
	public boolean hasFinishedWithException()
	{
		return itsState == S_FINISHED_EXCEPTION;
	}
	
	public boolean isCallingOutOfScope()
	{
		return itsCallingOutOfScope;
	}
	
	/**
	 * Writes the current value (one of the vXxxx fields) into the indicated cache slot
	 * of the given type.
	 */
	private void storeFieldCache(Type aType, int aCacheSlot)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT: itsRefCache[aCacheSlot] = vRef(); break;
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
		case Type.OBJECT: vRef(itsRefCache[aCacheSlot]); break;
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
		itsState = S_UNDEFINED;
		int theNextBlock = itsNextBlock;
		itsNextBlock = -1;
		
		proceed(theNextBlock);
	}
	
	private void allowedState(int aState)
	{
		if (itsState != aState) throw new IllegalStateException();
	}
	
	private void allowedStates(int... aStates)
	{
		for (int s : aStates) if (itsState == s) return;
		throw new IllegalStateException();
	}

	/**
	 * Starts the replay of the method, taking arguments from the parent replayer.
	 */
	public void startFromScope(AbstractMethodReplayer aParent)
	{
		allowedState(S_INITIALIZED);
		itsNextBlock = 0;
	}
	
	/**
	 * Starts the replay of the method, expecting explicit args.
	 */
	public void startFromOutOfScope()
	{
		allowedState(S_INITIALIZED);
		itsState = S_WAIT_ARGS;
	}
	
	/**
	 * Read the method arguments from the buffer
	 */
	public void args(_ByteBuffer aBuffer)
	{
		allowedState(S_WAIT_ARGS);
		itsNextBlock = 0;
		int theCount = aBuffer.getInt();
		
		
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
	
	public abstract void evNewArray(ObjectId aObject);
	public abstract void evObjectInitialized(ObjectId aObject);
	public abstract void evExceptionGenerated(IBehaviorInfo aBehavior, int aBytecodeIndex, ObjectId aException);
	public abstract void evHandlerReached(int aLocation);
	public abstract void constructorTarget(long aId);

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
		itsState = S_WAIT_FIELD;
		itsNextBlock = aNextBlock;
		itsExpectedFieldCacheSlot = aCacheSlot;
		expect(aSort);
	}
	
	protected void expectArray(int aSort, int aNextBlock)
	{
		itsState = S_WAIT_ARRAY;
		itsNextBlock = aNextBlock;
		expect(aSort);
	}
	
	protected void expectClassCst(int aNextBlock)
	{
		itsState = S_WAIT_CST;
		itsNextBlock = aNextBlock;
	}
	
	protected void expectNewArray(int aNextBlock)
	{
		itsState = S_WAIT_NEWARRAY;
		itsNextBlock = aNextBlock;
	}
	
	/**
	 * Reads a value corresponding to the specified type from the stream, and set it 
	 * as current value.
	 */
	private void readValue(Type aType, _ByteBuffer aBuffer)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT: vRef(new ObjectId(aBuffer.getLong())); break;
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
	
	// Set local variable slot
	protected abstract void lRefSet(int aSlot, ObjectId v);
	protected abstract void lIntSet(int aSlot, int v);
	protected abstract void lDoubleSet(int aSlot, double v);
	protected abstract void lFloatSet(int aSlot, float v);
	protected abstract void lLongSet(int aSlot, long v);
	
	
	protected ObjectId nextTmpId()
	{
		return new ObjectId(itsTmpIdManager.nextId());
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
}
