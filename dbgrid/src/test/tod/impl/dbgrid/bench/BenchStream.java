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
package tod.impl.dbgrid.bench;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class BenchStream
{
	public static void main(String[] args) throws IOException
	{
		byte[] buffer = new byte[10*1000*1000];
		
		long t0 = System.currentTimeMillis();
		
		for(int i=0;i<100;i++)
		{
			DataInputStream stream = new DataInputStream(new ByteArrayInputStream(buffer));
			while (stream.available() > 100)
			{
				stream.readByte();
				stream.readBoolean();
				stream.readLong();
				stream.readInt();
				stream.readShort();
			}
		}
		
		long t1 = System.currentTimeMillis();
		
		float dt = (t1-t0)/1000f;
		System.out.println("executed in "+dt+"s");
	}
}
