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

import tod.core.database.structure.IStructureDatabase;
import tod2.agent.Message;
import tod2.agent.io._ByteBuffer;

public abstract class MethodReplayer
{
	// States
	static final int S_UNDEFINED = 0; 
	static final int S_INITIALIZED = 1; // The replayer is ready to start
	static final int S_WAIT_ARGS = 2; // Waiting for method args
	static final int S_STARTED = 3; // The replayer has started replaying
	static final int S_WAIT_FIELD = 4; // Waiting for a field value
	static final int S_WAIT_ARRAY = 5; // Waiting for an array slot value
	static final int S_WAIT_NEWARRAY = 6; // Waiting for a new array ref
	static final int S_WAIT_CST = 7; // Waiting for a class constant (LDC)
	static final int S_WAIT_EXCEPTION = 8; // Waiting for an exception
	static final int S_EXCEPTION_THROWN = 9; // An exception was thrown, expect handler or exit
	
	// Public states (used by ThreadReplayer)
	public static final int S_FINISHED_NORMAL = 10; // Execution finished normally
	public static final int S_FINISHED_EXCEPTION = 11; // Execution finished because an exception was thrown
	public static final int S_CALLING_MONITORED = 12; // Processing invocation of monitored code
	public static final int S_CALLING_UNMONITORED = 13; // Processing invocation of unmonitored code
	
	private int itsState = S_INITIALIZED;
	private ThreadReplayer itsThreadReplayer;
	
	/**
	 * Finishes the setup of this replayer (we don't add these args
	 * to the constructor to simplify generated code). 
	 */
	public void setup(ThreadReplayer aThreadReplayer)
	{
		itsThreadReplayer = aThreadReplayer;
	}
	
	public ThreadReplayer getThreadReplayer()
	{
		return itsThreadReplayer;
	}
	
	public IStructureDatabase getDatabase()
	{
		return getThreadReplayer().getDatabase();
	}
	
	public int getState()
	{
		return itsState;
	}
	
	protected void setState(int aState)
	{
		itsState = aState;
	}
	
	protected void allowedState(int aState)
	{
		if (itsState != aState) throw new IllegalStateException();
	}
	
	protected void allowedStates(int... aStates)
	{
		for (int s : aStates) if (itsState == s) return;
		throw new IllegalStateException();
	}

	public abstract void processMessage(byte aMessage, _ByteBuffer aBuffer);

	/**
	 * Transfers a value from the source replayer's stack to this replayer's stack.
	 */
	public abstract void transferResult(InScopeMethodReplayer aSource);
	
	/**
	 * Transfers a value from the input buffer (usually from {@link Message#BEHAVIOR_ENTER_ARGS})
	 * to this replayer's stack.
	 */
	public abstract void transferResult(_ByteBuffer aBuffer);
	
	public abstract void expectException();

}
