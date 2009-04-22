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
package tod.gui.activities.objecthistory;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ObjectId;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import tod.gui.activities.IEventSeed;
import tod.gui.kit.html.HtmlDoc;
import tod.gui.kit.html.HtmlText;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * Seed for {@link ObjectHistoryActivityPanel}.
 * The set of boolean properties are filters that permit to show or hide particular events
 * along two axes: the role of the object in the event, and the kind of event.
 * @author gpothier
 */
public class ObjectHistorySeed extends ActivitySeed
implements IEventSeed
{
	private final ObjectId itsObject;
	
	/**
	 * Timestamp of the first event displayed by this view.
	 */
	private final IRWProperty<Long> pTimestamp = new SimpleRWProperty<Long>(this);
	
	/**
	 * Currently selected event.
	 */
	private final IRWProperty<ILogEvent> pSelectedEvent = new SimpleRWProperty<ILogEvent>(this);
	
	private final IRWProperty<Boolean> pShowRole_Target = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowRole_Value = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowRole_Arg = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowRole_Result = new SimpleRWProperty<Boolean>(this);
	
	private final IRWProperty<Boolean> pShowKind_BehaviorCall = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowKind_FieldWrite = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowKind_LocalWrite = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowKind_ArrayWrite = new SimpleRWProperty<Boolean>(this);
	private final IRWProperty<Boolean> pShowKind_Exception = new SimpleRWProperty<Boolean>(this);
	
	public ObjectHistorySeed(ILogBrowser aLog, ObjectId aObject)
	{
		super(aLog);
		itsObject = aObject;
		
		pShowRole_Target.set(true);
		pShowRole_Value.set(true);
		pShowRole_Arg.set(true);
		pShowRole_Result.set(true);
		
		pShowKind_BehaviorCall.set(true);
		pShowKind_FieldWrite.set(true);
		pShowKind_LocalWrite.set(true);
		pShowKind_ArrayWrite.set(true);
		pShowKind_Exception.set(true);
	}
	
	@Override
	public Class< ? extends ActivityPanel> getComponentClass()
	{
		return ObjectHistoryActivityPanel.class;
	}

	public ObjectId getObject()
	{
		return itsObject;
	}
	
	public IRWProperty<Long> pTimestamp()
	{
		return pTimestamp;
	}
	
	public IRWProperty<ILogEvent> pSelectedEvent()
	{
		return pSelectedEvent;
	}
	
	public IRWProperty<Boolean> pShowRole_Target()
	{
		return pShowRole_Target;
	}
	
	public IRWProperty<Boolean> pShowRole_Value()
	{
		return pShowRole_Value;
	}
	
	public IRWProperty<Boolean> pShowRole_Arg()
	{
		return pShowRole_Arg;
	}
	
	public IRWProperty<Boolean> pShowRole_Result()
	{
		return pShowRole_Result;
	}
	
	public IRWProperty<Boolean> pShowKind_BehaviorCall()
	{
		return pShowKind_BehaviorCall;
	}
	
	public IRWProperty<Boolean> pShowKind_FieldWrite()
	{
		return pShowKind_FieldWrite;
	}
	
	public IRWProperty<Boolean> pShowKind_LocalWrite()
	{
		return pShowKind_LocalWrite;
	}
	
	public IRWProperty<Boolean> pShowKind_ArrayWrite()
	{
		return pShowKind_ArrayWrite;
	}
	
	public IRWProperty<Boolean> pShowKind_Exception()
	{
		return pShowKind_Exception;
	}

	
	public IRWProperty<ILogEvent> pEvent()
	{
		return pSelectedEvent;
	}

	@Override
	public String getKindDescription()
	{
		return "Object history";
	}

	@Override
	public String getShortDescription()
	{
		return itsObject.toString();
	}
	
	
}
