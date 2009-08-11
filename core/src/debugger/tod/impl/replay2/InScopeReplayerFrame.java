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
import tod2.agent.MonitoringMode;

public abstract class InScopeReplayerFrame extends ReplayerFrame
{
	private final String itsName;
	private final int itsAccess;
	
	private final Type[] itsArgTypes;
	private final Type itsReturnType;
	
	private ThreadReplayer itsReplayer;
	private boolean itsFromScope;
	private BufferStream itsStream;
	
	/**
	 * @param aCacheCounts "encoded" cache counts.
	 */
	protected InScopeReplayerFrame(
			String aName, 
			int aAccess, 
			String aDescriptor)
	{
		itsName = aName;
		itsAccess = aAccess;
		
		itsArgTypes = Type.getArgumentTypes(aDescriptor);
		itsReturnType = Type.getReturnType(aDescriptor);
	}
	
	public void setup(ThreadReplayer aReplayer, boolean aFromScope)
	{
		itsReplayer = aReplayer;
		itsFromScope = aFromScope;
	}
	
	public ThreadReplayer getReplayer()
	{
		return itsReplayer;
	}
	
	public IStructureDatabase getDatabase()
	{
		return getReplayer().getDatabase();
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
	
	protected byte getNextMessage()
	{
		return itsReplayer.getNextMessage();
	}
	
	protected byte peekNextMessage()
	{
		return itsReplayer.peekNextMessage();
	}
	
	protected ReplayerFrame createClassloaderFrame()
	{
		throw new UnsupportedOperationException();
	}
	
	protected void invokeClassloader()
	{
		throw new UnsupportedOperationException();
	}
	
	protected ObjectId readException()
	{
		ExceptionInfo theInfo = itsReplayer.readExceptionInfo();
		// TODO: register exception
		return theInfo.exception;
	}
	
	protected ReplayerFrame invoke(int aBehaviorId)
	{
		int theMode = itsReplayer.getBehaviorMonitoringMode(aBehaviorId);
//		IBehaviorInfo theBehavior = getDatabase().getBehavior(aBehaviorId, true);
//		Type theExpectedType = Type.getType(theBehavior.getReturnType().getJvmName());

		byte theMessage = -1;
		loop:
		while(true)
		{
			theMessage = getNextMessage();
			switch(theMessage)
			{
			case Message.CLASSLOADER_ENTER:
				invokeClassloader();
				break;
				
			case Message.INSCOPE_CLINIT_ENTER:
			{
				int theBehaviorId = itsStream.getInt();
				InScopeReplayerFrame theReplayer = itsReplayer.createInScopeReplayer(this, theBehaviorId);
				theReplayer.invokeVoid();
				break;
			}
				
			case Message.OUTOFSCOPE_CLINIT_ENTER:
			{
				EnveloppeReplayerFrame theReplayer = itsReplayer.createEnveloppeReplayer(this);
				theReplayer.invokeVoid();
				break;
			}
			
			default: break loop;
			}
		}
		
		switch(theMode)
		{
		case MonitoringMode.FULL:
		case MonitoringMode.ENVELOPPE:
			return invokeMonitored(theMessage);
			
		case MonitoringMode.NONE:
			return invokeUnmonitored(theMessage);
			
		default:
			throw new RuntimeException("Not handled: "+theMode);
		}
	}
	
	private ReplayerFrame invokeMonitored(byte aMessage)
	{
		switch(aMessage)
		{
			case Message.INSCOPE_BEHAVIOR_ENTER:
			{
				int theBehaviorId = itsReplayer.getBehIdReceiver().receiveFull(itsStream);
				return itsReplayer.createInScopeReplayer(this, theBehaviorId);
			}
				
			case Message.INSCOPE_BEHAVIOR_ENTER_DELTA:
			{
				int theBehaviorId = itsReplayer.getBehIdReceiver().receiveDelta(itsStream);
				return itsReplayer.createInScopeReplayer(this, theBehaviorId);
			}

			case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
				return itsReplayer.createEnveloppeReplayer(this);
				
			default: throw new IllegalStateException(Message._NAMES[aMessage]);
			
		}
		
	}
	
	private ReplayerFrame invokeUnmonitored(byte aMessage)
	{
		throw new UnsupportedOperationException();
	}
	
	protected ObjectId nextTmpId()
	{
		return new ObjectId(itsReplayer.getTmpIdManager().nextId());
	}
	
	protected void waitObjectInitialized(ObjectId aId)
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.OBJECT_INITIALIZED) throw new IllegalStateException(Message._NAMES[theMessage]);
		
		ObjectId theActualRef = itsReplayer.readRef();
		itsReplayer.getTmpIdManager().associate(aId.getId(), theActualRef.getId());
	}
	
	protected void waitConstructorTarget(ObjectId aId)
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.CONSTRUCTOR_TARGET) throw new IllegalStateException(Message._NAMES[theMessage]);
		
		ObjectId theActualRef = itsReplayer.readRef();
		itsReplayer.getTmpIdManager().associate(aId.getId(), theActualRef.getId());
	}
	
	protected static boolean cmpId(ObjectId id1, ObjectId id2)
	{
		if (id1 == null && id2 == null) return true;
		if (id1 == null || id2 == null) return false;
		return id1.getId() == id2.getId();
	}
	
	protected static void throwRtEx(int aArg, String aMessage)
	{
		throw new IllegalStateException(aMessage+": "+aArg);
	}
	
	protected static void throwRtEx(String aMessage)
	{
		throw new IllegalStateException(aMessage);
	}
}
