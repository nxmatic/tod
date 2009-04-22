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

import java.util.ArrayList;
import java.util.List;

import tod.core.database.browser.IObjectInspector;
import tod.tools.interpreter.JIterator;
import zz.jinterp.JInstance;

/**
 * A structure inspector whose entries are retrieved through an iterator.
 * @author gpothier
 */
public abstract class IteratorStructureInspector<T extends IObjectInspector.IEntryInfo> extends StructureInspector
{
	private JIterator itsIterator;
	private List<T> itsEntries = new ArrayList<T>();

	
	public IteratorStructureInspector(IObjectInspector aOriginal, String aClassName)
	{
		super(aOriginal, aClassName);
	}

	protected abstract JInstance getIteratorInstance0();
	
	protected abstract T createEntry(int aIndex, JInstance aEntryInstance);
	
	private void ensureEntriesAvailable(int aCount)
	{
		getEntryCount(); // Ensure field count is computed
		assert aCount <= getEntryCount(): aCount;
		
		if (itsIterator == null)
		{
			JInstance theIteratorInstance = getIteratorInstance0();
			itsIterator = new JIterator(getInterpreter(), theIteratorInstance);
		}
		
		while(itsEntries.size() < aCount)
		{
			JInstance theEntry = itsIterator.next();
			itsEntries.add(createEntry(itsEntries.size(), theEntry));
		}
	}
	
	public List<IEntryInfo> getEntries(int aRangeStart, int aRangeSize)
	{
		ensureEntriesAvailable(aRangeStart+aRangeSize);

		List<IEntryInfo> theResult = new ArrayList<IEntryInfo>();
			
		for(int i=aRangeStart;i<Math.min(aRangeStart+aRangeSize, getEntryCount());i++)
		{
			theResult.add(itsEntries.get(i));
		}

		return theResult;
	}
}
