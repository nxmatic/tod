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
package tod.experiments.bench;

import java.nio.ByteOrder;

import tod.BenchBase;
import tod.BenchBase.BenchResults;

public class ByteBufferVSArrayBench
{
	private static final int N = 1000000;
	
	public static void main(String[] args)
	{
		final tod.utils.ByteBuffer theByteBuffer = tod.utils.ByteBuffer.allocate(4096);
		final java.nio.ByteBuffer theJByteBuffer = java.nio.ByteBuffer.allocate(4096);
		final java.nio.IntBuffer theJByteBufferAsIntBuffer = theJByteBuffer.asIntBuffer();
		final java.nio.IntBuffer theJIntBuffer = java.nio.IntBuffer.allocate(1024);
		theJByteBuffer.order(ByteOrder.nativeOrder());
		final int[] theArray = new int[1024];
		
		runByteBuffer(theByteBuffer, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int t = runByteBuffer(theByteBuffer, N);
				System.out.println(t);
			}
		});
		System.out.println("ByteBuffer: " + b);
		
		runJByteBuffer(theJByteBuffer, N);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int t = runJByteBuffer(theJByteBuffer, N);
				System.out.println(t);
			}
		});
		System.out.println("Java ByteBuffer: " + b);
		
		runJIntBuffer(theJIntBuffer, N);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int t = runJIntBuffer(theJIntBuffer, N);
				System.out.println(t);
			}
		});
		System.out.println("Java IntBuffer: " + b);
		
		runJByteBufferAsIntBuffer(theJByteBufferAsIntBuffer, N);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int t = runJByteBufferAsIntBuffer(theJByteBufferAsIntBuffer, N);
				System.out.println(t);
			}
		});
		System.out.println("Java ByteByffer as IntBuffer: " + b);
		
		runArray(theArray, N);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int t = runArray(theArray, N);
				System.out.println(t);
			}
		});
		System.out.println("Array: " + b);

	}
	
	private static int runByteBuffer(tod.utils.ByteBuffer aBuffer, int aCount)
	{
		int t = 0;
		for(int i=0;i<aCount;i++)
		{
			aBuffer.position(0);
			for (int j=0;j<1000;j++) t += aBuffer.getInt();
		}
		
		return t;
	}
	
	private static int runJByteBuffer(java.nio.ByteBuffer aBuffer, int aCount)
	{
		int t = 0;
		for(int i=0;i<aCount;i++)
		{
			aBuffer.position(0);
			for (int j=0;j<1000;j++) t += aBuffer.getInt();
		}
		
		return t;
	}
	
	private static int runJByteBufferAsIntBuffer(java.nio.IntBuffer aBuffer, int aCount)
	{
		int t = 0;
		for(int i=0;i<aCount;i++)
		{
			aBuffer.position(0);
			for (int j=0;j<1000;j++) t += aBuffer.get();
		}
		
		return t;
	}
	
	private static int runJIntBuffer(java.nio.IntBuffer aBuffer, int aCount)
	{
		int t = 0;
		for(int i=0;i<aCount;i++)
		{
			aBuffer.position(0);
			for (int j=0;j<1000;j++) t += aBuffer.get();
		}
		
		return t;
	}
	
	private static int runArray(int[] aArray, int aCount)
	{
		int t = 0;
		for(int i=0;i<aCount;i++)
		{
			for (int j=0;j<1000;j++) t += aArray[j];
		}
		
		return t;
	}
}
