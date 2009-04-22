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
package tod.core.config;

import tod.core.config.ClassSelector.AllCS;
import tod.core.config.ClassSelector.WorkingSetClassSelector;
import tod.tools.parsers.ParseException;
import tod.utils.ConfigUtils;

/**
 * Permits to define the static configuration of the logging
 * system. This configuration must be set up before the logging
 * weaving starts. 
 * <p>
 * Lets the user select which packages/classes should be subject to
 * logging, and which kind of events can be logged. Classes and
 * events excluded from the static config cannot be activated 
 * at runtime; however, classes and events included in the static
 * config can be activated and deactivated at any moment at runtime.
 * @author gpothier
 */
public class StaticConfig
{
	private static StaticConfig INSTANCE = new StaticConfig();

	public static StaticConfig getInstance()
	{
		return INSTANCE;
	}

	private StaticConfig()
	{
	}
	
	/**
	 * Indicates if it is still possible to change the static 
	 * config.
	 */
	private boolean itsFrozen = false;
	
	public static final String PARAM_LOG_METHODS = "log-methods";
	private boolean itsLogMethods = true;
	
	public static final String PARAM_LOG_INSTANTIATIONS = "log-instantiations";	
	private boolean itsLogInstantiations = true;
	
	public static final String PARAM_LOG_FIELDWRITE = "log-fieldwrite";
	private boolean itsLogFieldWrite = true;

	public static final String PARAM_LOG_LOCALVARIABLEWRITE = "log-localvariablewrite";
	private boolean itsLogLocalVariableWrite = true;
	
	public static final String PARAM_LOG_PARAMETERS = "log-parameters";
	private boolean itsLogParameters = true;
	
	public static final String PARAM_COLLECTOR_PORT = "collector-port";
	
	public static final String PARAM_LOGGING_WORKINGSET = "logging-workingset";
	public static final String PARAM_IDENTIFICATION_WORKINGSET = "identification-workingset";

	private ClassSelector itsLoggingClassSelector = AllCS.getInstance();
	private ClassSelector itsIdentificationClassSelector = AllCS.getInstance();
	
	/**
	 * Reads static config from system properties.
	 */
	public void readConfig()
	{
		checkState();
		try
		{
			itsLogFieldWrite = ConfigUtils.readBoolean(PARAM_LOG_FIELDWRITE, itsLogFieldWrite);
			itsLogInstantiations = ConfigUtils.readBoolean(PARAM_LOG_INSTANTIATIONS, itsLogInstantiations);
			itsLogMethods = ConfigUtils.readBoolean(PARAM_LOG_METHODS, itsLogMethods);
			itsLogParameters = ConfigUtils.readBoolean(PARAM_LOG_PARAMETERS, itsLogParameters);
			
			String theLoggingWorkingSet = ConfigUtils.readString(PARAM_LOGGING_WORKINGSET, null);
			if (theLoggingWorkingSet != null)
				itsLoggingClassSelector = new WorkingSetClassSelector(theLoggingWorkingSet);
			
			String theIdentificationWorkingSet = ConfigUtils.readString(PARAM_IDENTIFICATION_WORKINGSET, null);
			if (theIdentificationWorkingSet != null)
				itsIdentificationClassSelector = new WorkingSetClassSelector(theIdentificationWorkingSet);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Exception reading StaticConfig", e);
		}
	}
	
	public void checkState()
	{
		if (itsFrozen) throw new IllegalStateException("Cannot make changes to static config after weaving");
	}
	
	/**
	 * Indicates if field write access should be logged.
	 */
	public boolean getLogFieldWrite()
	{
		return itsLogFieldWrite;
	}

	/**
	 * Indicates if field write access should be logged.
	 */
	public void setLogFieldWrite(boolean aLogFieldWrite)
	{
		checkState();
		itsLogFieldWrite = aLogFieldWrite;
	}
	
	/**
	 * Indicates if local variables access should be logged.
	 */
	public boolean getLogLocalVariableWrite()
	{
		return itsLogLocalVariableWrite;
	}

	/**
	 * Indicates if local variables access should be logged.
	 */
	public void setLogLocalVariableWrite(boolean aLogLocalVariableWrite)
	{
		itsLogLocalVariableWrite = aLogLocalVariableWrite;
	}

	/**
	 * Indicates if method enter/exit events should be
	 * logged. 
	 */
	public boolean getLogMethods()
	{
		return itsLogMethods;
	}

	/**
	 * Indicates if method enter/exit events should be
	 * logged. 
	 */
	public void setLogMethods(boolean aLogMethods)
	{
		checkState();
		itsLogMethods = aLogMethods;
	}

	/**
	 * Indicates if instantiations should be logged.
	 */
	public boolean getLogInstantiations()
	{
		return itsLogInstantiations;
	}
	
	/**
	 * Indicates if instantiations should be logged.
	 */
	public void setLogInstantiations(boolean aLogInstantiations)
	{
		checkState();
		itsLogInstantiations = aLogInstantiations;
	}

	/**
	 * Idicates if method and constructor parameters
	 * should be logged.
	 */
	public boolean getLogParameters()
	{
		return itsLogParameters;
	}
	
	/**
	 * Idicates if method and constructor parameters
	 * should be logged.
	 */
	public void setLogParameters(boolean aLogParameters)
	{
		itsLogParameters = aLogParameters;
	}
	
	/**
	 * Returns the class selector that filters the classes
	 * that must be identifiable.
	 */
	public ClassSelector getIdentificationClassSelector()
	{
		return itsIdentificationClassSelector;
	}

	/**
	 * Sets the class selector that filters the classes
	 * that must be identifiable.
	 */
	public void setIdentificationClassSelector(ClassSelector aIdentificationClassSelector)
	{
		itsIdentificationClassSelector = aIdentificationClassSelector;
	}

	/**
	 * Returns the class selector that filters the classes
	 * that must be logged.
	 */
	public ClassSelector getLoggingClassSelector()
	{
		return itsLoggingClassSelector;
	}

	/**
	 * Sets the class selector that filters the classes
	 * that must be logged.
	 */
	public void setLoggingClassSelector(ClassSelector aLoggingClassSelector)
	{
		itsLoggingClassSelector = aLoggingClassSelector;
	}

	/**
	 * Not part of the API.
	 * Prevent further changes to the static config.
	 */
	public void freeze ()
	{
		itsFrozen = true;
	}
	
}
