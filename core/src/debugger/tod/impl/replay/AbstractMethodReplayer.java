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

import org.objectweb.asm.Type;

import tod.agent.io._ByteBuffer;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.bci.asm2.BCIUtils;

public abstract class AbstractMethodReplayer
{
	// States
	private static int s = 0;
	static final int S_START = s++; 
	static final int S_WAIT_FIELD = s++;

	
	private int itsCurrentBlock = 0;
	private int itsState = S_START;
	
	/**
	 * The type for expected values (for field reads and behavior calls).
	 */
	private Type itsExpectedType;
	
	private ObjectId itsRefValue;
	private boolean itsBooleanValue;
	private byte itsByteValue;
	private char itsCharValue;
	private double itsDoubleValue;
	private float itsFloatValue;
	private int itsIntValue;
	private long itsLongValue;
	private short itsShortValue;

	/**
	 * Read the method arguments from the buffer
	 */
	public abstract void args(_ByteBuffer aBuffer);
	
	/**
	 * Directly set the method arguments.
	 */
	public abstract void setArgs(Object[] aArgs);
	
	public void start()
	{
		proceed();
	}
	
	public void evFieldRead(_ByteBuffer aBuffer)
	{
		if (itsState != S_WAIT_FIELD) throw new IllegalStateException();
	}
	
	public void evFieldRead_Same()
	{
		if (itsState != S_WAIT_FIELD) throw new IllegalStateException();		
	}
	
	public abstract void evArrayRead(_ByteBuffer aBuffer);
	public abstract void evNew(Object aObject);
	public abstract void evObjectInitialized(Object aObject);
	public abstract void evExceptionGenerated(IBehaviorInfo aBehavior, int aBytecodeIndex, Object aException);
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
	
	protected void expectField(int aSort, int aNextBlock)
	{
		itsState = S_WAIT_FIELD;
		itsCurrentBlock = aNextBlock;
		expect(aSort);
	}
	
	private void readValue(Type aType, _ByteBuffer aBuffer)
	{
		switch(aType.getSort())
		{
		case Type.OBJECT:
			itsRefValue = new ObjectId(aBuffer.getLong());
			break;
			
		case Type.BOOLEAN: 
			itsBooleanValue = aBuffer.get() != 0;
			break;
			
		case Type.BYTE:
			itsByteValue = aBuffer.get();
			break;
			
		case Type.CHAR:
			itsCharValue = aBuffer.getChar();
			break;
			
		case Type.DOUBLE:
			itsDoubleValue = aBuffer.getDouble();
			break;
			
		case Type.FLOAT:
			itsFloatValue = aBuffer.getFloat();
			break;
			
		case Type.INT:
			itsIntValue = aBuffer.getInt();
			break;
			
		case Type.LONG:
			itsLongValue = aBuffer.getLong();
			break;
			
		case Type.SHORT:
			itsShortValue = aBuffer.getShort();
			break;
			
		default: throw new RuntimeException("Unknown type: "+aType);
		}
	}
	
	protected ObjectId vRef()
	{
		return itsRefValue;
	}
	
	protected boolean vBoolean()
	{
		return itsBooleanValue;
	}
	
	protected byte vByte()
	{
		return itsByteValue;
	}
	
	protected char vChar()
	{
		return itsCharValue;
	}
	
	protected double vDouble()
	{
		return itsDoubleValue;
	}
	
	protected float vFloat()
	{
		return itsFloatValue;
	}
	
	protected int vInt()
	{
		return itsIntValue;
	}
	
	protected long vLong()
	{
		return itsLongValue;
	}
	
	protected short vShort()
	{
		return itsShortValue;
	}

	protected abstract void proceed();
}
