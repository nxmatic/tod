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
package tod.core.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.common.event.ArrayWriteEvent;
import tod.impl.common.event.BehaviorExitEvent;
import tod.impl.common.event.Event;
import tod.impl.common.event.ExceptionGeneratedEvent;
import tod.impl.common.event.FieldWriteEvent;
import tod.impl.common.event.LocalVariableWriteEvent;
import tod.impl.common.event.NewArrayEvent;
import tod.impl.database.structure.standard.BehaviorInfo;
import tod.impl.database.structure.standard.FieldInfo;
import tod.impl.database.structure.standard.ThreadInfo;
import tod.impl.local.event.ConstructorChainingEvent;
import tod.impl.local.event.InstantiationEvent;
import tod.impl.local.event.MethodCallEvent;

public class EventGenerator
{
	private Random itsRandom;
	private TimestampGenerator itsTimestampGenerator;
	private TimestampGenerator itsParentTimestampGenerator;
	
	private int itsThreadsRange;
	private int itsDepthRange;
	private int itsBytecodeRange;
	private int itsAdviceSourceIdRange;
	private int itsBehaviorRange;
	private int itsFieldRange;
	private int itsVariableRange;
	private int itsObjectRange;

	private List<IThreadInfo> itsThreads = new ArrayList<IThreadInfo>();
	private List<IBehaviorInfo> itsBehaviors = new ArrayList<IBehaviorInfo>();
	private List<IFieldInfo> itsFields = new ArrayList<IFieldInfo>();

	public EventGenerator(
			long aSeed,
			int aThreadsRange, 
			int aDepthRange, 
			int aBytecodeRange, 
			int aBehaviorRange, 
			int aAdviceSourceIdRange, 
			int aFieldRange, 
			int aVariableRange, 
			int aObjectRange)
	{
		itsRandom = new Random(aSeed);
		itsTimestampGenerator = new TimestampGenerator(aSeed);		
		itsParentTimestampGenerator = new TimestampGenerator(aSeed);		
		
		itsThreadsRange = aThreadsRange;
		itsDepthRange = aDepthRange;
		itsBytecodeRange = aBytecodeRange;
		itsBehaviorRange = aBehaviorRange;
		itsAdviceSourceIdRange = aAdviceSourceIdRange;
		itsFieldRange = aFieldRange;
		itsVariableRange = aVariableRange;
		itsObjectRange = aObjectRange;
		
		for(int i=0;i<itsThreadsRange;i++) itsThreads.add(new ThreadInfo(null, i, 0, ""+i));
		for(int i=0;i<itsBehaviorRange;i++) itsBehaviors.add(new BehaviorInfo(null, i, null, ""+i, 0, "", null, null));
		for(int i=0;i<itsFieldRange;i++) itsFields.add(new FieldInfo(null, i, null, ""+i, null, 0));
	}

	public EventGenerator(long aSeed)
	{
		this(aSeed, 100, 100, 100, 100, 100, 100, 100, 100);
	}
	
	public ILogEvent next()
	{
		int theType = genType();
		Event theEvent;
		switch (theType)
		{
		case 0:
			BehaviorExitEvent theExitEvent = new BehaviorExitEvent(null);
			theEvent = theExitEvent;
			theExitEvent.setHasThrown(itsRandom.nextBoolean());
			theExitEvent.setResult(genObject());
			break;
			
		case 1:
			MethodCallEvent theCallEvent = new MethodCallEvent(null);
			theEvent = theCallEvent;
			theCallEvent.setDirectParent(itsRandom.nextBoolean());
			theCallEvent.setArguments(genArgs());
			theCallEvent.setCalledBehavior(genBehavior());
			theCallEvent.setExecutedBehavior(genBehavior());
			theCallEvent.setTarget(genObject());
			break;
			
		case 2:
			ExceptionGeneratedEvent theExceptionGeneratedEvent = new ExceptionGeneratedEvent(null);
			theEvent = theExceptionGeneratedEvent;
			theExceptionGeneratedEvent.setException(genObject());
			break;
			
		case 3:
			FieldWriteEvent theFieldWriteEvent = new FieldWriteEvent(null);
			theEvent = theFieldWriteEvent;
			theFieldWriteEvent.setField(genFieldId());
			theFieldWriteEvent.setTarget(genObject());
			theFieldWriteEvent.setValue(genObject());
			break;
			
		case 4:
			InstantiationEvent theInstantiationEvent = new InstantiationEvent(null);
			theEvent = theInstantiationEvent;
			theInstantiationEvent.setDirectParent(itsRandom.nextBoolean());
			theInstantiationEvent.setArguments(genArgs());
			theInstantiationEvent.setCalledBehavior(genBehavior());
			theInstantiationEvent.setExecutedBehavior(genBehavior());
			theInstantiationEvent.setTarget(genObject());
			break;
			
		case 5:
			LocalVariableWriteEvent theLocalWriteEvent = new LocalVariableWriteEvent(null);
			theEvent = theLocalWriteEvent;
			theLocalWriteEvent.setVariable(null);
			theLocalWriteEvent.setValue(genObject());
			break;
			
		case 6:
			ConstructorChainingEvent theChainingEvent = new ConstructorChainingEvent(null);
			theEvent = theChainingEvent;
			theChainingEvent.setDirectParent(itsRandom.nextBoolean());
			theChainingEvent.setArguments(genArgs());
			theChainingEvent.setCalledBehavior(genBehavior());
			theChainingEvent.setExecutedBehavior(genBehavior());
			theChainingEvent.setTarget(genObject());
			break;
		
		case 7:
			ArrayWriteEvent theArrayWriteEvent = new ArrayWriteEvent(null);
			theEvent = theArrayWriteEvent;
			theArrayWriteEvent.setTarget(genObject());
			theArrayWriteEvent.setIndex(genDepth()); // hmmmm...
			theArrayWriteEvent.setValue(genObject());
			break;
			
		case 8:
			NewArrayEvent theNewArrayEvent = new NewArrayEvent(null);
			theEvent = theNewArrayEvent;
			theNewArrayEvent.setInstance(genObject());
			theNewArrayEvent.setType(null);
			theNewArrayEvent.setArraySize(genBytecodeIndex()); // hmmm....
			break;

		default: throw new RuntimeException("Not handled: "+theType); 
		}
		
		theEvent.setThread(genThread());
		theEvent.setDepth(genDepth());
		theEvent.setTimestamp(itsTimestampGenerator.next());
		theEvent.setProbeId(genProbeId());
		theEvent.setParentTimestamp(genParentTimestamp());
		
		return theEvent;
	}

	public int genType()
	{
		return itsRandom.nextInt(9);
	}
	
	public long genParentTimestamp()
	{
		return itsParentTimestampGenerator.next();
	}
	
	public IThreadInfo genThread()
	{
		return itsThreads.get(itsRandom.nextInt(itsThreadsRange));
	}
	
	public int genDepth()
	{
		return itsRandom.nextInt(itsDepthRange);
	}
	
	public int genProbeId()
	{
		return itsRandom.nextInt(itsBehaviorRange); //TODO: fix if necessary
	}
	
	public IBehaviorInfo genBehavior()
	{
		return itsBehaviors.get(itsRandom.nextInt(itsBehaviorRange));
	}
	
	public IFieldInfo genFieldId()
	{
		return itsFields.get(itsRandom.nextInt(itsFieldRange));
	}
	
	public int genVariableId()
	{
		return itsRandom.nextInt(itsVariableRange) + 1;
	}
	
	public int genBytecodeIndex()
	{
		return itsRandom.nextInt(itsBytecodeRange);
	}
	
	public int genAdviceSourceId()
	{
		return itsRandom.nextInt(itsAdviceSourceIdRange);
	}
	
	public Object genObject()
	{
		return new ObjectId(itsRandom.nextInt(itsObjectRange) + 1);
	}
	
	public Object[] genArgs()
	{
		int theCount = itsRandom.nextInt(10);
		Object[] theArgs = new Object[theCount];
		for (int i = 0; i < theArgs.length; i++) theArgs[i] = genObject();
		return theArgs;
	}

}