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
package tod.impl.dbgrid;

import java.util.Random;

import tod.core.database.TimestampGenerator;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.impl.dbgrid.messages.MessageType;

public class IdGenerator
{
	private long itsSeed;
	private Random itsRandom;
	private TimestampGenerator itsTimestampGenerator;
	private TimestampGenerator itsParentTimestampGenerator;
	
	private int itsThreadsRange;
	private int itsDepthRange;
	private int itsBytecodeRange;
	private int itsAdviceSourceIdRange;
	private int itsBehaviorRange;
	private int itsClassRange;
	private int itsFieldRange;
	private int itsVariableRange;
	private int itsObjectRange;
	private int itsArrayIndexRange;
	private int itsProbeRange;

	public IdGenerator(
			long aSeed,
			int aThreadsRange, 
			int aDepthRange, 
			int aBytecodeRange, 
			int aBehaviorRange, 
			int aAdviceSourceIdRange, 
			int aFieldRange, 
			int aVariableRange, 
			int aObjectRange,
			int aArrayIndexRange)
	{
		itsSeed = aSeed;
		reset();
		itsTimestampGenerator = new TimestampGenerator(aSeed);		
		itsParentTimestampGenerator = new TimestampGenerator(aSeed);		
		
		itsThreadsRange = aThreadsRange;
		itsDepthRange = aDepthRange;
		itsBytecodeRange = aBytecodeRange;
		itsBehaviorRange = aBehaviorRange;
		itsClassRange = itsBehaviorRange;
		itsAdviceSourceIdRange = aAdviceSourceIdRange;
		itsFieldRange = aFieldRange;
		itsVariableRange = aVariableRange;
		itsObjectRange = aObjectRange;
		itsArrayIndexRange = aArrayIndexRange;
		
		itsProbeRange = itsBehaviorRange*itsBytecodeRange/100;
	}
	
	public void reset()
	{
		itsRandom = new Random(itsSeed);		
	}
	
	/**
	 * Fills the given structure database with a number of structural elements
	 * in accordance with the ranges of this generator.
	 */
	public void fillStructureDatabase(IMutableStructureDatabase aDatabase)
	{
		for(int i=1;i<=itsClassRange;i++) 
		{
			aDatabase.addClass(i, "Class"+i);
		}
		
		for(int i=1;i<=itsBehaviorRange;i++) 
		{
			aDatabase.getClass((i%itsClassRange)+1, true).addBehavior(i, "Beh"+i, "()V", i%10 == 0);
		}
		
		for(int i=1;i<=itsFieldRange;i++) 
		{
			IMutableClassInfo theClass = aDatabase.getClass(((i+8)%itsClassRange)+1, true);
			aDatabase.getClass((i%itsClassRange)+1, true).addField(i, "Field"+i, theClass, i%10 == 0);
		}
		
		for(int i=1;i<=itsProbeRange;i++)
		{
			aDatabase.addProbe(
					getProbeBehavior(i), 
					getProbeBytecode(i), 
					null, 
					getProbeAdviceSrcId(i));
		}

	}
	
	protected int getProbeBehavior(int aProbeId)
	{
		return ((aProbeId*3 + 7)%itsBehaviorRange)+1;
	}
	
	protected int getProbeBytecode(int aProbeId)
	{
		return ((aProbeId/2 + 9)%itsBytecodeRange);
	}
	
	protected int getProbeAdviceSrcId(int aProbeId)
	{
		return ((aProbeId*2 + 3)%itsAdviceSourceIdRange)+1;
	}
	

	public MessageType genType()
	{
		// -1 to avoid OUTPUT, -2 to adjust the probability of EXIT, as there are three ways to ENTER
		int theIndex = itsRandom.nextInt(MessageType.VALUES.length-1+2)-2;
		if (theIndex < 0) theIndex = 0;
		return MessageType.VALUES[theIndex];
	}
	
	public long genTimestamp()
	{
		return itsTimestampGenerator.next();
	}
	
	public int genThreadId()
	{
		return itsRandom.nextInt(itsThreadsRange) + 1;
	}
	
	public int genDepth()
	{
		return itsRandom.nextInt(itsDepthRange);
	}
	
	public int genProbeId()
	{
		return itsRandom.nextInt(itsProbeRange) + 1;
	}
	
	public int genBehaviorId()
	{
		return itsRandom.nextInt(itsBehaviorRange) + 1;
	}
	
	public int genTypeId()
	{
		return itsRandom.nextInt(itsClassRange) + 1;
	}
	
	public int genFieldId()
	{
		return itsRandom.nextInt(itsFieldRange) + 1;
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
	
	public ObjectId genObject()
	{
		return new ObjectId(itsRandom.nextInt(itsObjectRange) + 1);
	}
	
	public int genArrayIndex()
	{
		return itsRandom.nextInt(itsArrayIndexRange);
	}
	
	public boolean genBoolean()
	{
		return itsRandom.nextBoolean();
	}
	
	public Object[] genArgs()
	{
		int theCount = itsRandom.nextInt(10);
		Object[] theArgs = new Object[theCount];
		for (int i = 0; i < theArgs.length; i++) theArgs[i] = genObject();
		return theArgs;
	}

	public int[] genAdviceCFlow()
	{
		int theCount = itsRandom.nextInt(100);
		theCount -= 94;
		if (theCount <= 0) return null;
		int[] theResult = new int[theCount];
		for(int i=0;i<theCount;i++) theResult[i] = genAdviceSourceId();
		return theResult;
	}

	public int getDepthRange()
	{
		return itsDepthRange;
	}
	
}
