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
package tod.utils;

import java.nio.BufferOverflowException;

import zz.utils.bit.BitUtils;

public class BitBuffer
{
	/**
	 * The integer MASKS[i] has the (32-i)th bit set and all other bits are 0.
	 */
	public static final int[] IMASKS = new int[32];
	public static final int[] ILOWPASSMASKS = new int[33];
	
	static
	{
		for(int i=0, j=1;i<32;i++, j<<=1) 
		{
			IMASKS[i] = j;
			ILOWPASSMASKS[i] = j-1;
		}
		ILOWPASSMASKS[32] = 0xffffffff;
	}

	
	private int itsPos;
	private int itsLimit;
	private int[] itsData;
	
	protected BitBuffer(int[] aData)
	{
		itsData = aData;
		clear();
	}
	
	public static BitBuffer allocate(int aSize)
	{
		return new BitBuffer(new int[(aSize+31)/32]);
	}
	
	protected final int[] data()
	{
		return itsData;
	}
	
	protected final void _data(int[] aData)
	{
		itsData = aData;
	}
	
	protected void checkRemaining(int aRequested)
	{
		if (aRequested > remaining()) throw new BufferOverflowException();		
	}
	
	public final int remaining()
	{
		return itsLimit - itsPos;
	}
	
	public final int position()
	{
		return itsPos;
	}
	
	public final void position(int aPosition)
	{
		itsPos = aPosition;
	}
	
	public final int limit()
	{
		return itsLimit;
	}
	
	public final void limit(int aLimit) 
	{
		itsLimit = aLimit;		
	}
	
	public final int capacity()
	{
		return itsData.length*32;
	}
	
	public final BitBuffer flip()
	{
		itsLimit = itsPos;
		itsPos = 0;
		return this;
	}
	
	public final BitBuffer clear()
	{
		itsPos = 0;
		itsLimit = capacity();
		return this;
	}
	
	public void put(int aBits, int aCount)
	{
		checkRemaining(aCount);
		aBits &= ILOWPASSMASKS[aCount];
		
		int index = itsPos;
		int offset = index & 31;
		index >>= 5;
		
		// Write lower order bits
		int lbits = aBits << offset;
		itsData[index] |= lbits;

		// Write higher order bits
		int rem = offset + aCount - 32; 
		if (rem > 0)
		{
			int hbits = aBits >>> (aCount-rem);
			itsData[index+1] |= hbits;
		}
		
		itsPos += aCount;
	}
	
	public int getInt(int aCount)
	{
		checkRemaining(aCount);
		
		int index = itsPos;
		int offset = index & 31;
		index >>= 5;

		int result = 0;
		
		int lbits = itsData[index] >>> offset;
		result |= lbits;

		int rem = offset + aCount - 32; 
		if (rem > 0)
		{
			int hbits = itsData[index+1] << (32-offset);
			result |= hbits;
		}
		
		itsPos += aCount;
		result &= ILOWPASSMASKS[aCount];
		return result;
	}
	
	public void put(long aBits, int aCount)
	{
		if (aCount <= 32) put((int) aBits, aCount);
		else
		{
			put((int) aBits, 32);
			put((int) (aBits >>> 32), aCount-32);
		}
	}
	
	public long getLong(int aCount)
	{
		if (aCount <= 32) return getInt(aCount);
		else
		{
			int l = getInt(32);
			long h = getInt(aCount-32);
			return l | (h << 32);
		}
	}
	
	public void putUnary(int aValue)
	{
		while(aValue > 0)
		{
			int c = Math.min(32, aValue);
			put(0xffffffff, c);
			aValue -= c;
		}
		put(0, 1);
	}

	public int getUnary()
	{
		// TODO: optimize, see http://www.steike.com/code/bits/debruijn/
		int index = itsPos;
		int offset = index & 31;
		index >>= 5;
			
		int mask = IMASKS[offset];
		int data = itsData[index++];
		int result = 0;
		
		while(true)
		{
			itsPos++;
			if ((data & mask) == 0) return result;
			result++;
			mask <<= 1;
			if (mask == 0)
			{
				mask = 1;
				data = itsData[index++];
			}
		}
	}
}
