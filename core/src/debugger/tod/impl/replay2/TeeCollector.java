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

import tod.core.database.structure.ObjectId;

/**
 * This event collector forwards all messages to several underlying collectors.
 * @author gpothier
 */
public class TeeCollector extends EventCollector
{
	private final EventCollector[] itsCollectors;

	public TeeCollector(EventCollector... aCollectors)
	{
		itsCollectors = aCollectors;
	}

	@Override
	public void fieldRead(ObjectId aTarget, int aFieldSlotIndex)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.fieldRead(aTarget, aFieldSlotIndex);
	}

	@Override
	public void fieldWrite(ObjectId aTarget, int aFieldSlotIndex)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.fieldWrite(aTarget, aFieldSlotIndex);
	}

	@Override
	public void arrayRead(ObjectId aTarget, int aIndex)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.arrayRead(aTarget, aIndex);
	}

	@Override
	public void arrayWrite(ObjectId aTarget, int aIndex)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.arrayWrite(aTarget, aIndex);
	}

	@Override
	public void sync(long aTimestamp)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.sync(aTimestamp);
	}

	@Override
	public void value(ObjectId aValue)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.value(aValue);
	}

	@Override
	public void value(int aValue)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.value(aValue);
	}

	@Override
	public void value(long aValue)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.value(aValue);
	}

	@Override
	public void value(float aValue)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.value(aValue);
	}

	@Override
	public void value(double aValue)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.value(aValue);
	}

	@Override
	public void localsSnapshot(LocalsSnapshot aSnapshot)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.localsSnapshot(aSnapshot);
	}

	@Override
	public void enter(int aBehaviorId, int aArgsCount)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.enter(aBehaviorId, aArgsCount);
	}

	@Override
	public void exit()
	{
		for(EventCollector theCollector : itsCollectors) theCollector.exit();
	}

	@Override
	public void exitException()
	{
		for(EventCollector theCollector : itsCollectors) theCollector.exitException();
	}

	@Override
	public void registerString(ObjectId aId, String aString)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.registerString(aId, aString);
	}

	@Override
	public void associateIds(long aTmpId, long aRealId)
	{
		for(EventCollector theCollector : itsCollectors) theCollector.associateIds(aTmpId, aRealId);
	}
	
	
}
