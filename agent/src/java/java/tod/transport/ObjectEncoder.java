/*
 * Created on Jan 13, 2009
 */
package java.tod.transport;


import java.tod.util._IdentityHashMap;
import java.tod.util._StringBuilder;

import tod2.access.TODAccessor;
import tod2.agent.ObjectValue;
import tod2.agent.ObjectValue.FieldValue;
import tod2.agent.io._ByteBuffer;

/**
 * This class handles our custom serialization of objects.
 * See {@link ObjectValue}
 * @author gpothier
 */
public class ObjectEncoder
{
	/**
	 * Encodes an object into the specified buffer.
	 */
	public static void encode(Object aObject, _ByteBuffer aBuffer)
	{
		encode(aObject, aBuffer, new _IdentityHashMap<ObjectValue, Integer>());
	}
	
	private static void encode(Object aObject, _ByteBuffer aBuffer, _IdentityHashMap<ObjectValue, Integer> aMapping)
	{
		if (aObject == null)
		{
			aBuffer.put(ObjectValue.TYPE_NULL);
			return;
		}
		
		Class theClass = aObject.getClass();
		
		if (theClass == String.class)
		{
			String v = (String) aObject;
			writeString(v, aBuffer);
		}
		else if (theClass == Integer.class)
		{
			Integer v = (Integer) aObject;
			aBuffer.put(ObjectValue.TYPE_INT);
			aBuffer.putInt(v.intValue());
		}
		else if (theClass == Long.class)
		{
			Long v = (Long) aObject;
			aBuffer.put(ObjectValue.TYPE_LONG);
			aBuffer.putLong(v.longValue());
		}
		else if (theClass == Byte.class)
		{
			Byte v = (Byte) aObject;
			aBuffer.put(ObjectValue.TYPE_BYTE);
			aBuffer.put(v.byteValue());
		}
		else if (theClass == Short.class)
		{
			Short v = (Short) aObject;
			aBuffer.put(ObjectValue.TYPE_SHORT);
			aBuffer.putShort(v.shortValue());
		}
		else if (theClass == Character.class)
		{
			Character v = (Character) aObject;
			aBuffer.put(ObjectValue.TYPE_CHAR);
			aBuffer.putChar(v.charValue());
		}
		else if (theClass == Float.class)
		{
			Float v = (Float) aObject;
			aBuffer.put(ObjectValue.TYPE_FLOAT);
			aBuffer.putFloat(v.floatValue());
		}
		else if (theClass == Double.class)
		{
			Double v = (Double) aObject;
			aBuffer.put(ObjectValue.TYPE_DOUBLE);
			aBuffer.putDouble(v.doubleValue());
		}
		else if (theClass == Boolean.class)
		{
			Boolean v = (Boolean) aObject;
			aBuffer.put(ObjectValue.TYPE_BOOLEAN);
			aBuffer.put(v.booleanValue() ? (byte) 1 : (byte) 0);
		}
		else if (theClass == ObjectValue.class)
		{
			ObjectValue v = (ObjectValue) aObject;
			
			Integer theId = aMapping.get(v);
			if (theId == null)
			{
				theId = aMapping.size()+1;
				aMapping.put(v, theId);
				writeObjectValue(v, aBuffer, aMapping);
			}
			else
			{
				aBuffer.put(ObjectValue.TYPE_REF);
				aBuffer.putInt(theId.intValue());
			}
		}
		else 
		{
			_StringBuilder b = new _StringBuilder();
			b.append("Not handled: ");
			b.append(aObject);
			b.append(" (");
			b.append(aObject.getClass());
			b.append(")");
			throw new RuntimeException(b.toString());
		}
	}

	private static void writeString(String v, _ByteBuffer aBuffer)
	{
		aBuffer.put(ObjectValue.TYPE_STRING);
		aBuffer.putString(v);
	}

	private static void writeObjectValue(ObjectValue aObjectValue, _ByteBuffer aBuffer, _IdentityHashMap<ObjectValue, Integer> aMapping)
	{
		aBuffer.put(ObjectValue.TYPE_VALUE);
		
		aBuffer.putString(aObjectValue.getClassName());
		aBuffer.put(aObjectValue.isThrowable() ? (byte) 1 : (byte) 0);
		
		FieldValue[] theFields = aObjectValue.getFields();
		aBuffer.putInt(theFields.length);
		for(FieldValue theField : theFields)
		{
			aBuffer.putString(theField.fieldName);
			encode(theField.value, aBuffer, aMapping);
		}
	}
}
