/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.experiments;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import tod.BenchBase;
import tod.BenchBase.BenchResults;
import tod.utils.ArrayCast;

public class ArrayCastTest
{
	private static final int n = 1000000;
	
	public static void main(String[] args)
	{
		BenchResults theResults;
		theResults = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				testNIO();
			}
		});
		
		System.out.println(theResults);

		theResults = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				testB2I();
			}
		});

		System.out.println(theResults);
	}
	
	private static void testNIO()
	{
		ByteBuffer theByteBuffer = ByteBuffer.allocateDirect(4096);
		theByteBuffer.order(ByteOrder.nativeOrder());
		IntBuffer theIntBuffer = theByteBuffer.asIntBuffer();
		
		int[] theBuffer = new int[1024];
		
		for(int i=0;i<n;i++)
		{
			theIntBuffer.position(0);
			theIntBuffer.get(theBuffer);
		}
	}
	
	private static void testNIO2()
	{
		byte[] theByteBuffer = new byte[4096];
		int[] theIntBuffer = new int[1024];
		
		for(int i=0;i<n;i++)
		{
			ByteBuffer theByteBufferW = ByteBuffer.wrap(theByteBuffer);
			theByteBufferW.order(ByteOrder.nativeOrder());
			IntBuffer theIntBufferW = theByteBufferW.asIntBuffer();

			theIntBufferW.position(0);
			theIntBufferW.get(theIntBuffer);
		}
	}
	
	private static void testB2I()
	{
		byte[] theByteBuffer = new byte[4096];
		int[] theIntBuffer = new int[1024];

		for(int i=0;i<n;i++)
		{
			ArrayCast.b2i(theByteBuffer, theIntBuffer);
		}
	}
}
