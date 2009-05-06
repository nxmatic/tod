/*
 * Created on Apr 23, 2009
 */
package tod.impl.bci.asm2;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tod.core.bci.IInstrumenter;
import tod.core.config.ClassSelector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.impl.bci.asm.ASMInstrumenter;
import tod.impl.bci.asm.BCIUtils;
import tod.tools.parsers.ParseException;
import tod.tools.parsers.workingset.WorkingSetFactory;

/**
 * A new version of the instrumenter ({@link ASMInstrumenter}) that reduces the runtime
 * overhead by sending a minimal amount of information. 
 * @author gpothier
 */
public class ASMInstrumenter2 implements IInstrumenter
{
	private final IMutableStructureDatabase itsDatabase;

	private ClassSelector itsGlobalSelector;
	private ClassSelector itsTraceSelector;
	
	/**
	 * Maps signatures to signature groups. Each signature groups contains method groups.
	 */
	private final Map<String, MethodSignatureGroup> itsSignatureGroups = new HashMap<String, MethodSignatureGroup>();


	public ASMInstrumenter2(IMutableStructureDatabase aDatabase)
	{
		itsDatabase = aDatabase;
	}

	public Iterable<String> getSpecialCaseClasses()
	{
		return Collections.EMPTY_LIST;
	}

	private ClassSelector parseWorkingSet(String aWorkingSet)
	{
		try
		{
			return WorkingSetFactory.parseWorkingSet(aWorkingSet);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Cannot parse selector: "+aWorkingSet, e);
		}
	}
	

	public void setTraceWorkingSet(String aWorkingSet)
	{
		itsTraceSelector = parseWorkingSet(aWorkingSet);
	}
	
	public void setGlobalWorkingSet(String aWorkingSet)
	{
		itsGlobalSelector = parseWorkingSet(aWorkingSet);
	}
	
	public boolean isInScope(String aClassName)
	{
		return BCIUtils.acceptClass(aClassName, itsGlobalSelector)
			&& BCIUtils.acceptClass(aClassName, itsTraceSelector);
	}
	
	public IMutableStructureDatabase getStructureDatabase()
	{
		return itsDatabase;
	}
	
	public InstrumentedClass instrumentClass(String aClassName, byte[] aBytecode, boolean aUseJava14)
	{
		return new ClassInstrumenter(this, aClassName, aBytecode, aUseJava14).proceed();
	}

	/**
	 * Represents a group of methods that have the same signature and belong to related types.
	 * Within a group, the methods are either all not instrumented, or all enveloppe/fully instrumented.
	 * During the loading of the system, the appearance of some classes or interfaces can cause groups to
	 * be merged, in which case their instrumentation kind must be unified.
	 * @author gpothier
	 */
	public static class MethodGroup
	{
		private final String itsSignature;
		private final Set<IClassInfo> itsTypes = new HashSet<IClassInfo>();
		private boolean itsInstrumented;
		
		public MethodGroup(String aSignature)
		{
			itsSignature = aSignature;
		}
		
		public boolean hasType(IClassInfo aClass)
		{
			return itsTypes.contains(aClass);
		}
	}
	
	/**
	 * A group of methods that have the same signature. Each such group contains a number of method groups.
	 * @author gpothier
	 */
	public static class MethodSignatureGroup
	{
		private final String itsSignature;
		private final Set<MethodGroup> itsGroups = new HashSet<MethodGroup>();
		
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
	}
}
