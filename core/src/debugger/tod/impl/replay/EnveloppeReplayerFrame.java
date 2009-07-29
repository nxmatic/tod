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

import tod.impl.replay.ThreadReplayer.ExceptionInfo;
import tod.impl.server.BufferStream;
import tod2.agent.Message;

public class EnveloppeReplayerFrame extends ReplayerFrame
{
	private byte itsLastMessage = -1;
	private ExceptionInfo itsLastException = null;
	private State itsState = State.DEFAULT;

	@Override
	public void processMessage(byte aMessage, BufferStream aBuffer)
	{
		switch(aMessage)
		{
		case Message.EXCEPTION:
		{
			itsLastException = getThreadReplayer().readExceptionInfo(aBuffer);
			// TODO: register the exception
			break;
		}
		
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

		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_NORMAL: getThreadReplayer().returnNormal(); break;
		case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION: getThreadReplayer().returnException(); break;
		
		default: throw new RuntimeException("Command not handled: "+Message._NAMES[aMessage]);
		}
		
		itsLastMessage = aMessage;
	}
		
	private void evInScopeBehaviorEnter(int aBehaviorId)
	{
		InScopeReplayerFrame theChild = getThreadReplayer().createInScopeReplayer(this, aBehaviorId);
		theChild.startFromOutOfScope();
		getThreadReplayer().pushFrame(theChild);
	}
	
	private void evOutOfScopeBehaviorEnter()
	{
		EnveloppeReplayerFrame theChild = getThreadReplayer().createEnveloppeReplayer(this);
		getThreadReplayer().pushFrame(theChild);
	}
	
	private void evInScopeClinitEnter(int aBehaviorId)
	{
		InScopeReplayerFrame theChild = getThreadReplayer().createInScopeReplayer(this, aBehaviorId);
		theChild.startFromOutOfScope();
		getThreadReplayer().pushFrame(theChild);
	}
	
	private void evOutOfScopeClinitEnter()
	{
		EnveloppeReplayerFrame theChild = getThreadReplayer().createEnveloppeReplayer(this);
		getThreadReplayer().pushFrame(theChild);
	}
	
	private void evClassloaderEnter()
	{
		ClassloaderWrapperReplayerFrame theChild = getThreadReplayer().createClassloaderReplayer(this);
		getThreadReplayer().pushFrame(theChild);
	}

	private static enum State
	{
		DEFAULT, EXPECT_RESULT;
	}
}
