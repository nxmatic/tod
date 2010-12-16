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
package tod.impl.server;

import gnu.trove.TLongArrayList;

import java.util.ArrayList;
import java.util.List;

import zz.utils.Utils;

/**
 * A per-thread index of raw trace packets.
 * For now this is a quickly hacked in-memory solution
 * @author gpothier
 */
public class RawTraceThreadIndex
{
	private static List<TLongArrayList> itsThreadIndexes = new ArrayList<TLongArrayList>();
	
	public static void startThreadPacket(int aThreadId, long aOffset)
	{
		TLongArrayList theIndex = Utils.listGet(itsThreadIndexes, aThreadId);
		if (theIndex == null)
		{
			theIndex = new TLongArrayList();
			Utils.listSet(itsThreadIndexes, aThreadId, theIndex);
		}
		theIndex.add(aOffset);
	}
	
	public static long getNextThreadPacketOffset(int aThreadId, long aCurrentPacketOffset)
	{
		TLongArrayList theIndex = Utils.listGet(itsThreadIndexes, aThreadId);
		int thePos = theIndex.binarySearch(aCurrentPacketOffset);
		assert thePos >= 0;
		if (thePos == theIndex.size()-1) return -1;
		else return theIndex.get(thePos+1);
	}
}
