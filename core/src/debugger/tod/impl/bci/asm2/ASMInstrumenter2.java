/*
 * Created on Apr 23, 2009
 */
package tod.impl.bci.asm2;

import java.util.Collections;

import tod.core.bci.IInstrumenter;
import tod.core.config.ClassSelector;
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
	private final MethodGroupManager itsMethodGroupManager; 

	private ClassSelector itsGlobalSelector;
	private ClassSelector itsTraceSelector;
	
	

	public ASMInstrumenter2(IMutableStructureDatabase aDatabase)
	{
		itsDatabase = aDatabase;
		itsMethodGroupManager = new MethodGroupManager(this);
	}
	
	public MethodGroupManager getMethodGroupManager()
	{
		return itsMethodGroupManager;
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
		if (aUseJava14) throw new RuntimeException("Java 1.4 mode not yet supported in asm2");
		return new ClassInstrumenter(this, aClassName, aBytecode, aUseJava14).proceed();
	}

}
