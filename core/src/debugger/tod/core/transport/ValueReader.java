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
package tod.core.transport;

import java.io.DataInput;
import java.io.IOException;

import tod.agent.AgentConfig;
import tod.agent.ValueType;
import tod.core.DebugFlags;
import tod.core.database.structure.ObjectId;

/**
 * Permits to read registered objects.
 * @author gpothier
 */
public class ValueReader
{
	private static ValueType readValueType (DataInput aStream) throws IOException
	{
		byte theByte = aStream.readByte();
		return ValueType.VALUES[theByte];
	}
	
    public static Object[] readArguments(DataInput aStream) throws IOException
    {
        int theCount = aStream.readInt();
        Object[] theArguments = new Object[theCount];
        
        for (int i=0;i<theCount;i++)
        {
            theArguments[i] = readValue(aStream);
        }
        return theArguments;
    }
    
	public static Object readValue (DataInput aStream) throws IOException
	{
		ValueType theType = readValueType(aStream);
		switch (theType)
		{
			case NULL:
				return null;
				
			case BOOLEAN:
				return new Boolean (aStream.readByte() != 0);
				
			case BYTE:
				return new Byte (aStream.readByte());
				
			case CHAR:
				return new Character (aStream.readChar());
				
			case INT:
				return new Integer (aStream.readInt());
				
			case LONG:
				return new Long (aStream.readLong());
				
			case FLOAT:
				return new Float (aStream.readFloat());
				
			case DOUBLE:
				return new Double (aStream.readDouble());
				
			case OBJECT_UID:
			{
				long theObjectId = readObjectId(aStream);
				return new ObjectId(theObjectId);
			}
				
			default:
				throw new RuntimeException("Unexpected message: "+theType);
		}
	}
	
	public static long readObjectId(DataInput aStream) throws IOException
	{
		long theObjectId = aStream.readLong();
		if (DebugFlags.IGNORE_HOST) theObjectId >>>= AgentConfig.HOST_BITS;
		return theObjectId;
	}
}
