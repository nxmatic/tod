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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import zz.utils.Utils;

public class ReplayerLoader extends ClassLoader
{
	private final ClassLoader itsParent;
	private final Map<String, byte[]> itsClassesMap = new HashMap<String, byte[]>();

	public ReplayerLoader(ClassLoader aParent)
	{
		itsParent = aParent;
	}
	
	public void addClass(String aName, byte[] aBytecode)
	{
		itsClassesMap.put(aName, aBytecode);
	}

	private boolean shouldLoad(String aName)
	{
		return aName.startsWith("tod.impl.replay2.") && ! aName.equals(getClass().getName());
	}
	
	@Override
	public Class loadClass(String aName) throws ClassNotFoundException
	{
		byte[] theBytecode = itsClassesMap.get(aName);
		if (theBytecode == null && shouldLoad(aName)) theBytecode = getClassBytecode(aName.replace('.', '/'));
		
		if (theBytecode != null) 
		{
			return super.defineClass(aName, theBytecode, 0, theBytecode.length);
		}
		else 
		{
			return itsParent.loadClass(aName);
		}
	}
	
	public static byte[] getClassBytecode(String aClassName)
	{
		try
		{
			InputStream theStream = ReplayerLoader.class.getResourceAsStream("/"+aClassName+".class");
			return Utils.readInputStream_byte(theStream);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	

}