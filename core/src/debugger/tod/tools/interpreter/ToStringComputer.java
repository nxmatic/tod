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


import tod.core.config.TODConfig;
import tod.core.database.browser.IObjectInspector;
import tod.tools.interpreter.TODInterpreter.TODInstance;
import zz.jinterp.JBehavior;
import zz.jinterp.JClass;
import zz.jinterp.JInstance;
import zz.jinterp.JObject;
import zz.jinterp.JType;
import zz.jinterp.JPrimitive.JVoid;

/**
 * Permits to compute the result of toString on reconstituted objects.
 * @author gpothier
 */
public class ToStringComputer
{
	static final JObject NULL = new JVoid();
	
	private final IObjectInspector itsInspector;
	private final TODInterpreter itsInterpreter;
	
	
	public ToStringComputer(TODConfig aConfig, IObjectInspector aInspector)
	{
		itsInspector = aInspector;
		
		itsInterpreter = new TODInterpreter(aConfig, aInspector.getLogBrowser());
		itsInterpreter.setRefEvent(itsInspector.getReferenceEvent());
	}
	
	/**
	 * Computes the toString of the inspected object. 
	 */
	public String compute()
	{
		JObject[] args = {}; 
		JType theType = itsInterpreter.convertType(itsInspector.getType());
		
		JObject theResult;
		if (theType instanceof JClass)
		{
			JClass theClass = (JClass) theType;
			JBehavior theBehavior = theClass.getVirtualBehavior("toString", "()Ljava/lang/String;");
			TODInstance theInstance = itsInterpreter.newInstance(theClass, itsInspector);
			theResult = theBehavior.invoke(null, theInstance, args);
		}
		else throw new RuntimeException("Not handled: "+theType);
		
		if (theResult instanceof JInstance)
		{
			JInstance theInstance = (JInstance) theResult;
			return itsInterpreter.toString(theInstance);
		}
		else throw new RuntimeException("Unexpected result: "+theResult);
	}
	
}
