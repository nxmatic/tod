/*
 * Created on Jan 12, 2009
 */
package tod.agent.io;

import java.io.DataInputStream;
import java.io.IOException;



/**
 * A little-endian byte buffer similar to that of NIO.
 * @author gpothier
 */
public class _ByteBuffer
{
	private int itsPos;
	private int itsMark;
	private int itsLimit;
	private byte[] itsBytes;
	
	protected _ByteBuffer(byte[] aBytes)
	{
		itsBytes = aBytes;
		clear();
	}

	public static _ByteBuffer allocate(int aSize)
	{
		return new _ByteBuffer(new byte[aSize]);
	}
	
	public static _ByteBuffer wrap(byte[] aBuffer)
	{
		return new _ByteBuffer(aBuffer);
	}
	
	public final byte[] array()
	{
		return itsBytes;
	}
	
	public void _array(byte[] aBytes)
	{
		itsBytes = aBytes;
	}
	
	protected void checkRemaining(int aRequested)
	{
		if (aRequested > remaining()) throw new _BufferOverflowException();
	}
	
	public final void put(byte[] aBytes, int aOffset, int aLength)
	{
		checkRemaining(aLength);
		System.arraycopy(aBytes, aOffset, itsBytes, itsPos, aLength);
		itsPos += aLength;
	}
	
	public final void put(byte v)
	{
		checkRemaining(1);
		itsBytes[itsPos++] = v;
	}
	
	public final byte get()
	{
		checkRemaining(1);
		return itsBytes[itsPos++];
	}
	
	public final void putChar(char v)
	{
		checkRemaining(2);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int0(v);
		itsBytes[thePos + 1] = int1(v);
		itsPos += 2;
	}
	
	public final char getChar()
	{
		checkRemaining(2);
		int thePos = itsPos;
		char theResult = makeChar(
				itsBytes[thePos + 1],
				itsBytes[thePos + 0]);
		itsPos += 2;
		
		return theResult;
	}
	
	public final void putShort(short v)
	{
		checkRemaining(2);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int0(v);
		itsBytes[thePos + 1] = int1(v);
		itsPos += 2;
	}
	
	public final void putInt(int v)
	{
		checkRemaining(4);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int0(v);
		itsBytes[thePos + 1] = int1(v);
		itsBytes[thePos + 2] = int2(v);
		itsBytes[thePos + 3] = int3(v);
		itsPos += 4;
	}
	
	public final void putIntB(int v)
	{
		checkRemaining(4);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int3(v);
		itsBytes[thePos + 1] = int2(v);
		itsBytes[thePos + 2] = int1(v);
		itsBytes[thePos + 3] = int0(v);
		itsPos += 4;
	}
	
	public final int getInt()
	{
		checkRemaining(4);
		int thePos = itsPos;
		int theResult = makeInt(
				itsBytes[thePos + 3],
				itsBytes[thePos + 2],
				itsBytes[thePos + 1],
				itsBytes[thePos + 0]);
		itsPos += 4;
		
		return theResult;
	}
	
	public final void putLong(long v)
	{
		checkRemaining(8);
		int thePos = itsPos;
		itsBytes[thePos + 0] = long0(v);
		itsBytes[thePos + 1] = long1(v);
		itsBytes[thePos + 2] = long2(v);
		itsBytes[thePos + 3] = long3(v);
		itsBytes[thePos + 4] = long4(v);
		itsBytes[thePos + 5] = long5(v);
		itsBytes[thePos + 6] = long6(v);
		itsBytes[thePos + 7] = long7(v);
		itsPos += 8;
	}
	
	public final void putFloat(float v)
	{
		putInt(Float.floatToRawIntBits(v));
	}
	
	public final void putDouble(double v)
	{
		putLong(Double.doubleToRawLongBits(v));
	}
	
	/**
	 * Writes a representation of the string into this buffer.
	 */
	public final void putString(String aString)
	{
		putInt(aString.length());
		for(int i=0;i<aString.length();i++) putChar(aString.charAt(i));
	}

	/**
	 * Reads a string written by {@link #putString(String)}
	 */
	public final String getString()
	{
		int theLength = getInt();
		char[] theChars = new char[theLength];
		for(int i=0;i<theLength;i++) theChars[i] = getChar();
		return new String(theChars);
	}

	/**
	 * Retrieves a little-endian long from a {@link DataInputStream}.
	 */
	public static long getLongL(DataInputStream aStream) throws IOException
	{
		byte b0 = aStream.readByte();
		byte b1 = aStream.readByte();
		byte b2 = aStream.readByte();
		byte b3 = aStream.readByte();
		byte b4 = aStream.readByte();
		byte b5 = aStream.readByte();
		byte b6 = aStream.readByte();
		byte b7 = aStream.readByte();
		return makeLong(b7, b6, b5, b4, b3, b2, b1, b0);
	}
	
	/**
	 * Retrieves a little-endian int from a {@link DataInputStream}.
	 */
	public static int getIntL(DataInputStream aStream) throws IOException
	{
		byte b0 = aStream.readByte();
		byte b1 = aStream.readByte();
		byte b2 = aStream.readByte();
		byte b3 = aStream.readByte();
		return makeInt(b3, b2, b1, b0);
	}
	
	/**
	 * Retrieves a big-endian int from a {@link DataInputStream}.
	 */
	public static int getIntB(DataInputStream aStream) throws IOException
	{
		byte b0 = aStream.readByte();
		byte b1 = aStream.readByte();
		byte b2 = aStream.readByte();
		byte b3 = aStream.readByte();
		return makeInt(b0, b1, b2, b3);
	}
	
	/**
	 * Retrieves a little-endian char from a {@link DataInputStream}.
	 */
	public static char getCharL(DataInputStream aStream) throws IOException
	{
		byte b0 = aStream.readByte();
		byte b1 = aStream.readByte();
		return makeChar(b1, b0);
	}
	
	/**
	 * Retrieves a big-endian char from a {@link DataInputStream}.
	 */
	public static char getCharB(DataInputStream aStream) throws IOException
	{
		byte b0 = aStream.readByte();
		byte b1 = aStream.readByte();
		return makeChar(b0, b1);
	}
	
	/**
	 * Reads a stream written by {@link #putString(String)} from a
	 * {@link DataInputStream}.
	 */
	public static String getString(DataInputStream aStream) throws IOException
	{
		int theLength = getIntL(aStream);
		char[] theChars = new char[theLength];
		for(int i=0;i<theLength;i++) theChars[i] = getCharL(aStream);
		return new String(theChars);
	}
	
	public final int remaining()
	{
		return itsLimit - itsPos;
	}
	
	public final int position()
	{
		return itsPos;
	}
	
	public final void position(int aPosition)
	{
		itsPos = aPosition;
	}
	
	public final int limit()
	{
		return itsLimit;
	}
	
	public final void limit(int aLimit) 
	{
		itsLimit = aLimit;		
	}
	
	public final int capacity()
	{
		return itsBytes.length;
	}
	
    public final _ByteBuffer flip()
	{
		itsLimit = itsPos;
		itsPos = 0;
		itsMark = -1;
		return this;
	}
    
    public final _ByteBuffer clear()
	{
		itsPos = 0;
		itsLimit = itsBytes.length;
		itsMark = -1;
		return this;
	}
    
    public final void clear(byte[] aNewData)
    {
    	itsBytes = aNewData;
    	clear();
    }

    private static byte int3(int x) { return (byte)(x >> 24); }
    private static byte int2(int x) { return (byte)(x >> 16); }
    private static byte int1(int x) { return (byte)(x >>  8); }
    private static byte int0(int x) { return (byte)(x >>  0); }

    private static byte long7(long x) { return (byte)(x >> 56); }
    private static byte long6(long x) { return (byte)(x >> 48); }
    private static byte long5(long x) { return (byte)(x >> 40); }
    private static byte long4(long x) { return (byte)(x >> 32); }
    private static byte long3(long x) { return (byte)(x >> 24); }
    private static byte long2(long x) { return (byte)(x >> 16); }
    private static byte long1(long x) { return (byte)(x >>  8); }
    private static byte long0(long x) { return (byte)(x >>  0); }
    
    public static long makeLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0)
	{
		return ((b7 & 0xff) << 56) | ((b6 & 0xff) << 48) | ((b5 & 0xff) << 40) | ((b4 & 0xff) << 32)
			| ((b3 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff) << 0);
	}

    public static int makeInt(byte b3, byte b2, byte b1, byte b0)
    {
    	return ((b3 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff) << 0);
    }
    
    public static char makeChar(byte b1, byte b0)
	{
		return (char) ((b1 << 8) | (b0 & 0xff));
	}


}
