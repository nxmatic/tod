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
package tod.impl.dbgrid.dispatch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;

import zz.utils.ArrayStack;

/**
 * This is the peer of {@link NodeConnector} on the master's side.
 * @author gpothier
 */
public class NodeProxy
{
	private final RINodeConnector itsConnector;
	private BufferedSender itsSender;
	private BSOutputStream itsOut;
	private DataOutputStream itsDataOut;
	
	public NodeProxy(RINodeConnector aConnector, Socket aSocket)
	{
		itsConnector = aConnector;
		try
		{
			itsSender = new BufferedSender(aConnector.getNodeId(), aSocket.getOutputStream());
			itsOut = new BSOutputStream(itsSender);
			itsDataOut = new DataOutputStream(itsOut);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	
	/**
	 * Returns the output stream used to send data to the node.
	 */
	public DataOutputStream getOutStream()
	{
		return itsDataOut;
	}
	
	/**
	 * Returns the number of bytes currently queued in this proxy's buffer
	 * (for load balancing).
	 */
	public int getQueueSize()
	{
		return itsSender.getQueueSize();
	}
	
	
	/**
	 * Sends buffers of data to the remote end. Buffers are queued and sent
	 * asynchronously.
	 * @author gpothier
	 */
	private static class BufferedSender extends Thread
	{
		private OutputStream itsStream;
		
		private LinkedList<Buffer> itsQueue = new LinkedList<Buffer>();
		private ArrayStack<Buffer> itsFreeBuffers = new ArrayStack<Buffer>();
		private int itsSize;
		
		private boolean itsFlushed = true;

		private final int itsNodeId;
		
		public BufferedSender(int aNodeId, OutputStream aStream)
		{
			itsNodeId = aNodeId;
			itsStream = aStream;
			for (int i=0;i<1024;i++) itsFreeBuffers.push(new Buffer(new byte[4096]));
			start();
		}

		public Buffer getFreeBuffer()
		{
			synchronized(itsFreeBuffers)
			{
				try
				{
					while (itsFreeBuffers.isEmpty()) itsFreeBuffers.wait();
					Buffer theBuffer = itsFreeBuffers.pop();
					theBuffer.reset();
					return theBuffer;
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		
		private void addFreeBuffer(Buffer aBuffer)
		{
			synchronized (itsFreeBuffers)
			{
				itsFreeBuffers.push(aBuffer);
				itsFreeBuffers.notifyAll();
			}
		}
		
		public synchronized void pushBuffer(Buffer aBuffer)
		{
			itsQueue.addLast(aBuffer);
			itsSize += aBuffer.length;
			notifyAll();
		}
		
		private synchronized Buffer popBuffer()
		{
			try
			{
				while(itsQueue.isEmpty()) wait();
				Buffer theBuffer = itsQueue.removeFirst();
				itsSize -= theBuffer.length;
				notifyAll();
				return theBuffer;
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}

		/**
		 * Returns the number of bytes queued in this sender.
		 */
		protected int getQueueSize()
		{
			return itsSize;
		}
		
		public void waitFlushed()
		{
			try
			{
				synchronized (itsStream)
				{
					int s = getQueueSize();
					long t0 = System.currentTimeMillis();
					while(! itsFlushed) itsStream.wait();					
					long t1 = System.currentTimeMillis();
					float t = (t1-t0)/1000f;
					System.out.println(String.format(
							"[BufferedSender] Flushed %d bytes in %.2fs for node %s ",
							s,
							t,
							itsNodeId));
				}
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					Buffer theBuffer = popBuffer();
					itsStream.write(theBuffer.data, 0, theBuffer.length);
					if (theBuffer.flush)
					{
						synchronized (itsStream)
						{
							itsFlushed = false;
							itsStream.flush();
							itsFlushed = true;
							itsStream.notifyAll();
						}
					}
					else addFreeBuffer(theBuffer);
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class Buffer
	{
		public final byte[] data;
		
		/**
		 * Amount of data used.
		 */
		public int length = 0;
		
		/**
		 * Whether this buffer is a flush marker
		 */
		public boolean flush = false; 
		
		public Buffer(byte[] aData)
		{
			data = aData;
		}
		
		public void reset()
		{
			length = 0;
			flush = false;
		}
	}
	
	/**
	 * An output stream that writes data to buffers of a {@link BufferedSender}.
	 * @author gpothier
	 */
	private static class BSOutputStream extends OutputStream
	{
		private BufferedSender itsSender;
		
		private Buffer itsCurrentBuffer;
		
		public BSOutputStream(BufferedSender aSender)
		{
			itsSender = aSender;
			newBuffer();
		}
		
		private void newBuffer()
		{
			if (itsCurrentBuffer != null) itsSender.pushBuffer(itsCurrentBuffer);
			itsCurrentBuffer = itsSender.getFreeBuffer();
		}

		@Override
		public void write(byte[] aB, int aOff, int aLen) 
		{
			int theRemaining = aLen;
			int theOffset = aOff;
			while(theRemaining > 0)
			{
				int theChunk = Math.min(theRemaining, itsCurrentBuffer.data.length-itsCurrentBuffer.length);
				System.arraycopy(aB, theOffset, itsCurrentBuffer.data, itsCurrentBuffer.length, theChunk);
				itsCurrentBuffer.length += theChunk;
				theOffset += theChunk;
				theRemaining -= theChunk;
				if (itsCurrentBuffer.length == itsCurrentBuffer.data.length) newBuffer();
			}
		}

		@Override
		public void write(int aB) 
		{
			if (itsCurrentBuffer.length == itsCurrentBuffer.data.length) newBuffer();
			itsCurrentBuffer.data[itsCurrentBuffer.length++] = (byte) aB;
		}
		
		@Override
		public void flush() 
		{
			itsCurrentBuffer.flush = true;
			itsSender.pushBuffer(itsCurrentBuffer);
			itsCurrentBuffer = null;
			itsSender.waitFlushed();
			newBuffer();
		}
	}

}
