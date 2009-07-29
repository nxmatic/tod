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

import java.util.Arrays;

import org.objectweb.asm.Type;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.bci.asm2.BCIUtils;
import tod.impl.replay.ThreadReplayer.ExceptionInfo;
import tod.impl.server.BufferStream;
import tod2.agent.Message;
import tod2.agent.MonitoringMode;
import zz.utils.primitive.IntArray;

public abstract class InScopeReplayerFrame extends ReplayerFrame
{
	// States
	private static enum State
	{
	S_UNDEFINED, 
	S_INITIALIZED, // The replayer is ready to start
	S_WAIT_ARGS, // Waiting for method args
	S_STARTED, // The replayer has started replaying
	S_WAIT_FIELD, // Waiting for a field value
	S_WAIT_ARRAY, // Waiting for an array slot value
	S_WAIT_ARRAYLENGTH, // Waiting for an array length
	S_WAIT_NEWARRAY, // Waiting for a new array ref
	S_WAIT_CST, // Waiting for a class constant (LDC)
	S_WAIT_EXCEPTION, // Waiting for an exception
	S_EXCEPTION_THROWN, // An exception was thrown, expect handler or exit
	S_WAIT_OBJECTINITIALIZED, // Waiting for an object initialized message
	S_WAIT_CONSTRUCTORTARGET, // Waiting for a constructor target message
	S_FINISHED_NORMAL, // Execution finished normally
	S_FINISHED_EXCEPTION, // Execution finished because an exception was thrown
	S_CALLING_MONITORED, // Processing invocation of monitored code
	S_CALLING_UNMONITORED, // Processing invocation of unmonitored code
	S_WAIT_INSCOPE_RESULT, // Waiting for the result of an in-scope call
	S_WAIT_INSCOPE_CLINIT_RESULT, // Waiting for the result of an in-scope call
	S_WAIT_OUTOFSCOPE_RESULT, // Waiting for the result of a monitored call
	S_WAIT_CLASSLOADER_ENTER, 
	S_WAIT_CLASSLOADER_EXIT, 
	}	
	
	private final String itsName;
	private final int itsAccess;
	
	private final Type[] itsArgTypes;
	private final Type itsReturnType;
	
	private State itsState = State.S_INITIALIZED;
	
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
	
	private PostClassloaderAction itsPostClassloaderAction;

	/** For clinit executions */
	private State itsNextState = null;
	private Type itsNextExpectedType = null;

	
	private ObjectId itsRefValue;
	private double itsDoubleValue;
	private float itsFloatValue;
	private int itsIntValue;
	private long itsLongValue;

	private final ObjectId[] itsRefCache;
	private final double[] itsDoubleCache;
	private final float[] itsFloatCache;
	private final int[] itsIntCache;
	private final long[] itsLongCache;
	
	private PrimitiveMultiStack itsSaveStack;
	private PrimitiveMultiStack itsArgsStack;
	
	/**
	 * Block id corresponding to each handler id. Indexed by handler id.
	 */
	private final IntArray itsHandlerBlocks = new IntArray();
	
	/**
	 * @param aCacheCounts "encoded" cache counts.
	 */
	protected InScopeReplayerFrame(
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
		
		int theIntCount = aCacheCounts.charAt(Type.INT) 
				+ aCacheCounts.charAt(Type.BOOLEAN) 
				+ aCacheCounts.charAt(Type.BYTE) 
				+ aCacheCounts.charAt(Type.CHAR) 
				+ aCacheCounts.charAt(Type.SHORT); 
		
		itsRefCache = new ObjectId[aCacheCounts.charAt(Type.OBJECT)];
		itsDoubleCache = new double[aCacheCounts.charAt(Type.DOUBLE)];
		itsFloatCache = new float[aCacheCounts.charAt(Type.FLOAT)];
		itsIntCache = new int[theIntCount];
		itsLongCache = new long[aCacheCounts.charAt(Type.LONG)];

		for(int i=0;i<aHandlerBlocks.length();i++) itsHandlerBlocks.set(i, aHandlerBlocks.charAt(i));
	}
	
	@Override
	public void setup(ThreadReplayer aThreadReplayer, boolean aFromScope)
	{
		super.setup(aThreadReplayer, aFromScope);
		itsSaveStack = aThreadReplayer.getPMS();
		itsArgsStack = aThreadReplayer.getPMS();
	}
	
	@Override
	public void dispose(ThreadReplayer aThreadReplayer)
	{
		super.dispose(aThreadReplayer);
		aThreadReplayer.releasePMS(itsSaveStack);
		itsSaveStack = null;
		aThreadReplayer.releasePMS(itsArgsStack);
		itsArgsStack = null;
	}
	
	public State getState()
	{
		return itsState;
	}
	
	protected void setState(State aState)
	{
		itsState = aState;
	}
	
	protected void allowedState(State aState)
	{
		if (itsState != aState) throw new IllegalStateException("Expected "+aState+", but is "+itsState);
	}
	
	protected void allowedStates(State... aStates)
	{
		for (State s : aStates) if (itsState == s) return;
		throw new IllegalStateException("Expected "+Arrays.asList(aStates)+", but is "+itsState);
	}

	@Override
	public void processMessage(byte aMessage, BufferStream aBuffer)
	{
		switch(aMessage)
		{
		case Message.BEHAVIOR_ENTER_ARGS: args(aBuffer); break;
		case Message.ARRAY_READ: evArrayRead(aBuffer); break;
		case Message.ARRAY_LENGTH: evArrayLength(aBuffer); break;
		case Message.CONSTANT: evCst(aBuffer); break;
		case Message.FIELD_READ: evFieldRead(aBuffer); break;
		case Message.FIELD_READ_SAME: evFieldRead_Same(); break;
		case Message.NEW_ARRAY: evNewArray(aBuffer); break;
		case Message.EXCEPTION: evExceptionGenerated(aBuffer); break;
		case Message.HANDLER_REACHED: evHandlerReached(aBuffer); break;
		case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION: evExitException(aBuffer); break;
		
		case Message.OBJECT_INITIALIZED: 
			evObjectInitialized(getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Message.CONSTRUCTOR_TARGET: 
			evConstructorTarget(getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			evInScopeBehaviorEnter(getThreadReplayer().getBehIdReceiver().receiveFull(aBuffer)); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			evInScopeBehaviorEnter(getThreadReplayer().getBehIdReceiver().receiveDelta(aBuffer)); 
			break;
			
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER: evOutOfScopeBehaviorEnter(); break;
		case Message.INSCOPE_CLINIT_ENTER: evInScopeClinitEnter(aBuffer.getInt()); break;
		case Message.OUTOFSCOPE_CLINIT_ENTER: evOutOfScopeClinitEnter(); break;
		case Message.CLASSLOADER_ENTER: evClassloaderEnter(); break;
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_RESULT: evOutOfScopeBehaviorExitResult(aBuffer); break;
			
		default: throw new IllegalStateException(""+Message._NAMES[aMessage]);
		}
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
			
		case Type.BOOLEAN: 
		case Type.BYTE: 
		case Type.CHAR:
		case Type.SHORT: 
		case Type.INT: 
			itsIntCache[aCacheSlot] = vInt(); 
			break;
			
		case Type.DOUBLE: itsDoubleCache[aCacheSlot] = vDouble(); break;
		case Type.FLOAT: itsFloatCache[aCacheSlot] = vFloat(); break;
		case Type.LONG: itsLongCache[aCacheSlot] = vLong(); break;
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
			
		case Type.BOOLEAN: 
		case Type.BYTE: 
		case Type.CHAR: 
		case Type.SHORT: 
		case Type.INT: 
			vInt(itsIntCache[aCacheSlot]); 
			break;
			
		case Type.DOUBLE: vDouble(itsDoubleCache[aCacheSlot]); break;
		case Type.FLOAT: vFloat(itsFloatCache[aCacheSlot]); break;
		case Type.LONG: vLong(itsLongCache[aCacheSlot]); break;
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
		setState(State.S_UNDEFINED);
		int theNextBlock = itsNextBlock;
		itsNextBlock = -1;
		
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("Proceed: %d", theNextBlock);
		proceed(theNextBlock);
	}
	
	private void transferArg(InScopeReplayerFrame aParent, Type aType, int aSlot)
	{
		switch(aType.getSort())
		{
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
		case Type.INT: 
			lIntSet(aSlot, aParent.aIntPop()); 
			break;
			
		case Type.OBJECT:
		case Type.ARRAY:
			lRefSet(aSlot, aParent.aRefPop()); 
			break;
			
		case Type.DOUBLE: lDoubleSet(aSlot, aParent.aDoublePop()); break;
		case Type.FLOAT: lFloatSet(aSlot, aParent.aFloatPop()); break;
		case Type.LONG: lLongSet(aSlot, aParent.aLongPop()); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}	
	}
	
	@Override
	public void transferResult(InScopeReplayerFrame aSource)
	{
		allowedStates(State.S_WAIT_INSCOPE_CLINIT_RESULT, State.S_WAIT_INSCOPE_RESULT);
		if (itsExpectObjectInitialized) throw new IllegalStateException();
		
		Type theType = aSource.itsReturnType;
		if (! theType.equals(itsExpectedType)) throw new IllegalStateException("Expected "+itsExpectedType+", got "+theType);
		
		switch(theType.getSort())
		{
        case Type.BOOLEAN: 
        case Type.BYTE: 
        case Type.CHAR: 
        case Type.SHORT: 
		case Type.INT: 
			vInt(aSource.vInt()); 
			break;
			
		case Type.DOUBLE: vDouble(aSource.vDouble()); break;
		case Type.FLOAT: vFloat(aSource.vFloat()); break;
		case Type.LONG: vLong(aSource.vLong()); break;
			
		case Type.OBJECT:
		case Type.ARRAY:
			vRef(aSource.vRef()); 
			break;
			
		case Type.VOID: break;
		
		default: throw new RuntimeException("Unknown type: "+theType);
		}	

		if (itsState == State.S_WAIT_INSCOPE_CLINIT_RESULT)
		{
			itsState = itsNextState;
			itsExpectedType = itsNextExpectedType;
		}
		else
		{
			next();
		}
	}
	
	@Override
	public void transferResult(BufferStream aBuffer)
	{
		switch(itsExpectedType.getSort())
		{
		case Type.OBJECT:
		case Type.ARRAY:
			sRefPush(getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Type.BOOLEAN: 
		case Type.BYTE: 
		case Type.CHAR: 
		case Type.SHORT: 
		case Type.INT: 
			vInt(aBuffer.getInt()); 
			break;
			
		case Type.DOUBLE: vDouble(aBuffer.getDouble()); break;
		case Type.FLOAT: vFloat(aBuffer.getFloat()); break;
		case Type.LONG: vLong(aBuffer.getLong()); break;
		
		case Type.VOID: break;
		
		default: throw new RuntimeException("Unknown type: "+itsExpectedType);
		}	
		
		if (itsExpectObjectInitialized)
		{
			if (itsExpectedType.getSort() != Type.VOID) throw new IllegalStateException();
			itsExpectObjectInitialized = false;
			if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectObjectInitialized");
			setState(State.S_WAIT_OBJECTINITIALIZED);
		}
		else
		{
			next();
		}
	}
	
	/**
	 * Starts the replay of the method, taking arguments from the parent replayer.
	 */
	public void startFromScope(InScopeReplayerFrame aParent)
	{
		allowedState(State.S_INITIALIZED);
		
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
			else lRefSet(0, aParent.aRefPop());
		}

		itsNextBlock = 0;
		setState(State.S_STARTED);
		next();
	}
	
	/**
	 * Starts the replay of the method, expecting explicit args.
	 */
	public void startFromOutOfScope()
	{
		allowedState(State.S_INITIALIZED);
		setState(State.S_WAIT_ARGS);
	}
	
	/**
	 * Reads a value corresponding to the specified type from the stream, and set it 
	 * as current value.
	 */
	private void readArg(BufferStream aBuffer, Type aType, int aSlot)
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
	
	/**
	 * Read the method arguments from the buffer
	 */
	public void args(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_ARGS);
		
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
		setState(State.S_STARTED);
		next();
	}
	
	
	
	private void evFieldRead(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_FIELD);
		
		readValue(itsExpectedType, aBuffer);
		if (itsExpectedFieldCacheSlot >= 0) storeFieldCache(itsExpectedType, itsExpectedFieldCacheSlot);

		next();
	}
	
	private void evFieldRead_Same()
	{
		allowedState(State.S_WAIT_FIELD);
		if (itsExpectedFieldCacheSlot < 0) throw new IllegalStateException();
		
		loadFieldCache(itsExpectedType, itsExpectedFieldCacheSlot);

		next();
	}
	
	private void evArrayRead(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_ARRAY);
		readValue(itsExpectedType, aBuffer);
		next();
	}
	
	private void evArrayLength(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_ARRAYLENGTH);
		int theLength = aBuffer.getInt();
		vInt(theLength);
		next();
	}
	
	private void evCst(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_CST);
		readValue(BCIUtils.getType(Type.OBJECT), aBuffer);
		next();
	}
	
	private void evNewArray(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_NEWARRAY);
		readValue(BCIUtils.getType(Type.OBJECT), aBuffer);
		next();
	}
	
	private void evObjectInitialized(ObjectId aObject)
	{
		allowedState(State.S_WAIT_OBJECTINITIALIZED);
		ObjectId theTmpId = sRefPeek();
		getThreadReplayer().getTmpIdManager().associate(theTmpId.getId(), aObject.getId());
		next();
	}
	
	private void evConstructorTarget(ObjectId aObject)
	{
		allowedState(State.S_WAIT_CONSTRUCTORTARGET);
		ObjectId theTmpId = lRefGet(0);
		getThreadReplayer().getTmpIdManager().associate(theTmpId.getId(), aObject.getId());
		next();
	}
	
	private void evExceptionGenerated(BufferStream aBuffer)
	{
		ExceptionInfo theExceptionInfo = getThreadReplayer().readExceptionInfo(aBuffer);
		
		evExceptionGenerated(
				theExceptionInfo.behaviorId, 
				theExceptionInfo.bytecodeIndex, 
				theExceptionInfo.exception);
	}
	

	public void evExceptionGenerated(int aBehaviorId, int aBytecodeIndex, ObjectId aException)
	{
		setState(State.S_EXCEPTION_THROWN);
		vRef(aException);
	}
	
	private void evHandlerReached(BufferStream aBuffer)
	{
		allowedState(State.S_EXCEPTION_THROWN);
		int theHandlerId = aBuffer.getInt();
		itsNextBlock = itsHandlerBlocks.get(theHandlerId);
		next();
	}
	
	private void evExitException(BufferStream aBuffer)
	{
		allowedStates(State.S_EXCEPTION_THROWN);
		getThreadReplayer().returnException();
		setState(State.S_FINISHED_EXCEPTION);
		itsNextBlock = -1;
	}
	
	private void constructorTarget(long aId)
	{
		ObjectId theTmpId = lRefGet(0);
		getThreadReplayer().getTmpIdManager().associate(theTmpId.getId(), aId);
		next();
	}

	private void evInScopeBehaviorEnter(int aBehaviorId)
	{
		allowedState(State.S_CALLING_MONITORED);
		InScopeReplayerFrame theChild = getThreadReplayer().createInScopeReplayer(this, aBehaviorId);
		getThreadReplayer().pushFrame(theChild);
		setState(State.S_WAIT_INSCOPE_RESULT);
		theChild.startFromScope(this);
	}
	
	private void evOutOfScopeBehaviorEnter()
	{
		allowedState(State.S_CALLING_MONITORED);
		EnveloppeReplayerFrame theChild = getThreadReplayer().createEnveloppeReplayer(this);
		getThreadReplayer().pushFrame(theChild);
		if (itsExpectedType.getSort() != Type.VOID) setState(State.S_WAIT_OUTOFSCOPE_RESULT);
		else setState(State.S_STARTED);
	}
	
	private void evInScopeClinitEnter(int aBehaviorId)
	{
		allowedState(State.S_CALLING_MONITORED);
		InScopeReplayerFrame theChild = getThreadReplayer().createInScopeReplayer(this, aBehaviorId);
		getThreadReplayer().pushFrame(theChild);
		itsNextState = itsState;
		setState(State.S_WAIT_INSCOPE_CLINIT_RESULT);
		itsNextExpectedType = itsExpectedType;
		itsExpectedType = Type.VOID_TYPE;
		theChild.startFromScope(this);
	}
	
	private void evOutOfScopeClinitEnter()
	{
		allowedState(State.S_CALLING_MONITORED);
		EnveloppeReplayerFrame theChild = getThreadReplayer().createEnveloppeReplayer(this);
		getThreadReplayer().pushFrame(theChild);
		// State is not changed, we are still expecting the actual call
	}
	
	private void evClassloaderEnter()
	{
		allowedState(State.S_WAIT_CLASSLOADER_ENTER);
		ClassloaderWrapperReplayerFrame theChild = getThreadReplayer().createClassloaderReplayer(this);
		getThreadReplayer().pushFrame(theChild);
		setState(State.S_WAIT_CLASSLOADER_EXIT);
	}
	
	@Override
	public void classloaderReturned()
	{
		allowedState(State.S_WAIT_CLASSLOADER_EXIT);
		itsPostClassloaderAction.run();
		itsPostClassloaderAction = null;
	}
	
	private void evOutOfScopeBehaviorExitResult(BufferStream aBuffer)
	{
		allowedState(State.S_WAIT_OUTOFSCOPE_RESULT);
		readValue(itsExpectedType, aBuffer);
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
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectField");
		setState(State.S_WAIT_FIELD);
		itsNextBlock = aNextBlock;
		itsExpectedFieldCacheSlot = aCacheSlot;
		expect(aSort);
	}
	
	protected void expectArray(int aSort, int aNextBlock)
	{
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectArray");
		setState(State.S_WAIT_ARRAY);
		itsNextBlock = aNextBlock;
		expect(aSort);
	}
	
	protected void expectArrayLength(int aNextBlock)
	{
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectArrayLength");
		setState(State.S_WAIT_ARRAYLENGTH);
		itsNextBlock = aNextBlock;
	}
	
	protected void expectCst(int aNextBlock)
	{
		if (ThreadReplayer.ECHO) if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectCst");
		
		if (getThreadReplayer().peekNextMessage() == Message.CLASSLOADER_ENTER)
		{
			setState(State.S_WAIT_CLASSLOADER_ENTER);
			itsPostClassloaderAction = new PostClassloaderCst(aNextBlock);
			return;
		}
		else
		{
			expectCst_postClassloader(aNextBlock);
		}
	}
	
	protected void expectCst_postClassloader(int aNextBlock)
	{
		setState(State.S_WAIT_CST);
		itsNextBlock = aNextBlock;
	}
	
	protected void expectNewArray(int aNextBlock)
	{
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectNewArray");
		
		if (getThreadReplayer().peekNextMessage() == Message.CLASSLOADER_ENTER)
		{
			setState(State.S_WAIT_CLASSLOADER_ENTER);
			itsPostClassloaderAction = new PostClassloaderNewArray(aNextBlock);
			return;
		}
		else
		{
			expectNewArray_postClassloader(aNextBlock);
		}
	}

	protected void expectNewArray_postClassloader(int aNextBlock)
	{
		setState(State.S_WAIT_NEWARRAY);
		itsNextBlock = aNextBlock;
	}
	
	protected void expectConstructorTarget(int aNextBlock)
	{
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectConstructorTarget");
		setState(State.S_WAIT_CONSTRUCTORTARGET);
		itsNextBlock = aNextBlock;
	}
	
	@Override
	public void expectException()
	{
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("expectException");
		setState(State.S_WAIT_EXCEPTION);
		itsNextBlock = -1;
	}
	
	/**
	 * If the next message is not an exception, proceed, otherwise
	 * expect exception.
	 */
	public void checkCast(int aNextBlock)
	{
		if (ThreadReplayer.ECHO) getThreadReplayer().echo("checkCast");
		if (getThreadReplayer().peekNextMessage() != Message.EXCEPTION)
		{
			itsNextBlock = aNextBlock;
			next();
		}
		else
		{
			expectException();
		}
	}
	
	protected void processReturn()
	{
		getThreadReplayer().returnNormal(this);
		setState(State.S_FINISHED_NORMAL);
		itsNextBlock = -1;
	}
	
	/**
	 * Reads a value corresponding to the specified type from the stream, and set it 
	 * as current value.
	 */
	private void readValue(Type aType, BufferStream aBuffer)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT:
		case Type.ARRAY:
			vRef(getThreadReplayer().readValue(aBuffer)); 
			break;
			
		case Type.BOOLEAN: vInt(aBuffer.get()); break;
		case Type.BYTE: vInt(aBuffer.get()); break;
		case Type.CHAR: vInt(aBuffer.getChar()); break;
		case Type.SHORT: vInt(aBuffer.getShort()); break;
		case Type.INT: vInt(aBuffer.getInt()); break;
		case Type.DOUBLE: vDouble(aBuffer.getDouble()); break;
		case Type.FLOAT: vFloat(aBuffer.getFloat()); break;
		case Type.LONG: vLong(aBuffer.getLong()); break;
		default: throw new RuntimeException("Unknown type: "+aType);
		}
	}
	
	// Get current value
	protected ObjectId vRef() { return itsRefValue; }
	protected double vDouble() { return itsDoubleValue; }
	protected float vFloat() { return itsFloatValue; }
	protected int vInt() { return itsIntValue; }
	protected long vLong() { return itsLongValue; }

	// Set current value
	protected void vRef(ObjectId v) { itsRefValue = v; }
	protected void vDouble(double v) { itsDoubleValue = v; }
	protected void vFloat(float v) { itsFloatValue = v; }
	protected void vInt(int v) { itsIntValue = v; }
	protected void vLong(long v) { itsLongValue = v; }
	
	// Push to operand stack
	protected void sRefPush(ObjectId v) { itsSaveStack.refPush(v); }
	protected void sIntPush(int v) { itsSaveStack.intPush(v); }
	protected void sDoublePush(double v) { itsSaveStack.doublePush(v); }
	protected void sFloatPush(float v) { itsSaveStack.floatPush(v); }
	protected void sLongPush(long v) { itsSaveStack.longPush(v); }
	
	// Pop from operand stack
	protected ObjectId sRefPop() { return itsSaveStack.refPop(); }
	protected int sIntPop() { return itsSaveStack.intPop(); }
	protected double sDoublePop() { return itsSaveStack.doublePop(); }
	protected float sFloatPop() { return itsSaveStack.floatPop(); }
	protected long sLongPop() { return itsSaveStack.longPop(); }
	
	// Peek operand stack
	protected ObjectId sRefPeek() { return itsSaveStack.refPeek(); }
	
	// Push to arg stack
	protected void aRefPush(ObjectId v) { itsArgsStack.refPush(v); }
	protected void aIntPush(int v) { itsArgsStack.intPush(v); }
	protected void aDoublePush(double v) { itsArgsStack.doublePush(v); }
	protected void aFloatPush(float v) { itsArgsStack.floatPush(v); }
	protected void aLongPush(long v) { itsArgsStack.longPush(v); }
	
	// Pop from arg stack
	protected ObjectId aRefPop() { return itsArgsStack.refPop(); }
	protected int aIntPop() { return itsArgsStack.intPop(); }
	protected double aDoublePop() { return itsArgsStack.doublePop(); }
	protected float aFloatPop() { return itsArgsStack.floatPop(); }
	protected long aLongPop() { return itsArgsStack.longPop(); }
	
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

	
	/**
	 * Processes an invocation
	 */
	protected void invoke(int aBehaviorId, int aNextBlock, boolean aExpectObjectInitialized)
	{
		getThreadReplayer().processStatelessMessages();
		
		// If the next message is a classloader enter, we must process its execution first
		// because a TRACEDMETHODS_VERSION might come next
		if (getThreadReplayer().peekNextMessage() == Message.CLASSLOADER_ENTER)
		{
			setState(State.S_WAIT_CLASSLOADER_ENTER);
			itsPostClassloaderAction = new PostClassloaderInvocation(aBehaviorId, aNextBlock, aExpectObjectInitialized);
			return;
		}
		else
		{
			invoke_postClassloader(aBehaviorId, aNextBlock, aExpectObjectInitialized);
		}
	}
	
	protected void invoke_postClassloader(int aBehaviorId, int aNextBlock, boolean aExpectObjectInitialized)
	{
		// If the next message is TRACEDMETHODS_VERSION, process it before going on,
		// because it might affect the monitoring mode for the call
		getThreadReplayer().processStatelessMessages();
		
		int theMode = getThreadReplayer().getBehaviorMonitoringMode(aBehaviorId);
		IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
		itsExpectedType = Type.getType(theBehavior.getReturnType().getJvmName());
		
		switch(theMode)
		{
		case MonitoringMode.FULL:
		case MonitoringMode.ENVELOPPE:
			if (ThreadReplayer.ECHO) getThreadReplayer().echo("invoke (monitored): "+theBehavior+" ["+aExpectObjectInitialized+"]");
			setState(State.S_CALLING_MONITORED);
			break;
			
		case MonitoringMode.NONE:
			if (ThreadReplayer.ECHO) getThreadReplayer().echo("invoke (unmonitored): "+theBehavior+" ["+aExpectObjectInitialized+"]");
			UnmonitoredReplayerFrame theChild = getThreadReplayer().createUnmonitoredReplayer(this);
			getThreadReplayer().pushFrame(theChild);
			setState(State.S_CALLING_UNMONITORED);
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
	
	private static abstract class PostClassloaderAction
	{
		protected abstract void run();
	}
	
	private class PostClassloaderInvocation extends PostClassloaderAction
	{
		final int behaviorId;
		final int nextBlock;
		final boolean expectObjectInitialized;

		public PostClassloaderInvocation(int aBehaviorId, int aNextBlock, boolean aExpectObjectInitialized)
		{
			behaviorId = aBehaviorId;
			nextBlock = aNextBlock;
			expectObjectInitialized = aExpectObjectInitialized;
		}
		
		@Override
		protected void run()
		{
			invoke_postClassloader(behaviorId, nextBlock, expectObjectInitialized);
		}
	}
	
	private class PostClassloaderCst extends PostClassloaderAction
	{
		final int nextBlock;

		public PostClassloaderCst(int aNextBlock)
		{
			nextBlock = aNextBlock;
		}
		
		@Override
		protected void run()
		{
			expectCst_postClassloader(nextBlock);
		}
	}
	
	private class PostClassloaderNewArray extends PostClassloaderAction
	{
		final int nextBlock;
		
		public PostClassloaderNewArray(int aNextBlock)
		{
			nextBlock = aNextBlock;
		}
		
		@Override
		protected void run()
		{
			expectNewArray_postClassloader(nextBlock);
		}
	}
}
