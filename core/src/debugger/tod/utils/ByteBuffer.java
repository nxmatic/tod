/*
 * Created on Jan 12, 2009
 */
package tod.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;

/**
 * A little-endian byte buffer similar to that of NIO.
 * @author gpothier
 */
public class ByteBuffer
{
	private int itsPos;
	private int itsMark;
	private int itsLimit;
	private byte[] itsBytes;
	
	protected ByteBuffer(byte[] aBytes)
	{
		itsBytes = aBytes;
		clear();
	}

	public static ByteBuffer allocate(int aSize)
	{
		return new ByteBuffer(new byte[aSize]);
	}
	
	public static ByteBuffer wrap(byte[] aBuffer)
	{
		return new ByteBuffer(aBuffer);
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
		if (aRequested > remaining()) throw new BufferOverflowException();
	}
	
	public final void put(byte[] aBytes, int aOffset, int aLength)
	{
		checkRemaining(aLength);
		int thePos = itsPos;
		System.arraycopy(aBytes, aOffset, itsBytes, thePos, aLength);
		itsPos = thePos+aLength;
	}
	
	public final void get(byte[] aBuffer, int aOffset, int aLength)
	{
		checkRemaining(aLength);
		int thePos = itsPos;
		System.arraycopy(itsBytes, thePos, aBuffer, aOffset, aLength);
		itsPos = thePos+aLength;
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
	
	public final byte peek()
	{
		checkRemaining(1);
		return itsBytes[itsPos];
	}
	
	public final void putChar(char v)
	{
		checkRemaining(2);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int0(v);
		itsBytes[thePos + 1] = int1(v);
		itsPos = thePos+2;
	}
	
	public final void putChars(char[] v, int aOffset, int aCount)
	{
		checkRemaining(2*aCount);
		int thePos = itsPos;
		for(int i=aOffset;i<aOffset+aCount;i++)
		{
			char c = v[i];
			itsBytes[thePos + 0] = int0(c);
			itsBytes[thePos + 1] = int1(c);
			thePos += 2;
		}
		itsPos = thePos;
	}
	
	public final char getChar()
	{
		checkRemaining(2);
		int thePos = itsPos;
		char theResult = makeChar(
				itsBytes[thePos + 1],
				itsBytes[thePos + 0]);
		itsPos = thePos+2;
		
		return theResult;
	}
	
	public final void putShort(short v)
	{
		checkRemaining(2);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int0(v);
		itsBytes[thePos + 1] = int1(v);
		itsPos = thePos+2;
	}
	
	public final short getShort()
	{
		checkRemaining(2);
		int thePos = itsPos;
		short theResult = makeShort(
				itsBytes[thePos + 1],
				itsBytes[thePos + 0]);
		itsPos = thePos+2;
		
		return theResult;
	}
	
	public final void putInt(int v)
	{
		checkRemaining(4);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int0(v);
		itsBytes[thePos + 1] = int1(v);
		itsBytes[thePos + 2] = int2(v);
		itsBytes[thePos + 3] = int3(v);
		itsPos = thePos+4;
	}
	
	public final void putIntB(int v)
	{
		checkRemaining(4);
		int thePos = itsPos;
		itsBytes[thePos + 0] = int3(v);
		itsBytes[thePos + 1] = int2(v);
		itsBytes[thePos + 2] = int1(v);
		itsBytes[thePos + 3] = int0(v);
		itsPos = thePos+4;
	}
	
	public final void putInts(int[] v)
	{
		putInts(v, 0, v.length);
	}
	
	public final void putInts(int[] v, int aOffset, int aCount)
	{
		checkRemaining(4*aCount);
		int thePos = itsPos;
		for(int i=aOffset;i<aOffset+aCount;i++)
		{
			int c = v[i];
			itsBytes[thePos + 0] = int0(c);
			itsBytes[thePos + 1] = int1(c);
			itsBytes[thePos + 2] = int2(c);
			itsBytes[thePos + 3] = int3(c);
			thePos += 4;
		}
		itsPos = thePos;
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
		itsPos = thePos+4;
		
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
		itsPos = thePos+8;
	}
	
	public final void putLongs(long[] v)
	{
		putLongs(v, 0, v.length);
	}
	
	public final void putLongs(long[] v, int aOffset, int aCount)
	{
		checkRemaining(8*aCount);
		int thePos = itsPos;
		for(int i=aOffset;i<aOffset+aCount;i++)
		{
			long c = v[i];
			itsBytes[thePos + 0] = long0(c);
			itsBytes[thePos + 1] = long1(c);
			itsBytes[thePos + 2] = long2(c);
			itsBytes[thePos + 3] = long3(c);
			itsBytes[thePos + 4] = long4(c);
			itsBytes[thePos + 5] = long5(c);
			itsBytes[thePos + 6] = long6(c);
			itsBytes[thePos + 7] = long7(c);
			thePos += 8;
		}
		itsPos = thePos;
	}
	
	public final long getLong()
	{
		checkRemaining(8);
		int thePos = itsPos;
		long theResult = makeLong(
				itsBytes[thePos + 7],
				itsBytes[thePos + 6],
				itsBytes[thePos + 5],
				itsBytes[thePos + 4],
				itsBytes[thePos + 3],
				itsBytes[thePos + 2],
				itsBytes[thePos + 1],
				itsBytes[thePos + 0]);
		itsPos = thePos+8;
		
		return theResult;
	}
	
	public final void putFloat(float v)
	{
		putInt(Float.floatToRawIntBits(v));
	}
	
	public final void putFloats(float[] v)
	{
		putFloats(v, 0, v.length);
	}
	
	public final void putFloats(float[] v, int aOffset, int aCount)
	{
		checkRemaining(4*aCount);
		for(int i=aOffset;i<aOffset+aCount;i++) putFloat(v[i]);
	}
	
	public final float getFloat()
	{
		return Float.intBitsToFloat(getInt());
	}
	
	public final void putDouble(double v)
	{
		putLong(Double.doubleToRawLongBits(v));
	}
	
	public final void putDoubles(double[] v)
	{
		putDoubles(v, 0, v.length);
	}
	
	public final void putDoubles(double[] v, int aOffset, int aCount)
	{
		checkRemaining(8*aCount);
		for(int i=aOffset;i<aOffset+aCount;i++) putDouble(v[i]);
	}
	
	public final double getDouble()
	{
		return Double.longBitsToDouble(getLong());
	}
	
	public final void putBoolean(boolean v)
	{
		put(v ? (byte) 1 : (byte) 0);
	}
	
	public final boolean getBoolean()
	{
		return get() != 0;
	}
	
	/**
	 * Writes a representation of the string into this buffer.
	 */
	public final void putString(String aString)
	{
		throw new UnsupportedOperationException("Reimplement");
//		char[] c = TODAccessor.getStringChars(aString);
//		int o = TODAccessor.getStringOffset(aString);
//		int l = TODAccessor.getStringCount(aString);
//		
//		putInt(l);
//		putChars(c, o, l);
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
	public static long getLongL(InputStream aStream) throws IOException
	{
		final int l = 8;
		byte[] b = new byte[l];
		int c = 0;
		while (c < l) c += aStream.read(b, c, l-c);
		
		return makeLong(b[7], b[6], b[5], b[4], b[3], b[2], b[1], b[0]);
	}
	
	/**
	 * Retrieves a little-endian int from a {@link DataInputStream}.
	 */
	public static int getIntL(InputStream aStream) throws IOException
	{
		final int l = 4;
		byte[] b = new byte[l];
		int c = 0;
		while (c < l) c += aStream.read(b, c, l-c);
		
		return makeInt(b[3], b[2], b[1], b[0]);
	}
	
	/**
	 * Retrieves a big-endian int from a {@link DataInputStream}.
	 */
	public static int getIntB(InputStream aStream) throws IOException
	{
		final int l = 4;
		byte[] b = new byte[l];
		int c = 0;
		while (c < l) c += aStream.read(b, c, l-c);
		
		return makeInt(b[0], b[1], b[2], b[3]);
	}
	
	/**
	 * Retrieves a little-endian char from a {@link DataInputStream}.
	 */
	public static char getCharL(InputStream aStream) throws IOException
	{
		final int l = 2;
		byte[] b = new byte[l];
		int c = 0;
		while (c < l) c += aStream.read(b, c, l-c);
		
		return makeChar(b[1], b[0]);
	}
	
	/**
	 * Retrieves a big-endian char from a {@link DataInputStream}.
	 */
	public static char getCharB(InputStream aStream) throws IOException
	{
		final int l = 2;
		byte[] b = new byte[l];
		int c = 0;
		while (c < l) c += aStream.read(b, c, l-c);
		
		return makeChar(b[0], b[1]);
	}
	
	/**
	 * Reads a stream written by {@link #putString(String)} from a
	 * {@link DataInputStream}.
	 */
	public static String getString(InputStream aStream) throws IOException
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
	
	public final ByteBuffer flip()
	{
		itsLimit = itsPos;
		itsPos = 0;
		itsMark = -1;
		return this;
	}
	
	public final ByteBuffer clear()
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
	
	/**
	 * Returns a copy of the backing array from start to current position.
	 */
	public final byte[] toArray()
	{
		byte[] theResult = new byte[itsPos];
		System.arraycopy(itsBytes, 0, theResult, 0, itsPos);
		return theResult;
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
		return ((b7 & 0xffL) << 56) | ((b6 & 0xffL) << 48) | ((b5 & 0xffL) << 40) | ((b4 & 0xffL) << 32)
			| ((b3 & 0xffL) << 24) | ((b2 & 0xffL) << 16) | ((b1 & 0xffL) << 8) | ((b0 & 0xffL) << 0);
	}

	public static int makeInt(byte b3, byte b2, byte b1, byte b0)
	{
		return ((b3 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff) << 0);
	}
	
	public static char makeChar(byte b1, byte b0)
	{
		return (char) ((b1 << 8) | (b0 & 0xff));
	}

	public static short makeShort(byte b1, byte b0)
	{
		return (short) ((b1 << 8) | (b0 & 0xff));
	}
	

}
