/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.evdb1;

import static tod.impl.evdb1.DebuggerGridConfig1.STRUCTURE_OBJECT_COUNT;

import java.util.Random;

import tod.impl.dbgrid.EventGenerator;
import tod.impl.evdb1.SplittedConditionHandler;
import tod.impl.evdb1.db.RoleIndexSet;
import tod.impl.evdb1.queries.BehaviorCondition;
import tod.impl.evdb1.queries.BytecodeLocationCondition;
import tod.impl.evdb1.queries.CompoundCondition;
import tod.impl.evdb1.queries.Conjunction;
import tod.impl.evdb1.queries.DepthCondition;
import tod.impl.evdb1.queries.Disjunction;
import tod.impl.evdb1.queries.EventCondition;
import tod.impl.evdb1.queries.FieldCondition;
import tod.impl.evdb1.queries.ThreadCondition;
import tod.impl.evdb1.queries.TypeCondition;
import tod.impl.evdb1.queries.VariableCondition;

/**
 * Randomly generates {@link EventCondition}s.
 * @author gpothier
 */
public class ConditionGenerator1 
{
	private Random itsRandom;
	private EventGenerator itsEventGenerator;
	
	/**
	 * Current depth of the generated condition.
	 * Not thread-safe!
	 */
	private int itsLevel = 0;
	
	public ConditionGenerator1(long aSeed, EventGenerator aEventGenerator)
	{
		itsRandom = new Random(aSeed);
		itsEventGenerator = aEventGenerator;
	}

	public EventCondition next()
	{
		itsLevel = 0;
		return next(0.5f);
	}
	
	/**
	 * Generates a random condition, with a specified probability of generating
	 * a simple condition (vs. compound contition).
	 */
	public EventCondition next(float aSimpleProbability)
	{
		float f = itsRandom.nextFloat();
		
		return f < aSimpleProbability ?
				nextSimpleCondition()
				: nextCompoundCondition();
	}
	
	public EventCondition nextSimpleCondition()
	{
		switch(itsRandom.nextInt(8))
		{
		case 0: return new BehaviorCondition(itsEventGenerator.genBehaviorId(), genBehaviorRole());
		case 1: return new BytecodeLocationCondition(itsEventGenerator.genBytecodeIndex());
		case 2: return new FieldCondition(itsEventGenerator.genFieldId());
		case 3:
			long theId = itsRandom.nextInt(STRUCTURE_OBJECT_COUNT-1) + 1;
			return SplittedConditionHandler.OBJECTS.createCondition(
					theId,
					genObjectRole());
			
		case 4: return new ThreadCondition(itsEventGenerator.genThreadId());
		case 5: return new TypeCondition(itsEventGenerator.genType());
		case 6: return new VariableCondition(itsEventGenerator.genVariableId());
		case 7: return new DepthCondition(itsEventGenerator.genDepth());
		default: throw new RuntimeException("Not handled");
		}
	}
	
	private byte genBehaviorRole()
	{
		switch(itsRandom.nextInt(3))
		{
		case 0: return RoleIndexSet.ROLE_BEHAVIOR_ANY;
		case 1: return RoleIndexSet.ROLE_BEHAVIOR_CALLED;
		case 2: return RoleIndexSet.ROLE_BEHAVIOR_EXECUTED;
		default: throw new RuntimeException("Not handled");
		}
	}
	
	private byte genObjectRole()
	{
		switch(itsRandom.nextInt(5))
		{
		case 0: return RoleIndexSet.ROLE_OBJECT_EXCEPTION;
		case 1: return RoleIndexSet.ROLE_OBJECT_RESULT;
		case 2: return RoleIndexSet.ROLE_OBJECT_TARGET;
		case 3: return RoleIndexSet.ROLE_OBJECT_VALUE;
		case 4: return (byte) itsRandom.nextInt(10);
		default: throw new RuntimeException("Not handled");
		}
	}
	
	public EventCondition nextCompoundCondition()
	{
		itsLevel++;
		if (itsRandom.nextBoolean()) return nextConjunction();
		else return nextDisjunction();
	}
	
	public EventCondition nextConjunction()
	{
		Conjunction theConjunction = new Conjunction(false, false);
		fillCompoundCondition(theConjunction);
		return theConjunction;
	}
	
	public EventCondition nextDisjunction()
	{
		Disjunction theDisjunction = new Disjunction();
		fillCompoundCondition(theDisjunction);
		return theDisjunction;
	}
	
	private void fillCompoundCondition(CompoundCondition aCondition)
	{
		int theCount = itsRandom.nextInt(9)+1;
		for(int i=0;i<theCount;i++)
		{
			aCondition.addCondition(next(itsLevel < 3 ? 0.9f : 1f));
		}
	}
}
