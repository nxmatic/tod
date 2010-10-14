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

import java.util.ArrayList;
import java.util.List;

import tod.core.database.structure.ObjectId;

/**
 * An event collector that reifies the event into objects that are placed in a list.
 * @author gpothier
 */
public class ReifyEventCollector extends EventCollector
{
	private List<Event> itsEvents = new ArrayList<Event>();
	private ValuedEvent itsCurrentValuedEvent;
	private BehaviorCallEvent itsCurrentCallEvent;
	private int itsRemainingArgs;

	public List<Event> getEvents()
	{
		return itsEvents;
	}
	
	private void addEvent(Event aEvent)
	{
		itsEvents.add(aEvent);
		if (itsCurrentCallEvent != null) itsCurrentCallEvent.addChild(aEvent);
		if (itsEvents.size() % 100000 == 0) System.out.println(itsEvents.size());
	}
	
	@Override
	public void fieldRead(ObjectId aTarget, int aFieldId)
	{
		FieldReadEvent theEvent = new FieldReadEvent(aTarget, aFieldId);
		addEvent(theEvent);
		itsCurrentValuedEvent = theEvent;
	}

	@Override
	public void fieldWrite(ObjectId aTarget, int aFieldId)
	{
		FieldWriteEvent theEvent = new FieldWriteEvent(aTarget, aFieldId);
		addEvent(theEvent);
		itsCurrentValuedEvent = theEvent;
	}

	@Override
	public void arrayRead(ObjectId aTarget, int aIndex)
	{
		ArrayReadEvent theEvent = new ArrayReadEvent(aTarget, aIndex);
		addEvent(theEvent);
		itsCurrentValuedEvent = theEvent;
	}
	
	@Override
	public void arrayWrite(ObjectId aTarget, int aIndex)
	{
		ArrayWriteEvent theEvent = new ArrayWriteEvent(aTarget, aIndex);
		addEvent(theEvent);
		itsCurrentValuedEvent = theEvent;
	}
	
	@Override
	public void sync(long aTimestamp)
	{
		addEvent(new SyncEvent(aTimestamp));
	}

	private void storeValue(Object aValue)
	{
		if (itsRemainingArgs > 0)
		{
			itsCurrentCallEvent.addArg(aValue);
			itsRemainingArgs--;
		}
		else
		{
			itsCurrentValuedEvent.setValue(aValue);
			itsCurrentValuedEvent = null;		
		}
	}
	
	@Override
	public void value(ObjectId aValue)
	{
		storeValue(aValue);
	}

	@Override
	public void value(int aValue)
	{
		storeValue(aValue);
	}

	@Override
	public void value(long aValue)
	{
		storeValue(aValue);
	}

	@Override
	public void value(float aValue)
	{
		storeValue(aValue);
	}

	@Override
	public void value(double aValue)
	{
		storeValue(aValue);
	}

	@Override
	public void enter(int aBehaviorId, int aArgsCount)
	{
		BehaviorCallEvent theEvent = new BehaviorCallEvent(itsCurrentCallEvent, aBehaviorId, aArgsCount);
		addEvent(theEvent);
		itsCurrentCallEvent = theEvent;
		itsRemainingArgs = aArgsCount;
	}

	@Override
	public void exit()
	{
		BehaviorReturnEvent theEvent = new BehaviorReturnEvent();
		addEvent(theEvent);
		if (itsCurrentCallEvent != null) itsCurrentCallEvent = itsCurrentCallEvent.getParent();
	}

	@Override
	public void exitException()
	{
		BehaviorThrowEvent theEvent = new BehaviorThrowEvent();
		addEvent(theEvent);
		if (itsCurrentCallEvent != null) itsCurrentCallEvent = itsCurrentCallEvent.getParent();
	}

	public static abstract class Event
	{
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
		public FieldWriteEvent(ObjectId aObjectId, int aFieldId)
		{
			super(aObjectId, aFieldId);
		}
	}

	public static class FieldReadEvent extends FieldEvent
	{
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
		public ArrayWriteEvent(ObjectId aObjectId, int aFieldId)
		{
			super(aObjectId, aFieldId);
		}
	}
	
	public static class ArrayReadEvent extends ArrayEvent
	{
		public ArrayReadEvent(ObjectId aObjectId, int aFieldId)
		{
			super(aObjectId, aFieldId);
		}
	}
	
	public static class LocalWriteEvent extends ValuedEvent
	{
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
		private final BehaviorCallEvent itsParent;
		private final int itsBehavioId;
		private final Object[] itsArguments;
		private int itsArgIndex;
		private List<Event> itsChildren;
		
		public BehaviorCallEvent(BehaviorCallEvent aParent, int aBehavioId, int aArgsCount)
		{
			itsParent = aParent;
			itsBehavioId = aBehavioId;
			itsArguments = aArgsCount != 0 ? new Object[aArgsCount] : null;
		}
		
		public BehaviorCallEvent getParent()
		{
			return itsParent;
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
		public BehaviorReturnEvent()
		{
			super();
		}
	}
	
	public static class BehaviorThrowEvent extends ValuedEvent
	{
		public BehaviorThrowEvent()
		{
			super();
		}
	}
	
	public static class SyncEvent extends Event
	{
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
