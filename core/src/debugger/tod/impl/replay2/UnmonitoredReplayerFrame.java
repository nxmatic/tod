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

public class UnmonitoredReplayerFrame extends ReplayerFrame

{
	private ObjectId itsRefResult;
	private int itsIntResult;
	private long itsLongResult;
	private float itsFloatResult;
	private double itsDoubleResult;
	
	private byte itsLastMessage;
	private ObjectId itsLastException;
	
	private boolean itsRootFrame;
	
	public void setRootFrame(boolean aRootFrame)
	{
		itsRootFrame = aRootFrame;
	}
	
	@Override
	public boolean isInScope()
	{
		return false;
	}
	
	protected void replay()
	{
		try
		{
			while(! itsRootFrame || hasMoreMessages())
			{
				byte m = getNextMessage();
				
				boolean theContinue = replay(m);
				if (! theContinue) break;
			}
		}
		catch (ReplayerException e)
		{
			popped();
			throw e;
		}
	}
	
	/**
	 * Processes an individual message
	 * @return Wheter to continue or not.
	 */
	protected boolean replay(byte aMessage)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION:
		{
			itsLastException = readException();
			break;
		}
		
		case Message.INSCOPE_BEHAVIOR_ENTER: 
			evInScopeBehaviorEnter(getReplayer().getBehIdReceiver().receiveFull(getStream())); 
			break;
			
		case Message.INSCOPE_BEHAVIOR_ENTER_DELTA: 
			evInScopeBehaviorEnter(getReplayer().getBehIdReceiver().receiveDelta(getStream())); 
			break;
			
		case Message.OUTOFSCOPE_BEHAVIOR_ENTER: evOutOfScopeBehaviorEnter(); break;
		case Message.INSCOPE_CLINIT_ENTER: evInScopeClinitEnter(getStream().getInt()); break;
		case Message.OUTOFSCOPE_CLINIT_ENTER: evOutOfScopeClinitEnter(); break;
		case Message.CLASSLOADER_ENTER: evClassloaderEnter(); break;

		case Message.UNMONITORED_BEHAVIOR_CALL_RESULT: readResult(); return false;
		case Message.UNMONITORED_BEHAVIOR_CALL_EXCEPTION: throw new UnmonitoredBehaviorCallException();
//			byte m = getNextMessage();
//			if (m != Message.EXCEPTION) throw new UnexpectedMessageException(m);
//			itsLastException = readException();
//			break;
		
		case Message.HANDLER_REACHED:
			if (itsLastMessage != Message.EXCEPTION) throw new IllegalStateException();
			throw new HandlerReachedException(itsLastException, readInt());
			
		case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION: throw new BehaviorExitException(); 
		
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
	
		itsLastMessage = aMessage;
		
		return true;
	}
	
	private void evInScopeBehaviorEnter(int aBehaviorId)
	{
		InScopeReplayerFrame theChild = getReplayer().createInScopeFrame(this, aBehaviorId, "bid: "+aBehaviorId);
		theChild.invoke_OOS();
	}
	
	private void evOutOfScopeBehaviorEnter()
	{
		EnveloppeReplayerFrame theChild = getReplayer().createEnveloppeFrame(this, null, null);
		try
		{
			theChild.invoke_OOS();
		}
		catch(BehaviorExitException e)
		{
			expectException();
		}
	}
	
	private void evInScopeClinitEnter(int aBehaviorId)
	{
		InScopeReplayerFrame theChild = getReplayer().createInScopeFrame(this, aBehaviorId, "bid: "+aBehaviorId);
		try
		{
			theChild.invoke_OOS();
		}
		catch(BehaviorExitException e)
		{
			expectException();
		}
	}
	
	private void evOutOfScopeClinitEnter()
	{
		EnveloppeReplayerFrame theChild = getReplayer().createEnveloppeFrame(this, null, null);
		try
		{
			theChild.invoke_OOS();
		}
		catch(BehaviorExitException e)
		{
			expectException();
		}
	}
	
	private void evClassloaderEnter()
	{
		ClassloaderWrapperReplayerFrame theChild = getReplayer().createClassloaderFrame(this, null);
		try
		{
			theChild.invoke_OOS();
		}
		catch(BehaviorExitException e)
		{
			expectException();
		}
	}
	
	protected void expectException()
	{
		byte m = getNextMessage();
		switch(m)
		{
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION:
			// For unknown reasons, sometimes the EXCEPTION message is not generated
			// This seems to be related to exceptions generated in native code during class loading
			throw new BehaviorExitException();
			
		case Message.EXCEPTION:
			itsLastException = readException();
			break;
			
		default:
			throw new UnexpectedMessageException(m); 
		}
	}
	


	protected void readResult()
	{
		switch(getReturnType().getSort())
		{
		case Type.BOOLEAN: 
			itsIntResult = readBoolean() ? 1 : 0;
			break;
			
		case Type.BYTE:
			itsIntResult = readByte();
			break;
			
		case Type.CHAR:
			itsIntResult = readChar();
			break;
			
		case Type.SHORT:
			itsIntResult = readShort();
			break;
			
		case Type.INT:
			itsIntResult = readInt();
			break;
		
		case Type.LONG:
			itsLongResult = readLong();
			break;
		
		case Type.DOUBLE:
			itsDoubleResult = readDouble();
			break;
			
		case Type.FLOAT:
			itsFloatResult = readFloat();
			break;
			
		case Type.OBJECT:
		case Type.ARRAY:
			itsRefResult = readRef();
			
		case Type.VOID:
			break;
		
		default: throw new RuntimeException("Unexpected type: "+getReturnType());
		}
	}
	
	public double invokeDouble()
	{
		replay();
		return itsDoubleResult;
	}

	public double invokeDouble_S()
	{
		replay();
		return itsDoubleResult;
	}
	
	public float invokeFloat()
	{
		replay();
		return itsFloatResult;
	}

	public float invokeFloat_S()
	{
		replay();
		return itsFloatResult;
	}
	
	public int invokeInt()
	{
		replay();
		return itsIntResult;
	}

	public int invokeInt_S()
	{
		replay();
		return itsIntResult;
	}
	
	public long invokeLong()
	{
		replay();
		return itsLongResult;
	}

	public long invokeLong_S()
	{
		replay();
		return itsLongResult;
	}
	
	public ObjectId invokeRef()
	{
		replay();
		return itsRefResult;
	}

	public ObjectId invokeRef_S()
	{
		replay();
		return itsRefResult;
	}
	
	public void invokeVoid()
	{
		replay();
	}
	
	@Override
	public void invokeVoid_S()
	{
		replay();
	}
	
	@Override
	public void invoke_OOS()
	{
		replay();
	}

}
