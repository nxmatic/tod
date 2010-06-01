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

import static tod.impl.bci.asm2.BCIUtils.*;

import org.objectweb.asm.Type;

import tod.impl.bci.asm2.SyntaxInsnList;

public class SList extends SyntaxInsnList
{
	public SList()
	{
		super();
	}
	
	/**
	 * Creates a {@link RuntimeException} with the given message and leaves it on the stack
	 */
	public void createRTEx(String aMessage)
	{
		LDC(aMessage);
		INVOKESTATIC(CLS_REPLAYERFRAME, "createRtEx", "(Ljava/lang/String;)Ljava/lang/Exception;");
	}

	/**
	 * Creates a {@link UnsupportedOperationException} with the given message and leaves it on the stack
	 */
	public void createUnsupportedEx(String aMessage)
	{
		LDC(aMessage);
		INVOKESTATIC(CLS_REPLAYERFRAME, "createUnsupportedEx", "(Ljava/lang/String;)Ljava/lang/Exception;");
	}
	
	/**
	 * Creates a runtime exception with an int arg that must be on the stack. Leaves the resulting
	 * exception on the stack
	 */
	public void createRTExArg(String aMessage)
	{
		LDC(aMessage);
		INVOKESTATIC(CLS_REPLAYERFRAME, "createRtEx", "(ILjava/lang/String;)Ljava/lang/Exception;");
	}
	
	public void invokeReadRef()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readRef", "()"+DSC_OBJECTID);
	}
	
	public void invokeReadInt()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readInt", "()I");
	}
	
	public void invokeReadBoolean()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readBoolean", "()Z");
	}
	
	public void invokeReadByte()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readByte", "()B");
	}
	
	public void invokeReadChar()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readChar", "()C");
	}
	
	public void invokeReadShort()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readShort", "()S");
	}
	
	public void invokeReadFloat()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readFloat", "()F");
	}
	
	public void invokeReadLong()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readLong", "()J");
	}
	
	public void invokeReadDouble()
	{
		ALOAD(0);
		INVOKEVIRTUAL(CLS_INSCOPEREPLAYERFRAME, "readDouble", "()D");
	}
	
	public void invokeRead(Type aType)
	{
		switch(aType.getSort())
		{
		case Type.ARRAY:
		case Type.OBJECT: invokeReadRef(); break;
		case Type.INT: invokeReadInt(); break;
		case Type.BOOLEAN: invokeReadBoolean(); break;
		case Type.BYTE: invokeReadByte(); break;
		case Type.CHAR: invokeReadChar(); break;
		case Type.SHORT: invokeReadShort(); break;
		case Type.FLOAT: invokeReadFloat(); break;
		case Type.LONG: invokeReadLong(); break;
		case Type.DOUBLE: invokeReadDouble(); break;
		default: throw new RuntimeException("Not handled: "+aType);
		}
	}
}