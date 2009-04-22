package tod.agent;

import tod.tools.parsers.ParseException;
import tod.tools.parsers.workingset.AbstractClassSet;
import tod.tools.parsers.workingset.WorkingSetFactory;

public class ScopeManager
{
	private AbstractClassSet itsWorkingSet;
	private final NativeAgentConfig itsConfig;
	
	
	public ScopeManager(NativeAgentConfig aConfig) 
	{
		itsConfig= aConfig;
		itsConfig.log(0, "creating WorkingSet with config: --" + itsConfig.getWorkingSet() + "--");
		try{
			itsWorkingSet = WorkingSetFactory.parseWorkingSet(itsConfig.getWorkingSet());
		}catch (Exception e ){
			itsConfig.log(0, "Error while parsing config : Aborting session");
			System.exit(0);
		}
	}
	
	/**
	 * Indicates if the specified class is in the instrumentation scope.
	 */
	protected boolean isInScope(String aName)
	{
		//Forbidden classes have already being checked into the tod-agent-skel
		try
		{
			AbstractClassSet theClassSet = getWorkingSet();
			boolean isAccept = theClassSet.accept(aName.replace('/', '.'));
			return isAccept;
		}
		catch (Exception e)
		{
			itsConfig.log(0, "Error while parsing the working set: (using default filter) ");
			e.printStackTrace();
		}

		return true;
	}

	protected AbstractClassSet getWorkingSet() throws ParseException
	{
		return itsWorkingSet;
	}
	
	
	
}
