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
package tod.core.session;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.gui.IGUIManager;
import tod.impl.local.LocalSessionFactory;

/**
 * Manages the different session types available.
 * @author gpothier
 */
public class SessionTypeManager
{
	public static final String SESSIONTYPE_REMOTE = "tod-dbgrid-remote";
	public static final String SESSIONTYPE_LOCAL = "tod-dbgrid-local";
	public static final String SESSIONTYPE_MEMORY = "tod-memory";
	public static final String SESSIONTYPE_COUNT = "tod-count";
	
	private static SessionTypeManager INSTANCE = new SessionTypeManager();

	public static SessionTypeManager getInstance()
	{
		return INSTANCE;
	}

	private SessionTypeManager()
	{
		// Register known session types.
		registerType(SESSIONTYPE_REMOTE, "tod.impl.dbgrid.RemoteGridSessionFactory");
		registerType(SESSIONTYPE_LOCAL, "tod.impl.dbgrid.LocalGridSessionFactory");
		registerType(SESSIONTYPE_MEMORY, LocalSessionFactory.class.getName());
	}
	
	private final Map<String, SessionType> itsSchemeMap = 
		new HashMap<String, SessionType>();
	
	/**
	 * Registers a session type.
	 * @param aScheme The URL schema of the type.
	 * @param aClassName The class that implements the type.
	 */
	public void registerType(String aScheme, String aClassName)
	{
		SessionType theSessionType = new SessionType(aScheme, aClassName);
		itsSchemeMap.put(aScheme, theSessionType);
	}
	
	/**
	 * Returns the {@link ISession} subclass that handles the given schema. 
	 */
	public ISession createSession(IGUIManager aGUIManager, URI aUri, TODConfig aConfig)
	{
		try
		{
			System.out.println(String.format("Creating session %s", aUri));
			SessionType theSessionType = itsSchemeMap.get(aUri.getScheme());
			ISessionFactory theFactory = theSessionType.getFactory();
			return theFactory.create(aGUIManager, aUri, aConfig);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static class SessionType
	{
		/**
		 * Name of the class that implements the session type
		 */
		public final String factoryClassName;
		
		/**
		 * URL schema for the session type.
		 */
		public final String schema;

		public SessionType(String aSchema, String aFactoryClassName)
		{
			schema = aSchema;
			factoryClassName = aFactoryClassName;
		}
		
		public ISessionFactory getFactory()
		{
			try
			{
				Class<ISessionFactory> theClass = (Class) Class.forName(factoryClassName);
				return theClass.newInstance();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
