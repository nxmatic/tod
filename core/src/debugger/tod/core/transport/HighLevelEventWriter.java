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

import java.io.DataOutputStream;
import java.io.IOException;

import tod.agent.Output;
import tod.agent.ValueType;
import tod.core.database.structure.ObjectId;

/**
 * Provides the methods used to encode streamed log data. Non-static methods are
 * not thread-safe, but {@link SocketCollector} maintains one
 * {@link HighLevelEventWriter} per thread.
 */
public class HighLevelEventWriter
{
	private DataOutputStream itsBuffer;
	
	public void setReceiver(DataOutputStream aReceiver)
	{
		itsBuffer = aReceiver;
	}
	
	private void sendMessageType(DataOutputStream aStream, HighLevelEventType aType) throws IOException
	{
		aStream.writeByte(aType.ordinal());
	}
	
	private void sendMethodCall(
			HighLevelEventType aMessageType, 
			int aThreadId,
			long aParentTimestamp,
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehavior,
			int aExecutedBehavior, 
			Object aTarget, 
			Object[] aArguments) throws IOException
	{
		sendMessageType(itsBuffer, aMessageType);
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		itsBuffer.writeBoolean(aDirectParent);
		itsBuffer.writeInt(aCalledBehavior);
		itsBuffer.writeInt(aExecutedBehavior);
		sendValue(itsBuffer, aTarget, aTimestamp);
		sendArguments(itsBuffer, aArguments, aTimestamp);

//		itsBuffer.send(aMessageType);
	}

	public void sendMethodCall(
			int aThreadId,
			long aParentTimestamp,
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehavior,
			int aExecutedBehavior,
			Object aTarget,
			Object[] aArguments) throws IOException
	{
		sendMethodCall(
				HighLevelEventType.METHOD_CALL, 
				aThreadId,
				aParentTimestamp,
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aDirectParent, 
				aCalledBehavior,
				aExecutedBehavior,
				aTarget,
				aArguments);
	}

	public void sendInstantiation(
			int aThreadId,
			long aParentTimestamp,
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehavior,
			int aExecutedBehavior,
			Object aTarget,
			Object[] aArguments) throws IOException
	{
		sendMethodCall(
				HighLevelEventType.INSTANTIATION,
				aThreadId,
				aParentTimestamp,
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aDirectParent, 
				aCalledBehavior, 
				aExecutedBehavior, 
				aTarget, 
				aArguments);
	}

	public void sendSuperCall(
			int aThreadId,
			long aParentTimestamp,
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent, 
			int aCalledBehavior,
			int aExecutedBehavior,
			Object aTarget,
			Object[] aArguments) throws IOException
	{
		sendMethodCall(
				HighLevelEventType.SUPER_CALL,
				aThreadId,
				aParentTimestamp,
				aDepth, 
				aTimestamp,
				aAdviceCFlow,
				aProbeId,
				aDirectParent, 
				aCalledBehavior,
				aExecutedBehavior,
				aTarget, 
				aArguments);
	}

	public void sendBehaviorExit(
			int aThreadId,
			long aParentTimestamp, 
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			int aBehaviorId, 
			boolean aHasThrown,
			Object aResult) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.BEHAVIOR_EXIT); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		itsBuffer.writeInt(aBehaviorId);
		itsBuffer.writeBoolean(aHasThrown);
		sendValue(itsBuffer, aResult, aTimestamp);

//		itsBuffer.send(HighLevelEventType.BEHAVIOR_EXIT);
	}

	public void sendFieldWrite(
			int aThreadId,
			long aParentTimestamp,
			int aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			int aFieldLocationId,
			Object aTarget, 
			Object aValue) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.FIELD_WRITE); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		itsBuffer.writeInt(aFieldLocationId);
		sendValue(itsBuffer, aTarget, aTimestamp);
		sendValue(itsBuffer, aValue, aTimestamp);

//		itsBuffer.send(HighLevelEventType.FIELD_WRITE);
	}

	public void sendNewArray(
			int aThreadId, 
			long aParentTimestamp,
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId, 
			Object aTarget,
			int aBaseTypeId,
			int aSize) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.NEW_ARRAY); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		sendValue(itsBuffer, aTarget, aTimestamp);
		itsBuffer.writeInt(aBaseTypeId);
		itsBuffer.writeInt(aSize);

//		itsBuffer.send(HighLevelEventType.NEW_ARRAY);
	}

	public void sendArrayWrite(
			int aThreadId,
			long aParentTimestamp, 
			int aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aTarget,
			int aIndex, 
			Object aValue) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.ARRAY_WRITE); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		sendValue(itsBuffer, aTarget, aTimestamp);
		itsBuffer.writeInt(aIndex);
		sendValue(itsBuffer, aValue, aTimestamp);

//		itsBuffer.send(HighLevelEventType.ARRAY_WRITE);
	}

	public void sendLocalWrite(
			int aThreadId,
			long aParentTimestamp,
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId, 
			int aVariableId,
			Object aValue) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.LOCAL_VARIABLE_WRITE); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		itsBuffer.writeInt(aVariableId);
		sendValue(itsBuffer, aValue, aTimestamp);

//		itsBuffer.send(HighLevelEventType.LOCAL_VARIABLE_WRITE);
	}

	public void sendInstanceOf(
			int aThreadId,
			long aParentTimestamp, 
			int aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aObject,
			int aTypeId,
			boolean aResult) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.INSTANCEOF); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		sendValue(itsBuffer, aObject, aTimestamp);
		itsBuffer.writeInt(aTypeId);
		itsBuffer.writeBoolean(aResult);

//		itsBuffer.send(HighLevelEventType.INSTANCEOF);
	}

	public void sendException(
			int aThreadId,
			long aParentTimestamp,
			int aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			String aMethodName,
			String aMethodSignature, 
			String aMethodDeclaringClassSignature, 
			int aOperationBytecodeIndex,
			Object aException) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.EXCEPTION_BYNAME); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeUTF(aMethodName);
		itsBuffer.writeUTF(aMethodSignature);
		itsBuffer.writeUTF(aMethodDeclaringClassSignature);
		itsBuffer.writeShort(aOperationBytecodeIndex);
		sendValue(itsBuffer, aException, aTimestamp);

//		itsBuffer.send(HighLevelEventType.EXCEPTION);
	}

	public void sendException(
			int aThreadId,
			long aParentTimestamp,
			int aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aBehaviorId, 
			Object aException) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.EXCEPTION_BYID); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aBehaviorId);
		sendValue(itsBuffer, aException, aTimestamp);

//		itsBuffer.send(HighLevelEventType.EXCEPTION);
	}

	public void sendOutput(
			int aThreadId,
			long aParentTimestamp,
			int aDepth, 
			long aTimestamp,
			int[] aAdviceCFlow,
			Output aOutput,
			byte[] aData) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.OUTPUT); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeByte((byte) aOutput.ordinal());
		itsBuffer.writeInt(aData.length);
		itsBuffer.write(aData);

//		itsBuffer.send(HighLevelEventType.OUTPUT);
	}

	public void sendInstanceOf(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aObject, 
			int aTypeId,
			boolean aResult) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.INSTANCEOF); 
		sendStd(itsBuffer, aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow);
		itsBuffer.writeInt(aProbeId);
		sendValue(itsBuffer, aObject, aTimestamp);
		itsBuffer.writeInt(aTypeId);
		itsBuffer.writeBoolean(aResult);
		
//		itsBuffer.send(HighLevelEventType.INSTANCEOF);
	}

	public void sendRegister(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.REGISTER_OBJECT); 
		itsBuffer.writeLong(aObjectUID);
		itsBuffer.writeLong(aTimestamp);
		itsBuffer.writeInt(aData.length); // Send data length because we don't actually write packet size
		itsBuffer.write(aData);
		itsBuffer.writeBoolean(aIndexable);
		
//		itsBuffer.send(HighLevelEventType.REGISTER_OBJECT);
	}
	
	public void sendRegisterRefObject(long aId, long aTimestamp, long aClassId) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.REGISTER_REFOBJECT); 
		itsBuffer.writeLong(aId);
		itsBuffer.writeLong(aTimestamp);
		itsBuffer.writeLong(aClassId);
	}

	public void sendRegisterClass(long aId, long aLoaderId, String aName) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.REGISTER_CLASS); 
		itsBuffer.writeLong(aId);
		itsBuffer.writeLong(aLoaderId);
		itsBuffer.writeUTF(aName);
	}

	public void sendRegisterClassLoader(long aId, long aClassId) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.REGISTER_CLASSLOADER); 
		itsBuffer.writeLong(aId);
		itsBuffer.writeLong(aClassId);
	}
	


	public void sendThread(
			int aThreadId, 
			long aJVMThreadId,
			String aName) throws IOException
	{
		sendMessageType(itsBuffer, HighLevelEventType.REGISTER_THREAD); 
		itsBuffer.writeInt(aThreadId);
		itsBuffer.writeLong(aJVMThreadId);
		itsBuffer.writeUTF(aName);

//		itsBuffer.send(HighLevelEventType.REGISTER_THREAD);
	}

	private void sendStd(
			DataOutputStream aStream,
			int aThreadId, 
			long aParentTimestamp,
			int aDepth,
			long aTimestamp,
			int[] aAdviceCFlow) throws IOException
	{
		aStream.writeInt(aThreadId);
		aStream.writeLong(aParentTimestamp);
		aStream.writeShort(aDepth);
		aStream.writeLong(aTimestamp);
		aStream.writeByte(aAdviceCFlow != null ? aAdviceCFlow.length : 0);
		if (aAdviceCFlow != null) for (int theSrcId : aAdviceCFlow) aStream.writeShort(theSrcId);
	}

	/**
	 * Sends an argument to the socket. This method handles arrays, single
	 * objects or null values.
	 */
	private void sendArguments(
			DataOutputStream aStream, 
			Object[] aArguments, 
			long aTimestamp) throws IOException
	{
		aStream.writeInt(aArguments != null ? aArguments.length : 0);

		if (aArguments != null) for (Object theArgument : aArguments)
			sendValue(aStream, theArgument, aTimestamp);
	}

	private void sendValue(DataOutputStream aStream, Object aValue, long aTimestamp) throws IOException
	{
		sendValue(aStream, aValue, aTimestamp, -1);
	}

	private void sendValue(DataOutputStream aStream, Object aValue, long aTimestamp, long aDefer) throws IOException
	{
		if (aValue == null)
		{
			sendValueType(aStream, ValueType.NULL);
		}
		else if (aValue instanceof Boolean)
		{
			Boolean theBoolean = (Boolean) aValue;
			sendValueType(aStream, ValueType.BOOLEAN);
			aStream.writeByte(theBoolean.booleanValue() ? 1 : 0);
		}
		else if (aValue instanceof Byte)
		{
			Byte theByte = (Byte) aValue;
			sendValueType(aStream, ValueType.BYTE);
			aStream.writeByte(theByte.byteValue());
		}
		else if (aValue instanceof Character)
		{
			Character theCharacter = (Character) aValue;
			sendValueType(aStream, ValueType.CHAR);
			aStream.writeChar(theCharacter.charValue());
		}
		else if (aValue instanceof Integer)
		{
			Integer theInteger = (Integer) aValue;
			sendValueType(aStream, ValueType.INT);
			aStream.writeInt(theInteger.intValue());
		}
		else if (aValue instanceof Long)
		{
			Long theLong = (Long) aValue;
			sendValueType(aStream, ValueType.LONG);
			aStream.writeLong(theLong.longValue());
		}
		else if (aValue instanceof Float)
		{
			Float theFloat = (Float) aValue;
			sendValueType(aStream, ValueType.FLOAT);
			aStream.writeFloat(theFloat.floatValue());
		}
		else if (aValue instanceof Double)
		{
			Double theDouble = (Double) aValue;
			sendValueType(aStream, ValueType.DOUBLE);
			aStream.writeDouble(theDouble.doubleValue());
		}
		else if (aValue instanceof ObjectId)
		{
			ObjectId theId = (ObjectId) aValue;
			sendValueType(aStream, ValueType.OBJECT_UID);
			aStream.writeLong(theId.getId());
		}
		else throw new IllegalArgumentException(""+aValue); 
	}

	private static void sendValueType(DataOutputStream aStream, ValueType aType) throws IOException
	{
		aStream.writeByte(aType.ordinal());
	}

//	/**
//	 * Per-thread byte buffer for preparing packets.
//	 * 
//	 * @author gpothier
//	 */
//	private class MyBuffer extends DataOutputStream
//	{
//		public MyBuffer()
//		{
//			super(new ByteArrayOutputStream());
//		}
//		
//		public void send(HighLevelEventType aMessageType) throws IOException
//		{
//			send((byte) aMessageType.ordinal());
//		}
//		
//		private void send(byte aMessageType) throws IOException
//		{
//			flush();
//			ByteArrayOutputStream theByteOut = (ByteArrayOutputStream) out;
//			itsReceiver.receive(aMessageType, theByteOut);
//			theByteOut.reset();
//		}
//	}
//
//	/**
//	 * A class that receives event buffers.
//	 * @author gpothier
//	 */
//	public abstract class BufferReceiver
//	{
//		public abstract void receive(byte aMessageType, ByteArrayOutputStream aBuffer) throws IOException;
//	}
}
