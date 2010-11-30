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

import gnu.trove.TByteArrayList;
import gnu.trove.TIntArrayList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tod.core.database.structure.ObjectId;

/**
 * An event collector that reifies the event into objects that are placed in a list.
 * @author gpothier
 */
public class ReifyEventCollector extends EventCollector
{
	private EventList itsEventList;
	
	public ReifyEventCollector(int aThreadId, long aBlockId)
	{
		this(new EventList(aThreadId, aBlockId));
	}

	public ReifyEventCollector(EventList aEventList)
	{
		itsEventList = aEventList;
	}

	public EventList getEventList()
	{
		return itsEventList;
	}
	
	@Override
	public void fieldRead(ObjectId aTarget, int aFieldSlotIndex)
	{
		itsEventList.startEvent(EventList.FieldReadEvent.TYPE);
		itsEventList.put(aTarget);
		itsEventList.put(aFieldSlotIndex);
	}

	@Override
	public void fieldWrite(ObjectId aTarget, int aFieldSlotIndex)
	{
		itsEventList.startEvent(EventList.FieldWriteEvent.TYPE);
		itsEventList.put(aTarget);
		itsEventList.put(aFieldSlotIndex);
	}

	@Override
	public void arrayRead(ObjectId aTarget, int aIndex)
	{
		itsEventList.startEvent(EventList.ArrayReadEvent.TYPE);
		itsEventList.put(aTarget);
		itsEventList.put(aIndex);
	}
	
	@Override
	public void arrayWrite(ObjectId aTarget, int aIndex)
	{
		itsEventList.startEvent(EventList.ArrayWriteEvent.TYPE);
		itsEventList.put(aTarget);
		itsEventList.put(aIndex);
	}
	
	@Override
	public void localWrite(int aSlot)
	{
		itsEventList.startEvent(EventList.LocalWriteEvent.TYPE);
		itsEventList.put(aSlot);
	}
	
	@Override
	public void sync(long aTimestamp)
	{
		itsEventList.startEvent(EventList.SyncEvent.TYPE);
	}

	@Override
	public void value(ObjectId aValue)
	{
		itsEventList.put(aValue);
	}

	@Override
	public void value(int aValue)
	{
		itsEventList.put(aValue);
	}

	@Override
	public void value(long aValue)
	{
		itsEventList.put(aValue);
	}

	@Override
	public void value(float aValue)
	{
		itsEventList.put(aValue);
	}

	@Override
	public void value(double aValue)
	{
		itsEventList.put(aValue);
	}

	@Override
	public void enter(int aBehaviorId, int aArgsCount)
	{
		itsEventList.startEvent(EventList.BehaviorCallEvent.TYPE);
		itsEventList.put(aArgsCount);
	}

	@Override
	public void exit()
	{
		itsEventList.startEvent(EventList.BehaviorReturnEvent.TYPE);
	}

	@Override
	public void exitException()
	{
		itsEventList.startEvent(EventList.BehaviorThrowEvent.TYPE);
	}

	
	public static class EventList
	{
		private final int itsThreadId;
		private final long itsBlockId;
		
		private TByteArrayList itsEventTypes = new TByteArrayList(16384);
		private TIntArrayList itsDataOffsets = new TIntArrayList(16384);
		private ByteArrayOutputStream itsOut = new ByteArrayOutputStream(16384);
		private DataOutputStream itsDataOut = new DataOutputStream(itsOut);
		private byte[] itsDataBuffer;
		
		public EventList(int aThreadId, long aBlockId)
		{
			itsThreadId = aThreadId;
			itsBlockId = aBlockId;
		}

		public int getThreadId()
		{
			return itsThreadId;
		}
		
		public long getBlockId()
		{
			return itsBlockId;
		}
		
		private void startEvent(byte aType)
		{
			itsEventTypes.add(aType);
			itsDataOffsets.add(itsOut.size());
			itsDataBuffer = null;
		}
		
		public int size()
		{
			return itsEventTypes.size();
		}
		
		public boolean isCFlowEvent(int aIndex)
		{
			byte theType = getEventType(aIndex);
			switch(theType)
			{
			case BehaviorCallEvent.TYPE:
			case BehaviorReturnEvent.TYPE:
			case BehaviorThrowEvent.TYPE:
				return true;
				
			default:
				return false;
			}
		}
		
		public byte getEventType(int aIndex)
		{
			return itsEventTypes.get(aIndex);
		}
		
		private void put(byte aValue)
		{
			try
			{
				itsDataOut.writeByte(aValue);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		private void put(int aValue)
		{
			try
			{
				itsDataOut.writeInt(aValue);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		private void put(long aValue)
		{
			try
			{
				itsDataOut.writeLong(aValue);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		private void put(float aValue)
		{
			try
			{
				itsDataOut.writeFloat(aValue);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		private void put(double aValue)
		{
			try
			{
				itsDataOut.writeDouble(aValue);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		private void put(ObjectId aObjectId)
		{
			put(aObjectId != null ? aObjectId.getId() : 0L);
		}
		
		private static ObjectId getObjectId(DataInputStream aStream) throws IOException
		{
			long theId = aStream.readLong();
			return theId != 0 ? new ObjectId(theId) : null;
		}
		
		public Event getEvent(int aPosition)
		{
			try
			{
				int theOffset = itsDataOffsets.get(aPosition);
				if (itsDataBuffer == null) itsDataBuffer = itsOut.toByteArray();
				DataInputStream theData = new DataInputStream(new ByteArrayInputStream(itsDataBuffer));
				while(theOffset > 0) theOffset -= theData.skipBytes(theOffset);
				
				switch(getEventType(aPosition))
				{
				case FieldWriteEvent.TYPE:
					return new FieldWriteEvent(getObjectId(theData), theData.readInt());
				case FieldReadEvent.TYPE:
					return new FieldReadEvent(getObjectId(theData), theData.readInt());
				default:
					throw new RuntimeException("Not handled: "+getEventType(aPosition));
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		public static abstract class Event
		{
			/**
			 * Returns true for method call and return events.
			 */
			public boolean isCFlowEvent()
			{
				return false;
			}
		}
		
		private static abstract class ValuedEvent extends Event
		{
			private Object itsValue;

			void setValue(Object aValue)
			{
				itsValue = aValue;
			}
		}
		
		public static abstract class FieldEvent extends ValuedEvent
		{
			private final ObjectId itsObjectId;
			private final int itsFieldId;
			
			public FieldEvent(ObjectId aObjectId, int aFieldId)
			{
				itsObjectId = aObjectId;
				itsFieldId = aFieldId;
			}
			
			public ObjectId getObjectId()
			{
				return itsObjectId;
			}
			
			public int getFieldId()
			{
				return itsFieldId;
			}
		}

		public static class FieldWriteEvent extends FieldEvent
		{
			public static final byte TYPE = 1;
			
			public FieldWriteEvent(ObjectId aObjectId, int aFieldId)
			{
				super(aObjectId, aFieldId);
			}
		}

		public static class FieldReadEvent extends FieldEvent
		{
			public static final byte TYPE = 2;

			public FieldReadEvent(ObjectId aObjectId, int aFieldId)
			{
				super(aObjectId, aFieldId);
			}
		}
		
		public static abstract class ArrayEvent extends ValuedEvent
		{
			private final ObjectId itsObjectId;
			private final int itsIndex;
			
			public ArrayEvent(ObjectId aObjectId, int aIndex)
			{
				itsObjectId = aObjectId;
				itsIndex = aIndex;
			}
			
			public ObjectId getObjectId()
			{
				return itsObjectId;
			}

			public int getIndex()
			{
				return itsIndex;
			}
		}
		
		public static class ArrayWriteEvent extends ArrayEvent
		{
			public static final byte TYPE = 3;

			public ArrayWriteEvent(ObjectId aObjectId, int aFieldId)
			{
				super(aObjectId, aFieldId);
			}
		}
		
		public static class ArrayReadEvent extends ArrayEvent
		{
			public static final byte TYPE = 4;

			public ArrayReadEvent(ObjectId aObjectId, int aFieldId)
			{
				super(aObjectId, aFieldId);
			}
		}
		
		public static class LocalWriteEvent extends ValuedEvent
		{
			public static final byte TYPE = 5;

			private final int itsSlot;

			public LocalWriteEvent(int aSlot)
			{
				itsSlot = aSlot;
			}

			public int getSlot()
			{
				return itsSlot;
			}
		}
		
		public static class BehaviorCallEvent extends Event
		{
			public static final byte TYPE = 6;
			
			private final int itsBehavioId;
			private final Object[] itsArguments;
			private int itsArgIndex;
			private List<Event> itsChildren;
			
			public BehaviorCallEvent(int aBehavioId, int aArgsCount)
			{
				itsBehavioId = aBehavioId;
				itsArguments = aArgsCount != 0 ? new Object[aArgsCount] : null;
			}
			
			@Override
			public boolean isCFlowEvent()
			{
				return true;
			}
			
			public int getBehavioId()
			{
				return itsBehavioId;
			}
			
			public void addArg(Object aValue)
			{
				itsArguments[itsArgIndex++] = aValue;
			}
			
			public void addChild(Event aEvent)
			{
				if (itsChildren == null) itsChildren = new ArrayList<Event>();
				itsChildren.add(aEvent);
			}
		}
		
		public static class BehaviorReturnEvent extends ValuedEvent
		{
			public static final byte TYPE = 7;
			
			public BehaviorReturnEvent()
			{
				super();
			}

			@Override
			public boolean isCFlowEvent()
			{
				return true;
			}
		}
		
		public static class BehaviorThrowEvent extends ValuedEvent
		{
			public static final byte TYPE = 8;

			public BehaviorThrowEvent()
			{
				super();
			}

			@Override
			public boolean isCFlowEvent()
			{
				return true;
			}
		}
		
		public static class SyncEvent extends Event
		{
			public static final byte TYPE = 9;

			private final long itsTimestamp;

			public SyncEvent(long aTimestamp)
			{
				itsTimestamp = aTimestamp;
			}
			
			public long getTimestamp()
			{
				return itsTimestamp;
			}
		}

	}
}
