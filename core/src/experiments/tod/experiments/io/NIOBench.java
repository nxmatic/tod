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
package tod.experiments.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Random;

import zz.utils.Utils;

public class NIOBench
{
	private static final int S = 50000000;
	private static final int N = 20;
	
	public static void main(String[] args)
	{
		int[] theIntData = new int[S/4];
		long[] theLongData = new long[S/8];
		Random theRandom = new Random(7);
		for(int i=0;i<theIntData.length;i++) theIntData[i] = theRandom.nextInt();
		for(int i=0;i<theLongData.length;i++) theLongData[i] = theRandom.nextLong();
		
		for(int i=0;i<2;i++)
		{
			testByteBufferL(theIntData);
			testOurBufferL(theIntData);

			testByteBufferB(theIntData);
			testOurBufferB(theIntData);
			
			testByteBufferL(theLongData);
			testOurBufferL(theLongData);

			testByteBufferB(theLongData);
			testOurBufferB(theLongData);
			
//			testDOS(theData);

			testIntBuffer(theIntData);
			testOurIntBuffer(theIntData);
			
			System.out.println();
		}
	}
	
	private static void testByteBufferL(int[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		ByteBuffer theBuffer = ByteBuffer.allocate(aValues.length*4);
		theBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i=0;i<N;i++)
		{
			for (int v : aValues) theBuffer.putInt(v);
			theBuffer.clear();
		}

		long l1 = System.currentTimeMillis();
		
		Utils.println("testByteBufferL(int[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testOurBufferL(int[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		OurByteBuffer theBuffer = OurByteBuffer.allocate(aValues.length*4);
		for (int i=0;i<N;i++)
		{
			for (int v : aValues) theBuffer.putIntL(v);
			theBuffer.reset();
		}

		long l1 = System.currentTimeMillis();
		
		Utils.println("testOurBufferL(int[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testByteBufferB(int[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		ByteBuffer theBuffer = ByteBuffer.allocate(aValues.length*4);
		theBuffer.order(ByteOrder.BIG_ENDIAN);
		for (int i=0;i<N;i++)
		{
			for (int v : aValues) theBuffer.putInt(v);
			theBuffer.clear();
		}
		
		long l1 = System.currentTimeMillis();
		
		Utils.println("testByteBufferB(int[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testOurBufferB(int[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		OurByteBuffer theBuffer = OurByteBuffer.allocate(aValues.length*4);
		for (int i=0;i<N;i++)
		{
			for (int v : aValues) theBuffer.putIntB(v);
			theBuffer.reset();
		}
		
		long l1 = System.currentTimeMillis();
		
		Utils.println("testOurBufferB(int[]): %.2f", (l1-l0)*0.001f);
	}

	private static void testByteBufferL(long[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		ByteBuffer theBuffer = ByteBuffer.allocate(aValues.length*8);
		theBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i=0;i<N;i++)
		{
			for (long v : aValues) theBuffer.putLong(v);
			theBuffer.clear();
		}

		long l1 = System.currentTimeMillis();
		
		Utils.println("testByteBufferL(long[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testOurBufferL(long[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		OurByteBuffer theBuffer = OurByteBuffer.allocate(aValues.length*8);
		for (int i=0;i<N;i++)
		{
			for (long v : aValues) theBuffer.putLongL(v);
			theBuffer.reset();
		}

		long l1 = System.currentTimeMillis();
		
		Utils.println("testOurBufferL(long[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testByteBufferB(long[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		ByteBuffer theBuffer = ByteBuffer.allocate(aValues.length*8);
		theBuffer.order(ByteOrder.BIG_ENDIAN);
		for (int i=0;i<N;i++)
		{
			for (long v : aValues) theBuffer.putLong(v);
			theBuffer.clear();
		}
		
		long l1 = System.currentTimeMillis();
		
		Utils.println("testByteBufferB(long[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testOurBufferB(long[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		OurByteBuffer theBuffer = OurByteBuffer.allocate(aValues.length*8);
		for (int i=0;i<N;i++)
		{
			for (long v : aValues) theBuffer.putLongB(v);
			theBuffer.reset();
		}
		
		long l1 = System.currentTimeMillis();
		
		Utils.println("testOurBufferB(long[]): %.2f", (l1-l0)*0.001f);
	}
	

	private static void testDOS(int[] aValues)
	{
		try
		{
			System.gc();
			long l0 = System.currentTimeMillis();

			ByteArrayOutputStream theByteOut = new ByteArrayOutputStream(aValues.length*4);
			DataOutputStream theStream = new DataOutputStream(theByteOut);
			for (int i=0;i<N;i++)
			{
				for (int v : aValues) theStream.writeInt(v);
				theByteOut.reset();
			}
			
			long l1 = System.currentTimeMillis();
			
			Utils.println("testDOS(int[]): %.2f", (l1-l0)*0.001f);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void testIntBuffer(int[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		IntBuffer theBuffer = IntBuffer.allocate(aValues.length);
		for (int i=0;i<N;i++)
		{
			for (int v : aValues) theBuffer.put(v);
			theBuffer.clear();
		}

		long l1 = System.currentTimeMillis();
		
		Utils.println("testIntBufferL(int[]): %.2f", (l1-l0)*0.001f);
	}
	
	private static void testOurIntBuffer(int[] aValues)
	{
		System.gc();
		long l0 = System.currentTimeMillis();
		
		OurIntBuffer theBuffer = OurIntBuffer.allocate(aValues.length);
		for (int i=0;i<N;i++)
		{
			for (int v : aValues) theBuffer.putInt(v);
			theBuffer.reset();
		}

		long l1 = System.currentTimeMillis();
		
		Utils.println("testOurIntBufferL(int[]): %.2f", (l1-l0)*0.001f);
	}
	

	
	private static class OurByteBuffer
	{
		private int itsPos;
		private byte[] itsBytes;
		
		private OurByteBuffer(byte[] aBytes)
		{
			itsBytes = aBytes;
			itsPos = 0;
		}

		public static OurByteBuffer allocate(int aSize)
		{
			return new OurByteBuffer(new byte[aSize]);
		}
		
		public void putIntL(int v)
		{
			int thePos = itsPos;
			itsBytes[thePos + 0] = int0(v);
			itsBytes[thePos + 1] = int1(v);
			itsBytes[thePos + 2] = int2(v);
			itsBytes[thePos + 3] = int3(v);
			itsPos += 4;
		}
		
		public void putIntB(int v)
		{
			int thePos = itsPos;
			itsBytes[thePos + 0] = int3(v);
			itsBytes[thePos + 1] = int2(v);
			itsBytes[thePos + 2] = int1(v);
			itsBytes[thePos + 3] = int0(v);
			itsPos += 4;
		}
		
		public void putLongL(long v)
		{
			int thePos = itsPos;
			itsBytes[thePos + 0] = long0(v);
			itsBytes[thePos + 1] = long1(v);
			itsBytes[thePos + 2] = long2(v);
			itsBytes[thePos + 3] = long3(v);
			itsBytes[thePos + 4] = long4(v);
			itsBytes[thePos + 5] = long5(v);
			itsBytes[thePos + 6] = long6(v);
			itsBytes[thePos + 7] = long7(v);
			itsPos += 8;
		}
		
		public void putLongB(long v)
		{
			int thePos = itsPos;
			itsBytes[thePos + 0] = long7(v);
			itsBytes[thePos + 1] = long6(v);
			itsBytes[thePos + 2] = long5(v);
			itsBytes[thePos + 3] = long4(v);
			itsBytes[thePos + 4] = long3(v);
			itsBytes[thePos + 5] = long2(v);
			itsBytes[thePos + 6] = long1(v);
			itsBytes[thePos + 7] = long0(v);
			itsPos += 8;
		}
		
		public void reset()
		{
			itsPos = 0;
		}
		
	    private static byte int3(int x) { return (byte)(x >> 24); }
	    private static byte int2(int x) { return (byte)(x >> 16); }
	    private static byte int1(int x) { return (byte)(x >>  8); }
	    private static byte int0(int x) { return (byte)(x >>  0); }

	    private static byte long7(long x) { return (byte)(x >> 56); }
	    private static byte long6(long x) { return (byte)(x >> 48); }
	    private static byte long5(long x) { return (byte)(x >> 40); }
	    private static byte long4(long x) { return (byte)(x >> 32); }
	    private static byte long3(long x) { return (byte)(x >> 24); }
	    private static byte long2(long x) { return (byte)(x >> 16); }
	    private static byte long1(long x) { return (byte)(x >>  8); }
	    private static byte long0(long x) { return (byte)(x >>  0); }
	    

	}
	
	private static class OurIntBuffer
	{
		private int itsPos;
		private int[] itsInts;
		
		private OurIntBuffer(int[] aInts)
		{
			itsInts = aInts;
			itsPos = 0;
		}
		
		public static OurIntBuffer allocate(int aSize)
		{
			return new OurIntBuffer(new int[aSize]);
		}
		
		public void putInt(int v)
		{
			itsInts[itsPos++] = v;
		}
		
		public void reset()
		{
			itsPos = 0;
		}
	}
}
