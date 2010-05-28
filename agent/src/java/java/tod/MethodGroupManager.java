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
package java.tod;

import java.tod.gnu.trove.TByteArrayList;
import java.tod.gnu.trove.THashSet;
import java.tod.gnu.trove.TIntHashSet;
import java.tod.gnu.trove.TIntIterator;
import java.tod.gnu.trove.TIntObjectHashMap;
import java.tod.gnu.trove.TIntProcedure;
import java.tod.gnu.trove.TObjectProcedure;
import java.tod.io._IO;
import java.tod.util._ArrayList;

import tod2.agent.AgentConfig;
import tod2.agent.MonitoringMode;
import tod2.agent.io._ByteBuffer;
import tod2.agent.io._GrowingByteBuffer;

/**
 * Manages method groups. 
 * A method group contains methods that have the same signature and that belong to types that 
 * are related through inheritance.
 * Within a group, methods are either all non-monitored, or all monitored (monitored can mean
 * fully instrumented, or enveloppe instrumented).
 * During the loading of the system, the appearance of some classes or interfaces can cause groups to
 * be merged, in which case their instrumentation kind must be unified. In the agent (see {@link TracedMethods}),
 * all methods start as non-monitored. As groups are marked as monitored or merged, the instrumenter notifies
 * the agent.
 * @author gpothier
 */
public class MethodGroupManager 
{
	private static final MethodGroupManager INSTANCE = new MethodGroupManager();
	
	private static final boolean ECHO = false;

	private static final int itsObjectClassId = AgentConfig.FIRST_CLASS_ID;
	
	private _ArrayList<ClassInfo> itsClasses = new _ArrayList<ClassInfo>();
	private _ArrayList<MethodInfo> itsMethods = new _ArrayList<MethodInfo>();
	
	/**
	 * Maps signatures ids to signature groups. Each signature groups contains method groups.
	 */
	private final _ArrayList<MethodSignatureGroup> itsSignatureGroups = new _ArrayList<MethodSignatureGroup>();
	
	/**
	 * Maps types to their direct subtypes.
	 * Keys and values are class ids.
	 */
	private final IntSetMap itsChildrenMap = new IntSetMap();

	/**
	 * Contains the mode of each behavior
	 */
	private TByteArrayList itsModes = new TByteArrayList();
	
	/**
	 * Stores the current list of mode changes.
	 * The list can be reset at any time.
	 * The format of entries is { behaviorId: int, mode change: byte }
	 */
	private _GrowingByteBuffer itsChanges = _GrowingByteBuffer.allocate(4096);
	
	private MethodGroupManager()
	{
	}

	/**
	 * Sets up the class.
	 * Actually, this does nothing; it is only to ensure that the class
	 * is fully loaded before we start to use it.
	 */
	public static void setup()
	{
		INSTANCE.setup0();
	}
	
	private void setup0()
	{
		_IO.outi("[TOD] MGM Setup");
	}
	
	private ClassInfo getClass(int aId)
	{
		return itsClasses.get(aId);
	}
	
	private MethodInfo getMethod(int aId)
	{
		return itsMethods.get(aId);
	}

	private synchronized void addChange(int aBehaviorId, int aInstrumentationMode, int aCallMode)
	{
		byte theMode = itsModes.get0(aBehaviorId);
		if (ECHO)
		{
			_IO.outi("[MGM] addChange. {bid, Original mode, c.im, c.cm, v}: ", aBehaviorId, theMode, aInstrumentationMode, aCallMode, TracedMethods.version);
		}
		
		if (aInstrumentationMode >= 0) 
			theMode = (byte) ((theMode & ~MonitoringMode.MASK_INSTRUMENTATION) | aInstrumentationMode);
		if (aCallMode >= 0) 
			theMode = (byte) ((theMode & ~MonitoringMode.MASK_CALL) | aCallMode);
		itsModes.set0(aBehaviorId, theMode);
		
		itsChanges.putInt(aBehaviorId);
		itsChanges.put(theMode);
		
		TracedMethods.setMode(aBehaviorId, aInstrumentationMode, aCallMode);
	}
	
	private byte[] getChangesAndReset()
	{
		byte[] theResult = itsChanges.toArray();
		itsChanges.clear();
		return theResult;
	}
	
	private void markMonitored(MethodInfo aBehavior)
	{
		byte theMode = itsModes.get0(aBehavior.id);
		int theInstrumentationMode = theMode & MonitoringMode.MASK_INSTRUMENTATION;
		int theCallMode = theMode & MonitoringMode.MASK_CALL;

		if (ECHO)
		{
			_IO.outi("[MGM] markMonitored {id, old im, old cm}: ", aBehavior.id, theInstrumentationMode, theCallMode);
		}
		
		if (theInstrumentationMode != MonitoringMode.INSTRUMENTATION_NONE)
			throw new RuntimeException("Huh? Should not happen (bid: "+aBehavior.id+")");
		
		theInstrumentationMode = aBehavior.inScope ? MonitoringMode.INSTRUMENTATION_FULL : MonitoringMode.INSTRUMENTATION_ENVELOPPE;
		
		switch(theCallMode)
		{
		case MonitoringMode.CALL_UNMONITORED:
			theCallMode = MonitoringMode.CALL_MONITORED;
			break;
			
		case MonitoringMode.CALL_MONITORED:
		case MonitoringMode.CALL_UNKNOWN:
			theCallMode = -1;
			break;
			
		default: throw new RuntimeException("Not handled: "+theCallMode);
		}

		addChange(aBehavior.id, theInstrumentationMode, theCallMode);
	}
	
	private void markUnknown(MethodInfo aBehavior)
	{
		if (ECHO)
		{
			_IO.outi("[MGM] markUnknown id: ", aBehavior.id);
		}

		addChange(aBehavior.id, -1, MonitoringMode.CALL_UNKNOWN);
	}
	
	private ClassInfo parseClassInfo(int aId, byte[] aClassInfo)
	{
		_ByteBuffer theBuffer = _ByteBuffer.wrap(aClassInfo);
		
		int theSuperId = theBuffer.getInt();

		int theInterfaceCount = theBuffer.getShort();
		int[] theInterfaceIds = new int[theInterfaceCount];
		for(int i=0;i<theInterfaceCount;i++) theInterfaceIds[i] = theBuffer.getInt();
		
		int theMethodCount = theBuffer.getShort();
		MethodInfo[] theMethods = new MethodInfo[theMethodCount];

		ClassInfo theClass = new ClassInfo(aId, theSuperId, theInterfaceIds, theMethods);
		itsClasses.add(aId, theClass);

		for(int i=0;i<theMethodCount;i++)
		{
			int theMethodId = theBuffer.getInt();
			int theSigGroupId = theBuffer.getInt();
			boolean theNativeOrSkipped = theBuffer.getBoolean();
			boolean theClInit = theBuffer.getBoolean();
			boolean theInScope = theBuffer.getBoolean();
			
			MethodInfo theMethod = new MethodInfo(theClass, theMethodId, theSigGroupId, theNativeOrSkipped, theClInit, theInScope);
			theMethods[i] = theMethod;
			itsMethods.add(theMethodId, theMethod);
		}
		
		return theClass;
	}
	
	/**
	 * Updates groups knowing that inheritance information for the given
	 * class may have changed.
	 */
	public static void classLoaded(int aId, byte[] aClassInfo)
	{
		INSTANCE.classLoaded0(aId, aClassInfo);
	}
	
	private void classLoaded0(int aId, byte[] aClassInfo)
	{
		ClassInfo theClass = parseClassInfo(aId, aClassInfo);
		
		if (ECHO) _IO.outi("[MGM] classLoaded: ", theClass != null ? theClass.id : -1);
		
		// add edges
		if (theClass.superId != 0) addEdge(theClass.superId, theClass.id);
		for(int theInterfaceId : theClass.interfaceIds) addEdge(theInterfaceId, theClass.id);

		// Merge affected groups
		final TIntHashSet theHierarchy = getTypeHierarchy(theClass);
		final THashSet<MethodGroup> theGroups = new THashSet<MethodGroup>();
		itsSignatureGroups.forEach(new TObjectProcedure<MethodSignatureGroup>()
		{
			public boolean execute(final MethodSignatureGroup theSignatureGroup)
			{
				if (theSignatureGroup == null) return true;
				theGroups.clear();
				theHierarchy.forEach(new TIntProcedure()
				{
					public boolean execute(int theTypeId)
					{
						MethodGroup theGroup = theSignatureGroup.getGroup(theTypeId);
						if (theGroup != null) theGroups.add(theGroup);
						return true;
					}
				});
				
				theSignatureGroup.merge(theGroups);
				return true;
			}
		});

		for(MethodInfo theMethod : theClass.methods) behaviorLoaded(theClass, theMethod);

		// Send mode changes to server
		EventCollector.INSTANCE.sendModeChanges(getChangesAndReset());
	}

	/**
	 * Calls {@link #merge(MethodSignatureGroup, IClassInfo, IClassInfo)} on each signature group
	 */
	private void addEdge(int aParentId, int aChildId)
	{
		if (ECHO) _IO.outi("[MGM] adding edge: {parent <|- child}: ", aParentId, aChildId);
		itsChildrenMap.add(aParentId, aChildId);
	}
	
	private void behaviorLoaded(ClassInfo aClass, MethodInfo aBehavior)
	{
		if (ECHO) _IO.outi("[MGM] behaviorAdded: ", aBehavior.id);
		MethodSignatureGroup theSignatureGroup = getSignatureGroup(aBehavior.signatureGroupId);
		
		// Check if this method goes into an existing group
		MethodGroup theMethodGroup = findGroup(theSignatureGroup, aClass);
		if (theMethodGroup != null)
		{
			if (ECHO) _IO.outi("[MGM] behaviorAdded - adding to existing group {id}: ", theMethodGroup.getGroupId());
			theMethodGroup.add(aBehavior);
			if (theMethodGroup.isMonitored()) markMonitored(aBehavior);
			if (theMethodGroup.hasUnknown()) markUnknown(aBehavior);
		}
		else
		{
			theMethodGroup = theSignatureGroup.addSingleton(aBehavior);
			if (ECHO) _IO.outi("[MGM] behaviorAdded - created new group {id}: ", theMethodGroup.getGroupId());
		}
		
		// Check if the behavior is in scope
		if (aBehavior.inScope) theMethodGroup.markMonitored();
		if (aBehavior.nativeOrSkipped) theMethodGroup.markUnknown();
		
		// Always mark clinits monitored as they can be executed at any time (we need at least enveloppe messages)
		if (! aBehavior.inScope && ! theMethodGroup.isMonitored() && aBehavior.clInit)
		{
			markMonitored(aBehavior);
		}
	}
	
	
	/**
	 * Finds an existing suitable group for the given type inside a signature group.
	 * it is possible that hierarchy information present in the given type was not available
	 * previously, and thus some groups might need to be merged as a result of executing
	 * this method. 
	 */
	private MethodGroup findGroup(final MethodSignatureGroup aSignatureGroup, ClassInfo aType)
	{
		final THashSet<MethodGroup> theGroups = new THashSet<MethodGroup>();
		TIntHashSet theHierarchy = getTypeHierarchy(aType);
		theHierarchy.forEach(new TIntProcedure()
		{
			public boolean execute(int aTypeId)
			{
				ClassInfo theType = MethodGroupManager.this.getClass(aTypeId);
				if (theType != null)
				{
					MethodGroup theGroup = aSignatureGroup.getGroup(theType.id);
					if (theGroup != null) theGroups.add(theGroup);
				}
				return true;
			}
		});
		
		return aSignatureGroup.merge(theGroups);
	}
	
	/**
	 * Returns all the ancestors and descendants of the given type.
	 */
	private TIntHashSet getTypeHierarchy(ClassInfo aType)
	{
		TIntHashSet theTypes = new TIntHashSet();
		theTypes.add(itsObjectClassId);
		fillAncestors(theTypes, aType);
		fillDescendants(theTypes, aType);
		theTypes.add(aType.id);
		return theTypes;
	}
	
	private void fillAncestors(TIntHashSet aTypes, ClassInfo aType)
	{
		if (aType == null) return;
		
		int theSupertypeId = aType.superId;		
		
		if (theSupertypeId != 0)
		{
			aTypes.add(theSupertypeId);
			fillAncestors(aTypes, getClass(theSupertypeId));
		}
		
		for(int theInterfaceId : aType.interfaceIds)
		{
			aTypes.add(theInterfaceId);
			fillAncestors(aTypes, getClass(theInterfaceId));
		}
	}

	private void fillDescendants(TIntHashSet aTypes, ClassInfo aType)
	{
		if (aType == null) return;

		TIntHashSet theChildren = itsChildrenMap.getSet(aType.id);
		if (theChildren != null) 
		{
			TIntIterator theIterator = theChildren.iterator();
			while(theIterator.hasNext())
			{
				int theChildId = theIterator.next();
				ClassInfo theChild = getClass(theChildId);
				aTypes.add(theChildId);
				fillDescendants(aTypes, theChild);
				fillAncestors(aTypes, theChild);
			}
		}
	}
	
	
	private MethodSignatureGroup getSignatureGroup(int aSignatureGroupId)
	{
		MethodSignatureGroup theGroup = itsSignatureGroups.get(aSignatureGroupId);
		if (theGroup == null)
		{
			theGroup = new MethodSignatureGroup();
			itsSignatureGroups.add(aSignatureGroupId, theGroup);
		}
		return theGroup;
	}

	private int itsNextGroupId = 1;
	private synchronized int nextGroupId()
	{
		return itsNextGroupId++;
	}
	
	/**
	 * Represents a group of methods that have the same signature and belong to related types.
	 * @author gpothier
	 */
	public class MethodGroup
	{
		private final int itsGroupId;

		private final TIntHashSet itsTypeIds = new TIntHashSet();
		private final _ArrayList<MethodInfo> itsBehaviors = new _ArrayList<MethodInfo>(1);
		private boolean itsMonitored = false;
		private boolean itsHasUnknown = false;
		
		public MethodGroup(MethodInfo aInitialBehavior)
		{
			itsGroupId = nextGroupId();
			add(aInitialBehavior);
		}
		
		public int getGroupId()
		{
			return itsGroupId;
		}
		
		public void add(MethodInfo aBehavior)
		{
			itsTypeIds.add(aBehavior.declaringClass.id);
			itsBehaviors.add(aBehavior);
		}
		
		public boolean hasType(int aClassId)
		{
			return itsTypeIds.contains(aClassId);
		}
		
		public _ArrayList<MethodInfo> getBehaviors()
		{
			return itsBehaviors;
		}
		
		public boolean isMonitored()
		{
			return itsMonitored;
		}
		
		public void markMonitored()
		{
			if (itsMonitored) return;
			itsMonitored = true;
			if (ECHO) _IO.outi("[MGM] group.markMonitored: ", getGroupId());
			getBehaviors().forEach(new TObjectProcedure<MethodInfo>()
			{
				public boolean execute(MethodInfo aMethod)
				{
					MethodGroupManager.this.markMonitored(aMethod);
					return true;
				}
			});
		}
		
		public boolean hasUnknown()
		{
			return itsHasUnknown;
		}
		
		public void markUnknown()
		{
			if (itsHasUnknown) return;
			itsHasUnknown = true;
			if (ECHO) _IO.outi("[MGM] group.markUnknown: ", getGroupId());
			
			getBehaviors().forEach(new TObjectProcedure<MethodInfo>()
			{
				public boolean execute(MethodInfo aMethod)
				{
					MethodGroupManager.this.markUnknown(aMethod);
					return true;
				}
			});
		}
	}
	
	/**
	 * A group of methods that have the same signature. Each such group contains a number of method groups.
	 * @author gpothier
	 */
	public class MethodSignatureGroup
	{
		private final _ArrayList<MethodGroup> itsGroups = new _ArrayList<MethodGroup>(1);
		
		/**
		 * Retrieves the method group to which the method of the given type belongs.
		 */
		public MethodGroup getGroup(int aClassId)
		{
			MethodGroup theResult = null;
			for(int i=0;i<itsGroups.size();i++)
			{
				MethodGroup theGroup = itsGroups.get(i); 
				if (theGroup.hasType(aClassId)) 
				{
					if (theResult != null) throw new RuntimeException("Several groups contain the same "+aClassId);
					theResult = theGroup;
				}
			}
			return theResult;
		}
		
		public MethodGroup addSingleton(MethodInfo aBehavior)
		{
			MethodGroup theGroup = new MethodGroup(aBehavior);
			itsGroups.add(theGroup);
			return theGroup;
		}
		
		/**
		 * Merges two method groups. If one of the groups was monitored and the other wasn't,
		 * the methods from the unmonitored group are marked as monitored.
		 * @return The group that contains the result of the merge and that remains in the signature group
		 * (the other group is discarded).
		 */
		public MethodGroup merge(final MethodGroup g1, MethodGroup g2)
		{
			if (ECHO)
			{
				_IO.outi("[MGM] merging groups: ", g1.getGroupId(), g2.getGroupId());
				_IO.outb("[MGM] monitored: ", g1.isMonitored(), g2.isMonitored());
				_IO.outb("[MGM] unknown: ", g1.hasUnknown(), g2.hasUnknown());
			}

			// Merge monitoring
			if (g1.isMonitored()) g2.markMonitored();
			if (g2.isMonitored()) g1.markMonitored();
			
			// Merge unknown
			if (g1.hasUnknown()) g2.markUnknown();
			if (g2.hasUnknown()) g1.markUnknown();
			
			// Merge both groups (into g1)
			
			g2.getBehaviors().forEach(new TObjectProcedure<MethodInfo>()
			{
				public boolean execute(MethodInfo aMethod)
				{
					g1.add(aMethod);
					return true;
				}
			});

			
			itsGroups.remove(g2);
			return g1;
		}
		
		/**
		 * Same as {@link #merge(MethodGroup, MethodGroup)}, but with N groups.
		 */
		public MethodGroup merge(THashSet<MethodGroup> aGroups)
		{
			final MethodGroup[] theResult = new MethodGroup[1];
			aGroups.forEach(new TObjectProcedure<MethodGroup>()
			{
				public boolean execute(MethodGroup aGroup)
				{
					if (theResult[0] == null) theResult[0] = aGroup;
					else theResult[0] = merge(theResult[0], aGroup);
					return true;
				}
			});

			return theResult[0];
		}
	}

	/**
	 * A Map with int keys where the values are sets of ints. 
	 * @author gpothier
	 */
	public static class IntSetMap
	{
		private final TIntObjectHashMap<TIntHashSet> itsMap = new TIntObjectHashMap<TIntHashSet>();
		
		public void add(int aKey, int aValue)
		{
			TIntHashSet theSet = itsMap.get(aKey);
			if (theSet == null)
			{
				theSet = new TIntHashSet();
				itsMap.put(aKey, theSet);
			}
			theSet.add(aValue);
		}
		
		public boolean remove(int aKey, int aValue)
		{
			TIntHashSet theSet = itsMap.get(aKey);
			if (theSet == null) return false;
			else return theSet.remove(aValue);
		}
		
		public TIntHashSet getSet(int aKey)
		{
			return itsMap.get(aKey);
		}
	}
	
	private static class ClassInfo
	{
		public final int id;
		public final int superId;
		public final int[] interfaceIds;
		public final MethodInfo[] methods;
		
		public ClassInfo(int aId, int aSuperId, int[] aInterfaceIds, MethodInfo[] aMethods)
		{
			id = aId;
			superId = aSuperId;
			interfaceIds = aInterfaceIds;
			methods = aMethods;
		}
		
	}
	
	private static class MethodInfo
	{
		public final ClassInfo declaringClass;
		public final int id;
		public final int signatureGroupId;
		public final boolean nativeOrSkipped;
		public final boolean clInit;
		public final boolean inScope;
		
		public MethodInfo(ClassInfo aDeclaringClass, int aId, int aSignatureGroupId, boolean aNativeOrSkipped, boolean aClInit, boolean aInScope)
		{
			declaringClass = aDeclaringClass;
			id = aId;
			signatureGroupId = aSignatureGroupId;
			nativeOrSkipped = aNativeOrSkipped;
			clInit = aClInit;
			inScope = aInScope;
		}
	}
}
