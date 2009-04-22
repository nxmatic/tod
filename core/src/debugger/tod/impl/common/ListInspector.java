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

import tod.core.database.browser.IObjectInspector;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import tod.tools.interpreter.TODInterpreter.TODInstance;
import zz.jinterp.JBehavior;
import zz.jinterp.JInstance;
import zz.jinterp.JInterpreter;
import zz.jinterp.JObject;
import zz.jinterp.JPrimitive.JInt;

public class ListInspector extends IteratorStructureInspector<ListInspector.ItemInfo>
{
	public ListInspector(IObjectInspector aOriginal)
	{
		super(aOriginal, "java/util/List");
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
		JBehavior theIteratorBehavior = getObjectClass().getVirtualBehavior("iterator", "()Ljava/util/Iterator;");
		return (JInstance) theIteratorBehavior.invoke(null, getInstance(), JInterpreter.NOARGS);
	}
	
	@Override
	protected ItemInfo createEntry(int aIndex, JInstance aEntryInstance)
	{
		return new ItemInfo(aIndex, aEntryInstance);
	}

	public EntryValue[] getEntryValue(IEntryInfo aEntry)
	{
		JInstance theItem = ((ItemInfo) aEntry).getItem();
		
		if (theItem instanceof TODInstance)
		{
			TODInstance theInstance = (TODInstance) theItem;
			ObjectId theId = theInstance.getObjectId();
			ILogEvent theSetter = null;
			
			return new EntryValue[] {new EntryValue(theId, theSetter)};
		}
		else
		{
			return null;
		}
	}

	public EntryValue[] nextEntryValue(IEntryInfo aEntry)
	{
		return null;
	}

	public EntryValue[] previousEntryValue(IEntryInfo aEntry)
	{
		return null;
	}



	private class ItemInfo implements IObjectInspector.IEntryInfo
	{
		private final int itsIndex;
		private final JInstance itsItem;
		
		public ItemInfo(int aIndex, JInstance aItem)
		{
			itsIndex = aIndex;
			itsItem = aItem;
		}

		public JInstance getItem()
		{
			return itsItem;
		}
		
		public String getName()
		{
			return "[" + itsIndex + "]";
		}
	}

}
