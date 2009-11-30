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

import tod2.agent.io._ByteBuffer;

/**
 * Mimics the API of {@link _ByteBuffer} but simulates an infinite buffer.
 * Clients may have to wait while the buffer stream is waiting for a new buffer.
 * The buffer stream assumes that primitive operations do not span multiple buffers.
 * @author gpothier
 */
public class BufferStream 
{
	private _ByteBuffer itsOldBuffer;
	private _ByteBuffer itsCurrentBuffer;
	private volatile _ByteBuffer itsNextBuffer;
	private boolean itsFinished = false;
	
	public BufferStream(_ByteBuffer aBuffer)
	{
		itsCurrentBuffer = aBuffer;
	}

	private void checkBuffer()
	{
		if (itsCurrentBuffer == null || itsCurrentBuffer.remaining() == 0)
		{
			_ByteBuffer theOld = itsCurrentBuffer; // Do not put into itsOldBuffer yet in case there is already an old buffer
			itsCurrentBuffer = getNextBuffer();
			itsOldBuffer = theOld;
		}
	}
	
	private synchronized _ByteBuffer getNextBuffer()
	{
		if (itsFinished) return null;
		try
		{
			while (itsNextBuffer == null && ! itsFinished) wait();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}

		_ByteBuffer theBuffer = itsNextBuffer;
		itsNextBuffer = null;
		notifyAll();
		return theBuffer;
	}
	
	/**
	 * Makes a new source buffer available to the stream. 
	 * @return The previous buffer
	 */
	synchronized _ByteBuffer pushBuffer(_ByteBuffer aBuffer)
	{
		try
		{
			while (itsNextBuffer != null) wait();
			itsNextBuffer = aBuffer;
			if (aBuffer == null) itsFinished = true;
			notifyAll();
			_ByteBuffer theOldBuffer = itsOldBuffer;
			itsOldBuffer = null;
			return theOldBuffer;
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public final int remaining()
	{
		checkBuffer();
		return itsFinished ? 0 : Integer.MAX_VALUE;
	}
	
	public final int position()
	{
		return itsCurrentBuffer.position();
	}	
	
	public final void get(byte[] aBuffer, int aOffset, int aLength)
	{
		checkBuffer();
		itsCurrentBuffer.get(aBuffer, aOffset, aLength);
	}
	
	public final byte get()
	{
		checkBuffer();
		return itsCurrentBuffer.get();
	}
	
	public final byte peek()
	{
		checkBuffer();
		return itsCurrentBuffer.peek();
	}
	
	public final char getChar()
	{
		checkBuffer();
		return itsCurrentBuffer.getChar();
	}
	
	public final short getShort()
	{
		checkBuffer();
		return itsCurrentBuffer.getShort();
	}
	
	public final int getInt()
	{
		checkBuffer();
		return itsCurrentBuffer.getInt();
	}
	
	public final long getLong()
	{
		checkBuffer();
		return itsCurrentBuffer.getLong();
	}
	
	public final float getFloat()
	{
		checkBuffer();
		return itsCurrentBuffer.getFloat();
	}
	
	public final double getDouble()
	{
		checkBuffer();
		return itsCurrentBuffer.getDouble();
	}
	
	/**
	 * Reads a string written by {@link #putString(String)}
	 */
	public final String getString()
	{
		checkBuffer();
		return itsCurrentBuffer.getString();
	}
}
