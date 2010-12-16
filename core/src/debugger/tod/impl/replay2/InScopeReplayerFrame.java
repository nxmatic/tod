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
package tod.impl.replay2;

import tod.core.database.structure.ObjectId;
import tod.impl.replay2.ThreadReplayer.ExceptionInfo;
import tod.impl.server.BufferStream.EndOfStreamException;
import tod2.agent.Message;
import zz.utils.Utils;

public class InScopeReplayerFrame 
{
	public static int expectAndSendIntFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		int theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readInt();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static int expectAndSendIntFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, int aCachedValue)
	{
		int theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue =  aReplayer.readInt(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static byte expectAndSendByteFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		byte theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readByte();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static byte expectAndSendIntFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, byte aCachedValue)
	{
		byte theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue =  aReplayer.readByte(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static boolean expectAndSendBooleanFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		boolean theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readBoolean();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue ? 1 : 0);
		
		return theValue;
	}
	
	public static boolean expectAndSendBooleanFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, boolean aCachedValue)
	{
		boolean theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue =  aReplayer.readBoolean(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue ? 1 : 0);
		
		return theValue;
	}
	
	public static char expectAndSendCharFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		char theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readChar();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static char expectAndSendCharFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, char aCachedValue)
	{
		char theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue =  aReplayer.readChar(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static short expectAndSendShortFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		short theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readShort();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static short expectAndSendShortFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, short aCachedValue)
	{
		short theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue =  aReplayer.readShort(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static long expectAndSendLongFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		long theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readLong();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static long expectAndSendLongFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, long aCachedValue)
	{
		long theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue =  aReplayer.readLong(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static float expectAndSendFloatFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		float theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readFloat();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static float expectAndSendFloatFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, float aCachedValue)
	{
		float theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue = aReplayer.readFloat(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static double expectAndSendDoubleFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		double theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readDouble();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static double expectAndSendDoubleFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, double aCachedValue)
	{
		double theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue = aReplayer.readDouble(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static ObjectId expectAndSendRefFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex)
	{
		ObjectId theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.FIELD_READ) theValue = aReplayer.readRef();
		else throw new UnexpectedMessageException(m);
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static ObjectId expectAndSendRefFieldRead(ThreadReplayer aReplayer, ObjectId aTarget, int aFieldSlotIndex, ObjectId aCachedValue)
	{
		ObjectId theValue;
		
		byte m = getNextMessageConsumingClassloading(aReplayer);
		switch(m)
		{
		case Message.FIELD_READ: theValue = aReplayer.readRef(); break;
		case Message.FIELD_READ_SAME: theValue = aCachedValue; break;
		default: throw new UnexpectedMessageException(m);
		}
		
		aReplayer.getCollector().fieldRead(aTarget, aFieldSlotIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static void expectException(ThreadReplayer aReplayer)
	{
		byte m = aReplayer.getNextMessage();
		if (m == Message.EXCEPTION) 
		{
			ExceptionInfo theExceptionInfo = aReplayer.readExceptionInfo();
			ObjectId theException = theExceptionInfo.exception;
			m = aReplayer.getNextMessage();
			switch(m)
			{
			case Message.HANDLER_REACHED: 
				throw new HandlerReachedException(theException, aReplayer.readInt());
				
			case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION:
			case Message.INSCOPE_BEHAVIOR_EXIT_EXCEPTION:
			case Message.CLASSLOADER_EXIT_EXCEPTION:
				throw new BehaviorExitException();
				
			case Message.EXCEPTION:
				// TODO: For now we consider that it means the program was terminated...
				throw new EndOfStreamException();
				
			default: throw new UnexpectedMessageException(m);
			}
		}
		else if (m == Message.HANDLER_REACHED)
		{
			// TODO: For some reason the EXCEPTION message is not always generated, this should be investigated 
			throw new HandlerReachedException(null, aReplayer.readInt());
		}
		else throw new UnexpectedMessageException(m); 
	}
	
	public static void expectException_ClassLoader(ThreadReplayer aReplayer)
	{
		byte m = aReplayer.getNextMessage();
		if (m == Message.EXCEPTION) 
		{
			ExceptionInfo theExceptionInfo = aReplayer.readExceptionInfo();
			ObjectId theException = theExceptionInfo.exception;
			m = aReplayer.getNextMessage();
			switch(m)
			{
			case Message.CLASSLOADER_EXIT_EXCEPTION:
				throw new BehaviorExitException();
				
			default: throw new UnexpectedMessageException(m);
			}
		}
		throw new UnexpectedMessageException(m); 
	}
	
	public static void expectException_OOS(ThreadReplayer aReplayer)
	{
		byte m = aReplayer.getNextMessage();
		if (m == Message.EXCEPTION) 
		{
			ExceptionInfo theExceptionInfo = aReplayer.readExceptionInfo();
			ObjectId theException = theExceptionInfo.exception;
			m = aReplayer.getNextMessage();
			switch(m)
			{
			case Message.OUTOFSCOPE_BEHAVIOR_EXIT_EXCEPTION:
				throw new BehaviorExitException();
				
			default: throw new UnexpectedMessageException(m);
			}
		}
		throw new UnexpectedMessageException(m); 
	}
	
	public static ObjectId expectConstant(ThreadReplayer aReplayer)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.CONSTANT) return aReplayer.readRef();
		else throw new UnexpectedMessageException(m);
	}
	
	public static int expectInstanceofOutcome(ThreadReplayer aReplayer)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.INSTANCEOF_OUTCOME) return aReplayer.readByte();
		else throw new UnexpectedMessageException(m);
	}
	
	public static ObjectId expectNewArray(ThreadReplayer aReplayer)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m == Message.NEW_ARRAY) return aReplayer.readRef();
		else throw new UnexpectedMessageException(m);
	}
	
	public static boolean expectAndSendBooleanArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		boolean theValue = aReplayer.readBoolean();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue ? 1 : 0);
		
		return theValue;
	}

	public static byte expectAndSendByteArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		byte theValue = aReplayer.readByte();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static char expectAndSendCharArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		char theValue = aReplayer.readChar();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static double expectAndSendDoubleArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		double theValue = aReplayer.readDouble();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static float expectAndSendFloatArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		float theValue = aReplayer.readFloat();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static int expectAndSendIntArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		int theValue = aReplayer.readInt();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static long expectAndSendLongArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		long theValue = aReplayer.readLong();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static ObjectId expectAndSendRefArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		ObjectId theValue = aReplayer.readRef();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	public static short expectAndSendShortArrayRead(ThreadReplayer aReplayer, ObjectId aTarget, int aIndex)
	{
		byte m = getNextMessageConsumingClassloading(aReplayer);
		if (m != Message.ARRAY_READ) throw new UnexpectedMessageException(m);
		
		short theValue = aReplayer.readShort();
		
		aReplayer.getCollector().arrayRead(aTarget, aIndex);
		aReplayer.getCollector().value(theValue);
		
		return theValue;
	}
	
	
	public static int expectArrayLength(ThreadReplayer aReplayer)
	{
		byte m = aReplayer.getNextMessage();
		if (m == Message.ARRAY_LENGTH) return aReplayer.readInt();
		else throw new UnexpectedMessageException(m);
	}
	
	/**
	 * Returns normally if the next message is not an exception
	 */
	public static void checkCast(ThreadReplayer aReplayer)
	{
		if (aReplayer.isExceptionNext()) expectException(aReplayer);
	}
	
	/**
	 * Consumes all classloading-related messages (classloader enter, clinit),
	 * and returns the next (non classloading-related) message.
	 * @return
	 */
	public static byte getNextMessageConsumingClassloading(ThreadReplayer aReplayer)
	{
		skipClassloading(aReplayer);
		return aReplayer.getNextMessage();
	}
	

	private static void skipClassloading(ThreadReplayer aReplayer)
	{
		aReplayer.peekNextMessageConsumingClassloading();
	}
	
	
	public static TmpObjectId nextTmpId(ThreadReplayer aReplayer)
	{
		return new TmpObjectId(aReplayer.getTmpIdManager().nextId());
	}
	
	public static TmpObjectId nextTmpId_skipClassloading(ThreadReplayer aReplayer)
	{
		skipClassloading(aReplayer);
		return nextTmpId(aReplayer);
	}
	
	public static void waitObjectInitialized(ThreadReplayer aReplayer, ObjectId aId)
	{
		TmpObjectId theObjectId = (TmpObjectId) aId;
		byte theMessage = aReplayer.getNextMessage();
		if (theMessage != Message.OBJECT_INITIALIZED) throw new UnexpectedMessageException(theMessage);
		
		ObjectId theActualRef = aReplayer.readRef();
		aReplayer.getTmpIdManager().associate(aId.getId(), theActualRef.getId());
		
		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL)
		{
			Utils.println("ObjectInitialized [old: %d, new %d]", aId.getId(), theActualRef.getId());
		}
		
		theObjectId.setId(theActualRef.getId());
	}
	
	public static void waitConstructorTarget(ThreadReplayer aReplayer, ObjectId aId)
	{
		byte theMessage = aReplayer.getNextMessage();
		if (theMessage != Message.CONSTRUCTOR_TARGET) throw new UnexpectedMessageException(theMessage);
		
		ObjectId theActualRef = aReplayer.readRef();
		aReplayer.getTmpIdManager().associate(aId.getId(), theActualRef.getId());

		if (ThreadReplayer.ECHO && ThreadReplayer.ECHO_FORREAL)
		{
			Utils.println("ConstructorTarget [old: %d, new %d]", aId.getId(), theActualRef.getId());
		}

		((TmpObjectId) aId).setId(theActualRef.getId());
	}
	
	/**
	 * Checks that the next message is {@link Message#BEHAVIOR_ENTER_ARGS}. 
	 */
	public static void waitArgs(ThreadReplayer aReplayer)
	{
		byte theMessage = aReplayer.getNextMessage();
		if (theMessage != Message.BEHAVIOR_ENTER_ARGS) throw new UnexpectedMessageException(theMessage);
	}
	
	public static boolean cmpId(ObjectId id1, ObjectId id2)
	{
		if (id1 == null && id2 == null) return true;
		if (id1 == null || id2 == null) return false;
		return id1.getId() == id2.getId();
	}
	
}
