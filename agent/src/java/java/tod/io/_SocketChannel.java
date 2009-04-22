/*
 * Created on Jan 12, 2009
 */
package java.tod.io;

import tod.agent.io._ByteBuffer;
import tod.agent.io._CannotConnectException;
import tod.agent.io._EOFException;
import tod.agent.io._IOException;

/**
 * A socket channel similar to that of NIO
 * @author gpothier
 */
public class _SocketChannel
{
	private int itsFD;
	
	private _SocketChannel(int aFd)
	{
		itsFD = aFd;
	}

	public static _SocketChannel open(String aHostname, int aPort) throws _CannotConnectException
	{
		int theFD = open0(aHostname, aPort);
		_IO.out("Connected: "+theFD);
		if (theFD == -1) throw new _CannotConnectException();
		return new _SocketChannel(theFD);
	}
	
	private void checkFD() throws _IOException
	{
		if (itsFD == -1) throw new _IOException("Channel closed");
	}
	
	public int write(_ByteBuffer aBuffer) throws _IOException
	{
		checkFD();
		int n = write0(itsFD, aBuffer.array(), aBuffer.position(), aBuffer.remaining());

		if (n == -1) throw new _IOException("Could not write");
		else if (n == -2) throw new _IOException("Write failed");
		else if (n < 0) throw new RuntimeException("Bad return value: "+n);
		
		aBuffer.position(aBuffer.position()+n);
		return n;
	}
	
	public int read(_ByteBuffer aBuffer) throws _IOException
	{
		checkFD();
		int n = read0(itsFD, aBuffer.array(), aBuffer.position(), aBuffer.remaining());
		
		if (n == -1) return -1;
		else if (n == -2) throw new _IOException("Could not read");
		else if (n == -3) throw new _IOException("Read failed");
		else if (n < 0) throw new RuntimeException("Bad return value: "+n);
		
		aBuffer.position(aBuffer.position()+n);
		return n;
	}
	
	public void readFully(_ByteBuffer aBuffer) throws _IOException
	{
		int theCount = aBuffer.remaining();
		while(theCount > 0)
		{
			int n = read(aBuffer);
			if (n == -1) throw new _EOFException();
			theCount -= n;
		}
	}
	
	/**
	 * Whether there is some input to be read from the socket.
	 */
	public boolean hasInput() throws _IOException
	{
		checkFD();
		return in_avail0(itsFD) > 0;
	}
	
	public void flush() throws _IOException
	{
		checkFD();
		int r = flush0(itsFD);
		if (r == -1) throw new _IOException("Cannot flush");
		else if (r == -2) throw new _IOException("Flush failed");
		else if (r != 0) throw new RuntimeException("Bad return value: "+r);
	}
	
	public void close() throws _IOException
	{
		if (itsFD == -1) return;
		flush();
		close0(itsFD);
		itsFD = -1;
	}
	
	private native static int open0(String aHostname, int aPort);
	private native static int flush0(int aFD);
	private native static int close0(int aFD);
	private native static int write0(int aFD, byte[] aBuffer, int aPos, int aLength);
	private native static int read0(int aFD, byte[] aBuffer, int aPos, int aLength);
	private native static int in_avail0(int aFD);
}
