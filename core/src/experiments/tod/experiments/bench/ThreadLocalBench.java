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

import tod.BenchBase;
import tod.BenchBase.BenchResults;
import zz.utils.primitive.ByteArray;

public class ThreadLocalBench
{
	private static int t;
	private static final long N = 1000000000;
	
	private static ThreadLocal<Thread> l = new ThreadLocal<Thread>()
	{
		@Override
		protected Thread initialValue()
		{
			return Thread.currentThread();
		}
	};
	
	public static void main(String[] args)
	{
		System.out.println("Warming up");
		thread(N);
		array(N);
		threadLocal(N);
		
		System.out.println("Starting bench");
		
		BenchResults b0 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				thread(N);
			}
		});
		System.out.println("Thread: " + b0);
		System.out.println(t);

		BenchResults b1 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				array(N);
			}
		});
		System.out.println("Array: " + b1);
		System.out.println(t);		
		
		BenchResults b2 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				threadLocal(N);
			}
		});
		System.out.println("ThreadLocal: " + b2);
		System.out.println(t);


	}
	
	private static void thread(long n) 
	{
		for(long i=0;i<n;i++)
		{
			Thread th = Thread.currentThread();
			t += th.getId();
		}
	}
	
	private static void threadLocal(long n) 
	{
		for(long i=0;i<n;i++)
		{
			Thread th = l.get();
			t += th.getId();
		}
	}
	
	private static void array(long n)
	{
		for(long i=0;i<n;i++)
		{
			t += B.getMode(3);
		}
	}
	
	private static class B
	{
		private static final ByteArray modes = new ByteArray(16384);

		public static byte getMode(int i)
		{
			return modes.get(i);
		}
	}
}
