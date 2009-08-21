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
	
	@Override
	protected byte getNextMessage()
	{
		byte m = super.getNextMessage();
		if (m == Message.EXCEPTION)
		{
			readException();
			m = super.getNextMessage();
			if (m == Message.HANDLER_REACHED) throw new HandlerReachedException(readInt());
			else if (m == Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION) throw new BehaviorExitException();
			else throw new UnexpectedMessageException(m);
		}
		return m;
	}
	
	protected ReplayerFrame createClassloaderFrame()
	{
		throw new UnsupportedOperationException();
	}
	
	protected void invokeClassloader()
	{
		throw new UnsupportedOperationException();
	}
	
	protected void expectException()
	{
		byte m = getNextMessage();
		throw new UnexpectedMessageException(m);
	}
	
	protected ObjectId expectConstant()
	{
		while(true)
		{
			byte m = getNextMessage();
			if (m == Message.CONSTANT) return readRef();
			else if (m == Message.CLASSLOADER_ENTER) invokeClassloader();
			else throw new UnexpectedMessageException(m);
		}
	}
	
	/**
	 * Note: can't read the value here as the type is not static
	 * (otherwise we would need a switch, not efficient).
	 */
	protected void expectArrayRead()
	{
		byte m = getNextMessage();
		if (m == Message.ARRAY_READ) return;
		else throw new UnexpectedMessageException(m);
	}
	
	protected int expectArrayLength()
	{
		byte m = getNextMessage();
		if (m == Message.ARRAY_LENGTH) return readInt();
		else throw new UnexpectedMessageException(m);
	}
	
	/**
	 * Returns normally if the next message is not an exception
	 */
	protected void checkCast()
	{
		if (isExceptionNext()) expectException();
	}
	
	protected ReplayerFrame invoke(int aBehaviorId)
	{
		int theMode = getReplayer().getBehaviorMonitoringMode(aBehaviorId);
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
				int theBehaviorId = readInt();
				InScopeReplayerFrame theReplayer = getReplayer().createInScopeFrame(this, theBehaviorId);
				theReplayer.invokeVoid();
				break;
			}
				
			case Message.OUTOFSCOPE_CLINIT_ENTER:
			{
				EnveloppeReplayerFrame theReplayer = getReplayer().createEnveloppeFrame(this);
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
				int theBehaviorId = getReplayer().getBehIdReceiver().receiveFull(getStream());
				return getReplayer().createInScopeFrame(this, theBehaviorId);
			}
				
			case Message.INSCOPE_BEHAVIOR_ENTER_DELTA:
			{
				int theBehaviorId = getReplayer().getBehIdReceiver().receiveDelta(getStream());
				return getReplayer().createInScopeFrame(this, theBehaviorId);
			}

			case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
				return getReplayer().createEnveloppeFrame(this);
				
			default: throw new UnexpectedMessageException(aMessage);
			
		}
		
	}
	
	private ReplayerFrame invokeUnmonitored(byte aMessage)
	{
		throw new UnsupportedOperationException();
	}
	
	protected ObjectId nextTmpId()
	{
		return new ObjectId(getReplayer().getTmpIdManager().nextId());
	}
	
	protected void waitObjectInitialized(ObjectId aId)
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.OBJECT_INITIALIZED) throw new UnexpectedMessageException(theMessage);
		
		ObjectId theActualRef = readRef();
		getReplayer().getTmpIdManager().associate(aId.getId(), theActualRef.getId());
	}
	
	protected void waitConstructorTarget(ObjectId aId)
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.CONSTRUCTOR_TARGET) throw new UnexpectedMessageException(theMessage);
		
		ObjectId theActualRef = readRef();
		getReplayer().getTmpIdManager().associate(aId.getId(), theActualRef.getId());
	}
	
	protected static boolean cmpId(ObjectId id1, ObjectId id2)
	{
		if (id1 == null && id2 == null) return true;
		if (id1 == null || id2 == null) return false;
		return id1.getId() == id2.getId();
	}
	
}
