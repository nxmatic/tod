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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import tod.BenchBase;
import tod.BenchBase.BenchResults;

import zz.utils.PublicCloneable;

public class CloneVsCreate
{
	private static final int N = 100000000;

	public static void main(String[] args) throws Exception
	{
		Class cls = MyFunClass.class;
		final Constructor ctor = cls.getDeclaredConstructor();
		final MyFactory factory = new MyFactory();
		final MyFunClass instance = new MyFunClass();

		BenchResults b;
		
		// Factory
		runFactory(factory);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				runFactory(factory);
			}
		});
		System.out.println("Factory: "+b);

		// Reflection
		runReflect(ctor);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				runReflect(ctor);
			}
		});
		System.out.println("Reflect: "+b);
		
		// Clone
		runClone(instance);
		b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				runClone(instance);
			}
		});
		System.out.println("Clone: "+b);
	}
	
	private static void runFactory(MyFactory f)
	{
		for(int i=0;i<N;i++) f.create();
	}
	
	private static void runReflect(Constructor c)
	{
		try
		{
			for(int i=0;i<N;i++) c.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static void runClone(MyFunClass o)
	{
		for(int i=0;i<N;i++) o.clone();
	}
	
	private static class MyFactory
	{
		public MyFunClass create()
		{
			return new MyFunClass();
		}
	}
	
	public static class MyFunClass extends PublicCloneable
	{
		
	}
}
