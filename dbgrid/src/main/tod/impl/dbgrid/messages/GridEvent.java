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
package tod.impl.dbgrid.messages;

import java.io.Serializable;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.impl.common.event.Event;
import tod.impl.database.structure.standard.FieldInfo;
import tod.impl.database.structure.standard.ThreadInfo;
import tod.impl.dbgrid.GridLogBrowser;
import zz.utils.PublicCloneable;

public abstract class GridEvent extends PublicCloneable 
implements Serializable
{
	private static final long serialVersionUID = 8938721555658900935L;

	private transient IStructureDatabase itsStructureDatabase;
	
	/**
	 * We can find the parent event using only its timestamp,
	 * as it necessarily belongs to the same thread as this
	 * event
	 */
	private long itsParentTimestamp;
	private int itsThread;
	private int itsDepth;
	private long itsTimestamp;

	
	private int itsProbeId;
	private int[] itsAdviceCFlow;
	
	public GridEvent(IStructureDatabase aStructureDatabase)
	{
		itsStructureDatabase = aStructureDatabase;
	}

	protected void set(
			int aThread, 
			int aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			long aParentTimestamp)
	{
		itsThread = aThread;
		itsDepth = aDepth;
		itsTimestamp = aTimestamp;
		itsAdviceCFlow = aAdviceCFlow;
		itsProbeId = aProbeId;
		itsParentTimestamp = aParentTimestamp;
	}

	
	/**
	 * Transforms this event into a {@link ILogEvent}
	 */
	public abstract ILogEvent toLogEvent(GridLogBrowser aBrowser);
	
	/**
	 * Initializes common event fields. Subclasses can use this method in the
	 * implementation of {@link #toLogEvent(ILogBrowser)}
	 */
	protected void initEvent(GridLogBrowser aBrowser, Event aEvent)
	{
		// If no browser is specified, make up a thread info
		// This is the case when using PredicateCondition.
		IThreadInfo theThread = aBrowser != null ?
				((GridLogBrowser) aBrowser).getThread(getThread())
				: new ThreadInfo(null, getThread(), 0, "Unknown");
				
		aEvent.setThread(theThread);
		aEvent.setTimestamp(getTimestamp());
		aEvent.setDepth(getDepth());
		aEvent.setProbeId(getProbeId()); 
		aEvent.setAdviceCFlow(getAdviceCFlow());
		aEvent.setParentTimestamp(getParentTimestamp());
	}
	
	/**
	 * Utility for implementation of {@link #toLogEvent(ILogBrowser)}
	 */
	protected IBehaviorInfo getBehaviorInfo(ILogBrowser aBrowser, int aId)
	{
		assert aBrowser == null || aBrowser.getStructureDatabase() == itsStructureDatabase;
		return itsStructureDatabase.getBehavior(aId, false); 
	}
	
	/**
	 * Utility for implementation of {@link #toLogEvent(ILogBrowser)}
	 */
	protected ITypeInfo getTypeInfo(ILogBrowser aBrowser, int aId)
	{
		assert aBrowser == null || aBrowser.getStructureDatabase() == itsStructureDatabase;
		return itsStructureDatabase.getType(aId, true); 
	}
	
	/**
	 * Utility for implementation of {@link #toLogEvent(ILogBrowser)}
	 */
	protected IArrayTypeInfo getArrayTypeInfo(ILogBrowser aBrowser, ITypeInfo aBaseType, int aDimensions)
	{
		assert aBrowser == null || aBrowser.getStructureDatabase() == itsStructureDatabase;
		return itsStructureDatabase.getArrayType(aBaseType, aDimensions); 
	}
	
	/**
	 * Utility for implementation of {@link #toLogEvent(ILogBrowser)}
	 */
	protected IFieldInfo getFieldInfo(ILogBrowser aBrowser, int aId)
	{
		if (aBrowser != null)
		{
			return aBrowser.getStructureDatabase().getField(aId, true);
		}
		else
		{
			return new FieldInfo(null, aId, null, null, null, false);
		}
	}

	/**
	 * Should be used only when an event is deserialized.
	 */
	public void _setStructureDatabase(IStructureDatabase aStructureDatabase)
	{
		itsStructureDatabase = aStructureDatabase;
	}
	
	public IStructureDatabase getStructureDatabase()
	{
		return itsStructureDatabase;
	}
	
	/**
	 * Returns the type of this event.
	 */
	public abstract MessageType getEventType();
	
	/**
	 * Returns the type of this event.
	 */
	public final MessageType getMessageType()
	{
		return getEventType();
	}

	public int getProbeId()
	{
		return itsProbeId;
	}
	
	public ProbeInfo getProbeInfo()
	{
		return getProbeId() > 0 ?
				itsStructureDatabase.getProbeInfo(getProbeId())
				: null;
	}
	
	public int[] getAdviceCFlow()
	{
		return itsAdviceCFlow;
	}
	
	public long getParentTimestamp()
	{
		return itsParentTimestamp;
	}

	public int getThread()
	{
		return itsThread;
	}

	public int getDepth()
	{
		return itsDepth;
	}

	public long getTimestamp()
	{
		return itsTimestamp;
	}
	
	/**
	 * Whether this event matches a {@link BehaviorCondition}
	 */
	public boolean matchBehaviorCondition(int aBehaviorId, byte aRole)
	{
		return false;
	}
	
	/**
	 * Whether this event matches a {@link FieldCondition}
	 */
	public boolean matchFieldCondition(int aFieldId)
	{
		return false;
	}
	
	/**
	 * Whether this event matches a {@link ArrayIndexCondition}
	 */
	public boolean matchIndexCondition(int aPart, int aPartialKey)
	{
		return false;
	}
	
	/**
	 * Whether this event matches a {@link VariableCondition}
	 */
	public boolean matchVariableCondition(int aVariableId)
	{
		return false;
	}
	
	/**
	 * Whether this event matches a {@link ObjectCondition}
	 */
	public boolean matchObjectCondition(int aPart, int aPartialKey, byte aRole)
	{
		return false;
	}
	
	/**
	 * Whether this event is a behavior call event.
	 */
	public boolean isCall()
	{
		return false;
	}
	
	/**
	 * Whether this event is a behavior exit event.
	 */
	public boolean isExit()
	{
		return false;
	}
	
	/**
	 * Internal version of toString, used by subclasses.
	 */
	protected String toString0()
	{
		return String.format(
				"th: %d, pid: %d, t: %d",
				itsThread,
				itsProbeId,
				itsTimestamp); 
	}
	
	/**
	 * Returns true if the specified event is the same as this event,
	 * ie their thread and timestamp are equal.
	 * If both events come from the same (sane) database, it should mean
	 * they are equal.
	 */
	public boolean sameEvent(GridEvent e)
	{
		return e.itsTimestamp == itsTimestamp
			&& e.itsThread == itsThread;
	}
}
