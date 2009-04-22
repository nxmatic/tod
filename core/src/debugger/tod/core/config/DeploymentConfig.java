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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration options for deployment (native agent version, database
 * version...). Note that deployment options can be overridden by system
 * properties.
 * 
 * @author gpothier
 */
public class DeploymentConfig
{
	public static final String AGENTNAME_PROPERTY = "tod.agent.name";
	public static final String DATABASECLASS_PROPERTY = "tod.db.class";
	public static final String DB_SCOPE_CHECK_PROPERTY = "db.scope.check";

	private static String itsAgentName;
	private static String itsDatabaseClassName;
	private static String itsDbScopeCheck;

	static
	{
		Properties theProperties = readProperties();

		itsAgentName = System.getProperty(
				AGENTNAME_PROPERTY, 
				theProperties.getProperty(AGENTNAME_PROPERTY));

		itsDatabaseClassName = System.getProperty(
				DATABASECLASS_PROPERTY, 
				theProperties.getProperty(DATABASECLASS_PROPERTY));

		String theDBScopeCheck = System.getProperty(
				DB_SCOPE_CHECK_PROPERTY, 
				theProperties.getProperty(DB_SCOPE_CHECK_PROPERTY));
		
		itsDbScopeCheck = theDBScopeCheck == null ? "true" : theDBScopeCheck;

	}

	public static Properties readProperties()
	{
		try
		{
			InputStream theStream = DeploymentConfig.class.getResourceAsStream("/config.properties");
			Properties theProperties = new Properties();
			theProperties.load(theStream);
			return theProperties;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the name of the native agent to use.
	 */
	public static String getNativeAgentName()
	{
		return itsAgentName;
	}

	/**
	 * Returns the name of the database class.
	 */
	public static String getDatabaseClass()
	{
		return itsDatabaseClassName;
	}

	public static String getDbScopeCheck()
	{
		return itsDbScopeCheck;
	}
}
