/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.core.transport;

import java.io.DataInputStream;
import java.io.IOException;
import java.tod.transport.ObjectEncoder;
import java.util.HashMap;
import java.util.Map;

import tod.utils.ByteBuffer;
import tod2.agent.ObjectValue;
import tod2.agent.ObjectValue.FieldValue;

/**
 * Decodes objects encoded by {@link ObjectEncoder}
 * @author gpothier
 */
public class ObjectDecoder
{
	public static Object decode(DataInputStream aStream)
	{
		try
		{
			return decode(aStream, new HashMap<Integer, ObjectValue>());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static Object decode(DataInputStream aStream, Map<Integer, ObjectValue> aMapping) throws IOException
	{
		byte theType = aStream.readByte();
		
		switch(theType)
		{
		case ObjectValue.TYPE_NULL: return null;
		case ObjectValue.TYPE_STRING: return ByteBuffer.getString(aStream);
		case ObjectValue.TYPE_INT: return ByteBuffer.getIntL(aStream); 
		case ObjectValue.TYPE_LONG: return ByteBuffer.getLongL(aStream);
		case ObjectValue.TYPE_BYTE: return aStream.readByte();
		case ObjectValue.TYPE_SHORT: return (short) ByteBuffer.getCharL(aStream);
		case ObjectValue.TYPE_CHAR: return ByteBuffer.getCharL(aStream);
		case ObjectValue.TYPE_FLOAT: return Float.intBitsToFloat(ByteBuffer.getIntL(aStream));
		case ObjectValue.TYPE_DOUBLE: return Double.longBitsToDouble(ByteBuffer.getLongL(aStream));
		case ObjectValue.TYPE_BOOLEAN: return aStream.readChar() != 0;
		case ObjectValue.TYPE_VALUE: return readObjectValue(aStream, aMapping); 
		case ObjectValue.TYPE_REF:
			int theId = ByteBuffer.getIntL(aStream);
			Object theValue = aMapping.get(theId);
			if (theValue == null) throw new RuntimeException("No mapping for "+theId);
			return theValue;
			
		default: throw new RuntimeException("Not handled: "+theType);
		}
	}
	
	private static ObjectValue readObjectValue(DataInputStream aStream, Map<Integer, ObjectValue> aMapping) throws IOException
	{
		String theClassName = ByteBuffer.getString(aStream);
		boolean theThrowable = aStream.readByte() != 0;
		
		ObjectValue theResult = new ObjectValue(theClassName, theThrowable);
		aMapping.put(aMapping.size()+1, theResult);
		
		int theFieldCount = ByteBuffer.getIntL(aStream);
		
		FieldValue[] theFields = new FieldValue[theFieldCount];
		for(int i=0;i<theFieldCount;i++)
		{
			String theFieldName = ByteBuffer.getString(aStream);
			Object theFieldValue = decode(aStream, aMapping);
			
			theFields[i] = new FieldValue(theFieldName, theFieldValue);
		}
		
		theResult.setFields(theFields);
		
		return theResult;
	}
}
