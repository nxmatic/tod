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

import java.util.BitSet;

import tod.BenchBase;
import tod.BenchBase.BenchResults;

import zz.utils.primitive.IntArray;

public class BitStackBench
{
	private static final int N = 100000;
	private static final int M = 10000;
	
	public static void main(String[] args)
	{
		bench(new DirectInt());
		bench(new FatByte());
		bench(new ManualDynamicFatByte());
		bench(new ManualDynamicInt());
		bench(new DynamicInt());
		bench(new BitSetBacked());
		bench(new DirectLong());
	}
	
	// Note that we duplicate the bench methods because using an abstract class and reusing the same
	// bench methods favor the first executed bench (probably because of polymorphic call optimisation)
	
	private static void bench(final BitSetBacked aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(BitSetBacked aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	private static void bench(final DynamicInt aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(DynamicInt aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	private static void bench(final DirectInt aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(DirectInt aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	private static void bench(final DirectLong aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(DirectLong aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	private static void bench(final ManualDynamicInt aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(ManualDynamicInt aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	private static void bench(final FatByte aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(FatByte aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	private static void bench(final ManualDynamicFatByte aStack)
	{
		bench(aStack, N);
		BenchResults b = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				bench(aStack, N);
			}
		});
		System.out.println(aStack + ": " + b);
	}
	
	private static void bench(ManualDynamicFatByte aStack, int n)
	{
		for(int i=0;i<n;i++)
		{
			for(int j=0;j<M;j++) aStack.push((j & 1) == 0);
			for(int j=0;j<M;j++) aStack.peek();
			for(int j=0;j<M;j++) aStack.pop();
		}
	}
	
	public static class BitSetBacked 
	{
		private BitSet itsBitSet = new BitSet();
		private int itsSize = 0;
		
		public void push(boolean aValue)
		{
			itsBitSet.set(itsSize++, aValue);
		}
		
		public boolean pop()
		{
			if (itsSize == 0) throw new RuntimeException("Stack is empty");
			return itsBitSet.get(--itsSize); 
		}
		
		public boolean peek()
		{
			return itsBitSet.get(itsSize-1);
		}
	}

	public static class DirectLong 
	{
		private long[] itsValues = new long[512];
		private long itsMask = 1L << 63;
		private int itsIndex = -1;
		
		public void push(boolean aValue)
		{
			itsMask <<= 1;
			
			if (itsMask == 0)
			{
				itsIndex++;
				itsMask = 1;
			}
			
			if (aValue) itsValues[itsIndex] |= itsMask;
			else itsValues[itsIndex] &= ~itsMask;
			
		}

		public boolean pop()
		{
			boolean value = (itsValues[itsIndex] & itsMask) != 0;
				
			itsMask >>>= 1;
			if (itsMask == 0)
			{
				itsIndex--;
				itsMask = 1L << 63;
			}
			
			return value;
		}
		
		public boolean peek()
		{
			return (itsValues[itsIndex] & itsMask) != 0;
		}
	}
	
	public static class DirectInt 
	{
		private int[] itsValues = new int[1024];
		private int itsMask = 1 << 31;
		private int itsIndex = -1;
		
		public void push(boolean aValue)
		{
			itsMask <<= 1;
			
			if (itsMask == 0)
			{
				itsIndex++;
				itsMask = 1;
			}
			
			if (aValue) itsValues[itsIndex] |= itsMask;
			else itsValues[itsIndex] &= ~itsMask;
			
		}
		
		public boolean pop()
		{
			boolean value = (itsValues[itsIndex] & itsMask) != 0;
			
			itsMask >>>= 1;
			if (itsMask == 0)
			{
				itsIndex--;
				itsMask = 1 << 31;
			}
			
			return value;
		}
		
		public boolean peek()
		{
			return (itsValues[itsIndex] & itsMask) != 0;
		}
	}
	
	public static class ManualDynamicInt 
	{
		private int[] itsValues = new int[1024];
		private int itsMask = 1 << 31;
		private int itsIndex = -1;
		
		public void push(boolean aValue)
		{
			itsMask <<= 1;
			
			if (itsMask == 0)
			{
				itsIndex++;
				itsMask = 1;
			}
			
			int theLength = itsValues.length;
			if (itsIndex >= theLength)
			{
				int[] newArray = new int[theLength*2];
				System.arraycopy(itsValues, 0, newArray, 0, theLength);
				itsValues = newArray;
			}
			
			if (aValue) itsValues[itsIndex] |= itsMask;
			else itsValues[itsIndex] &= ~itsMask;
			
		}
		
		public boolean pop()
		{
			boolean value = (itsValues[itsIndex] & itsMask) != 0;
			
			itsMask >>>= 1;
			if (itsMask == 0)
			{
				itsIndex--;
				itsMask = 1 << 31;
			}
			
			return value;
		}
		
		public boolean peek()
		{
			return (itsValues[itsIndex] & itsMask) != 0;
		}
	}
	
	public static class DynamicInt 
	{
		private IntArray itsValues = new IntArray();
		private int itsMask = 1 << 31;
		private int itsIndex = -1;
		
		public void push(boolean aValue)
		{
			itsMask <<= 1;
			
			if (itsMask == 0)
			{
				itsIndex++;
				itsMask = 1;
			}
			
			int v = itsValues.get(itsIndex);
			if (aValue) v |= itsMask;
			else v &= ~itsMask;
			itsValues.set(itsIndex, v);
		}
		
		public boolean pop()
		{
			int v = itsValues.get(itsIndex);
			boolean value = (v & itsMask) != 0;
			
			itsMask >>>= 1;
			if (itsMask == 0)
			{
				itsIndex--;
				itsMask = 1 << 31;
			}
			
			return value;
		}
		
		public boolean peek()
		{
			int v = itsValues.get(itsIndex);
			return (v & itsMask) != 0;
		}
	}

	public static class FatByte 
	{
		private byte[] itsValues = new byte[1024*32];
		private int itsSize = 0;
		
		public void push(boolean aValue)
		{
			itsValues[itsSize++] = aValue ? (byte) 1 : (byte) 0;
		}
		
		public boolean pop()
		{
			return itsValues[--itsSize] != 0;
		}
		
		public boolean peek()
		{
			return itsValues[itsSize-1] != 0;
		}
	}
	
	public static class ManualDynamicFatByte 
	{
		private byte[] itsValues = new byte[1024*32];
		private int itsSize = 0;
		
		public void push(boolean aValue)
		{
			int theLength = itsValues.length;
			if (itsSize >= theLength)
			{
				byte[] newArray = new byte[theLength*2];
				System.arraycopy(itsValues, 0, newArray, 0, theLength);
				itsValues = newArray;
			}
			itsValues[itsSize++] = aValue ? (byte) 1 : (byte) 0;
		}
		
		public boolean pop()
		{
			return itsValues[--itsSize] != 0;
		}
		
		public boolean peek()
		{
			return itsValues[itsSize-1] != 0;
		}
	}
	
}
