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
package tod.impl.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tod.core.DebugFlags;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.IEventPredicate;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.LocationUtils;
import tod.core.database.browser.ILogBrowser.Query;
import tod.core.database.event.ExternalPointer;
import tod.core.database.event.IArrayWriteEvent;
import tod.core.database.event.IBehaviorCallEvent;
import tod.core.database.event.ICreationEvent;
import tod.core.database.event.IFieldWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.event.INewArrayEvent;
import tod.core.database.event.IWriteEvent;
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMemberInfo;
import tod.core.database.structure.IPrimitiveTypeInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.tools.monitoring.Monitored;
import tod.utils.TODUtils;
import zz.utils.Sieve;

/**
 * Implementation of {@link tod.core.database.browser.IObjectInspector} based 
 * on API only.
 * @author gpothier
 */
public class ObjectInspector implements IObjectInspector
{
	private final ILogBrowser itsLogBrowser;
	
	/**
	 * Cached Uncertain java.lang.Object
	 */
	private final IClassInfo itsUObjectClass;
	
	/**
	 * Cached unknown array class
	 */
	private final IArrayTypeInfo itsUArrayClass;
	
	private ObjectId itsObjectId;
	
	private Delegate itsDelegate;
	
	private Map<IEntryInfo, IEventBrowser> itsBrowsersMap = new HashMap<IEntryInfo, IEventBrowser>();
	private ITypeInfo itsType;
	
	private ICreationEvent itsCreationEvent;
	private boolean itsCreationEventValid = false;
	
	/**
	 * The event relative to which values are reconstituted.
	 */
	private ILogEvent itsReferenceEvent;
	
	private ObjectInspector(ILogBrowser aLogBrowser)
	{
		itsLogBrowser = aLogBrowser;
		itsUObjectClass = aLogBrowser.getStructureDatabase().getClass("java.lang.Object", true).createUncertainClone();
		itsUArrayClass = aLogBrowser.getStructureDatabase().getArrayType(itsUObjectClass, 1);
	}
	
	/**
	 * For non-static inspector
	 */
	public ObjectInspector(ILogBrowser aLogBrowser, ObjectId aObjectId)
	{
		this(aLogBrowser);
		itsObjectId = aObjectId;
	}
	
	/**
	 * For static inspector
	 */
	public ObjectInspector(ILogBrowser aLogBrowser, IClassInfo aClass)
	{
		this(aLogBrowser);
		itsType = aClass;
		itsObjectId = null;
		itsDelegate = new ObjectDelegate();
	}
	
	public ILogBrowser getLogBrowser()
	{
		return itsLogBrowser;
	}

	public ObjectId getObject()
	{
		return itsObjectId;
	}
	
	
	@Monitored
	public ICreationEvent getCreationEvent()
	{
		if (itsObjectId == null) throw new UnsupportedOperationException("This inspector is for static fields");
		if (itsCreationEvent == null) 
		{
			TODUtils.log(1,"[ObjectInspector] Retrieving creation event for object: "+getObject());
			IEventFilter theFilter = itsLogBrowser.createTargetFilter(getObject());
			IEventBrowser theBrowser = itsLogBrowser.createBrowser(theFilter);
			
			// Creation is the first event if it has been captured
			// Check for timestamp because of concurrency & accuracy of timer.
			long theTimestamp = 0;
			while (theBrowser.hasNext())
			{
				ILogEvent theEvent = theBrowser.next();
				if (theTimestamp == 0) theTimestamp = theEvent.getTimestamp();
				else if (theTimestamp != theEvent.getTimestamp()) break;
				
				if (theEvent instanceof ICreationEvent)
				{
					itsCreationEvent = (ICreationEvent) theEvent;
					break;
				}
			}
		}
		return itsCreationEvent;
	}
	
	@Monitored
	public ITypeInfo getType()
	{
		if (itsType == null)
		{
			ICreationEvent theCreationEvent = getCreationEvent();
			
			if (theCreationEvent != null)
			{
				itsType = theCreationEvent.getType();
			}
			else if (DebugFlags.TRY_GUESS_TYPE)
			{
				// Try to guess type
				IEventFilter theFilter = itsLogBrowser.createTargetFilter(itsObjectId);
				IEventBrowser theBrowser = itsLogBrowser.createBrowser(theFilter);
				
				int theTries = 5;
				Sieve<ITypeInfo> theCandidates = new Sieve<ITypeInfo>()
				{
					@Override
					protected long getScore(ITypeInfo aElement)
					{
						return getTypeScore(aElement);
					}
				};
				
				while(theTries > 0 && theBrowser.hasNext())
				{
					ILogEvent theEvent = theBrowser.next();
					if (theEvent instanceof IFieldWriteEvent)
					{
						IFieldWriteEvent theFieldWriteEvent = (IFieldWriteEvent) theEvent;
						theCandidates.add(theFieldWriteEvent.getField().getDeclaringType());
					}
					else if (theEvent instanceof IBehaviorCallEvent)
					{
						IBehaviorCallEvent theBehaviorCallEvent = (IBehaviorCallEvent) theEvent;
						IBehaviorInfo theCalledBehavior = theBehaviorCallEvent.getCalledBehavior();
						IBehaviorInfo theExecutedBehavior = theBehaviorCallEvent.getExecutedBehavior();
						if (theCalledBehavior != null) theCandidates.add(theCalledBehavior.getDeclaringType());
						if (theExecutedBehavior != null) theCandidates.add(theExecutedBehavior.getDeclaringType());
					}
					else if (theEvent instanceof IArrayWriteEvent)
					{
						IArrayWriteEvent theArrayWriteEvent = (IArrayWriteEvent) theEvent;
						theCandidates.add(itsUArrayClass);
						break;
					} 
					else throw new RuntimeException("Not handled: "+theEvent);
					theTries--;
				}
				
				ITypeInfo theType = theCandidates.getBest();
				if (theType != null) itsType = theType.createUncertainClone();
			}
			
			// Note that we cannot give a null
			if (itsType == null) itsType = getLogBrowser().getStructureDatabase().getUnknownClass();
		}
		return itsType;
	}
	
	/**
	 * Returns the score of a type for type guessing
	 */
	private int getTypeScore(ITypeInfo aType)
	{
		if (aType instanceof IPrimitiveTypeInfo)
		{
			throw new RuntimeException("How did that happen?");
		}
		else if (aType instanceof IArrayTypeInfo)
		{
			return Integer.MAX_VALUE;
		}
		else if (aType instanceof IClassInfo)
		{
			IClassInfo theClass = (IClassInfo) aType;
			return getClassDepth(theClass);
		}
		else throw new RuntimeException("Not handled: "+aType);
	}
	
	/**
	 * Returns the depths of the given class in the hierarchy.
	 */
	private int getClassDepth(IClassInfo aClass)
	{
		int theDepth = 0;
		IClassInfo theCurrentClass = aClass;
		while(theCurrentClass != null)
		{
			theDepth++;
			theCurrentClass = theCurrentClass.getSupertype();
		}
		
		return theDepth;
	}

	
	@Monitored
	protected Delegate getDelegate()
	{
		if (itsDelegate == null)
		{
			ITypeInfo theType = getType();
			
			if (theType instanceof IArrayTypeInfo)
			{
				itsDelegate = new ArrayDelegate();
			}
			else if (theType instanceof IClassInfo)
			{
				itsDelegate = new ObjectDelegate();
			}
			else throw new RuntimeException("Not handled: "+theType);
		}
		
		return itsDelegate;
	}
	
	@Monitored
	public List<IEntryInfo> getEntries(int aRangeStart, int aRangeSize)
	{
		Delegate theDelegate = getDelegate();
		return theDelegate != UNAVAILABLE ? theDelegate.getEntries(aRangeStart, aRangeSize) : Collections.EMPTY_LIST;
	}
	
	public int getEntryCount()
	{
		Delegate theDelegate = getDelegate();
		return theDelegate != UNAVAILABLE ? theDelegate.getEntryCount() : 0;
	}

	public void setReferenceEvent(ILogEvent aEvent)
	{
		itsReferenceEvent = aEvent;
	}

	public ILogEvent getReferenceEvent()
	{
		return itsReferenceEvent;
	}
	
	private void checkReferenceEvent()
	{
		if (itsReferenceEvent == null) throw new IllegalStateException("No reference event");
	}
	
	public ILogEvent getCurrentEvent()
	{
		throw new UnsupportedOperationException();
	}

	@Monitored
	public EntryValue[] getEntryValue(IEntryInfo aEntry)
	{
		checkReferenceEvent();
		List<EntryValue> theResult = new ArrayList<EntryValue>();
		
		// Check if the ref event is the setter for this field
		if (itsReferenceEvent instanceof IFieldWriteEvent)
		{
			IFieldWriteEvent theEvent = (IFieldWriteEvent) itsReferenceEvent;
			if (theEvent.getField().equals(aEntry))
			{
				theResult.add(new EntryValue(theEvent.getValue(), theEvent));
				return theResult.toArray(new EntryValue[theResult.size()]);
			}
		}
		
		IEventBrowser theBrowser = getBrowser(aEntry);
		theBrowser.setPreviousEvent(itsReferenceEvent);
		
		long thePreviousTimestamp = -1;
		
		while (theBrowser.hasPrevious())
		{
			ILogEvent theEvent = theBrowser.previous();
			long theTimestamp = theEvent.getTimestamp();
			
			if (thePreviousTimestamp == -1) thePreviousTimestamp = theTimestamp;
			
			if (theTimestamp == thePreviousTimestamp) 
			{
				Object[] theNewValue = getNewValue(aEntry, theEvent);
				for (Object v : theNewValue) theResult.add(new EntryValue(v, theEvent));
			}
			else break;
		}
		
		if (theResult.isEmpty())
		{
			// If there is no result, use default initial value (null or 0).
			theResult.add(new EntryValue(getDelegate().getDefaultInitialValue(aEntry), null));
		}
		
		return theResult.toArray(new EntryValue[theResult.size()]);
	}
	
	@Monitored
	public EntryValue[] nextEntryValue(IEntryInfo aEntry)
	{
		IEventBrowser theBrowser = getBrowser(aEntry);
		EntryValue[] theEntryValue = getEntryValue(aEntry);
		
		List<EntryValue> theResult = new ArrayList<EntryValue>();
		for (EntryValue theValue : theEntryValue)
		{
			if (theValue.getSetter() == null) continue;
			theBrowser.setNextEvent(theValue.getSetter());
			ILogEvent theNext = theBrowser.next();
			assert theNext.equals(theValue.getSetter());
			
			if (! theBrowser.hasNext()) continue;
			theNext = theBrowser.next();
			
			itsReferenceEvent = theNext;
			Object[] theNewValue = getNewValue(aEntry, theNext);
			for (Object v : theNewValue) theResult.add(new EntryValue(v, theNext));
		}
		
		return theResult.isEmpty() ? null : theResult.toArray(new EntryValue[theResult.size()]);
	}

	@Monitored
	public EntryValue[] previousEntryValue(IEntryInfo aEntry)
	{
		IEventBrowser theBrowser = getBrowser(aEntry);
		EntryValue[] theEntryValue = getEntryValue(aEntry);
		
		List<EntryValue> theResult = new ArrayList<EntryValue>();
		for (EntryValue theValue : theEntryValue)
		{
			if (theValue.getSetter() == null) continue;
			theBrowser.setPreviousEvent(theValue.getSetter());
			ILogEvent thePrevious = theBrowser.previous();
			assert thePrevious.equals(theValue.getSetter());
			
			if (! theBrowser.hasPrevious()) continue;
			thePrevious = theBrowser.previous();
			
			itsReferenceEvent = thePrevious;
			Object[] theNewValue = getNewValue(aEntry, thePrevious);
			for (Object v : theNewValue) theResult.add(new EntryValue(v, thePrevious));
		}
		
		return theResult.isEmpty() ? null : theResult.toArray(new EntryValue[theResult.size()]);
	}

	/**
	 * Returns the possible values assigned to the given field by the given event.
	 * @param aEvent Must be an event returned by {@link #getBrowser(IFieldInfo)}.
	 */
	@Monitored
	public Object[] getNewValue(IEntryInfo aEntry, ILogEvent aEvent)
	{
		checkReferenceEvent();
		return getDelegate().getNewValue(aEntry, aEvent);
	}


	/**
	 * Returns an event browser for the specified field.
	 */
	@Monitored
	public IEventBrowser getBrowser(IEntryInfo aEntry)
	{
		IEventBrowser theBrowser = itsBrowsersMap.get(aEntry);
		if (theBrowser == null)
		{
			Delegate theDelegate = getDelegate();
			IEventFilter theFilter = theDelegate != UNAVAILABLE ? theDelegate.getFilter(aEntry) : null;
			theBrowser = itsLogBrowser.createBrowser(theFilter);
			itsBrowsersMap.put (aEntry, theBrowser);
		}
		
		return theBrowser;
	}
	
	/**
	 * The object inspector uses a delegate that permits to abstract away the 
	 * differences between regular objects and arrays.
	 * @author gpothier
	 */
	private static abstract class Delegate
	{
		/**
		 * Delegate method for {@link ObjectInspector#getEntryCount()}
		 */
		public abstract int getEntryCount();

		/**
		 * Delegate method for {@link ObjectInspector#getFields()}
		 */
		public abstract List<IEntryInfo> getEntries(int aRangeStart, int aRangeSize);

		/**
		 * Delegate method for {@link ObjectInspector#getFilter(IMemberInfo)}
		 */
		public abstract IEventFilter getFilter(IEntryInfo aEntry);
		
		/**
		 * Delegate method for {@link ObjectInspector#getNewValue(IFieldInfo, ILogEvent)}
		 */
		public abstract Object[] getNewValue(IEntryInfo aEntry, ILogEvent aEvent);
		
		/**
		 * Returns the default JVM-assigned value for the given entry.
		 */
		public abstract Object getDefaultInitialValue(IEntryInfo aEntry);
	}
	
	private static final Delegate UNAVAILABLE = new Delegate()
	{
		@Override
		public List<IEntryInfo> getEntries(int aRangeStart, int aRangeSize)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getEntryCount()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public IEventFilter getFilter(IEntryInfo aEntry)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object[] getNewValue(IEntryInfo aEntry, ILogEvent aEvent)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Object getDefaultInitialValue(IEntryInfo arg0)
		{
			throw new UnsupportedOperationException();
		}
	};
	
	private class ObjectDelegate extends Delegate
	{
		private List<IEntryInfo> itsFields;
		
		/**
		 * Recursively finds all inherited members of the inspected object.
		 */
		private void fillMembers (List<IEntryInfo> aMembers, ITypeInfo aType)
		{
			if (aType instanceof IClassInfo)
			{
				IClassInfo theClass = (IClassInfo) aType;
				
				for (IFieldInfo theField : theClass.getFields())
				{
					if (theField.isStatic() == (getObject() == null)) aMembers.add(new FieldEntryInfo(theField));
				}

				// Fill supertypes recursively
				ITypeInfo theSupertype = theClass.getSupertype();
				if (theSupertype != null) fillMembers(aMembers, theSupertype);
			}
		}
		
		private void checkFields()
		{
			if (itsFields == null)
			{
				itsFields = new ArrayList<IEntryInfo>();
				fillMembers(itsFields, getType());
			}
		}
		
		@Override
		public int getEntryCount()
		{
			checkFields();
			return itsFields.size();
		}

		@Override
		@Monitored
		public List<IEntryInfo> getEntries(int aRangeStart, int aRangeSize)
		{
			checkFields();
			List<IEntryInfo> theResult = new ArrayList<IEntryInfo>();
			for(int i=aRangeStart;i<Math.min(aRangeStart+aRangeSize, itsFields.size());i++) 
				theResult.add(itsFields.get(i));
			
			return theResult;
		}
		
		@Override
		public IEventFilter getFilter(IEntryInfo aEntry)
		{
			IFieldInfo theField = ((FieldEntryInfo) aEntry).getField();
			
			if (getObject() == null)
			{
				// static version
				return itsLogBrowser.createFieldFilter(theField);
			}
			else
			{
				// Non-static version
				return itsLogBrowser.createIntersectionFilter(
						itsLogBrowser.createFieldFilter(theField),
						itsLogBrowser.createTargetFilter(itsObjectId));
			}
		}
		
		@Override
		public Object[] getNewValue(IEntryInfo aEntry, ILogEvent aEvent)
		{
			IFieldInfo theField = ((FieldEntryInfo) aEntry).getField();

			IFieldWriteEvent theEvent = (IFieldWriteEvent) aEvent;
			if (! theEvent.getField().equals(theField)) 
			{
				throw new IllegalArgumentException("Argument mismatch: "+theField+", "+aEvent);
			}
			else 
			{
				return new Object[] {theEvent.getValue()};
			}
		}

		@Override
		public Object getDefaultInitialValue(IEntryInfo aEntry)
		{
			FieldEntryInfo theEntry = (FieldEntryInfo) aEntry;
			return theEntry.getField().getType().getDefaultInitialValue();
		}
	}

	private static IBehaviorInfo getArrayCopy(IStructureDatabase aStructureDatabase)
	{
		return LocationUtils.getBehavior(
				aStructureDatabase, 
				"java.lang.System", 
				"arraycopy", 
				"(Ljava.lang.Object;ILjava.lang.Object;II)V", 
				false);
	}
	
	private class ArrayDelegate extends Delegate
	{
		/**
		 * A reference to the {@link System#arraycopy(Object, int, Object, int, int)} method
		 */
		private IBehaviorInfo itsArrayCopy;

		public ArrayDelegate()
		{
			itsArrayCopy = getArrayCopy(itsLogBrowser.getStructureDatabase());
		}

		@Override
		public int getEntryCount()
		{
			INewArrayEvent theEvent = (INewArrayEvent) getCreationEvent();
			return theEvent.getArraySize();
		}

		@Override
		@Monitored
		public List<IEntryInfo> getEntries(int aRangeStart, int aRangeSize)
		{
			List<IEntryInfo> theResult = new ArrayList<IEntryInfo>();
			INewArrayEvent theEvent = (INewArrayEvent) getCreationEvent();
			int theSize = theEvent.getArraySize();
				
			for(int i=aRangeStart;i<Math.min(aRangeStart+aRangeSize, theSize);i++)
			{
				theResult.add(new ArraySlotEntryInfo(i));
			}

			return theResult;
		}
		
		@Override
		public IEventFilter getFilter(IEntryInfo aEntry)
		{
			int theIndex = ((ArraySlotEntryInfo) aEntry).getIndex();
			
			IEventFilter theFieldWriteFilter = itsLogBrowser.createIntersectionFilter(
					itsLogBrowser.createArrayWriteFilter(theIndex),
					itsLogBrowser.createTargetFilter(itsObjectId));
			
			if (itsArrayCopy == null)
			{
				// Can be null if the method was never called (by instrumented code)
				return theFieldWriteFilter;
			}
			else
			{
				IEventFilter theCopyFilter = itsLogBrowser.createPredicateFilter(
						new ArrayCopyFilter(theIndex), 
						itsLogBrowser.createIntersectionFilter(
								itsLogBrowser.createBehaviorCallFilter(itsArrayCopy),
								itsLogBrowser.createArgumentFilter(itsObjectId, 3)));
				
				return itsLogBrowser.createUnionFilter(
						theFieldWriteFilter,
						theCopyFilter);
			}
		}

		@Override
		@Monitored
		public Object[] getNewValue(IEntryInfo aEntry, ILogEvent aEvent)
		{
			int theIndex = ((ArraySlotEntryInfo) aEntry).getIndex();
			Object[] theResult;
			
			TODUtils.logf(0, "Retrieving slot %d of array %s", theIndex, itsObjectId);
			
			if (aEvent instanceof IWriteEvent)
			{
				IWriteEvent theWriteEvent = (IWriteEvent) aEvent;
				theResult = new Object[] {theWriteEvent.getValue()};
			}
			else 
			{
				ArraySlotTrack theQuery = new ArraySlotTrack(
						aEvent.getPointer(), 
						theIndex);
				
				theResult = getLogBrowser().exec(theQuery);
//				theResult = theQuery.run(getLogBrowser());
			}
			
			TODUtils.logf(0, "Retrieved slot %d of array %s -> %s", theIndex, itsObjectId, Arrays.asList(theResult));
			
			return theResult;
		}
		
		@Override
		public Object getDefaultInitialValue(IEntryInfo arg0)
		{
			return ((IArrayTypeInfo) getType()).getElementType().getDefaultInitialValue();
		}
	}
	
	/**
	 * This query tracks the value of an array slot, following arraycopy and IO operations
	 * @author gpothier
	 */
	private static class ArraySlotTrack extends Query<Object[]>
	implements Serializable
	{
		private final ExternalPointer itsCallEventPointer;
		private final int itsIndex;

		public ArraySlotTrack(ExternalPointer aCallEventPointer, int aIndex)
		{
			itsCallEventPointer = aCallEventPointer;
			itsIndex = aIndex;
		}

		public Object[] run(ILogBrowser aLogBrowser)
		{
			ILogEvent theEvent = aLogBrowser.getEvent(itsCallEventPointer);
			
			if (theEvent instanceof IBehaviorCallEvent)
			{
				IBehaviorCallEvent theCall = (IBehaviorCallEvent) theEvent;
				IBehaviorInfo theArrayCopy = getArrayCopy(aLogBrowser.getStructureDatabase());
				
				EntryValue[] theEntryValue = null;
				
				if (theArrayCopy.equals(theCall.getCalledBehavior()))
				{
					int theDestPos = (Integer) theCall.getArguments()[3];
					int theLength = (Integer) theCall.getArguments()[4];
					
					// Check if our slot is in the modified window
					assert itsIndex >= theDestPos && itsIndex < theDestPos+theLength;
					
					int theSrcPos = (Integer) theCall.getArguments()[1];
					ObjectId theSource = (ObjectId) theCall.getArguments()[0];
					
					// Reconstitute corresponding value in source array
					IObjectInspector theInspector = aLogBrowser.createObjectInspector(theSource);
					theInspector.setReferenceEvent(theCall);
					
					TODUtils.logf(0, "Looking up slot %d of src array %s", itsIndex-theSrcPos, theSource);
					theEntryValue = theInspector.getEntryValue(new ArraySlotEntryInfo(itsIndex-theDestPos+theSrcPos));
				}
				
				if (theEntryValue == null) return null;
				Object[] theResult = new Object[theEntryValue.length];
				for(int i=0;i<theEntryValue.length;i++) theResult[i] = theEntryValue[i].getValue();

				return theResult;
			}

			throw new IllegalArgumentException("Can't handle: "+theEvent);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result =
					prime * result
							+ ((itsCallEventPointer == null) ? 0 : itsCallEventPointer.hashCode());
			result = prime * result + itsIndex;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final ArraySlotTrack other = (ArraySlotTrack) obj;
			if (itsCallEventPointer == null)
			{
				if (other.itsCallEventPointer != null) return false;
			}
			else if (!itsCallEventPointer.equals(other.itsCallEventPointer)) return false;
			if (itsIndex != other.itsIndex) return false;
			return true;
		}
		
	}
	
	/**
	 * Selects only arraycopy calls that covered a given index.
	 * @author gpothier
	 */
	private static class ArrayCopyFilter implements IEventPredicate, Serializable
	{
		private static final long serialVersionUID = 1458736914058795108L;
		private final int itsIndex;

		public ArrayCopyFilter(int aIndex)
		{
			itsIndex = aIndex;
		}

		public boolean match(ILogEvent aEvent)
		{
			IBehaviorCallEvent theCall = (IBehaviorCallEvent) aEvent;
			int theDestPos = (Integer) theCall.getArguments()[3];
			int theLength = (Integer) theCall.getArguments()[4];
			
			return itsIndex >= theDestPos && itsIndex < theDestPos+theLength;
		}
	}
	
}
