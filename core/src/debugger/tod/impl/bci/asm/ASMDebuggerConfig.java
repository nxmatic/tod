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
package tod.impl.bci.asm;

import tod.core.config.ClassSelector;
import tod.core.config.TODConfig;
import tod.tools.parsers.ParseException;
import tod.tools.parsers.workingset.WorkingSetFactory;

/**
 * Aggregates configuration information for the ASM instrumenter.
 * @author gpothier
 */
public class ASMDebuggerConfig
{
	private ClassSelector itsGlobalSelector;
	private ClassSelector itsTraceSelector;
	private TODConfig itsTODConfig;
	/**
	 * Creates a default debugger configuration.
	 */
	public ASMDebuggerConfig(TODConfig aConfig)
	{
		itsTODConfig = aConfig;
		// Setup selectors
		setGlobalWorkingSet(aConfig.get(TODConfig.SCOPE_GLOBAL_FILTER));
		setTraceWorkingSet(aConfig.get(TODConfig.SCOPE_TRACE_FILTER));
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
	
	/**
	 * Returns the selector that indicates which classes should be
	 * instrumented so that execution of their methods is traced.
	 */
	public ClassSelector getTraceSelector()
	{
		return itsTraceSelector;
	}
	
	/**
	 * Returns the global selector. Classes not accepted by the global selector
	 * will not be instrumented at all even if they are accepted by other selectors.
	 */
	public ClassSelector getGlobalSelector()
	{
		return itsGlobalSelector;
	}
	
	/**
	 * Indicates wether the given class name is in the instrumentation scope
	 * (both in the global selector and in the trace selector).
	 */
	public boolean isInScope(String aClassName)
	{
		if (getTODConfig().get(TODConfig.AGENT_SKIP_CORE_CLASSE))
		{
			// These are the same is in agent.cpp 
			if (aClassName.startsWith("java/")
				|| aClassName.startsWith("sun/")
		// 		|| aName.startsWith("javax/")
				|| aClassName.startsWith("com/sun/")
				|| aClassName.startsWith("net/sourceforge/retroweaver/")) return false;
		}

		return BCIUtils.acceptClass(aClassName, getGlobalSelector())
			&& BCIUtils.acceptClass(aClassName, getTraceSelector());
	}


	public TODConfig getTODConfig()
	{
		return itsTODConfig;
	}
}
