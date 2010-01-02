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
import java.util.Set;

import tod.core.config.TODConfig;
import tod.gui.IGUIManager;
import zz.utils.Utils;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;


/**
 * Manages a pool of debugging sessions.
 * Currently, there can be only one session at a time.
 * @author gpothier
 */
public class TODSessionManager
{
	private static final TODSessionManager INSTANCE = new TODSessionManager();

	public static TODSessionManager getInstance()
	{
		return INSTANCE;
	}

	private TODSessionManager()
	{
	}
	
	private IRWProperty<ISession> pCurrentSession = new SimpleRWProperty<ISession>()
	{
		@Override
		protected void changed(ISession aOldValue, ISession aNewValue)
		{
			if (aOldValue != null) 
			{
				try
				{
					aOldValue.disconnect();
				}
				catch (RuntimeException e)
				{
					System.err.println("[TODSessionManager] Error while disconnecting:");
					e.printStackTrace();
				}
			}
		}
	};
	
	/**
	 * This propety contains the curent TOD session.
	 */
	public IRWProperty<ISession> pCurrentSession()
	{
		return pCurrentSession;
	}
	
	/**
	 * Drops the current session.
	 */
	public void killSession()
	{
		pCurrentSession.set(null);
	}
	
	/**
	 * Whether the given session is compatible with the given config.
	 * If this method returns null, the launch should be cancelled.
	 */
	protected Boolean isCompatible(IGUIManager aRequestor, ISession aSession, TODConfig aConfig)
	{
		if (aSession == null || ! aSession.isAlive()) return false;
		URI theSessionURI = SessionUtils.getSessionURI(aConfig);
		
		String theHostName = aConfig.get(TODConfig.CLIENT_NAME);
		assert aSession.getLogBrowser() != null;
		try
		{
			URI theCurrentSessionURI = aSession.getUri();
			TODConfig theCurrentConfig = aSession.getConfig();
			
			if (! compatible(theCurrentConfig, aConfig))
			{
				if (! msgIncompatibleType(aRequestor)) return false;
			}
			else if (! theCurrentSessionURI.getScheme().equals(theSessionURI.getScheme()))
			{
				if (! msgIncompatibleType(aRequestor)) return false;
			}
			else if (aSession.getLogBrowser().getHost(theHostName) != null)
			{
				if (! msgHasHost(aRequestor, theHostName)) return false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if (! msgError(aRequestor, e)) return null;
		}
		
		return true;
	}
	
	/**
	 * Returns a session suitable for a new launch, or null
	 * if launch should be cancelled.
	 * Also sets the current session.
	 */
	public ISession getSession(IGUIManager aRequestor, TODConfig aConfig, IProgramLaunch aLaunch)
	{
		pCurrentSession.set(null); // TODO: remove this and make session reuse work.
		
		ISession theCurrentSession = pCurrentSession.get();
		Boolean theCompatible = isCompatible(aRequestor, theCurrentSession, aConfig);
		if (theCompatible == null) 
		{
			pCurrentSession.set(null);
			return null;
		}
		else if (theCompatible) 
		{
			theCurrentSession.getLogBrowser().clear();
		}
		else
		{
			URI theSessionURI = SessionUtils.getSessionURI(aConfig);
			theCurrentSession = SessionTypeManager.getInstance().createSession(aRequestor, theSessionURI, aConfig);
			pCurrentSession.set(theCurrentSession);
		}
		
		theCurrentSession.getLaunches().add(aLaunch);
		return theCurrentSession;
	}

	
	private boolean msgIncompatibleType(IGUIManager aGUIManager)
	{
		return aGUIManager.showDialog(new IGUIManager.YesNoDialogType(
				"Cannot reuse current session", 
				"The current debugging session cannot be reused " +
				"because the newly requested session is of another " +
				"type. " +
				"Launch anyway, resetting the current session?"));
	}
	
	private boolean msgHasHost(IGUIManager aGUIManager, final String aHostName)
	{
		return aGUIManager.showDialog(new IGUIManager.YesNoDialogType(
				"Cannot reuse current session",
				"The current debugging session cannot be reused " +
				"because it already contains a trace of host " +
				"'"+aHostName+"'. " +
				"Launch anyway, resetting the current session?"));
	}
	
	private boolean msgError(IGUIManager aGUIManager, final Throwable aThrowable)
	{
		return aGUIManager.showDialog(new IGUIManager.OkCancelDialogTYpe(
						"Cannot reuse current session", 
						"An error occurred while trying to reuse the " +
						"current session. The current session will be " +
						"dropped"));
	}
	
	
	private static final Set<TODConfig.Item<?>> MODIFIABLE_ITEMS = (Set) Utils.createSet(
			TODConfig.AGENT_VERBOSE,
			TODConfig.INDEX_STRINGS,
			TODConfig.CLIENT_NAME
	);
	
	/**
	 * Tests if two configurations are compatible.
	 * @param aCurrentConfig The current config of a session.
	 * @param aNewConfig The new config that should be used
	 * @return True iff the current session can be reused.
	 */
	private boolean compatible(TODConfig aCurrentConfig, TODConfig aNewConfig)
	{
		for (TODConfig.Item<?> theItem : TODConfig.ITEMS)
		{
			if (MODIFIABLE_ITEMS.contains(theItem)) continue;
			if (!Utils.equalOrBothNull(
					aCurrentConfig.get(theItem), 
					aNewConfig.get(theItem))) return false;
		}
		return true;
	}
}
