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

import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.ObjectId;
import tod2.agent.Message;
import tod2.agent.MonitoringMode;
import zz.utils.Utils;

public abstract class InScopeReplayerFrame extends ReplayerFrame
{
	private int itsId;
	private String itsName;
	private int itsAccess;
	
	private Type[] itsArgTypes;
	private Type itsReturnType;	
	
	protected InScopeReplayerFrame()
	{
	}
	
	@Override
	public boolean isInScope()
	{
		return true;
	}
	
	public void setSignature(int aId, String aName, int aAccess, Type[] aArgTypes, Type aReturnType)
	{
		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL) System.out.println("InScopeReplayerFrame.InScopeReplayerFrame(): "+aName);
		itsId = aId;
		itsName = aName;
		itsAccess = aAccess;
		itsArgTypes = aArgTypes;
		itsReturnType = aReturnType;
	}
	
	private void processException()
	{
		ObjectId theException = readException();
		byte m = super.getNextMessage();
		switch(m)
		{
		case Message.HANDLER_REACHED: 
			throw new HandlerReachedException(theException, readInt());
			
		case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION:
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION: // Because when we process exceptions we are already in the outer frame
			throw new BehaviorExitException();
			
		case Message.UNMONITORED_BEHAVIOR_CALL_EXCEPTION:
			throw new UnmonitoredBehaviorCallException();
			
		default: throw new UnexpectedMessageException(m);
		}
	}
	
	@Override
	protected byte getNextMessage()
	{
		byte m = super.getNextMessage();
		if (m == Message.EXCEPTION) 
		{
			processException();
			throw new RuntimeException("processException should always throw an exception");
		}
		return m;
	}
	
	@Override
	protected byte peekNextMessage()
	{
		byte m = super.peekNextMessage();
//		if (m == Message.EXCEPTION)  
//		{
//			super.getNextMessage(); // Consume the message
//			processException();
//			throw new RuntimeException("processException should always throw an exception");
//		}
		return m;
	}
	
	protected void invokeClassloader()
	{
		ClassloaderWrapperReplayerFrame theChild = getReplayer().createClassloaderFrame(this, null);
		theChild.invoke_OOS();
	}
	
	protected void expectException()
	{
		byte m = getNextMessage();
		throw new UnexpectedMessageException(m); // should never get executed: getNextMessage should throw an exception
	}
	
	protected ObjectId expectConstant()
	{
		byte m = getNextMessageConsumingClassloading();
		if (m == Message.CONSTANT) return readRef();
		else throw new UnexpectedMessageException(m);
	}
	
	protected int expectInstanceofOutcome()
	{
		byte m = getNextMessageConsumingClassloading();
		if (m == Message.INSTANCEOF_OUTCOME) return readByte();
		else throw new UnexpectedMessageException(m);
	}
	
	protected ObjectId expectNewArray()
	{
		byte m = getNextMessageConsumingClassloading();
		if (m == Message.NEW_ARRAY) return readRef();
		else throw new UnexpectedMessageException(m);
	}
	

	
	/**
	 * Note: can't read the value here as the type is not static
	 * (otherwise we would need a switch, not efficient).
	 */
	protected void expectArrayRead()
	{
//		byte m = getNextMessage();
		byte m = getNextMessageConsumingClassloading();
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
	
	/**
	 * Consumes all classloading-related messages (classloader enter, clinit),
	 * and returns the next (non classloading-related) message.
	 * @return
	 */
	protected byte getNextMessageConsumingClassloading()
	{
		skipClassloading();
		return getNextMessage();
	}
	

	private byte peekNextMessageConsumingClassloading()
	{
		while(true)
		{
			byte theMessage = peekNextMessage();
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
				InScopeReplayerFrame theReplayer = getReplayer().createInScopeFrame(this, theBehaviorId, "bid: "+theBehaviorId);
				theReplayer.invokeVoid_S();
				break;
			}
				
			case Message.OUTOFSCOPE_CLINIT_ENTER:
			{
				getNextMessage();
				EnveloppeReplayerFrame theReplayer = getReplayer().createEnveloppeFrame(this, Type.VOID_TYPE, null);
				theReplayer.invokeVoid_S();
				break;
			}
			
			default: 
				return theMessage;
			}
		}
	}
	
	private void skipClassloading()
	{
		peekNextMessageConsumingClassloading();
	}
	
	protected ReplayerFrame invoke(int aBehaviorId)
	{
		byte theMessage = peekNextMessageConsumingClassloading();
		
		int theMode = getReplayer().getBehaviorMonitoringMode(aBehaviorId);
		int theCallMode = theMode & MonitoringMode.MASK_CALL;
		
		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL) Utils.println(
				"InScopeReplayerFrame.invoke(): [%s] (%d) %s", 
				LocationUtils.toMonitoringModeString(theMode), 
				aBehaviorId,
				getReplayer().getDatabase().getBehavior(aBehaviorId, true));
		
		switch(theCallMode)
		{
		case MonitoringMode.CALL_MONITORED:
			return invokeMonitored(theMessage, aBehaviorId);
			
		case MonitoringMode.CALL_UNKNOWN:
		case MonitoringMode.CALL_UNMONITORED:
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
				return getReplayer().createInScopeFrame(this, theBehaviorId, "bid: "+theBehaviorId);
			}
				
			case Message.INSCOPE_BEHAVIOR_ENTER_DELTA:
			{
				getNextMessage();
				int theBehaviorId = getReplayer().getBehIdReceiver().receiveDelta(getStream());
				return getReplayer().createInScopeFrame(this, theBehaviorId, "bid: "+theBehaviorId);
			}

			case Message.OUTOFSCOPE_BEHAVIOR_ENTER:
				getNextMessage();
				return getReplayer().createEnveloppeFrame(this, getReplayer().getBehaviorReturnType(aBehaviorId), "bid: "+aBehaviorId);
				
			default: throw new UnexpectedMessageException(aMessage, "bid: "+aBehaviorId);
		}
	}
	
	private ReplayerFrame invokeUnmonitored(byte aMessage, int aBehaviorId)
	{
		return getReplayer().createUnmonitoredFrame(this, getReplayer().getBehaviorReturnType(aBehaviorId), "bid: "+aBehaviorId);
	}
	
	protected TmpObjectId nextTmpId()
	{
		return new TmpObjectId(getReplayer().getTmpIdManager().nextId());
	}
	
	protected TmpObjectId nextTmpId_skipClassloading()
	{
		skipClassloading();
		return nextTmpId();
	}
	
	protected void waitObjectInitialized(TmpObjectId aId)
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.OBJECT_INITIALIZED) throw new UnexpectedMessageException(theMessage);
		
		ObjectId theActualRef = readRef();
		getReplayer().getTmpIdManager().associate(aId.getId(), theActualRef.getId());
		
		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL)
		{
			Utils.println("ObjectInitialized [old: %d, new %d]", aId.getId(), theActualRef.getId());
		}
		
		aId.setId(theActualRef.getId());
	}
	
	protected void waitConstructorTarget(ObjectId aId)
	{
		byte theMessage = getNextMessage();
		if (theMessage != Message.CONSTRUCTOR_TARGET) throw new UnexpectedMessageException(theMessage);
		
		ObjectId theActualRef = readRef();
		getReplayer().getTmpIdManager().associate(aId.getId(), theActualRef.getId());

		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL)
		{
			Utils.println("ConstructorTarget [old: %d, new %d]", aId.getId(), theActualRef.getId());
		}

		((TmpObjectId) aId).setId(theActualRef.getId());
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
	
	public LocalsSnapshot createSnapshot(int aProbeId)
	{
		return getReplayer().createSnapshot(aProbeId);
	}
	
	public boolean isSnapshotDue()
	{
		return getReplayer().isSnapshotDue();
	}
	
	public LocalsSnapshot getSnapshotForResume()
	{
		return getReplayer().getSnapshotForResume();
	}

	/**
	 * A factory that creates a particular (generated) subclass of {@link InScopeReplayerFrame}.
	 * @author gpothier
	 */
	public static abstract class Factory
	{
		private int itsId;
		private String itsName;
		private int itsAccess;
		private Type[] itsArgTypes;
		private Type itsReturnType;

		public void setSignature(int aId, String aName, int aAccess, String aDescriptor)
		{
			itsId = aId;
			itsName = aName;
			itsAccess = aAccess;
			itsArgTypes = Type.getArgumentTypes(aDescriptor);
			itsReturnType = Type.getReturnType(aDescriptor);
		}

		public Type[] getArgTypes()
		{
			return itsArgTypes;
		}
		
		public Type getReturnType()
		{
			return itsReturnType;
		}
		
		public InScopeReplayerFrame create()
		{
			InScopeReplayerFrame theFrame = create0();
			theFrame.setSignature(itsId, itsName, itsAccess, itsArgTypes, itsReturnType);
			return theFrame;
		}
		
		protected abstract InScopeReplayerFrame create0();
	}
}
