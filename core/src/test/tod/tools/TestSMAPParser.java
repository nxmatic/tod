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
package tod.tools;

import junit.framework.Assert;

import org.junit.Test;

import tod.tools.parsers.ParseException;
import tod.tools.parsers.smap.SMAP;
import tod.tools.parsers.smap.SMAPFactory;

public class TestSMAPParser
{
	@Test
	public void test1() throws ParseException
	{
		String data = "SMAP\n"
			+"out.java\n"
			+"XYZ\n"
			+"*S XYZ\n"
			+"*F\n"
			+"0 in.java\n"
			+"*L\n" 
			+"123:207\n"
			+"130,3:210\n" 
			+"140:250,7\n" 
			+"160,3:300,2\n"
			+"*E\n";
		
		SMAP smap = SMAPFactory.parseSMAP(data);
		Assert.assertEquals(123, smap.getSourceLoc(207).lineNumber);
		Assert.assertEquals(130, smap.getSourceLoc(210).lineNumber);
		Assert.assertEquals(131, smap.getSourceLoc(211).lineNumber);
		Assert.assertEquals(132, smap.getSourceLoc(212).lineNumber);
		
		Assert.assertEquals(140, smap.getSourceLoc(250).lineNumber);
		Assert.assertEquals(140, smap.getSourceLoc(251).lineNumber);
		Assert.assertEquals(140, smap.getSourceLoc(252).lineNumber);
		Assert.assertEquals(140, smap.getSourceLoc(253).lineNumber);
		Assert.assertEquals(140, smap.getSourceLoc(254).lineNumber);
		Assert.assertEquals(140, smap.getSourceLoc(255).lineNumber);
		Assert.assertEquals(140, smap.getSourceLoc(256).lineNumber);
		
		Assert.assertEquals(160, smap.getSourceLoc(300).lineNumber);
		Assert.assertEquals(160, smap.getSourceLoc(301).lineNumber);
		
		Assert.assertEquals(161, smap.getSourceLoc(302).lineNumber);
		Assert.assertEquals(161, smap.getSourceLoc(303).lineNumber);
		
		Assert.assertEquals(162, smap.getSourceLoc(304).lineNumber);
		Assert.assertEquals(162, smap.getSourceLoc(305).lineNumber);
			
	}
}
