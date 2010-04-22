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

import org.objectweb.asm.Type;

import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.replay2.ThreadReplayer.ExceptionInfo;
import tod.impl.server.BufferStream;
import tod2.agent.Message;

public abstract class ReplayerFrame
{
	private ThreadReplayer itsReplayer;
	private BufferStream itsStream;
	
	/**
	 * Information used only for debugging the replayer. 
	 */
	private String itsDebugInfo;
	
	private boolean itsFromScope;
	private Type itsReturnType;
	
	public void setup(ThreadReplayer aReplayer, BufferStream aStream, String aDebugInfo, boolean aFromScope, Type aReturnType)
	{
		itsReplayer = aReplayer;
		itsStream = aStream;
		itsDebugInfo = aDebugInfo;
		itsFromScope = aFromScope;
		itsReturnType = aReturnType;
	}
	
	public ThreadReplayer getReplayer()
	{
		return itsReplayer;
	}
	
	public EventCollector getCollector()
	{
		return getReplayer().getCollector();
	}
	
	public BufferStream getStream()
	{
		return itsStream;
	}
	
	public boolean isFromScope()
	{
		return itsFromScope;
	}
	
	public Type getReturnType()
	{
		return itsReturnType;
	}
	
	public IStructureDatabase getDatabase()
	{
		return getReplayer().getDatabase();
	}
	
	protected boolean hasMoreMessages()
	{
		return itsReplayer.hasMoreMessages();
	}
	
	protected byte getNextMessage()
	{
		return itsReplayer.getNextMessage();
	}
	
	protected byte peekNextMessage()
	{
		return itsReplayer.peekNextMessage();
	}
	
	/**
	 * Whether the next message will be an exception.
	 * This method only peeks the next message.
	 */
	protected boolean isExceptionNext()
	{
		return itsReplayer.peekNextMessage() == Message.EXCEPTION;
	}

	protected ObjectId readRef()
	{
		return itsReplayer.readRef();
	}
	
	protected int readInt()
	{
		return itsStream.getInt();
	}
	
	protected boolean readBoolean()
	{
		return itsStream.get() != 0;
	}
	
	protected byte readByte()
	{
		return itsStream.get();
	}
	
	protected char readChar()
	{
		return itsStream.getChar();
	}
	
	protected short readShort()
	{
		return itsStream.getShort();
	}
	
	protected float readFloat()
	{
		return itsStream.getFloat();
	}
	
	protected long readLong()
	{
		return itsStream.getLong();
	}
	
	protected double readDouble()
	{
		return itsStream.getDouble();
	}
	
	protected ObjectId readException()
	{
		ExceptionInfo theInfo = itsReplayer.readExceptionInfo();
		// TODO: register exception
		return theInfo.exception;
	}

	public void invokeVoid_S()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Calls the appropriate invoke method, and doesn't return its result
	 */
	public void invoke_OOS()
	{
		throw new UnsupportedOperationException("Should be overridden: "+this);
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
	
}
