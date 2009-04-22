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
package tod.experiments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

public class StringSend
{
	private static final int N = 10000;
	private static final String S = "Hello! ";

	
	public static void main(String[] args)
	{
//		for(int i=0;i<5;i++)
		{
			new NullCoder().print();
			new SerCoder().print();
//			new ManualCoder().print();			
//			new ManualCoder2().print();			
			new ManualCoder3().print();			
			new ReflectCoder().print();			
		}
	}
	
	private static class StringGen
	{
		private final StringBuilder builder = new StringBuilder();
		
		public String next()
		{
			builder.append(S);
			return builder.toString();
		}
	}
	
	private static abstract class Coder
	{
		public void print()
		{
			long t = measure();
			System.out.println(getClass().getSimpleName()+": "+t);
		}
		
		public long measure()
		{
			ByteBuffer b = ByteBuffer.allocate(N*S.length()*2);
			b.order(ByteOrder.nativeOrder());
			
			StringGen g = new StringGen();
			
			long t0 = System.currentTimeMillis();
			
			for(int i=0;i<N;i++) 
			{
				String s = g.next();
				b.clear();
				code(b, s);
			}
			
			long t1 = System.currentTimeMillis();
			
			return t1-t0;
		}
		
		public abstract void code(ByteBuffer b, String s);
	}
	
	private static class NullCoder extends Coder
	{
		@Override
		public void code(ByteBuffer b, String s)
		{
		}
	}
	
	private static class SerCoder extends Coder
	{
		@Override
		public void code(ByteBuffer b, String s)
		{
			ByteArrayOutputStream theBuffer = new ByteArrayOutputStream();
			try
			{
				ObjectOutputStream theObjectOut = new ObjectOutputStream(theBuffer);
				theObjectOut.writeObject(s);
				theObjectOut.flush();
				
				b.put(theBuffer.toByteArray());
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class ManualCoder extends Coder
	{
		@Override
		public void code(ByteBuffer b, String s)
		{
			int l = s.length();
			for(int i=0;i<l;i++) b.putChar(s.charAt(i));
		}
	}
	
	private static class ManualCoder2 extends Coder
	{
		@Override
		public void code(ByteBuffer b, String s)
		{
			char[] c = s.toCharArray();
			int l = c.length;
			for(int i=0;i<l;i++) b.putChar(c[i]);
		}
	}
	
	private static class ManualCoder3 extends Coder
	{
		@Override
		public void code(ByteBuffer b, String s)
		{
			CharBuffer b2 = b.asCharBuffer();
			char[] c = s.toCharArray();
			b2.put(c);
		}
	}
	
	private static class ReflectCoder extends Coder
	{
		private final Field fValue;
		private final Field fOffset;
		private final Field fCount;
		
		public ReflectCoder()
		{
			try
			{
				fValue = String.class.getDeclaredField("value");
				fOffset = String.class.getDeclaredField("offset");
				fCount = String.class.getDeclaredField("count");
				
				fValue.setAccessible(true);
				fOffset.setAccessible(true);
				fCount.setAccessible(true);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void code(ByteBuffer b, String s)
		{
			CharBuffer b2 = b.asCharBuffer();
			try
			{
				char[] value = (char[]) fValue.get(s);
				int offset = (Integer) fOffset.get(s);
				int count = (Integer) fCount.get(s);
				b2.put(value, offset, count);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
