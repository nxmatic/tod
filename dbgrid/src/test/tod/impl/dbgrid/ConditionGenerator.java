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

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventFilter;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IThreadInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.impl.database.structure.standard.ThreadInfo;

/**
 * Random generator of conditions.
 * Uses an {@link ILogBrowser} as a factory
 * @author gpothier
 */
public final class ConditionGenerator
{
	private final ILogBrowser itsLogBrowser;
	private Random itsRandom;
	private IdGenerator itsIdGenerator;
	
	/**
	 * Current depth of the generated condition.
	 * Not thread-safe!
	 */
	private int itsLevel = 0;
	
	public ConditionGenerator(long aSeed, IdGenerator aEventGenerator, ILogBrowser aBrowser)
	{
		itsRandom = new Random(aSeed);
		itsIdGenerator = aEventGenerator;
		itsLogBrowser = aBrowser;
	}

	public IEventFilter next()
	{
		itsLevel = 0;
		return next(0.5f);
	}
	
	/**
	 * Generates a random condition, with a specified probability of generating
	 * a simple condition (vs. compound contition).
	 */
	public IEventFilter next(float aSimpleProbability)
	{
		float f = itsRandom.nextFloat();
		
		return f < aSimpleProbability ?
				nextSimpleCondition()
				: nextCompoundCondition();
	}
	
	public IEventFilter nextSimpleCondition()
	{
		switch(itsRandom.nextInt(7))
		{
		case 0: return itsLogBrowser.createBehaviorCallFilter(genBehavior());
		case 1: return itsLogBrowser.createOperationLocationFilter(genBehavior(), itsIdGenerator.genBytecodeIndex());
		case 2: return itsLogBrowser.createFieldFilter(genField());
		case 3: return itsLogBrowser.createObjectFilter(itsIdGenerator.genObject());
		case 4: return itsLogBrowser.createThreadFilter(genThread());
		case 5: return itsLogBrowser.createVariableWriteFilter(genVariable());
		case 6: return itsLogBrowser.createDepthFilter(itsIdGenerator.genDepth());
		default: throw new RuntimeException("Not handled");
		}
	}
		
	public ICompoundFilter nextCompoundCondition()
	{
		itsLevel++;
		if (itsRandom.nextBoolean()) return nextConjunction();
		else return nextDisjunction();
	}
	
	public ICompoundFilter nextConjunction()
	{
		ICompoundFilter theConjunction = itsLogBrowser.createIntersectionFilter();
		fillCompoundCondition(theConjunction);
		return theConjunction;
	}
	
	public ICompoundFilter nextDisjunction()
	{
		ICompoundFilter theDisjunction = itsLogBrowser.createUnionFilter();
		fillCompoundCondition(theDisjunction);
		return theDisjunction;
	}
	
	private void fillCompoundCondition(ICompoundFilter aCondition)
	{
		int theCount = itsRandom.nextInt(9)+1;
		for(int i=0;i<theCount;i++)
		{
			aCondition.add(next(itsLevel < 3 ? 0.9f : 1f));
		}
	}
	
	protected IBehaviorInfo genBehavior()
	{
		return itsLogBrowser.getStructureDatabase().getBehavior(itsIdGenerator.genBehaviorId(), true);
	}
	
	protected IFieldInfo genField()
	{
		return itsLogBrowser.getStructureDatabase().getField(itsIdGenerator.genFieldId(), true);
	}
	
	protected IThreadInfo genThread()
	{
		return new ThreadInfo(null, itsIdGenerator.genThreadId(), 0, null);
	}
	
	protected LocalVariableInfo genVariable()
	{
		return new LocalVariableInfo(0, 0, null, null, itsIdGenerator.genVariableId());
	}
}