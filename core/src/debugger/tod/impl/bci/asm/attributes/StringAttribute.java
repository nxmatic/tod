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
package tod.impl.bci.asm.attributes;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * An attribute that that is represented by an UTF-8 encoded string
 * @author gpothier
 */
public abstract class StringAttribute extends Attribute
{
	private static final Charset CHARSET = Charset.forName("UTF-8");
	
	public StringAttribute(String aType)
	{
		super(aType);
	}

	/**
	 * The read method that should be implemented by subclasses 
	 */
	protected abstract Attribute read(String aData, Label[] aLabels);
	
	@Override
	protected final Attribute read(
			ClassReader cr, 
			int off, 
			int len, 
			char[] buf, 
			int codeOff, 
			Label[] aLabels)
	{
		CharBuffer theCharBuffer = CHARSET.decode(ByteBuffer.wrap(cr.b, off, len));
		return read(theCharBuffer.toString(), aLabels);
	}
	
	/**
	 * The write method that should be implemented by subclasses.
	 */
	protected abstract String write(int len, int maxStack, int maxLocals);
	
	@Override
	protected final ByteVector write(
			ClassWriter cw, 
			byte[] code, 
			int len, 
			int maxStack, 
			int maxLocals)
	{
		String theData = write(len, maxStack, maxLocals);
		ByteBuffer theByteBuffer = CHARSET.encode(CharBuffer.wrap(theData));
		byte[] theArray = theByteBuffer.array();
		ByteVector bv = new ByteVector(theArray.length);
		bv.putByteArray(theArray, 0, theArray.length);
		return bv;
	}
}
