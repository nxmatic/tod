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
package tod.utils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketImpl;

public class NativeSocket
{
	private NativeStream itsInputStream;
	private NativeStream itsOutputStream;
	
	private static Method METHOD_GET_IMPL;
	private static Method METHOD_GET_FD;
	private final Socket itsSocket;
	static
	{
		try
		{
			METHOD_GET_IMPL = Socket.class.getDeclaredMethod("getImpl");
			METHOD_GET_IMPL.setAccessible(true);
			
			METHOD_GET_FD = SocketImpl.class.getDeclaredMethod("getFileDescriptor");
			METHOD_GET_FD.setAccessible(true);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public NativeSocket(Socket aSocket)
	{
		itsSocket = aSocket;
		try
		{
			SocketImpl theImpl = (SocketImpl) METHOD_GET_IMPL.invoke(aSocket);
			FileDescriptor theDescriptor = (FileDescriptor) METHOD_GET_FD.invoke(theImpl);
			
			itsInputStream = new SocketInputStream(theDescriptor);
			itsOutputStream = new SocketOutputStream(theDescriptor);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public NativeStream getInputStream()
	{
		return itsInputStream;
	}

	public NativeStream getOutputStream()
	{
		return itsOutputStream;
	}
	
	public void close() throws IOException
	{
		itsSocket.close();
	}
	
	public boolean isConnected()
	{
		return itsSocket.isConnected();
	}
	
	private class SocketInputStream extends NativeStream
	{
		private int itsFD;
		
		public SocketInputStream(FileDescriptor aDescriptor)
		{
			itsFD = getFD(aDescriptor);
		}
		
		@Override
		public void write(int[] aBuffer, int aOffset, int aSize)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int read(int[] aBuffer, int aOffset, int aSize)
		{
			if (aOffset != 0) throw new UnsupportedOperationException();
			return recv(itsFD, aBuffer, aSize);
		}
	}
	
	private class SocketOutputStream extends NativeStream
	{
		private int itsFD;
		
		public SocketOutputStream(FileDescriptor aDescriptor)
		{
			itsFD = getFD(aDescriptor);
		}
		
		@Override
		public void write(int[] aBuffer, int aOffset, int aSize) throws IOException
		{
			if (aOffset != 0) throw new UnsupportedOperationException();
			int n = send(itsFD, aBuffer, aSize);
			if (aSize*4 != n) throw new IOException("Sent only "+n+"bytes");
		}
		
		@Override
		public int read(int[] aBuffer, int aOffset, int aSize)
		{
			throw new UnsupportedOperationException();
		}
	}
	
}
