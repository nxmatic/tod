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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * An attribute that has no actual content but that is only used
 * to obtain updated label offsets.
 * @author gpothier
 */
public class DummyLabelsAttribute extends Attribute
{
	private List<Label> itsLabels = new ArrayList<Label>();

	public DummyLabelsAttribute()
	{
		super("DummyLabelsAttribute");
	}

	@Override
	protected Label[] getLabels()
	{
		return itsLabels.toArray(new Label[itsLabels.size()]);
	}

	public void add(Label aLabel)
	{
		itsLabels.add(aLabel);
	}
	
	@Override
	public boolean isCodeAttribute()
	{
		return true;
	}

	@Override
	protected Attribute read(
			ClassReader aCr,
			int aOff,
			int aLen,
			char[] aBuf,
			int aCodeOff,
			Label[] aLabels)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected ByteVector write(
			ClassWriter aCw,
			byte[] aCode,
			int aLen,
			int aMaxStack,
			int aMaxLocals)
	{
		return new ByteVector();
	}
	
	
}
