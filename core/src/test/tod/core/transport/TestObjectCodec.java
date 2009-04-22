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
package tod.core.transport;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.tod.ObjectValueFactory;
import java.tod.transport.ObjectEncoder;

import org.junit.Test;

import tod.agent.io._ByteBuffer;

/**
 * Test object encoding/decoding
 * This test requires to have the agent in the bootstrap classpath. 
 * @author gpothier
 */
public class TestObjectCodec
{
	@Test public void testCodec()
	{
		doTest("Hola");
		doTest(new FileNotFoundException("Hop"));
	}
	
	void doTest(Object aObject)
	{
		aObject = ObjectValueFactory.convert(aObject);
		_ByteBuffer theBuffer = _ByteBuffer.allocate(10000);
		ObjectEncoder.encode(aObject, theBuffer);
		byte[] theData = new byte[theBuffer.position()];
		System.arraycopy(theBuffer.array(), 0, theData, 0, theBuffer.position());
		
		DataInputStream theIn = new DataInputStream(new ByteArrayInputStream(theData));
		Object theObject = ObjectDecoder.decode(theIn);
	}
}
