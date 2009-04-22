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

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IObjectInspector;
import zz.jinterp.JBehavior;
import zz.jinterp.JClass;
import zz.jinterp.JInstance;
import zz.jinterp.JObject;
import zz.jinterp.JPrimitive.JInt;

public class MapInspector extends IteratorStructureInspector<MapInspector.EntryInfo>
{
	private final JClass itsMapEntryClass;
	private final JClass itsSetClass;
	
	private final JBehavior itsGetKeyMethod;
	private final JBehavior itsGetValueMethod;
	
	
	public MapInspector(IObjectInspector aOriginal)
	{
		super(aOriginal, "java/util/Map");

		itsMapEntryClass = getInterpreter().getClass("java/util/Map$Entry");
		itsSetClass = getInterpreter().getClass("java/util/Set");
		
		itsGetKeyMethod = itsMapEntryClass.getVirtualBehavior("getKey", "()Ljava/lang/Object;");
		itsGetValueMethod = itsMapEntryClass.getVirtualBehavior("getValue", "()Ljava/lang/Object;");
	}
	
	@Override
	protected int getEntryCount0()
	{
		JBehavior theBehavior = getObjectClass().getVirtualBehavior("size", "()I");
		JInt theResult = (JInt) theBehavior.invoke(null, getInstance(), new JObject[] {});
		return theResult.v;
	}

	@Override
	protected JInstance getIteratorInstance0()
	{
		JBehavior theEntrySetBehavior = getObjectClass().getVirtualBehavior("entrySet", "()Ljava/util/Set;");
		JInstance theEntrySet = (JInstance) theEntrySetBehavior.invoke(null, getInstance(), new JObject[] {});
		JBehavior theIteratorBehavior = itsSetClass.getVirtualBehavior("iterator", "()Ljava/util/Iterator;");
		return (JInstance) theIteratorBehavior.invoke(null, theEntrySet, new JObject[] {});
	}
	
	@Override
	protected EntryInfo createEntry(int aIndex, JInstance aEntryInstance)
	{
		return new EntryInfo(aEntryInstance);
	}

	public IEventBrowser getBrowser(IEntryInfo aEntry)
	{
		return null;
	}

	public EntryValue[] getEntryValue(IEntryInfo aEntry)
	{
		return null;
	}

	public EntryValue[] nextEntryValue(IEntryInfo aEntry)
	{
		return null;
	}

	public EntryValue[] previousEntryValue(IEntryInfo aEntry)
	{
		return null;
	}

	private class EntryInfo implements IObjectInspector.IEntryInfo
	{
		private final JInstance itsEntry;
		private final JInstance itsKey;
		private final JInstance itsValue;
		
		public EntryInfo(JInstance aEntry)
		{
			itsEntry = aEntry;
			itsKey = (JInstance) itsGetKeyMethod.invoke(null, itsEntry, new JObject[] {});
			itsValue = (JInstance) itsGetValueMethod.invoke(null, itsEntry, new JObject[] {});
		}

		public String getName()
		{
			return null;
		}
	}
}
