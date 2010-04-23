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
package tod.impl.database.structure.standard;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;

import java.io.Serializable;
import java.tod.TracedMethods;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IStructureDatabase.BehaviorMonitoringModeChange;
import tod2.agent.MonitoringMode;
import zz.utils.Utils;
import zz.utils.primitive.ByteArray;

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
public class MethodGroupManager implements IStructureDatabase.Listener, Serializable
{
	private static final long serialVersionUID = 173278295020871234L;
	
	private static final boolean ECHO = false;

	private transient StructureDatabase itsStructureDatabase;
	
	/**
	 * Maps signatures to signature groups. Each signature groups contains method groups.
	 */
	private final Map<String, MethodSignatureGroup> itsSignatureGroups = new HashMap<String, MethodSignatureGroup>();
	
	/**
	 * Maps types to their direct subtypes.
	 * Keys and values are class ids.
	 */
	private final IntSetMap itsChildrenMap = new IntSetMap();

	/**
	 * Keeps track of all mode changes.
	 * This is used in conjunction with {@link TracedMethods} versioning.
	 */
	private List<BehaviorMonitoringModeChange> itsChanges = new ArrayList<BehaviorMonitoringModeChange>();
	
	/**
	 * Contains the mode of each behavior
	 */
	private ByteArray itsModes = new ByteArray();

	public MethodGroupManager(StructureDatabase aStructureDatabase)
	{
		itsStructureDatabase = aStructureDatabase;
		itsStructureDatabase.addListener(this);
	}
	
	public void setDatabase(StructureDatabase aStructureDatabase)
	{
		itsStructureDatabase = aStructureDatabase;
	}
	
	public BehaviorMonitoringModeChange getChange(int aVersion)
	{
		return itsChanges.get(aVersion);
	}
	
	public void clearChanges()
	{
		itsChanges = new ArrayList<BehaviorMonitoringModeChange>();
	}
	
	public void addChange(BehaviorMonitoringModeChange aChange)
	{
		byte theMode = itsModes.get(aChange.behaviorId);
		if (ECHO)
		{
			Utils.println("[MGM] addChange. Original mode: %d, c.im: %d, c.cm: %d", theMode, aChange.instrumentationMode, aChange.callMode);
		}
		
		if (aChange.instrumentationMode >= 0) 
			theMode = (byte) ((theMode & ~MonitoringMode.MASK_INSTRUMENTATION) | aChange.instrumentationMode);
		if (aChange.callMode >= 0) 
			theMode = (byte) ((theMode & ~MonitoringMode.MASK_CALL) | aChange.callMode);
		itsModes.set(aChange.behaviorId, theMode);
		
		itsChanges.add(aChange);
	}
	
	private boolean isInScope(IBehaviorInfo aBehavior)
	{
//		if (aBehavior.isAbstract() || aBehavior.isNative()) return false;
		return itsStructureDatabase.isInScope(aBehavior.getDeclaringType().getName());
	}
	
	private String getSignature(IBehaviorInfo aBehavior)
	{
		String theName = aBehavior.getName();
		if ("<init>".equals(theName)) theName = "<init_"+ClassInfo.getTypeChars(aBehavior.getDeclaringType())+">";
		if ("<clinit>".equals(theName)) theName = "<clinit_"+ClassInfo.getTypeChars(aBehavior.getDeclaringType())+">";
		
		StringBuilder theBuilder = new StringBuilder(theName);
		theBuilder.append('|');
		for(ITypeInfo theType : aBehavior.getArgumentTypes()) theBuilder.append(ClassInfo.getTypeChars(theType));
		return theBuilder.toString();
	}
	
	private void markMonitored(IBehaviorInfo aBehavior)
	{
		byte theMode = itsModes.get(aBehavior.getId());
		int theInstrumentationMode = theMode & MonitoringMode.MASK_INSTRUMENTATION;
		int theCallMode = theMode & MonitoringMode.MASK_CALL;

		if (ECHO)
		{
			Utils.println("[MGM] markMonitored id: %d, old im: %d, old cm: %d", aBehavior.getId(), theInstrumentationMode, theCallMode);
		}
		
		if (theInstrumentationMode != MonitoringMode.INSTRUMENTATION_NONE)
			throw new RuntimeException("Huh? Should not happen (bid: "+aBehavior.getId()+")");
		
		theInstrumentationMode = isInScope(aBehavior) ? 
				MonitoringMode.INSTRUMENTATION_FULL 
				: MonitoringMode.INSTRUMENTATION_ENVELOPPE;
		
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

		BehaviorMonitoringModeChange theChange = new BehaviorMonitoringModeChange(
				aBehavior.getId(),
				theInstrumentationMode,
				theCallMode);
		
		addChange(theChange);
		itsStructureDatabase.fireMonitoringModeChanged(theChange);
	}
	
	private void markUnknown(IBehaviorInfo aBehavior)
	{
		if (ECHO)
		{
			Utils.println("[MGM] markUnknown id: %d", aBehavior.getId());
		}

		BehaviorMonitoringModeChange theChange = new BehaviorMonitoringModeChange(
				aBehavior.getId(),
				-1,
				MonitoringMode.CALL_UNKNOWN);
		
		addChange(theChange);
		itsStructureDatabase.fireMonitoringModeChanged(theChange);
	}
	
	public void classAdded(IClassInfo aClass)
	{
		classChanged(aClass);
	}

	/**
	 * Updates groups knowing that inheritance information for the given
	 * class may have changed.
	 */
	public void classChanged(IClassInfo aClass)
	{
		if (ECHO) Utils.println("[MGM] classChanged: %s", aClass);
		
		// add edges
		if (aClass.getSupertype() != null) addEdge(aClass.getSupertype(), aClass);
		
		if (aClass.getInterfaces() != null) 
			for(IClassInfo theInterface : aClass.getInterfaces()) addEdge(theInterface, aClass);

		// Merge affected groups
		Collection<IClassInfo> theHierarchy = getTypeHierarchy(aClass);

		Set<MethodGroup> theGroups = new HashSet<MethodGroup>();
		for(MethodSignatureGroup theSignatureGroup : itsSignatureGroups.values()) 
		{
			theGroups.clear();
			for (IClassInfo theType : theHierarchy)
			{
				MethodGroup theGroup = theSignatureGroup.getGroup(theType);
				if (theGroup != null) theGroups.add(theGroup);
			}
			
			theSignatureGroup.merge(theGroups);
		}
	}
	
	public void behaviorChanged(IBehaviorInfo aBehavior)
	{
		if (ECHO) Utils.println("[MGM] behaviorChanged: %s", aBehavior);
		MethodSignatureGroup theSignatureGroup = getSignatureGroup(getSignature(aBehavior));
		MethodGroup theMethodGroup = findGroup(theSignatureGroup, aBehavior.getDeclaringType());
		
		// Update flags
		if (aBehavior.isNative()) theMethodGroup.markUnknown();
	}

	/**
	 * Calls {@link #merge(MethodSignatureGroup, IClassInfo, IClassInfo)} on each signature group
	 */
	private void addEdge(IClassInfo aParent, IClassInfo aChild)
	{
		if (ECHO) Utils.println("[MGM] adding edge: %s <|- %s", aParent, aChild);
		itsChildrenMap.add(aParent.getId(), aChild.getId());
	}
	
	public static boolean isSkipped(String aClassName, String aMethodName, boolean aIsInScope)
	{
		if (aClassName.startsWith("java/lang/ref/")) return true;
//		if (!aIsInScope && aClassName.indexOf("ClassLoader") >= 0) return true;
		return false;
	}

	public void behaviorAdded(IBehaviorInfo aBehavior)
	{
		if (ECHO) Utils.println("[MGM] behaviorAdded: %s", aBehavior);
		MethodSignatureGroup theSignatureGroup = getSignatureGroup(getSignature(aBehavior));
		
		// Check if this method goes into an existing group
		MethodGroup theMethodGroup = findGroup(theSignatureGroup, aBehavior.getDeclaringType());
		if (theMethodGroup != null)
		{
			if (ECHO) Utils.println("[MGM] behaviorAdded - adding to existing group (id: %d)", theMethodGroup.getGroupId());
			theMethodGroup.add(aBehavior);
			if (theMethodGroup.isMonitored()) markMonitored(aBehavior);
			if (theMethodGroup.hasUnknown()) markUnknown(aBehavior);
		}
		else
		{
			theMethodGroup = theSignatureGroup.addSingleton(aBehavior);
			if (ECHO) Utils.println("[MGM] behaviorAdded - created new group (id: %d)", theMethodGroup.getGroupId());
		}
		
		// Check if the behavior is in scope
		boolean theInScope = isInScope(aBehavior);
		if (theInScope) theMethodGroup.markMonitored();
		if (aBehavior.isNative()) theMethodGroup.markUnknown();
		if (isSkipped(aBehavior.getDeclaringType().getName(), aBehavior.getName(), theInScope)) theMethodGroup.markUnknown();
		
		// Always mark clinits monitored as they can be executed at any time (we need at least enveloppe messages)
		if (!theInScope && ! theMethodGroup.isMonitored() && "<clinit>".equals(aBehavior.getName()))
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
	private MethodGroup findGroup(MethodSignatureGroup aSignatureGroup, IClassInfo aType)
	{
		Set<MethodGroup> theGroups = new HashSet<MethodGroup>();
		Collection<IClassInfo> theHierarchy = getTypeHierarchy(aType);
		for (IClassInfo theType : theHierarchy)
		{
			MethodGroup theGroup = aSignatureGroup.getGroup(theType);
			if (theGroup != null) theGroups.add(theGroup);
		}
		
		return aSignatureGroup.merge(theGroups);
	}
	
	/**
	 * Returns all the ancestors and descendants of the given type.
	 */
	private Set<IClassInfo> getTypeHierarchy(IClassInfo aType)
	{
		Set<IClassInfo> theTypes = new HashSet<IClassInfo>();
		fillAncestors(theTypes, aType);
		fillDescendants(theTypes, aType);
		theTypes.add(aType);
		return theTypes;
	}
	
	private void fillAncestors(Set<IClassInfo> aTypes, IClassInfo aType)
	{
		if (aType.getSupertype() != null)
		{
			aTypes.add(aType.getSupertype());
			fillAncestors(aTypes, aType.getSupertype());
		}
		
		if (aType.getInterfaces() != null) for(IClassInfo theInterface : aType.getInterfaces())
		{
			aTypes.add(theInterface);
			fillAncestors(aTypes, theInterface);
		}
	}

	private void fillDescendants(Set<IClassInfo> aTypes, IClassInfo aType)
	{
		TIntHashSet theChildren = itsChildrenMap.getSet(aType.getId());
		if (theChildren != null) 
		{
			TIntIterator theIterator = theChildren.iterator();
			while(theIterator.hasNext())
			{
				int theChildId = theIterator.next();
				IClassInfo theChild = itsStructureDatabase.getClass(theChildId, true);
				aTypes.add(theChild);
				fillDescendants(aTypes, theChild);
				fillAncestors(aTypes, theChild);
			}
		}
	}
	
	public void fieldAdded(IFieldInfo aField)
	{
	}
	

	public void monitoringModeChanged(BehaviorMonitoringModeChange aChange)
	{
	}

	/**
	 * Merges the method groups corresponding to n1 and n2 if they are distinct.
	 */
	private void merge(MethodSignatureGroup aSignatureGroup, Collection<IClassInfo> aClasses)
	{
		Set<MethodGroup> theGroups = new HashSet<MethodGroup>();
		for (IClassInfo theClass : aClasses)
		{
			MethodGroup theGroup = aSignatureGroup.getGroup(theClass);
			if (theGroup != null) theGroups.add(theGroup);
		}
		
		aSignatureGroup.merge(theGroups);
	}
	
	private MethodSignatureGroup getSignatureGroup(String aSignature)
	{
		MethodSignatureGroup theGroup = itsSignatureGroups.get(aSignature);
		if (theGroup == null)
		{
			theGroup = new MethodSignatureGroup(aSignature);
			itsSignatureGroups.put(aSignature, theGroup);
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
	public class MethodGroup implements Serializable
	{
		private static final long serialVersionUID = 7719483876342347891L;
		
		private final int itsGroupId;

		private final Set<IClassInfo> itsTypes = new HashSet<IClassInfo>();
		private final List<IBehaviorInfo> itsBehaviors = new ArrayList<IBehaviorInfo>(1);
		private boolean itsMonitored = false;
		private boolean itsHasUnknown = false;
		
		public MethodGroup(IBehaviorInfo aInitialBehavior)
		{
			itsGroupId = nextGroupId();
			add(aInitialBehavior);
		}
		
		public int getGroupId()
		{
			return itsGroupId;
		}
		
		public void add(IBehaviorInfo aBehavior)
		{
			itsTypes.add(aBehavior.getDeclaringType());
			itsBehaviors.add(aBehavior);
		}
		
		public boolean hasType(IClassInfo aClass)
		{
			return itsTypes.contains(aClass);
		}
		
		public List<IBehaviorInfo> getBehaviors()
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
			if (ECHO) Utils.println("[MGM] group.markMonitored(id: %d)", getGroupId());
			for (IBehaviorInfo theBehavior : getBehaviors()) MethodGroupManager.this.markMonitored(theBehavior);
		}
		
		public boolean hasUnknown()
		{
			return itsHasUnknown;
		}
		
		public void markUnknown()
		{
			if (itsHasUnknown) return;
			itsHasUnknown = true;
			if (ECHO) Utils.println("[MGM] group.markUnknown(id: %d)", getGroupId());
			for (IBehaviorInfo theBehavior : getBehaviors()) MethodGroupManager.this.markUnknown(theBehavior);
		}
	}
	
	/**
	 * A group of methods that have the same signature. Each such group contains a number of method groups.
	 * @author gpothier
	 */
	public class MethodSignatureGroup implements Serializable
	{
		private static final long serialVersionUID = 1029462904632099612L;
		
		private final String itsSignature;
		private final List<MethodGroup> itsGroups = new ArrayList<MethodGroup>(1);
		
		public MethodSignatureGroup(String aSignature)
		{
			itsSignature = aSignature;
		}
		
		/**
		 * Retrieves the method group to which the method of the given type belongs.
		 */
		public MethodGroup getGroup(IClassInfo aClass)
		{
			MethodGroup theResult = null;
			for (MethodGroup theGroup : itsGroups) 
			{
				if (theGroup.hasType(aClass)) 
				{
					if (theResult != null) throw new RuntimeException("Several groups contain the same "+aClass);
					theResult = theGroup;
				}
			}
			return theResult;
		}
		
		public MethodGroup addSingleton(IBehaviorInfo aBehavior)
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
		public MethodGroup merge(MethodGroup g1, MethodGroup g2)
		{
			if (ECHO)
			{
				Utils.println("[MGM] merging groups %d and %d", g1.getGroupId(), g2.getGroupId());
				Utils.println("[MGM] monitored: %s, %s", g1.isMonitored(), g2.isMonitored());
				Utils.println("[MGM] unknown: %s, %s", g1.hasUnknown(), g2.hasUnknown());
			}

			// Merge monitoring
			if (g1.isMonitored()) g2.markMonitored();
			if (g2.isMonitored()) g1.markMonitored();
			
			// Merge unknown
			if (g1.hasUnknown()) g2.markUnknown();
			if (g2.hasUnknown()) g1.markUnknown();
			
			// Merge both groups (into g1)
			for(IBehaviorInfo theBehavior : g2.getBehaviors()) g1.add(theBehavior);
			
			itsGroups.remove(g2);
			return g1;
		}
		
		/**
		 * Same as {@link #merge(MethodGroup, MethodGroup)}, but with N groups.
		 */
		public MethodGroup merge(Collection<MethodGroup> aGroups)
		{
			MethodGroup theResult = null;
			for(MethodGroup theGroup : aGroups)
			{
				if (theResult == null) theResult = theGroup;
				else theResult = merge(theResult, theGroup);
			}

			return theResult;
		}
	}

	/**
	 * A Map with int keys where the values are sets of ints. 
	 * @author gpothier
	 */
	public static class IntSetMap implements Serializable
	{
		private static final long serialVersionUID = 177413409867774L;
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
}
