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
package tod.impl.bci.asm;


/**
 * Instrumentation specification for a given class.
 * It is used to treat some cases 
 * @author gpothier
 */
public abstract class InstrumentationSpec
{
	public static InstrumentationSpec NORMAL = new All();
	
	public abstract boolean traceEnveloppe();
	public abstract boolean traceEntry();
	public abstract boolean traceExit();
	
	public abstract boolean traceFieldWrite(String aOwner, String aName, String aDesc);
	public abstract boolean traceVarWrite();
	public abstract boolean traceCall(String aOwner, String aName, String aDesc);
	public abstract boolean traceNewInstance();
	public abstract boolean traceNewArray();
	public abstract boolean traceArrayWrite();
	
	public abstract boolean traceInstanceOf();
	
	/**
	 * Whether the instrumenter should add a field to the class that
	 * indicates whether an instance was created by in-scope or out-of-scope
	 * code.
	 */
	public abstract boolean hasCreatedInScope();
	
	public static class All extends InstrumentationSpec
	{
		@Override
		public boolean traceArrayWrite()
		{
			return true;
		}

		@Override
		public boolean traceCall(String aOwner, String aName, String aDesc)
		{
			return true;
		}

		@Override
		public boolean traceEntry()
		{
			return true;
		}

		@Override
		public boolean traceEnveloppe()
		{
			return true;
		}

		@Override
		public boolean traceExit()
		{
			return true;
		}

		@Override
		public boolean traceFieldWrite(String aOwner, String aName, String aDesc)
		{
			return true;
		}

		@Override
		public boolean traceInstanceOf()
		{
			return true;
		}

		@Override
		public boolean traceNewArray()
		{
			return true;
		}

		@Override
		public boolean traceNewInstance()
		{
			return true;
		}

		@Override
		public boolean traceVarWrite()
		{
			return true;
		}
		
		@Override
		public boolean hasCreatedInScope()
		{
			return false;
		}
	}
	
	public static class None extends InstrumentationSpec
	{
		@Override
		public boolean traceArrayWrite()
		{
			return false;
		}

		@Override
		public boolean traceCall(String aOwner, String aName, String aDesc)
		{
			return false;
		}

		@Override
		public boolean traceEntry()
		{
			return false;
		}

		@Override
		public boolean traceEnveloppe()
		{
			return false;
		}

		@Override
		public boolean traceExit()
		{
			return false;
		}

		@Override
		public boolean traceFieldWrite(String aOwner, String aName, String aDesc)
		{
			return false;
		}

		@Override
		public boolean traceInstanceOf()
		{
			return false;
		}

		@Override
		public boolean traceNewArray()
		{
			return false;
		}

		@Override
		public boolean traceNewInstance()
		{
			return false;
		}

		@Override
		public boolean traceVarWrite()
		{
			return false;
		}

		@Override
		public boolean hasCreatedInScope()
		{
			return false;
		}
	}

}
