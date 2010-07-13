/*
 * Copyright 2003-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package java.tod.util;

import java.tod.io._IO;
import java.util.Arrays;

import tod2.access.TODAccessor;



/**
 * A mutable sequence of characters.  This class provides an API compatible
 * with <code>StringBuffer</code>, but with no guarantee of synchronization.
 * This class is designed for use as a drop-in replacement for
 * <code>StringBuffer</code> in places where the string buffer was being
 * used by a single thread (as is generally the case).   Where possible,
 * it is recommended that this class be used in preference to
 * <code>StringBuffer</code> as it will be faster under most implementations.
 *
 * <p>The principal operations on a <code>StringBuilder</code> are the
 * <code>append</code> and <code>insert</code> methods, which are
 * overloaded so as to accept data of any type. Each effectively
 * converts a given datum to a string and then appends or inserts the
 * characters of that string to the string builder. The
 * <code>append</code> method always adds these characters at the end
 * of the builder; the <code>insert</code> method adds the characters at
 * a specified point.
 * <p>
 * For example, if <code>z</code> refers to a string builder object
 * whose current contents are "<code>start</code>", then
 * the method call <code>z.append("le")</code> would cause the string
 * builder to contain "<code>startle</code>", whereas
 * <code>z.insert(4, "le")</code> would alter the string builder to
 * contain "<code>starlet</code>".
 * <p>
 * In general, if sb refers to an instance of a <code>StringBuilder</code>,
 * then <code>sb.append(x)</code> has the same effect as
 * <code>sb.insert(sb.length(),&nbsp;x)</code>.
 *
 * Every string builder has a capacity. As long as the length of the
 * character sequence contained in the string builder does not exceed
 * the capacity, it is not necessary to allocate a new internal
 * buffer. If the internal buffer overflows, it is automatically made larger.
 *
 * <p>Instances of <code>StringBuilder</code> are not safe for
 * use by multiple threads. If such synchronization is required then it is
 * recommended that {@link java.lang.StringBuffer} be used.
 *
 * @author	  Michael McCloskey
 * @see		 java.lang.StringBuffer
 * @see		 java.lang.String
 * @since	   1.5
 */
public final class _StringBuilder
{
	/**
	 * The value is used for character storage.
	 */
	char value[];

	/**
	 * The count is the number of characters used.
	 */
	int count;

	/**
	 * Constructs a string builder with no characters in it and an
	 * initial capacity of 16 characters.
	 */
	public _StringBuilder() {
		this(16);
	}

	/**
	 * Constructs a string builder with no characters in it and an
	 * initial capacity specified by the <code>capacity</code> argument.
	 *
	 * @param	  capacity  the initial capacity.
	 * @throws	 NegativeArraySizeException  if the <code>capacity</code>
	 *			   argument is less than <code>0</code>.
	 */
	public _StringBuilder(int capacity) {
		value = new char[capacity];
	}

	/**
	 * Constructs a string builder initialized to the contents of the
	 * specified string. The initial capacity of the string builder is
	 * <code>16</code> plus the length of the string argument.
	 *
	 * @param   str   the initial contents of the buffer.
	 * @throws	NullPointerException if <code>str</code> is <code>null</code>
	 */
	public _StringBuilder(String str) {
		this(str.length() + 16);
		append(str);
	}

	/**
	 * Constructs a string builder that contains the same characters
	 * as the specified <code>CharSequence</code>. The initial capacity of
	 * the string builder is <code>16</code> plus the length of the
	 * <code>CharSequence</code> argument.
	 *
	 * @param	  seq   the sequence to copy.
	 * @throws	NullPointerException if <code>seq</code> is <code>null</code>
	 */
	public _StringBuilder(CharSequence seq) {
		this(seq.length() + 16);
		append(seq);
	}

	/**
	 * Returns the length (character count).
	 *
	 * @return  the length of the sequence of characters currently
	 *		  represented by this object
	 */
	public int length() {
		return count;
	}

	/**
	 * Returns the current capacity. The capacity is the amount of storage
	 * available for newly inserted characters, beyond which an allocation
	 * will occur.
	 *
	 * @return  the current capacity
	 */
	public int capacity() {
		return value.length;
	}

	/**
	 * Ensures that the capacity is at least equal to the specified minimum.
	 * If the current capacity is less than the argument, then a new internal
	 * array is allocated with greater capacity. The new capacity is the
	 * larger of:
	 * <ul>
	 * <li>The <code>minimumCapacity</code> argument.
	 * <li>Twice the old capacity, plus <code>2</code>.
	 * </ul>
	 * If the <code>minimumCapacity</code> argument is nonpositive, this
	 * method takes no action and simply returns.
	 *
	 * @param   minimumCapacity   the minimum desired capacity.
	 */
	public void ensureCapacity(int minimumCapacity) {
		if (minimumCapacity > value.length) {
			expandCapacity(minimumCapacity);
		}
	}

	/**
	 * This implements the expansion semantics of ensureCapacity with no
	 * size check or synchronization.
	 */
	void expandCapacity(int minimumCapacity) {
		int newCapacity = (value.length + 1) * 2;
		if (newCapacity < 0) {
			newCapacity = Integer.MAX_VALUE;
		} else if (minimumCapacity > newCapacity) {
			newCapacity = minimumCapacity;
		}
		value = Arrays.copyOf(value, newCapacity);
	}

	/**
	 * Attempts to reduce storage used for the character sequence.
	 * If the buffer is larger than necessary to hold its current sequence of
	 * characters, then it may be resized to become more space efficient.
	 * Calling this method may, but is not required to, affect the value
	 * returned by a subsequent call to the {@link #capacity()} method.
	 */
	public void trimToSize() {
		if (count < value.length) {
			value = Arrays.copyOf(value, count);
		}
	}

	/**
	 * Sets the length of the character sequence.
	 * The sequence is changed to a new character sequence
	 * whose length is specified by the argument. For every nonnegative
	 * index <i>k</i> less than <code>newLength</code>, the character at
	 * index <i>k</i> in the new character sequence is the same as the
	 * character at index <i>k</i> in the old sequence if <i>k</i> is less
	 * than the length of the old character sequence; otherwise, it is the
	 * null character <code>'&#92;u0000'</code>.
	 *
	 * In other words, if the <code>newLength</code> argument is less than
	 * the current length, the length is changed to the specified length.
	 * <p>
	 * If the <code>newLength</code> argument is greater than or equal
	 * to the current length, sufficient null characters
	 * (<code>'&#92;u0000'</code>) are appended so that
	 * length becomes the <code>newLength</code> argument.
	 * <p>
	 * The <code>newLength</code> argument must be greater than or equal
	 * to <code>0</code>.
	 *
	 * @param	  newLength   the new length
	 * @throws	 IndexOutOfBoundsException  if the
	 *			   <code>newLength</code> argument is negative.
	 */
	public void setLength(int newLength) {
		if (newLength < 0)
			throw new StringIndexOutOfBoundsException(newLength);
		if (newLength > value.length)
			expandCapacity(newLength);

		if (count < newLength) {
			for (; count < newLength; count++)
				value[count] = '\0';
		} else {
			count = newLength;
		}
	}

	/**
	 * Returns the <code>char</code> value in this sequence at the specified index.
	 * The first <code>char</code> value is at index <code>0</code>, the next at index
	 * <code>1</code>, and so on, as in array indexing.
	 * <p>
	 * The index argument must be greater than or equal to
	 * <code>0</code>, and less than the length of this sequence.
	 *
	 * <p>If the <code>char</code> value specified by the index is a
	 * <a href="Character.html#unicode">surrogate</a>, the surrogate
	 * value is returned.
	 *
	 * @param	  index   the index of the desired <code>char</code> value.
	 * @return	 the <code>char</code> value at the specified index.
	 * @throws	 IndexOutOfBoundsException  if <code>index</code> is
	 *			 negative or greater than or equal to <code>length()</code>.
	 */
	public char charAt(int index) {
		if ((index < 0) || (index >= count))
			throw new StringIndexOutOfBoundsException(index);
		return value[index];
	}


	/**
	 * @see	 java.lang.String#valueOf(java.lang.Object)
	 * @see	 #append(java.lang.String)
	 */
	public _StringBuilder append(Object obj) {
		return append(String.valueOf(obj));
	}

	public _StringBuilder append(String str) {
		if (str == null) str = "null";
		int len = TODAccessor.getStringCount(str);
		int ofs = TODAccessor.getStringOffset(str);
		if (len == 0) return this;
		int newCount = count + len;
		if (newCount > value.length)
			expandCapacity(newCount);
		char[] buf = TODAccessor.getStringChars(str);
		for(int i=0;i<len;i++) value[count+i] = buf[i+ofs];
		count = newCount;
		return this;
	}

	// Appends the specified string builder to this sequence.
	private _StringBuilder append(StringBuilder sb) {
		if (sb == null)
			return append("null");
		int len = sb.length();
		int newcount = count + len;
		if (newcount > value.length)
			expandCapacity(newcount);
		sb.getChars(0, len, value, count);
		count = newcount;
		return this;
	}

	/**
	 * Appends the specified <tt>StringBuffer</tt> to this sequence.
	 * <p>
	 * The characters of the <tt>StringBuffer</tt> argument are appended,
	 * in order, to this sequence, increasing the
	 * length of this sequence by the length of the argument.
	 * If <tt>sb</tt> is <tt>null</tt>, then the four characters
	 * <tt>"null"</tt> are appended to this sequence.
	 * <p>
	 * Let <i>n</i> be the length of this character sequence just prior to
	 * execution of the <tt>append</tt> method. Then the character at index
	 * <i>k</i> in the new character sequence is equal to the character at
	 * index <i>k</i> in the old character sequence, if <i>k</i> is less than
	 * <i>n</i>; otherwise, it is equal to the character at index <i>k-n</i>
	 * in the argument <code>sb</code>.
	 *
	 * @param   sb   the <tt>StringBuffer</tt> to append.
	 * @return  a reference to this object.
	 */
//	public _StringBuilder append(StringBuffer sb) {
//		if (sb == null)
//			return append("null");
//		int len = sb.length();
//		int newCount = count + len;
//		if (newCount > value.length)
//			expandCapacity(newCount);
//		sb.getChars(0, len, value, count);
//		count = newCount;
//		return this;
//	}

	/**
	 * @throws IndexOutOfBoundsException {@inheritDoc}
	 */
	public _StringBuilder append(CharSequence s) {
		if (s == null)
			s = "null";
		if (s instanceof String)
			return this.append((String)s);
//		if (s instanceof StringBuffer)
//			return this.append((StringBuffer)s);
		if (s instanceof StringBuilder)
			return this.append((StringBuilder)s);
		return this.append(s, 0, s.length());
	}

	/**
	 * @throws	 IndexOutOfBoundsException {@inheritDoc}
	 */
	public _StringBuilder append(CharSequence s, int start, int end) {
		if (s == null)
			s = "null";
		if ((start < 0) || (end < 0) || (start > end) || (end > s.length()))
			throw new IndexOutOfBoundsException(
				"start " + start + ", end " + end + ", s.length() "
				+ s.length());
		int len = end - start;
		if (len == 0)
			return this;
		int newCount = count + len;
		if (newCount > value.length)
			expandCapacity(newCount);
		for (int i=start; i<end; i++)
			value[count++] = s.charAt(i);
		count = newCount;
		return this;
	}

	public _StringBuilder append(char str[]) {
		int newCount = count + str.length;
		if (newCount > value.length)
			expandCapacity(newCount);
		_Arrays.arraycopy(str, 0, value, count, str.length);
		count = newCount;
		return this;
	}

	public _StringBuilder append(char str[], int offset, int len) {
		int newCount = count + len;
		if (newCount > value.length)
			expandCapacity(newCount);
		_Arrays.arraycopy(str, offset, value, count, len);
		count = newCount;
		return this;
	}

	/**
	 * @see	 java.lang.String#valueOf(boolean)
	 * @see	 #append(java.lang.String)
	 */
	public _StringBuilder append(boolean b) {
		if (b) {
			int newCount = count + 4;
			if (newCount > value.length)
				expandCapacity(newCount);
			value[count++] = 't';
			value[count++] = 'r';
			value[count++] = 'u';
			value[count++] = 'e';
		} else {
			int newCount = count + 5;
			if (newCount > value.length)
				expandCapacity(newCount);
			value[count++] = 'f';
			value[count++] = 'a';
			value[count++] = 'l';
			value[count++] = 's';
			value[count++] = 'e';
		}
		return this;
	}

	public _StringBuilder append(char c) {
		int newCount = count + 1;
		if (newCount > value.length)
			expandCapacity(newCount);
		value[count++] = c;
		return this;
	}

	/**
	 * Places characters representing the integer i into the
	 * character array buf. The characters are placed into
	 * the buffer backwards starting with the least significant
	 * digit at the specified index (exclusive), and working
	 * backwards from there.
	 *
	 * Will fail if i == Integer.MIN_VALUE
	 */
	static void getChars(int i, int index, char[] buf) {
		int q, r;
		int charPos = index;
		char sign = 0;

		if (i < 0) {
			sign = '-';
			i = -i;
		}

		// Generate two digits per iteration
		while (i >= 65536) {
			q = i / 100;
		// really: r = i - (q * 100);
			r = i - ((q << 6) + (q << 5) + (q << 2));
			i = q;
			buf [--charPos] = DigitOnes[r];
			buf [--charPos] = DigitTens[r];
		}

		// Fall thru to fast mode for smaller numbers
		// assert(i <= 65536, i);
		for (;;) {
			q = (i * 52429) >>> (16+3);
			r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
			buf [--charPos] = digits [r];
			i = q;
			if (i == 0) break;
		}
		if (sign != 0) {
			buf [--charPos] = sign;
		}
	}

	/**
	 * All possible chars for representing a number as a String
	 */
	final static char[] digits = {
		'0' , '1' , '2' , '3' , '4' , '5' ,
		'6' , '7' , '8' , '9' , 'a' , 'b' ,
		'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
		'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
		'o' , 'p' , 'q' , 'r' , 's' , 't' ,
		'u' , 'v' , 'w' , 'x' , 'y' , 'z'
	};


	final static int [] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999,
		99999999, 999999999, Integer.MAX_VALUE };
	
	final static char [] DigitTens = {
		'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
		'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
		'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
		'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
		'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
		'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
		'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
		'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
		'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
		'9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
		} ;

	final static char [] DigitOnes = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		} ;



	// Requires positive x
	static int stringSize(int x) {
	for (int i=0; ; i++)
	if (x <= sizeTable[i])
	return i+1;
	}
	
	// Requires positive x
	static int stringSize(long x) {
		long p = 10;
		for (int i=1; i<19; i++) {
			if (x < p)
				return i;
			p = 10*p;
		}
		return 19;
	}


	/**
	 * @see	 java.lang.String#valueOf(int)
	 * @see	 #append(java.lang.String)
	 */
	public _StringBuilder append(int i) {
		if (i == Integer.MIN_VALUE) {
			append("-2147483648");
			return this;
		}
		int appendedLength = (i < 0) ? stringSize(-i) + 1
									 : stringSize(i);
		int spaceNeeded = count + appendedLength;
		if (spaceNeeded > value.length)
			expandCapacity(spaceNeeded);
		getChars(i, spaceNeeded, value);
		count = spaceNeeded;
		return this;
	}

	/**
	 * Places characters representing the integer i into the
	 * character array buf. The characters are placed into
	 * the buffer backwards starting with the least significant
	 * digit at the specified index (exclusive), and working
	 * backwards from there.
	 *
	 * Will fail if i == Long.MIN_VALUE
	 */
	static void getChars(long i, int index, char[] buf) {
		long q;
		int r;
		int charPos = index;
		char sign = 0;

		if (i < 0) {
			sign = '-';
			i = -i;
		}

		// Get 2 digits/iteration using longs until quotient fits into an int
		while (i > Integer.MAX_VALUE) {
			q = i / 100;
			// really: r = i - (q * 100);
			r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
			i = q;
			buf[--charPos] = DigitOnes[r];
			buf[--charPos] = DigitTens[r];
		}

		// Get 2 digits/iteration using ints
		int q2;
		int i2 = (int)i;
		while (i2 >= 65536) {
			q2 = i2 / 100;
			// really: r = i2 - (q * 100);
			r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
			i2 = q2;
			buf[--charPos] = DigitOnes[r];
			buf[--charPos] = DigitTens[r];
		}

		// Fall thru to fast mode for smaller numbers
		// assert(i2 <= 65536, i2);
		for (;;) {
			q2 = (i2 * 52429) >>> (16+3);
			r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
			buf[--charPos] = digits[r];
			i2 = q2;
			if (i2 == 0) break;
		}
		if (sign != 0) {
			buf[--charPos] = sign;
		}
	}


	/**
	 * @see	 java.lang.String#valueOf(long)
	 * @see	 #append(java.lang.String)
	 */
	public _StringBuilder append(long l) {
		if (l == Long.MIN_VALUE) {
			append("-9223372036854775808");
			return this;
		}
		int appendedLength = (l < 0) ? stringSize(-l) + 1
									 : stringSize(l);
		int spaceNeeded = count + appendedLength;
		if (spaceNeeded > value.length)
			expandCapacity(spaceNeeded);
		getChars(l, spaceNeeded, value);
		count = spaceNeeded;
		return this;
	}

//	/**
//	 * @see	 java.lang.String#valueOf(float)
//	 * @see	 #append(java.lang.String)
//	 */
//	public _StringBuilder append(float f) {
//		new FloatingDecimal(f).appendTo(this);
//		return this;
//	}
//
//	/**
//	 * @see	 java.lang.String#valueOf(double)
//	 * @see	 #append(java.lang.String)
//	 */
//	public _StringBuilder append(double d) {
//		super.append(d);
//		return this;
//	}
//
//	/**
//	 * @since 1.5
//	 */
//	public _StringBuilder appendCodePoint(int codePoint) {
//		super.appendCodePoint(codePoint);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder delete(int start, int end) {
//		super.delete(start, end);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder deleteCharAt(int index) {
//		super.deleteCharAt(index);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder replace(int start, int end, String str) {
//		super.replace(start, end, str);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder insert(int index, char str[], int offset,
//								int len)
//	{
//		super.insert(index, str, offset, len);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		java.lang.String#valueOf(java.lang.Object)
//	 * @see		#insert(int, java.lang.String)
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, Object obj) {
//		return insert(offset, String.valueOf(obj));
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, String str) {
//		super.insert(offset, str);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder insert(int offset, char str[]) {
//		super.insert(offset, str);
//		return this;
//	}
//
//	/**
//	 * @throws IndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder insert(int dstOffset, CharSequence s) {
//		if (s == null)
//			s = "null";
//		if (s instanceof String)
//			return this.insert(dstOffset, (String)s);
//		return this.insert(dstOffset, s, 0, s.length());
//	}
//
//	/**
//	 * @throws IndexOutOfBoundsException {@inheritDoc}
//	 */
//	public _StringBuilder insert(int dstOffset, CharSequence s,
//								int start, int end)
//	{
//		super.insert(dstOffset, s, start, end);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		java.lang.String#valueOf(boolean)
//	 * @see		#insert(int, java.lang.String)
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, boolean b) {
//		super.insert(offset, b);
//		return this;
//	}
//
//	/**
//	 * @throws IndexOutOfBoundsException {@inheritDoc}
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, char c) {
//		super.insert(offset, c);
//		return this;
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		java.lang.String#valueOf(int)
//	 * @see		#insert(int, java.lang.String)
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, int i) {
//		return insert(offset, String.valueOf(i));
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		java.lang.String#valueOf(long)
//	 * @see		#insert(int, java.lang.String)
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, long l) {
//		return insert(offset, String.valueOf(l));
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		java.lang.String#valueOf(float)
//	 * @see		#insert(int, java.lang.String)
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, float f) {
//		return insert(offset, String.valueOf(f));
//	}
//
//	/**
//	 * @throws StringIndexOutOfBoundsException {@inheritDoc}
//	 * @see		java.lang.String#valueOf(double)
//	 * @see		#insert(int, java.lang.String)
//	 * @see		#length()
//	 */
//	public _StringBuilder insert(int offset, double d) {
//		return insert(offset, String.valueOf(d));
//	}
//
//	/**
//	 * @throws NullPointerException {@inheritDoc}
//	 */
//	public int indexOf(String str) {
//		return indexOf(str, 0);
//	}
//
//	/**
//	 * @throws NullPointerException {@inheritDoc}
//	 */
//	public int indexOf(String str, int fromIndex) {
//		return String.indexOf(value, 0, count,
//							  str.toCharArray(), 0, str.length(), fromIndex);
//	}
//
//	/**
//	 * @throws NullPointerException {@inheritDoc}
//	 */
//	public int lastIndexOf(String str) {
//		return lastIndexOf(str, count);
//	}
//
//	/**
//	 * @throws NullPointerException {@inheritDoc}
//	 */
//	public int lastIndexOf(String str, int fromIndex) {
//		return String.lastIndexOf(value, 0, count,
//							  str.toCharArray(), 0, str.length(), fromIndex);
//	}
//
//	public _StringBuilder reverse() {
//		super.reverse();
//		return this;
//	}

	public String toString() {
		// Create a copy, don't share the array
		return new String(value, 0, count);
	}

	/**
	 * Save the state of the <tt>StringBuilder</tt> instance to a stream
	 * (that is, serialize it).
	 *
	 * @serialData the number of characters currently stored in the string
	 *			 builder (<tt>int</tt>), followed by the characters in the
	 *			 string builder (<tt>char[]</tt>).   The length of the
	 *			 <tt>char</tt> array may be greater than the number of
	 *			 characters currently stored in the string builder, in which
	 *			 case extra characters are ignored.
	 */
	private void writeObject(java.io.ObjectOutputStream s)
		throws java.io.IOException {
		s.defaultWriteObject();
		s.writeInt(count);
		s.writeObject(value);
	}

	/**
	 * readObject is called to restore the state of the StringBuffer from
	 * a stream.
	 */
	private void readObject(java.io.ObjectInputStream s)
		throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		count = s.readInt();
		value = (char[]) s.readObject();
	}

}
