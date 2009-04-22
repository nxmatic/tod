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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class NetBench
{
	public static void main(String[] args) throws IOException
	{
		if ("recv".equals(args[0])) receive();
		else if ("send".equals(args[0])) send(args[1]);
		else throw new IllegalArgumentException();
	}
	
	private static void receive() throws IOException
	{
		System.out.println("netbench-receive");
		ServerSocket theServerSocket = new ServerSocket(8085);
		Socket theSocket = theServerSocket.accept();
		long t0 = System.currentTimeMillis();
		InputStream theStream = theSocket.getInputStream();
		
		long n = 0;
		byte[] b = new byte[4096];
		
		int c;
		while((c = theStream.read(b)) >= 0) n += c;
		
		long t1 = System.currentTimeMillis();
		
		float dt = (t1-t0)/1000f;
		
		float bps = n/dt;
		System.out.println("Received "+n+" bytes in "+dt+"s: "+bps+"B/s");
	}
	
	private static void send(String aHost) throws IOException
	{
		System.out.println("netbench-send - "+aHost);
		Random theRandom = new Random();
		byte[] b = new byte[4096];
		for (int i=0;i<b.length;i++) b[i] = (byte) theRandom.nextInt();
		
		Socket theSocket = new Socket(aHost, 8085);
		long t0 = System.currentTimeMillis();
		OutputStream theStream = theSocket.getOutputStream();
		
		long n = 1000*1000*1000;
		
		for (long i=0;i<n;i+=b.length) theStream.write(b);
		
		long t1 = System.currentTimeMillis();
		
		float dt = (t1-t0)/1000f;
		
		float bps = n/dt;
		System.out.println("Sent "+n+" bytes in "+dt+"s: "+bps+"B/s");
	}
}
