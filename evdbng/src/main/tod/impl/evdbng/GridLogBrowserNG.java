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
package tod.impl.evdbng;

import java.util.List;

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.IEventPredicate;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.IObjectInspector;
import tod.core.database.browser.LocationUtils;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IHostInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import tod.impl.common.PrecomputedFilter;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.RIGridMaster;
import tod.impl.dbgrid.aggregator.GridEventBrowser;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.dbgrid.queries.EventIdCondition;
import tod.impl.evdbng.db.RoleIndexSet;
import tod.impl.evdbng.queries.AdviceCFlowCondition;
import tod.impl.evdbng.queries.AdviceSourceIdCondition;
import tod.impl.evdbng.queries.BehaviorCondition;
import tod.impl.evdbng.queries.BytecodeLocationCondition;
import tod.impl.evdbng.queries.CompoundCondition;
import tod.impl.evdbng.queries.Conjunction;
import tod.impl.evdbng.queries.DepthCondition;
import tod.impl.evdbng.queries.Disjunction;
import tod.impl.evdbng.queries.EventCondition;
import tod.impl.evdbng.queries.FieldCondition;
import tod.impl.evdbng.queries.PredicateCondition;
import tod.impl.evdbng.queries.RoleCondition;
import tod.impl.evdbng.queries.ThreadCondition;
import tod.impl.evdbng.queries.TypeCondition;
import tod.impl.evdbng.queries.VariableCondition;
import tod.utils.remote.RemoteStructureDatabase;

/**
 * Implementation of {@link ILogBrowser} for the grid backend.
 * This is the client-side object that interfaces with the {@link GridMaster}
 * for executing queries.
 * Note: it is remote because it must be accessed by the master.
 * @author gpothier
 */
public class GridLogBrowserNG extends GridLogBrowser
{
	private static final long serialVersionUID = 8081028730784416402L;

	
	private GridLogBrowserNG(
			ISession aSession,
			RIGridMaster aMaster,
			IStructureDatabase aStructureDatabase) 
	{
		super(aSession, aMaster, aStructureDatabase);
	}
	
//	public static GridLogBrowser createLocal(ISession aSession, GridMaster aMaster) 
//	{
//		return new GridLogBrowserNG(aSession, aMaster, aMaster.getStructureDatabase());
//	}
//	
	public static GridLogBrowser createRemote(ISession aSession, RIGridMaster aMaster)
	{
		return new GridLogBrowserNG(
				aSession,
				aMaster, 
				RemoteStructureDatabase.createDatabase(aMaster.getRemoteStructureDatabase()));
	}

	
	
	public IEventFilter createArgumentFilter(ObjectId aId)
	{
		return SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				RoleIndexSet.ROLE_OBJECT_ANYARG);
	}

	public IEventFilter createArgumentFilter(ObjectId aId, int aPosition)
	{
		return SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				(byte) aPosition);
	}
	
	public IEventFilter createValueFilter(ObjectId aId)
	{
		return SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				RoleIndexSet.ROLE_OBJECT_VALUE);
	}
	
	public IEventFilter createResultFilter(ObjectId aId)
	{
		return SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				RoleIndexSet.ROLE_OBJECT_RESULT);
	}
	
	public IEventFilter createOperationLocationFilter(IBehaviorInfo aBehavior, int aBytecodeIndex)
	{
		return createIntersectionFilter(
				new BehaviorCondition(aBehavior.getId(), RoleIndexSet.ROLE_BEHAVIOR_OPERATION),
				new BytecodeLocationCondition(aBytecodeIndex));
	}

	public IEventFilter createOperationLocationFilter(IBehaviorInfo aBehavior)
	{
		return new BehaviorCondition(aBehavior.getId(), RoleIndexSet.ROLE_BEHAVIOR_OPERATION);
	}
	
	public IEventFilter createOperationLocationFilter(ProbeInfo aProbe)
	{
		return createIntersectionFilter(
				new BehaviorCondition(aProbe.behaviorId, RoleIndexSet.ROLE_BEHAVIOR_OPERATION),
				new BytecodeLocationCondition(aProbe.bytecodeIndex));
	}
	
	public IEventFilter createBehaviorCallFilter()
	{
		return createUnionFilter(
				new TypeCondition(MessageType.METHOD_CALL),
				new TypeCondition(MessageType.INSTANTIATION),
				new TypeCondition(MessageType.SUPER_CALL));
	}

	public IEventFilter createBehaviorCallFilter(IBehaviorInfo aBehavior)
	{
		return new BehaviorCondition(aBehavior.getId(), RoleIndexSet.ROLE_BEHAVIOR_ANY_ENTER);
	}
	
	public IEventFilter createBehaviorCallFilter(IBehaviorInfo aCalledBehavior, IBehaviorInfo aExecutedBehavior)
	{
		if (aCalledBehavior == null && aExecutedBehavior == null) 
			throw new IllegalArgumentException("Both behaviors cannot be null");
		
		if (aCalledBehavior == null)
		{
			return new BehaviorCondition(aExecutedBehavior.getId(), RoleIndexSet.ROLE_BEHAVIOR_EXECUTED);
		}
		else if (aExecutedBehavior == null)
		{
			return new BehaviorCondition(aCalledBehavior.getId(), RoleIndexSet.ROLE_BEHAVIOR_CALLED);
		}
		else
		{
			return createIntersectionFilter(
					createBehaviorCallFilter(aCalledBehavior, null),
					createBehaviorCallFilter(null, aExecutedBehavior));
		}
	}


	public IEventFilter createExceptionGeneratedFilter()
	{
		return new TypeCondition(MessageType.EXCEPTION_GENERATED);
	}

	public IEventFilter createFieldFilter(IFieldInfo aField)
	{
		return new FieldCondition(aField.getId());
	}

	public IEventFilter createFieldWriteFilter()
	{
		return new TypeCondition(MessageType.FIELD_WRITE);
	}

	public IEventFilter createVariableWriteFilter()
	{
		return new TypeCondition(MessageType.LOCAL_VARIABLE_WRITE);
	}
	
	public IEventFilter createAdviceSourceIdFilter(int aAdviceSourceId)
	{
		return new AdviceSourceIdCondition(aAdviceSourceId);
	}

	public IEventFilter createAdviceCFlowFilter(int aAdviceSourceId)
	{
		return new AdviceCFlowCondition(aAdviceSourceId);
	}

	public IEventFilter createRoleFilter(BytecodeRole aRole)
	{
		return new RoleCondition(aRole);
	}

	public IEventFilter createArrayWriteFilter()
	{
		return new TypeCondition(MessageType.ARRAY_WRITE);
	}
	
	public IEventFilter createArrayWriteFilter(int aIndex)
	{
		return SplittedConditionHandler.INDEXES.createCondition(aIndex, (byte) 0);
	}

	public IEventFilter createVariableWriteFilter(LocalVariableInfo aVariable)
	{
		return new VariableCondition(aVariable.getIndex());
	}

	public IEventFilter createInstantiationFilter(ObjectId aId)
	{
		Conjunction theObjectCondition = SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				RoleIndexSet.ROLE_OBJECT_TARGET);
		
		return createIntersectionFilter(
				theObjectCondition,
				new TypeCondition(MessageType.INSTANTIATION));
	}

	public IEventFilter createInstantiationsFilter()
	{
		return new TypeCondition(MessageType.INSTANTIATION);
	}

	public IEventFilter createInstantiationsFilter(ITypeInfo aType)
	{
		if (aType instanceof IClassInfo)
		{
			IClassInfo theClass = (IClassInfo) aType;
			List<IBehaviorInfo> theConstructors = LocationUtils.getConstructors(theClass);
			if (theConstructors.isEmpty()) return new PrecomputedFilter(this);
			
			ICompoundFilter theFilter = createUnionFilter();
			for (IBehaviorInfo theConstructor : theConstructors)
				theFilter.add(createBehaviorCallFilter(null, theConstructor));
			
			return createIntersectionFilter(
					theFilter,
					createInstantiationsFilter());
		}
		else
		{
			throw new UnsupportedOperationException(""+aType);
		}
	}

	public IEventFilter createTargetFilter(ObjectId aId)
	{
		return SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				RoleIndexSet.ROLE_OBJECT_TARGET);	
	}

	public IEventFilter createObjectFilter(ObjectId aId)
	{
		return SplittedConditionHandler.OBJECTS.createCondition(
				ObjectCodecNG.getObjectId(aId, true),
				RoleIndexSet.ROLE_OBJECT_ANY);
	}
	
	public IEventFilter createHostFilter(IHostInfo aHost)
	{
		Iterable<IThreadInfo> theThreads = aHost.getThreads();
		CompoundCondition theCompound = new Disjunction();
		
		for (IThreadInfo theThread : theThreads) 
		{
			theCompound.add(createThreadFilter(theThread));
		}
		
		return theCompound;
	}
	
	public IEventFilter createEventFilter(ILogEvent aEvent)
	{
		return new EventIdCondition(this, aEvent);
	}

	public IEventFilter createThreadFilter(IThreadInfo aThread)
	{
		return new ThreadCondition(aThread.getId());
	}

	public IEventFilter createDepthFilter(int aDepth)
	{
		return new DepthCondition(aDepth);
	}

	
	@Override
	protected ICompoundFilter createIntersectionFilter0(IEventFilter... aFilters)
	{
		CompoundCondition theCompound = new Conjunction(false, false);
		for (IEventFilter theFilter : aFilters)
		{
			theCompound.add(theFilter);
		}
		
		return theCompound;			
	}

	@Override
	protected ICompoundFilter createUnionFilter0(IEventFilter... aFilters)
	{
		CompoundCondition theCompound = new Disjunction();
		for (IEventFilter theFilter : aFilters)
		{
			theCompound.add(theFilter);
		}
		
		return theCompound;
	}
	
	public IEventFilter createPredicateFilter(IEventPredicate aPredicate, IEventFilter aBaseFilter)
	{
		return new PredicateCondition((EventCondition) aBaseFilter, aPredicate);
	}

	@Override
	protected GridEventBrowser createBrowser0(IEventFilter aFilter)
	{
		if (aFilter instanceof EventCondition)
		{
			EventCondition theCondition = (EventCondition) aFilter;
			return new GridEventBrowser(this, theCondition);
		}
		else throw new IllegalArgumentException("Not handled: "+aFilter);
	}

	@Override
	protected IObjectInspector createObjectInspector0(ObjectId aObjectId)
	{
		return new GridObjectInspectorNG(this, aObjectId);
	}
	
}
