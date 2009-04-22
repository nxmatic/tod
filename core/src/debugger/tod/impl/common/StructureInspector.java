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
package tod.impl.common;

import tod.core.config.TODConfig;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.event.ICreationEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.tools.interpreter.TODInterpreter;
import tod.tools.interpreter.TODInterpreter.TODInstance;
import zz.jinterp.JBehavior;
import zz.jinterp.JClass;
import zz.jinterp.JObject;
import zz.jinterp.JPrimitive.JInt;

/**
 * An synthetic object inspector that wraps another inspector and
 * provides a structured view of the object (eg for List, Map, etc.).
 * @author gpothier
 */
public abstract class StructureInspector implements IObjectInspector
{
	private final IObjectInspector itsOriginal;

	private final TODInterpreter itsInterpreter;
	private final JClass itsObjectClass;
	private final TODInstance itsInstance;
	
	private int itsEntryCount = -1;

	/**
	 * 
	 * @param aOriginal The wrapped inspector
	 * @param aClassName The class of the inspected object. Must be compatible with
	 * the actual type of the object. Eg. can be Map for an HashMap
	 */
	public StructureInspector(IObjectInspector aOriginal, String aClassName)
	{
		itsOriginal = aOriginal;
		
		TODConfig theConfig = getLogBrowser().getSession().getConfig();
		itsInterpreter = new TODInterpreter(theConfig, getLogBrowser());
		itsObjectClass = itsInterpreter.getClass(aClassName);
		itsInstance = itsInterpreter.newInstance(itsObjectClass, itsOriginal);
	}
	
	public TODInterpreter getInterpreter()
	{
		return itsInterpreter;
	}
	
	public IObjectInspector getOriginal()
	{
		return itsOriginal;
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsOriginal.getLogBrowser();
	}

	public ObjectId getObject()
	{
		return itsOriginal.getObject();
	}
	
	/**
	 * Returns the {@link JClass} of the object, as specified in the constructor
	 * (might be a super type of the actual type).
	 */
	protected JClass getObjectClass()
	{
		return itsObjectClass;
	}
	
	public TODInstance getInstance()
	{
		return itsInstance;
	}

	public ILogEvent getReferenceEvent()
	{
		return itsOriginal.getReferenceEvent();
	}

	public void setReferenceEvent(ILogEvent aEvent)
	{
		itsOriginal.setReferenceEvent(aEvent);
	}

	public ITypeInfo getType()
	{
		return itsOriginal.getType();
	}

	public ICreationEvent getCreationEvent()
	{
		return itsOriginal.getCreationEvent();
	}
	
	public final int getEntryCount()
	{
		if (itsEntryCount == -1) itsEntryCount = getEntryCount0();
		return itsEntryCount;
	}

	protected abstract int getEntryCount0();
	
	public IEventBrowser getBrowser(IEntryInfo aEntry)
	{
		throw new UnsupportedOperationException();
	}
}
