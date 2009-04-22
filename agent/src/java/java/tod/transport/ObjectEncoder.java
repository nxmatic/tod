/*
 * Created on Jan 13, 2009
 */
package java.tod.transport;


import java.tod.util._IdentityHashMap;

import tod.agent.ObjectValue;
import tod.agent.ObjectValue.FieldValue;
import tod.agent.io._ByteBuffer;

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
		}
		else if (aObject instanceof String)
		{
			String v = (String) aObject;
			writeString(v, aBuffer);
		}
		else if (aObject instanceof Integer)
		{
			Integer v = (Integer) aObject;
			aBuffer.put(ObjectValue.TYPE_INT);
			aBuffer.putInt(v.intValue());
		}
		else if (aObject instanceof Long)
		{
			Long v = (Long) aObject;
			aBuffer.put(ObjectValue.TYPE_LONG);
			aBuffer.putLong(v.longValue());
		}
		else if (aObject instanceof Byte)
		{
			Byte v = (Byte) aObject;
			aBuffer.put(ObjectValue.TYPE_BYTE);
			aBuffer.put(v.byteValue());
		}
		else if (aObject instanceof Short)
		{
			Short v = (Short) aObject;
			aBuffer.put(ObjectValue.TYPE_SHORT);
			aBuffer.putShort(v.shortValue());
		}
		else if (aObject instanceof Character)
		{
			Character v = (Character) aObject;
			aBuffer.put(ObjectValue.TYPE_CHAR);
			aBuffer.putChar(v.charValue());
		}
		else if (aObject instanceof Float)
		{
			Float v = (Float) aObject;
			aBuffer.put(ObjectValue.TYPE_FLOAT);
			aBuffer.putFloat(v.floatValue());
		}
		else if (aObject instanceof Double)
		{
			Double v = (Double) aObject;
			aBuffer.put(ObjectValue.TYPE_DOUBLE);
			aBuffer.putDouble(v.doubleValue());
		}
		else if (aObject instanceof Boolean)
		{
			Boolean v = (Boolean) aObject;
			aBuffer.put(ObjectValue.TYPE_BOOLEAN);
			aBuffer.put(v.booleanValue() ? (byte) 1 : (byte) 0);
		}
		else if (aObject instanceof ObjectValue)
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
		else throw new RuntimeException("Not handled: "+aObject);
	}

	private static void writeString(String v, _ByteBuffer aBuffer)
	{
		aBuffer.put(ObjectValue.TYPE_STRING);
		aBuffer.putInt(v.length());
		for(int i=0;i<v.length();i++) aBuffer.putChar(v.charAt(i));
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
