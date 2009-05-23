/*
 * Created on Apr 23, 2009
 */
package tod.impl.bci.asm2;

import java.util.Collections;

import tod.core.bci.IInstrumenter;
import tod.core.config.ClassSelector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
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
	private ClassSelector itsIdSelector;
	
	

	public ASMInstrumenter2(TODConfig aConfig, IMutableStructureDatabase aDatabase)
	{
		setConfig(aConfig);
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
	
	public void setConfig(TODConfig aConfig)
	{
		itsTraceSelector = parseWorkingSet(aConfig.get(TODConfig.SCOPE_TRACE_FILTER));
		itsGlobalSelector = parseWorkingSet(aConfig.get(TODConfig.SCOPE_GLOBAL_FILTER));
		itsIdSelector = parseWorkingSet(aConfig.get(TODConfig.SCOPE_ID_FILTER));
	}
	
	public boolean isInScope(String aClassName)
	{
		return BCIUtils.acceptClass(aClassName, itsGlobalSelector)
			&& BCIUtils.acceptClass(aClassName, itsTraceSelector);
	}
	
	public boolean isInIdScope(String aClassName)
	{
		return BCIUtils.acceptClass(aClassName, itsIdSelector);
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
