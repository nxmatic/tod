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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

import tod.impl.bci.asm.attributes.SootAttribute.Entry;

/**
 * An attribute that uses a {@link DataInputStream} instead of a {@link ClassReader}.
 * @author gpothier
 */
public abstract class DataAttribute extends Attribute
{

	public DataAttribute(String aType)
	{
		super(aType);
	}

	/**
	 * The read method that should be implemented by subclasses 
	 */
	protected abstract Attribute read(DataInputStream aStream, Label[] aLabels) throws IOException;
	
	@Override
	protected final Attribute read(
			ClassReader cr, 
			int off, 
			int len, 
			char[] buf, 
			int codeOff, 
			Label[] aLabels)
	{
		try
		{
			DataInputStream theStream = new DataInputStream(new ByteArrayInputStream(cr.b, off, len));
			return read(theStream, aLabels);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The write method that should be implemented by subclasses.
	 */
	protected abstract void write(DataOutputStream aStream, int len, int maxStack, int maxLocals) throws IOException;
	
	@Override
	protected final ByteVector write(
			ClassWriter cw, 
			byte[] code, 
			int len, 
			int maxStack, 
			int maxLocals)
	{
		try
		{
			ByteArrayOutputStream theOut = new ByteArrayOutputStream();
			DataOutputStream theStream = new DataOutputStream(theOut);
			
			write(theStream, len, maxStack, maxLocals);
			
			theStream.flush();
			
			byte[] theArray = theOut.toByteArray();
			ByteVector bv = new ByteVector(theArray.length);
			bv.putByteArray(theArray, 0, theArray.length);
			return bv;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
