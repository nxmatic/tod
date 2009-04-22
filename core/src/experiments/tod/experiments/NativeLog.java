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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import tod.BenchBase;
import tod.BenchBase.BenchResults;

public class NativeLog
{
	static
	{
		System.loadLibrary("native-stream");
	}
	

	public static native void log(int i, byte b, short c, long d);
	
	public static void main(String[] args)
	{
		final int n = 100000000;
		final ByteArrayOutputStream theStream = new ByteArrayOutputStream(50);
		final DataOutputStream theDataStream = new DataOutputStream(theStream);
		
//		BenchResults r1 = BenchBase.benchmark(new Runnable()
//		{
//			public void run()
//			{
//				try
//				{
//					for(int i=0;i<n;i++)
//					{
//						theDataStream.writeInt(0x12345678);
//						theDataStream.writeByte(0x12);
//						theDataStream.writeShort(0x2468);
//						theDataStream.writeLong(0x1234567890abcdefL);
//						theStream.reset();
//					}
//				}
//				catch (IOException e)
//				{
//					throw new RuntimeException(e);
//				}
//			}
//		});
//		
//		System.out.println("DataOutputStream: "+r1);
		
		final B a = new B();
		
		BenchResults r2 = BenchBase.benchmark(new Runnable()
		{
			public void run()
			{
				for(int i=0;i<n;i++)
				{
					a.log(0x12345678, (byte) 0x12, (short) 0x2468, 0x1234567890abcdefL);
				}
			}
		});
		
		System.out.println("Native: "+r2);
	}
	
	private static abstract class A
	{
		public abstract void log(int i, byte b, short c, long d);
	}
	
	private static class B extends A
	{
		@Override
		public void log(int a, byte b, short c, long d)
		{
			NativeLog.log(a, b, c, d);
		}
	}
	
}
