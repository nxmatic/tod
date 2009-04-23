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
package tod.impl.evdbng;

import tod.core.database.structure.ObjectId;
import tod.impl.evdbng.db.file.PagedFile.PageIOStream;

/**
 * Provides methods to read and write objects to {@link PageIOStream}s.
 * @author gpothier
 */
public class ObjectCodecNG
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
			public int getObjectSize(Object aObject)
			{
				return 0;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return null;
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
			}
		},
		UID()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 8;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				long theUid = aStruct.readLong();
				assert theUid != 0;
				return new ObjectId(theUid);
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				ObjectId theId = (ObjectId) aObject;
				long theUid = theId.getId();
				assert theUid != 0;
				aStruct.writeLong(theUid);
			}
		}, 
		LONG()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 8;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return aStruct.readLong();
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Long theLong = (Long) aObject;
				aStruct.writeLong(theLong);
			}
		}, 
		INT()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 4;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return aStruct.readInt();
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Integer theInteger = (Integer) aObject;
				aStruct.writeInt(theInteger);
			}
		}, 
		CHAR()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 2;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return new Character((char) aStruct.readShort());
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Character theCharacter = (Character) aObject;
				aStruct.writeShort((short) theCharacter.charValue());
			}
		}, 
		SHORT()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 2;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return new Short(aStruct.readShort());
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Short theShort = (Short) aObject;
				aStruct.writeShort(theShort.shortValue());
			}
		}, 
		BYTE()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 1;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return aStruct.readByte();
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Byte theByte = (Byte) aObject;
				aStruct.writeByte(theByte.byteValue());
			}
		}, 
		BOOLEAN()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 1;
			}
			
			@Override
			public Object readObject(PageIOStream aStruct)
			{
				return aStruct.readBoolean();
			}
			
			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Boolean theBoolean = (Boolean) aObject;
				aStruct.writeBoolean(theBoolean.booleanValue());
			}
		}, 
		DOUBLE()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 8;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				long theBits = aStruct.readLong();
				return Double.longBitsToDouble(theBits);
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Double theDouble = (Double) aObject;
				aStruct.writeLong(Double.doubleToRawLongBits(theDouble));
			}
		}, 
		FLOAT()
		{
			@Override
			public int getObjectSize(Object aObject)
			{
				return 4;
			}

			@Override
			public Object readObject(PageIOStream aStruct)
			{
				int theBits = aStruct.readInt();
				return Float.intBitsToFloat(theBits);
			}

			@Override
			public void writeObject(PageIOStream aStruct, Object aObject)
			{
				Float theFloat = (Float) aObject;
				aStruct.writeInt(Float.floatToRawIntBits(theFloat));
			}
		};
		
		public abstract void writeObject(PageIOStream aStruct, Object aObject);
		public abstract Object readObject(PageIOStream aStruct);
		
		/**
		 * Returns the size in bytes of the object.
		 */
		public abstract int getObjectSize(Object aObject);
		
		/**
		 * Cached values; call to values() is costly. 
		 */
		public static final ObjectType[] VALUES = values();
	}
	
	private static void writeType(PageIOStream aStruct, ObjectType aType)
	{
		aStruct.writeByte(aType.ordinal());
	}
	
	private static ObjectType readType(PageIOStream aStream)
	{
		int theIndex = aStream.readByte();
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
	public static int getObjectSize(Object aObject)
	{
		ObjectType theType = findType(aObject);
//		ObjectType theType = ObjectType.DOUBLE;
		return 1 + theType.getObjectSize(aObject);
	}
	
	/**
	 * Writes an object to the specified struct. This method should be used by
	 * subclasses to serialize values.
	 */
	public static void writeObject(PageIOStream aPageBitStruct, Object aObject)
	{
		ObjectType theType = findType(aObject);
		writeType(aPageBitStruct, theType);
		theType.writeObject(aPageBitStruct, aObject);
	}
	
	/**
	 * Reads an object from the specified struct. This method should be used by
	 * subclasses to deserialize values.
	 */
	public static Object readObject(PageIOStream aPageBitStruct)
	{
		ObjectType theType = readType(aPageBitStruct);
		return theType.readObject(aPageBitStruct);
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
