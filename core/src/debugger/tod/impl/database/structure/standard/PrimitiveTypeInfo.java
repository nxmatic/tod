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
package tod.impl.database.structure.standard;

import org.objectweb.asm.Type;

import tod.core.database.structure.IPrimitiveTypeInfo;

public abstract class PrimitiveTypeInfo extends TypeInfo implements IPrimitiveTypeInfo
{
	private static final long serialVersionUID = 2145422655286109651L;
	
	public static final PrimitiveTypeInfo VOID = new PrimitiveTypeInfo("void", Type.VOID_TYPE, 0, 1)
	{
		public Object getDefaultInitialValue()
		{
			return null;
		}
	};
	
	public static final PrimitiveTypeInfo BOOLEAN = new PrimitiveTypeInfo("boolean", Type.BOOLEAN_TYPE, 1, 2)
	{
		public Object getDefaultInitialValue()
		{
			return Boolean.FALSE;
		}
	};
	
	public static final PrimitiveTypeInfo INT = new PrimitiveTypeInfo("int", Type.INT_TYPE, 1, 3)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	
	public static final PrimitiveTypeInfo LONG = new PrimitiveTypeInfo("long", Type.LONG_TYPE, 2, 4)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	
	public static final PrimitiveTypeInfo BYTE = new PrimitiveTypeInfo("byte", Type.BYTE_TYPE, 1, 5)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	
	public static final PrimitiveTypeInfo SHORT = new PrimitiveTypeInfo("short", Type.SHORT_TYPE, 1, 6)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	
	public static final PrimitiveTypeInfo CHAR = new PrimitiveTypeInfo("char", Type.CHAR_TYPE, 1, 7)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	
	public static final PrimitiveTypeInfo DOUBLE = new PrimitiveTypeInfo("double", Type.DOUBLE_TYPE, 2, 8)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	
	public static final PrimitiveTypeInfo FLOAT = new PrimitiveTypeInfo("float", Type.FLOAT_TYPE, 1, 9)
	{
		public Object getDefaultInitialValue()
		{
			return 0;
		}
	};
	

	public static final PrimitiveTypeInfo[] TYPES = {
		VOID, BOOLEAN, INT, LONG, BYTE, SHORT, CHAR, DOUBLE, FLOAT
	};
	
	private final int itsSize;
	private final String itsJvmName;

	public PrimitiveTypeInfo(String aName, Type aAsmType, int aSize, int aId)
	{
		super(null, aId, aName);
		itsJvmName = aAsmType.getDescriptor();
		itsSize = aSize;
	}

	public String getJvmName()
	{
		return itsJvmName;
	}
	
	public int getSize()
	{
		return itsSize;
	}

	public boolean isArray()
	{
		return false;
	}

	public boolean isPrimitive()
	{
		return true;
	}

	public boolean isVoid()
	{
		return "void".equals(getName());
	}
	
	/**
	 * Returns the type info corresponding to the specified id.
	 * Note that in a structure database, ids 1 to 9 are for primitive types.
	 */
	public static PrimitiveTypeInfo get(int aId)
	{
		return TYPES[aId-1];
	}
	
	public PrimitiveTypeInfo createUncertainClone()
	{
		throw new UnsupportedOperationException();
	}


}
