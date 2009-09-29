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

public class SpreadCondition
{
	private static final int N = 1000000;
	private static volatile boolean B = "true".startsWith("t");
	
	public static void main(String[] args)
	{
		int k = 0;
		for(int i=0;i<N;i++) k+=foo1(0, B);
		BenchResults b0 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int k = 0;
				for(int i=0;i<N;i++) k+=foo1(0, B);
			}
		});
		System.out.println(b0);
	
		k = 0;
		for(int i=0;i<N;i++) k+=foo2(0);
		BenchResults b1 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				int k = 0;
				for(int i=0;i<N;i++) k+=foo2(0);
			}
		});
		System.out.println(b1);
		
		System.out.println(l);
	}
	
	public static int foo1(int i, boolean b)
	{
		int j = i*2;
		for(int k=0;k<1000;k++)
		{
			if (b) log();
			j = bar2(i, j);
			if (b) log();
			i = bar1(j);
			if (b) log();
			j = bar2(i, j);
			if (b) log();
			i = bar1(j);
			if (b) log();
			j = bar2(i, j);
			if (b) log();
			i = bar1(j);
			if (b) log();
			j = bar2(i, j);
			if (b) log();
			i = bar1(j);
			if (b) log();
			j = bar2(i, j);
			if (b) log();
			i = bar1(j);
			if (b) log();
		}
		return i+j;
	}
	
	public static int foo2(int i)
	{
		int j = i*2;
		for(int k=0;k<1000;k++)
		{
			j = bar2(i, j);
			i = bar1(j);
		}
		return i+j;
	}
	
	public static int bar1(int i)
	{
		return i*4;
	}
	
	public static int bar2(int i, int j)
	{
		return i*j-2;
	}
	
	private static int l = 0;
	
	public static void log()
	{
		l++;
	}
}
