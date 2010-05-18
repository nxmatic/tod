/*
 * Created on Apr 23, 2009
 */
package tod.impl.bci.asm2;

import java.util.Collections;

import tod.core.bci.IInstrumenter;
import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;

/**
 * A new version of the instrumenter ({@link ASMInstrumenter}) that reduces the runtime
 * overhead by sending a minimal amount of information. 
 * @author gpothier
 */
public class ASMInstrumenter2 implements IInstrumenter
{
	private TODConfig itsConfig;
	private final IMutableStructureDatabase itsDatabase;

	public ASMInstrumenter2(TODConfig aConfig, IMutableStructureDatabase aDatabase)
	{
		setConfig(aConfig);
		itsDatabase = aDatabase;
	}
	
	public Iterable<String> getSpecialCaseClasses()
	{
		return Collections.EMPTY_LIST;
	}

	public TODConfig getConfig()
	{
		return itsConfig;
	}
	
	public void setConfig(TODConfig aConfig)
	{
		itsConfig = aConfig;
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
