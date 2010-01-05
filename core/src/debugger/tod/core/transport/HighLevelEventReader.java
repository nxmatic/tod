///*
//TOD - Trace Oriented Debugger.
//Copyright (c) 2006-2008, Guillaume Pothier
//All rights reserved.
//
//This program is free software; you can redistribute it and/or 
//modify it under the terms of the GNU General Public License 
//version 2 as published by the Free Software Foundation.
//
//This program is distributed in the hope that it will be useful, 
//but WITHOUT ANY WARRANTY; without even the implied warranty of 
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License 
//along with this program; if not, write to the Free Software 
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
//MA 02111-1307 USA
//
//Parts of this work rely on the MD5 algorithm "derived from the 
//RSA Data Security, Inc. MD5 Message-Digest Algorithm".
//*/
//package tod.core.transport;
//
//import static tod.core.transport.ValueReader.readArguments;
//import static tod.core.transport.ValueReader.readValue;
//
//import java.io.DataInputStream;
//import java.io.IOException;
//
//import tod.core.ILogCollector;
//import tod2.agent.Output;
//
//public class HighLevelEventReader
//{
//	private static final boolean READ_SIZE = false;
//	
//	public static void readPacket(
//			DataInputStream aStream, 
//			ILogCollector aCollector) throws IOException
//	{
//		HighLevelEventType theCommand = readEventType(aStream);
//		readPacket(aStream, aCollector, theCommand);
//	}
//	
//	public static void readPacket(
//			DataInputStream aStream,
//			ILogCollector aCollector, 
//			HighLevelEventType aType) throws IOException
//	{
//		switch (aType)
//		{
//			case INSTANTIATION:
//                readInstantiation(aStream, aCollector);
//                break;
//                
//			case SUPER_CALL:
//				readSuperCall(aStream, aCollector);
//				break;
//				
//			case METHOD_CALL:
//                readMethodCall(aStream, aCollector);
//                break;
//                
//			case BEHAVIOR_EXIT:
//                readBehaviorExit(aStream, aCollector);
//                break;
//                
//			case FIELD_WRITE:
//                readFieldWrite(aStream, aCollector);
//                break;
//                
//			case NEW_ARRAY:
//				readNewArray(aStream, aCollector);
//				break;
//				
//			case ARRAY_WRITE:
//				readArrayWrite(aStream, aCollector);
//				break;
//				
//			case LOCAL_VARIABLE_WRITE:
//				readLocalWrite(aStream, aCollector);
//				break;
//				
//			case OUTPUT:
//				readOutput(aStream, aCollector);
//				break;
//				
//			case EXCEPTION_BYNAME:
//				readExceptionByName(aStream, aCollector);
//				break;
//				
//			case EXCEPTION_BYID:
//				readExceptionById(aStream, aCollector);
//				break;
//				
//			case INSTANCEOF:
//				readInstantiation(aStream, aCollector);
//				break;
//				
//			case REGISTER_THREAD:
//				readThread(aStream, aCollector);
//				break;
//				
//			case REGISTER_OBJECT:
//				readRegister(aStream, aCollector);
//				break;
//				
//			case REGISTER_REFOBJECT:
//				readRegisterRef(aStream, aCollector);
//				break;
//				
//			case REGISTER_CLASS:
//				readRegisterClass(aStream, aCollector);
//				break;
//				
//			case REGISTER_CLASSLOADER:
//				readRegisterClassLoader(aStream, aCollector);
//				break;
//				
//			default:
//				throw new RuntimeException("Unexpected message: "+aType);
//		}
//	}
//	
//	private static HighLevelEventType readEventType (DataInputStream aStream) throws IOException
//	{
//		byte theByte = aStream.readByte();
//		return HighLevelEventType.VALUES[theByte];
//	}
//	
//	private static int[] readAdviceCFlow(DataInputStream aStream) throws IOException
//	{
//		int theSize = aStream.readByte();
//		if (theSize == 0) return null;
//		int[] theCFlow = new int[theSize];
//		for(int i=0;i<theSize;i++) theCFlow[i] = aStream.readShort();
//		return theCFlow;
//	}
//	
//	public static void readMethodCall(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.methodCall(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				aStream.readBoolean(),
//				aStream.readInt(),
//				aStream.readInt(),
//				readValue(aStream), readArguments(aStream));
//	}
//	
//	public static void readInstantiation(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.instantiation(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				aStream.readBoolean(),
//				aStream.readInt(),
//				aStream.readInt(),
//				readValue(aStream), readArguments(aStream));
//	}
//	
//	public static void readSuperCall(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.superCall(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				aStream.readBoolean(),
//				aStream.readInt(),
//				aStream.readInt(),
//				readValue(aStream), readArguments(aStream));
//
//	}
//	
//	public static void readBehaviorExit(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.behaviorExit(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				aStream.readInt(),
//				aStream.readBoolean(), readValue(aStream));
//	}
//	
//	public static void readFieldWrite(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.fieldWrite(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				aStream.readInt(),
//				readValue(aStream), readValue(aStream));
//	}
//	
//	public static void readNewArray(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.newArray(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				readValue(aStream),
//				aStream.readInt(), aStream.readInt());
//	}
//	
//	public static void readArrayWrite(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.arrayWrite(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				readValue(aStream),
//				aStream.readInt(), readValue(aStream));
//	}
//	
//	public static void readInstanceOf(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.instanceOf(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				readValue(aStream),
//				aStream.readInt(),
//				aStream.readBoolean());
//	}
//	
//	public static void readLocalWrite(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.localWrite(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				aStream.readInt(), readValue(aStream));
//	}
//	
//	public static void readExceptionByName(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.exception(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readUTF(),
//				aStream.readUTF(),
//				aStream.readUTF(),
//				aStream.readShort(), 
//				readValue(aStream));
//	}
//	
//	public static void readExceptionById(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.exception(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readShort(),
//				aStream.readLong(),
//				readAdviceCFlow(aStream),
//				aStream.readInt(),
//				readValue(aStream));
//	}
//	
//	public static void readOutput(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//        aCollector.output(
//        		aStream.readInt(),
//				aStream.readLong(),
//        		aStream.readShort(),
//        		aStream.readLong(),
//				readAdviceCFlow(aStream),
//                Output.VALUES[aStream.readByte()], readBytes(aStream));
//	}
//	
//	public static void readThread(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		aCollector.thread(
//				aStream.readInt(),
//				aStream.readLong(),
//				aStream.readUTF());
//	}
//
//	public static void readRegister(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		long theObjectId = aStream.readLong();
//		long theTimestamp = aStream.readLong();
//		int theDataSize = aStream.readInt();
//		byte[] theData = new byte[theDataSize];
//		aStream.readFully(theData);
//		boolean theIndexable = aStream.readBoolean();
//		aCollector.register(theObjectId, theData, theTimestamp, theIndexable);
//	}
//	
//	public static void readRegisterRef(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		long theObjectId = aStream.readLong();
//		long theTimestamp = aStream.readLong();
//		long theClassId = aStream.readLong();
//		aCollector.registerRefObject(theObjectId, theTimestamp, theClassId);
//	}
//	
//	public static void readRegisterClass(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		long theClassId = aStream.readLong();
//		long theLoaderId = aStream.readLong();
//		String theName = aStream.readUTF();
//		aCollector.registerClass(theClassId, theLoaderId, theName);
//	}
//	
//	public static void readRegisterClassLoader(DataInputStream aStream, ILogCollector aCollector) throws IOException
//	{
//		if (READ_SIZE) aStream.readInt(); // Packet size
//		long theLoaderId = aStream.readLong();
//		long theClassId = aStream.readLong();
//		aCollector.registerClassLoader(theLoaderId, theClassId);
//	}
//	
//	
//	
//    private static byte[] readBytes(DataInputStream aStream) throws IOException
//    {
//        int theLength = aStream.readInt();
//        byte[] theBytes = new byte[theLength];
//        aStream.readFully(theBytes);
//        return theBytes;
//    }
//    
//}
