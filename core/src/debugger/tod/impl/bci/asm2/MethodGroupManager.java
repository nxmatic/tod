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
package tod.impl.bci.asm2;

import java.tod.TracedMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tod.agent.MonitoringMode;
import tod.core.bci.IInstrumenter;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IStructureDatabase;
import zz.utils.ListMap;
import zz.utils.SetMap;

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
public class MethodGroupManager implements IStructureDatabase.Listener
{
	private final ASMInstrumenter2 itsInstrumenter;
	
	/**
	 * Maps signatures to signature groups. Each signature groups contains method groups.
	 */
	private final Map<String, MethodSignatureGroup> itsSignatureGroups = new HashMap<String, MethodSignatureGroup>();
	
	/**
	 * Maps types to their direct subtypes.
	 */
	private final SetMap<IClassInfo, IClassInfo> itsChildrenMap = new SetMap<IClassInfo, IClassInfo>();

	/**
	 * Collects pending changes until {@link #getModeChangesAndReset()} is called.
	 */
	private List<IInstrumenter.BehaviorMonitoringMode> itsChanges = new ArrayList<IInstrumenter.BehaviorMonitoringMode>();
	

	public MethodGroupManager(ASMInstrumenter2 aInstrumenter)
	{
		itsInstrumenter = aInstrumenter;
		itsInstrumenter.getStructureDatabase().addListener(this);
	}
	
	/**
	 * Returns a list of the needed changes, and clears the internal list.
	 */
	public List<IInstrumenter.BehaviorMonitoringMode> getModeChangesAndReset()
	{
		List<IInstrumenter.BehaviorMonitoringMode> theChanges = itsChanges;
		itsChanges = new ArrayList<IInstrumenter.BehaviorMonitoringMode>();
		return theChanges;
	}
	
	private boolean isInScope(IBehaviorInfo aBehavior)
	{
		return itsInstrumenter.isInScope(aBehavior.getDeclaringType().getName());
	}
	
	private String getSignature(IBehaviorInfo aBehavior)
	{
		String theSig = aBehavior.getName()+aBehavior.getSignature();
		int i = theSig.indexOf(')');
		return theSig.substring(0, i+1);
	}
	
	private void markMonitored(IBehaviorInfo aBehavior)
	{
		itsChanges.add(new IInstrumenter.BehaviorMonitoringMode(
				aBehavior.getId(), 
				isInScope(aBehavior) ? MonitoringMode.FULL : MonitoringMode.ENVELOPPE));
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
		if (aClass.getSupertype() != null) addEdge(aClass.getSupertype(), aClass);
		
		if (aClass.getInterfaces() != null) 
			for(IClassInfo theInterface : aClass.getInterfaces()) addEdge(theInterface, aClass);
	}

	public void behaviorAdded(IBehaviorInfo aBehavior)
	{
//		System.out.println("    Behavior added: "+LocationUtils.toString(aBehavior));
		MethodSignatureGroup theSignatureGroup = getSignatureGroup(getSignature(aBehavior));
		
		// Check if this method goes into an existing group
		MethodGroup theMethodGroup = findGroup(theSignatureGroup, aBehavior.getDeclaringType());
		if (theMethodGroup != null)
		{
			theMethodGroup.add(aBehavior);
			if (theMethodGroup.isMonitored()) markMonitored(aBehavior);
		}
		else
		{
			theMethodGroup = theSignatureGroup.addSingleton(aBehavior);
		}
		
		// Check if the behavior is in scope
		if (isInScope(aBehavior)) theMethodGroup.markMonitored();
	}
	
	/**
	 * Finds an existing suitable group for the given type inside a signature group
	 */
	private MethodGroup findGroup(MethodSignatureGroup aSignatureGroup, IClassInfo aType)
	{
		MethodGroup theResult = null;
		List<IClassInfo> theHierarchy = getTypeHierarchy(aType);
		for (IClassInfo theType : theHierarchy)
		{
			MethodGroup theGroup = aSignatureGroup.getGroup(theType);
			if (theGroup == null) continue;
			else if (theResult == null) theResult = theGroup;
			else if (theResult != theGroup) throw new RuntimeException("Inconsistency for "+aType);
		}
		
		return theResult;
	}
	
	/**
	 * Returns all the ancestors and descendants of the given type.
	 */
	private List<IClassInfo> getTypeHierarchy(IClassInfo aType)
	{
		List<IClassInfo> theTypes = new ArrayList<IClassInfo>();
		fillAncestors(theTypes, aType);
		fillDescendants(theTypes, aType);
		theTypes.add(aType);
		return theTypes;
	}
	
	private void fillAncestors(List<IClassInfo> aTypes, IClassInfo aType)
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

	private void fillDescendants(List<IClassInfo> aTypes, IClassInfo aType)
	{
		Set<IClassInfo> theChildren = itsChildrenMap.getSet(aType);
		if (theChildren != null) for(IClassInfo theChild : theChildren)
		{
			aTypes.add(theChild);
			fillDescendants(aTypes, theChild);
		}
	}
	
	public void fieldAdded(IFieldInfo aField)
	{
	}
	
	/**
	 * Calls {@link #merge(MethodSignatureGroup, IClassInfo, IClassInfo)} on each signature group
	 */
	private void addEdge(IClassInfo aParent, IClassInfo aChild)
	{
		itsChildrenMap.add(aParent, aChild);
		for(MethodSignatureGroup theSignatureGroup : itsSignatureGroups.values()) merge(theSignatureGroup, aParent, aChild);
	}

	/**
	 * Merges the method groups corresponding to n1 and n2 if they are distinct.
	 */
	private void merge(MethodSignatureGroup aSignatureGroup, IClassInfo n1, IClassInfo n2)
	{
		MethodGroup g1 = aSignatureGroup.getGroup(n1);
		MethodGroup g2 = aSignatureGroup.getGroup(n2);
		
		if (g1 != null && g2 != null && g1 != g2) aSignatureGroup.merge(g1, g2);
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

	/**
	 * Represents a group of methods that have the same signature and belong to related types.
	 * @author gpothier
	 */
	public class MethodGroup
	{
		private final Set<IClassInfo> itsTypes = new HashSet<IClassInfo>();
		private final List<IBehaviorInfo> itsBehaviors = new ArrayList<IBehaviorInfo>();
		private boolean itsMonitored = false;
		
		public MethodGroup(IBehaviorInfo aInitialBehavior)
		{
			add(aInitialBehavior);
		}
		
		public void add(IBehaviorInfo aBehavior)
		{
			itsTypes.add(aBehavior.getDeclaringType());
			itsBehaviors.add(aBehavior);
			
			if (itsBehaviors.size() != itsTypes.size()) throw new RuntimeException("Inconsistency");
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
			for (IBehaviorInfo theBehavior : getBehaviors()) MethodGroupManager.this.markMonitored(theBehavior);
		}
	}
	
	/**
	 * A group of methods that have the same signature. Each such group contains a number of method groups.
	 * @author gpothier
	 */
	public class MethodSignatureGroup
	{
		private final String itsSignature;
		private final List<MethodGroup> itsGroups = new ArrayList<MethodGroup>();
		
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
					if (theResult != null) throw new RuntimeException("Several groups contain the same ");
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
		 */
		public void merge(MethodGroup g1, MethodGroup g2)
		{
			if (g1.isMonitored() != g2.isMonitored())
			{
				MethodGroup theMonitored = g1.isMonitored() ? g1 : g2;
				MethodGroup theUnmonitored = !g1.isMonitored() ? g1 : g2;
				
				for(IBehaviorInfo theBehavior : theUnmonitored.getBehaviors()) 
				{
					markMonitored(theBehavior);
					theMonitored.add(theBehavior);
				}
				
				itsGroups.remove(theUnmonitored);
			}
			else
			{
				for(IBehaviorInfo theBehavior : g2.getBehaviors()) g1.add(theBehavior);
				itsGroups.remove(g2);
			}
		}
	}

}
