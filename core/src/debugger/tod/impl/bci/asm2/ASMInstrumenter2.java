/*
 * Created on Apr 23, 2009
 */
package tod.impl.bci.asm2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tod.core.bci.IInstrumenter;
import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IStructureDatabase.BehaviorMonitoringModeChange;
import tod.impl.bci.asm.ASMInstrumenter;

/**
 * A new version of the instrumenter ({@link ASMInstrumenter}) that reduces the runtime
 * overhead by sending a minimal amount of information. 
 * @author gpothier
 */
public class ASMInstrumenter2 implements IInstrumenter
{
	private TODConfig itsConfig;
	private final IMutableStructureDatabase itsDatabase;
	private final IStructureDatabase.Listener itsListener = new IStructureDatabase.Listener()
	{
		public void behaviorAdded(IBehaviorInfo aBehavior)
		{
		}

		public void classAdded(IClassInfo aClass)
		{
		}

		public void classChanged(IClassInfo aClass)
		{
		}

		public void fieldAdded(IFieldInfo aField)
		{
		}

		public void monitoringModeChanged(BehaviorMonitoringModeChange aChange)
		{
			itsChanges.add(aChange);
		}
	};
	
	/**
	 * Collects pending changes until {@link #getModeChangesAndReset()} is called.
	 */
	private List<BehaviorMonitoringModeChange> itsChanges = new ArrayList<BehaviorMonitoringModeChange>();
	
	public ASMInstrumenter2(TODConfig aConfig, IMutableStructureDatabase aDatabase)
	{
		setConfig(aConfig);
		itsDatabase = aDatabase;
		itsDatabase.addListener(itsListener);
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

	/**
	 * Returns a list of the needed changes, and clears the internal list.
	 */
	public List<BehaviorMonitoringModeChange> getModeChangesAndReset()
	{
		List<BehaviorMonitoringModeChange> theChanges = itsChanges;
		itsChanges = new ArrayList<BehaviorMonitoringModeChange>();
		return theChanges;
	}

}
