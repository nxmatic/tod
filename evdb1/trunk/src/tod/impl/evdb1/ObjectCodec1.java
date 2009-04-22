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
package tod.impl.evdb1;

import tod.core.database.structure.ObjectId;
import zz.utils.bit.BitStruct;
import zz.utils.bit.BitUtils;

/**
 * Provides methods to read and write objects to {@link BitStruct}s.
 * @author gpothier
 */
public class ObjectCodec1
{
	/**
	 * Enumerates the different kinds of objects that can be stored or read.
	 * @author gpothier
	 */
	private static enum ObjectType
	{
		NULL()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 0;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				return null;
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
			}
		},
		UID()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 64;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				long theUid = aStruct.readLong(64);
				assert theUid != 0;
				return new ObjectId(theUid);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				ObjectId theId = (ObjectId) aObject;
				long theUid = theId.getId();
				assert theUid != 0;
				aStruct.writeLong(theUid, 64);
			}
		}, 
		LONG()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 64;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				return aStruct.readLong(64);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Long theLong = (Long) aObject;
				aStruct.writeLong(theLong, 64);
			}
		}, 
		INT()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 32;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				return aStruct.readInt(32);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Integer theInteger = (Integer) aObject;
				aStruct.writeInt(theInteger, 32);
			}
		}, 
		CHAR()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 16;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				int theValue = aStruct.readInt(16);
				return new Character((char) theValue);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Character theCharacter = (Character) aObject;
				aStruct.writeInt(theCharacter.charValue(), 16);
			}
		}, 
		SHORT()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 16;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				int theValue = aStruct.readInt(16);
				return new Short((short) theValue);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Short theShort = (Short) aObject;
				aStruct.writeInt(theShort.shortValue(), 16);
			}
		}, 
		BYTE()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 8;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				return aStruct.readByte(8);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Byte theByte = (Byte) aObject;
				aStruct.writeInt(theByte.byteValue(), 8);
			}
		}, 
		BOOLEAN()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 1;
			}
			
			@Override
			public Object readObject(BitStruct aStruct)
			{
				return aStruct.readBoolean();
			}
			
			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Boolean theBoolean = (Boolean) aObject;
				aStruct.writeBoolean(theBoolean.booleanValue());
			}
		}, 
		DOUBLE()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 64;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				long theBits = aStruct.readLong(64);
				return Double.longBitsToDouble(theBits);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Double theDouble = (Double) aObject;
				aStruct.writeLong(Double.doubleToRawLongBits(theDouble), 64);
			}
		}, 
		FLOAT()
		{
			@Override
			public int getObjectBits(Object aObject)
			{
				return 32;
			}

			@Override
			public Object readObject(BitStruct aStruct)
			{
				int theBits = aStruct.readInt(32);
				return Float.intBitsToFloat(theBits);
			}

			@Override
			public void writeObject(BitStruct aStruct, Object aObject)
			{
				Float theFloat = (Float) aObject;
				aStruct.writeInt(Float.floatToRawIntBits(theFloat), 32);
			}
		};
		
		public abstract void writeObject(BitStruct aStruct, Object aObject);
		public abstract Object readObject(BitStruct aStruct);
		public abstract int getObjectBits(Object aObject);
		
		/**
		 * Cached values; call to values() is costly. 
		 */
		public static final ObjectType[] VALUES = values();
	}
	
	/**
	 * Number of bits necessary to represent an object type.
	 */
	private static final int TYPE_BITS = BitUtils.log2ceil(ObjectType.VALUES.length);
	
	private static void writeType(BitStruct aStruct, ObjectType aType)
	{
		aStruct.writeInt(aType.ordinal(), TYPE_BITS);
	}
	
	private static ObjectType readType(BitStruct aStruct)
	{
		int theIndex = aStruct.readInt(TYPE_BITS);
		return ObjectType.VALUES[theIndex];
	}
	
	private static ObjectType findType(Object aObject)
	{
		if (aObject == null) return ObjectType.NULL;
		
		Class theClass = aObject.getClass();
		
		// The following code is faster than using a map
		// (Pentium M 2ghz)
		if (theClass == Byte.class) return ObjectType.BYTE;
		else if (theClass == Boolean.class) return ObjectType.BOOLEAN;
		else if (theClass == Character.class) return ObjectType.CHAR;
		else if (theClass == Double.class) return ObjectType.DOUBLE;
		else if (theClass == Float.class) return ObjectType.FLOAT;
		else if (theClass == Integer.class) return ObjectType.INT;
		else if (theClass == Long.class) return ObjectType.LONG;
		else if (theClass == Short.class) return ObjectType.SHORT;
		else if (theClass == ObjectId.class) return ObjectType.UID;
		else throw new RuntimeException("Not handled: "+aObject+" ("+theClass+")");
	}
	
	/**
	 * Returns the number of bits necessary to serialize the given object.
	 */
	public static int getObjectBits(Object aObject)
	{
		ObjectType theType = findType(aObject);
//		ObjectType theType = ObjectType.DOUBLE;
		return theType.getObjectBits(aObject) + TYPE_BITS;
	}
	
	/**
	 * Writes an object to the specified struct. This method should be used by
	 * subclasses to serialize values.
	 */
	public static void writeObject(BitStruct aBitStruct, Object aObject)
	{
		ObjectType theType = findType(aObject);
		writeType(aBitStruct, theType);
		theType.writeObject(aBitStruct, aObject);
	}
	
	/**
	 * Reads an object from the specified struct. This method should be used by
	 * subclasses to deserialize values.
	 */
	public static Object readObject(BitStruct aBitStruct)
	{
		ObjectType theType = readType(aBitStruct);
		return theType.readObject(aBitStruct);
	}
	
	/**
	 * Returns the internal object id that corresponds to the given object.
	 * If the object is a {@link ObjectUID}, then its id converted to int
	 * is returned.
	 * @param aFail If true, the method fails with an exception if the object
	 * is not an {@link ObjectUID}. If false and the object is not an {@link ObjectUID},
	 * the method returns 0;
	 */
	public static long getObjectId(Object aObject, boolean aFail)
	{
		if (aObject instanceof ObjectId)
		{
			ObjectId theId = (ObjectId) aObject;
			return theId.getId();
		}
		else if (aFail) throw new RuntimeException("Not handled: "+aObject);
		else return 0;
	}
	
}
