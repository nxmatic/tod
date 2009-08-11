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

public class ParameterPassingBench 
{
	private static final int N = 500000000;
	
	public static void main(String[] args)
	{
		final ParameterPassing p = new ParameterPassing();
		
		final FalseFrame f0 = new FalseFrame();
		final FalseFrame f1 = new FalseFrame();
		final FalseFrame f2 = new FalseFrame();
		
		for(int i=0;i<N;i++) p.invoke0(f0);
		BenchResults b0 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				for(int i=0;i<N;i++) p.invoke0(f0);
			}
		});
		System.out.println(b0);
		System.out.println(f0.x);
		
//		for(int i=0;i<N;i++) p.invoke1(f1);
//		BenchResults b1 = BenchBase.benchmark(new Runnable()
//		{
//			public void run()
//			{
//				for(int i=0;i<N;i++) p.invoke1(f1);
//			}
//		});
//		System.out.println(b1);
//		System.out.println(f1.x);
//		
		for(int i=0;i<N;i++) p.invoke2(f2);		
		BenchResults b2 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				for(int i=0;i<N;i++) p.invoke2(f2);
			}
		});
		System.out.println(b2);
		System.out.println(f2.x);
	}
}
