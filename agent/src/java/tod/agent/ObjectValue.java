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
package tod.agent;

/**
 * Represents the value of some object. It is always possible to
 * deserialize an {@link ObjectValue} in the database's or client's JVM,
 * whereas it would not necessarily be possible to deserialize the actual object
 * (for classes that are not part of the JDK).  
 * @author gpothier
 */
public class ObjectValue
{
	// These constants are used for encoding/decoding
	public static final byte TYPE_STRING = 10;
	public static final byte TYPE_INT = 11;
	public static final byte TYPE_LONG = 12;
	public static final byte TYPE_BYTE = 13;
	public static final byte TYPE_CHAR = 14;
	public static final byte TYPE_SHORT = 15;
	public static final byte TYPE_FLOAT = 16;
	public static final byte TYPE_DOUBLE = 17;
	public static final byte TYPE_BOOLEAN = 18;
	public static final byte TYPE_VALUE = 19;
	public static final byte TYPE_REF = 20;
	public static final byte TYPE_NULL = 21;

	private String itsClassName;
	private FieldValue[] itsFields;
	private boolean itsThrowable;
	
	public ObjectValue(String aClassName, boolean aThrowable)
	{
		itsClassName = aClassName;
		itsThrowable = aThrowable;
	}
	
	public FieldValue[] getFields()
	{
		return itsFields;
	}

	public void setFields(FieldValue[] aFields)
	{
		itsFields = aFields;
	}

	public String getClassName()
	{
		return itsClassName;
	}

	/**
	 * Whether the represented object is a {@link Throwable}.
	 */
	public boolean isThrowable()
	{
		return itsThrowable;
	}
	
	/**
	 * Returns the value for the (first encountered match of the) given field. 
	 */
	public Object getFieldValue(String aFieldName)
	{
		for (FieldValue theValue : itsFields)
		{
			if (theValue.fieldName.equals(aFieldName)) return theValue.value;
		}
		return null;
	}

	/**
	 * Returns a user-readable representation of the object.
	 */
	public String asString()
	{
		return asString(1);
	}
	
	public String asString(int aLevel)
	{
		if (aLevel == 0) return "";
		StringBuilder theBuilder = new StringBuilder();
		for (FieldValue theFieldValue : itsFields)
		{
			theBuilder.append(theFieldValue.asString(aLevel-1));
			theBuilder.append(' ');
		}
		return theBuilder.toString();
	}
	
	@Override
	public String toString()
	{
		return "ObjectValue ["+asString()+"]";
	}
	
	public static class FieldValue
	{
		public final String fieldName;
		public final Object value;

		public FieldValue(String aFieldName, Object aValue)
		{
			fieldName = aFieldName;
			value = aValue;
		}
		
		public String asString(int aLevel)
		{
			String theValueString;
			if (value instanceof ObjectValue)
			{
				ObjectValue theObjectValue = (ObjectValue) value;
				theValueString = theObjectValue.asString(aLevel);
			}
			else theValueString = ""+value;
			
			return fieldName+"='"+theValueString+"'";
		}
	}

}
