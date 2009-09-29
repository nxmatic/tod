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

import tod.core.database.structure.ObjectId;
import tod2.agent.Message;
import tod2.agent.MonitoringMode;
import zz.utils.Utils;

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
		if (ThreadReplayer.ECHO) System.out.println("InScopeReplayerFrame.InScopeReplayerFrame(): "+aName);
		itsName = aName;
		itsAccess = aAccess;
		
		itsArgTypes = Type.getArgumentTypes(aDescriptor);
		itsReturnType = Type.getReturnType(aDescriptor);
	}
	
	private void processException()
	{
		ObjectId theException = readException();
		byte m = super.getNextMessage();
		if (m == Message.HANDLER_REACHED) throw new HandlerReachedException(theException, readInt());
		else if (m == Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION) throw new BehaviorExitException();
		else throw new UnexpectedMessageException(m);
	}
	
	@Override
	protected byte getNextMessage()
	{
		byte m = super.getNextMessage();
		if (m == Message.EXCEPTION) processException();
		return m;
	}
	
	@Override
	protected byte peekNextMessage()
	{
		byte m = super.peekNextMessage();
		if (m == Message.EXCEPTION) processException();
		return m;
	}
	
	protected void invokeClassloader()
	{
		ClassloaderWrapperReplayerFrame theChild = getReplayer().createClassloaderFrame(this);
		theChild.invoke_OOS();
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
	
	protected ObjectId expectNewArray()
	{
		while(true)
		{
			byte m = getNextMessage();
			if (m == Message.NEW_ARRAY) return readRef();
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
		byte theMessage = -1;
		loop:
		while(true)
		{
			theMessage = peekNextMessage();
			switch(theMessage)
			{
			case Message.CLASSLOADER_ENTER:
				getNextMessage();
				invokeClassloader();
				break;
				
			case Message.INSCOPE_CLINIT_ENTER:
			{
				getNextMessage();
				int theBehaviorId = readInt();
				InScopeReplayerFrame theReplayer = getReplayer().createInScopeFrame(this, theBehaviorId);
				theReplayer.invokeVoid_S();
				break;
			}
				
			case Message.OUTOFSCOPE_CLINIT_ENTER:
			{
				getNextMessage();
				EnveloppeReplayerFrame theReplayer = getReplayer().createEnveloppeFrame(this, null);
				theReplayer.invokeVoid_S();
				break;
			}
			
			default: break loop;
			}
		}
		
		int theMode = getReplayer().getBehaviorMonitoringMode(aBehaviorId);
		if (ThreadReplayer.ECHO) Utils.println(
				"InScopeReplayerFrame.invoke(): [%s] (%d) %s", 
				MonitoringMode.toString(theMode), 
				aBehaviorId,
				getReplayer().getDatabase().getBehavior(aBehaviorId, true));
		

		switch(theMode)
		{
		case MonitoringMode.FULL:
		case MonitoringMode.ENVELOPPE:
			return invokeMonitored(theMessage, aBehaviorId);
			
		case MonitoringMode.NONE:
			return invokeUnmonitored(theMessage, aBehaviorId);
			
		default:
			throw new RuntimeException("Not handled: "+theMode);
		}
	}
	
	private ReplayerFrame invokeMonitored(byte aMessage, int aBehaviorId)
	{
		switch(aMessage)
		{
			case Message.INSCOPE_BEHAVIOR_ENTER:
			{
				getNextMessage();
				int theBehaviorId = getReplayer().getBehIdReceiver().receiveFull(getStream());
				return getReplayer().createInScopeFrame(this, theBehaviorId);
			}
				
			case Message.INSCOPE_BEHAVIOR_ENTER_DELTA:
			{
				getNextMessage();
				int theBehaviorId = getReplayer().getBehIdReceiver().receiveDelta(getStream());
				return getReplayer().createInScopeFrame(this, theBehaviorId);
			}

			case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
				getNextMessage();
				return getReplayer().createEnveloppeFrame(this, getReplayer().getBehaviorReturnType(aBehaviorId));
				
			default: throw new UnexpectedMessageException(aMessage);
		}
	}
	
	private ReplayerFrame invokeUnmonitored(byte aMessage, int aBehaviorId)
	{
		return getReplayer().createUnmonitoredFrame(this, getReplayer().getBehaviorReturnType(aBehaviorId));
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
	
	/**
	 * Checks that the next message is {@link Message#BEHAVIOR_ENTER_ARGS}. 
	 */
	protected void waitArgs()
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.BEHAVIOR_ENTER_ARGS) throw new UnexpectedMessageException(theMessage);
	}
	
	protected static boolean cmpId(ObjectId id1, ObjectId id2)
	{
		if (id1 == null && id2 == null) return true;
		if (id1 == null || id2 == null) return false;
		return id1.getId() == id2.getId();
	}
	
}
