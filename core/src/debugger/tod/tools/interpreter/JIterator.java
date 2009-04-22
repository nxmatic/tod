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
package tod.tools.interpreter;

import java.util.Iterator;

import zz.jinterp.JBehavior;
import zz.jinterp.JClass;
import zz.jinterp.JInstance;
import zz.jinterp.JInterpreter;
import zz.jinterp.JPrimitive.JBoolean;

/**
 * Utility class that wraps an interpreted {@link Iterator}
 * @author gpothier
 */
public class JIterator implements Iterator<JInstance>
{
	private final JInterpreter itsInterpreter;
	
	private final JClass itsIteratorClass;
	private final JBehavior itsNextMethod;
	private final JBehavior itsHasNextMethod;
	
	private final JInstance itsInstance;
	
	public JIterator(JInterpreter aInterpreter, JInstance aIteratorInstance)
	{
		itsInterpreter = aInterpreter;
		itsInstance = aIteratorInstance;
		
		itsIteratorClass = itsInterpreter.getClass("java/util/Iterator");
		itsNextMethod = itsIteratorClass.getVirtualBehavior("next", "()Ljava/lang/Object;");
		itsHasNextMethod = itsIteratorClass.getVirtualBehavior("hasNext", "()Z");
	}
	
	public boolean hasNext()
	{
		JBoolean theResult = (JBoolean) itsHasNextMethod.invoke(null, itsInstance, JInterpreter.NOARGS);
		return theResult.v;
	}

	public JInstance next()
	{
		return (JInstance) itsNextMethod.invoke(null, itsInstance, JInterpreter.NOARGS);
	}

	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
}
