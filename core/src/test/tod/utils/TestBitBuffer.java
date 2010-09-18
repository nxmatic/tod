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
package tod.utils;

import junit.framework.Assert;

import org.junit.Test;


public class TestBitBuffer
{
	private static final int N = 1024;
	
	@Test public void testPutGetInt()
	{
		BitBuffer buffer = BitBuffer.allocate(N);
		
		int i = 0;
		int j = 1;
		while(i+j<N)
		{
			buffer.put(1, j);
			i += j;
			j++;
			if (j>32) j = 1;
		}

		buffer.flip();
		i = 0;
		j = 1;
		while(i+j<N)
		{
			int v = buffer.getInt(j);
			if (v != 1) Assert.fail(""+v+"@"+buffer.position());
			i += j;
			j++;
			if (j>32) j = 1;
		}
	}
	
	@Test public void testPutGetLong()
	{
		BitBuffer buffer = BitBuffer.allocate(N);
		
		int i = 0;
		int j = 1;
		while(i+j<N)
		{
			buffer.put((long) 1, j);
			i += j;
			j++;
			if (j>64) j = 1;
		}
		
		buffer.flip();
		i = 0;
		j = 1;
		while(i+j<N)
		{
			long v = buffer.getLong(j);
			if (v != 1) Assert.fail(""+v+"@"+buffer.position());
			i += j;
			j++;
			if (j>64) j = 1;
		}
	}
	
	@Test public void testUnary()
	{
		BitBuffer buffer = BitBuffer.allocate(N);
		
		int i = 0;
		int v = 0;
		
		while(i+v+1<N)
		{
			buffer.putUnary(v);
			i += v+1;
			v++;
			if (v > 128) v = 0;
		}
		
		buffer.flip();
		i = 0;
		v = 0;
		while(i+v+1<N)
		{
			int t = buffer.getUnary();
			if (t != v) Assert.fail(""+t+"@"+buffer.position());
			i += v+1;
			v++;
			if (v > 128) v = 0;
		}
	}
	
	@Test public void testGammaInt()
	{
		BitBuffer buffer = BitBuffer.allocate(N);

		for(int i=Short.MIN_VALUE;i<Short.MAX_VALUE;i++)
		{
			buffer.position(0);
			buffer.putGamma(i);
			buffer.position(0);
			Assert.assertEquals(i, buffer.getGammaInt());
		}
	}
}
