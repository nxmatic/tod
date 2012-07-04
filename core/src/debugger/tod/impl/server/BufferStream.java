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
package tod.impl.server;

import java.io.EOFException;
import java.util.concurrent.ArrayBlockingQueue;

import tod.utils.ByteBuffer;

/**
 * Mimics the API of {@link ByteBuffer} but simulates an infinite buffer.
 * Clients may have to wait while the buffer stream is waiting for a new buffer.
 * The buffer stream assumes that primitive operations do not span multiple buffers.
 * @author gpothier
 */
public class BufferStream 
{
	private static final PacketBuffer EOF = new PacketBuffer(new byte[0], -1);
	
	private ArrayBlockingQueue<PacketBuffer> itsBuffers = new ArrayBlockingQueue<PacketBuffer>(8);
	private PacketBuffer itsCurrentBuffer;
	private boolean itsFinished = false;
	
	/**
	 * Returns the offset from the beginning of the file of the currently processed packet.
	 */
	public long getPacketStartOffset()
	{
		return itsCurrentBuffer != null ? itsCurrentBuffer.getPacketStartOffset() : 0;
	}
	
	private void checkBuffer()
	{
		if (itsCurrentBuffer == null || itsCurrentBuffer.remaining() == 0)
		{
			try
			{
				if (! itsFinished || ! itsBuffers.isEmpty()) 
				{
					itsCurrentBuffer = itsBuffers.take();
					if (itsCurrentBuffer == EOF) itsCurrentBuffer = null;
				}
				else itsCurrentBuffer = null;
			}
			catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Makes a new source buffer available to the stream. 
	 * @return A free buffer, if available
	 */
	public void pushBuffer(PacketBuffer aBuffer)
	{
		try
		{
			if (aBuffer == null)
			{
				itsFinished = true;
				itsBuffers.put(EOF);
			}
			else if (! itsFinished)
			{
				itsBuffers.put(aBuffer);
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public final int remaining()
	{
		checkBuffer();
		return itsCurrentBuffer == null && itsFinished ? 0 : Integer.MAX_VALUE;
	}
	
	public final int position()
	{
		return itsCurrentBuffer != null ? itsCurrentBuffer.position() : 0;
	}	
	
	public final void get(byte[] aBuffer, int aOffset, int aLength)
	{
		checkBuffer();
		itsCurrentBuffer.get(aBuffer, aOffset, aLength);
	}
	
	public final byte get()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.get();
	}
	
	public final byte peek()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.peek();
	}
	
	public final char getChar()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getChar();
	}
	
	public final short getShort()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getShort();
	}
	
	public final int getInt()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getInt();
	}
	
	public final long getLong()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getLong();
	}
	
	public final float getFloat()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getFloat();
	}
	
	public final double getDouble()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getDouble();
	}
	
	/**
	 * Reads a string written by {@link #putString(String)}
	 */
	public final String getString()
	{
		checkBuffer();
		if (itsCurrentBuffer == null) throw new EndOfStreamException();
		return itsCurrentBuffer.getString();
	}
	
	public void skipAll()
	{
		itsFinished = true;
		itsBuffers.clear();
		itsCurrentBuffer = null;
	}
	
	public static class EndOfStreamException extends RuntimeException
	{
	}
}
