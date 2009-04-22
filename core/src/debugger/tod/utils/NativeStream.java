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

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;

public class NativeStream
{
	private long itsFID;
	
	protected NativeStream(long aFid)
	{
		itsFID = aFid;
	}
	
	protected NativeStream()
	{
	}
	
	protected static native int fileno(long aFID);
	protected static native long fdopen(FileDescriptor aFileDescriptor, String aMode);
	protected static native void setFD(FileDescriptor aFileDescriptor, int aFD);
	protected static native int getFD(FileDescriptor aFileDescriptor);
	
	protected static native int fwrite(long aFID, int[] aBuffer, int aOffset, int aSize);
	protected static native int fread(long aFID, int[] aBuffer, int aOffset, int aSize);
	protected static native int fflush(long aFID);
	protected static native int fseek(long aFID, long aOffset, int aOrigin);
	protected static native int feof(long aFID);
	
	protected static native long fopen(String aFileName, String aMode);
	protected static native int fclose(long aFID);
	
	protected static native int recv(int aFD, int[] aBuffer, int aSize);
	protected static native int send(int aFD, int[] aBuffer, int aSize);

	static
	{
		System.loadLibrary("native-stream");
	}
	
	protected void setFID(long aFID)
	{
		itsFID = aFID;
	}
	
	public void writeInt(int aValue) throws IOException
	{
		write(new int[] {aValue});
	}
	
	public void write(int[] aBuffer) throws IOException
	{
		write(aBuffer, 0, aBuffer.length);
	}
	
	public void write(int[] aBuffer, int aOffset, int aSize) throws IOException
	{
		fwrite(itsFID, aBuffer, aOffset, aSize);
	}
	
	public int readInt() throws IOException
	{
		int[] theBuffer = new int[1];
		read(theBuffer);
		return theBuffer[0];
	}
	
	public int read(int[] aBuffer) throws IOException
	{
		return read(aBuffer, 0, aBuffer.length);
	}
	
	public int read(int[] aBuffer, int aOffset, int aSize) throws IOException
	{
		return fread(itsFID, aBuffer, aOffset, aSize);
	}
	
	public void readFully(int[] aBuffer) throws IOException
	{
		int theCount = 0;
		while (theCount < aBuffer.length)
		{
			int theResult = read(aBuffer, theCount, aBuffer.length-theCount); 
			if (theResult == 0 && feof(itsFID) != 0) throw new EOFException();
			
			theCount += theResult;
		}
	}
	
	public void flush() throws IOException
	{
		int theResult = fflush(itsFID);
		if (theResult != 0) throw new IOException("Could not flush stream");
	}
	
	public void close() throws IOException
	{
		int theResult = fclose(itsFID);
		if (theResult != 0) throw new IOException("Could not close stream");
	}
	
}
