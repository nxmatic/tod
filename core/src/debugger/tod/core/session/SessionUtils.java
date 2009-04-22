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
import java.net.URISyntaxException;

import tod.Util;
import tod.core.config.TODConfig;

/**
 * Utilies to manage sessions.
 * @author gpothier
 */
public class SessionUtils
{
	/**
	 * Returns the URI of session that should be created for the
	 * specified config.
	 */
	public static URI getSessionURI(TODConfig aConfig)
	{
		try
		{
			String theType = aConfig.get(TODConfig.SESSION_TYPE);
			if (TODConfig.SESSION_MEMORY.equals(theType)) 
			{
				return new URI(SessionTypeManager.SESSIONTYPE_MEMORY, "/", null);
			}
			else if (TODConfig.SESSION_LOCAL.equals(theType)) 
			{
				return new URI(SessionTypeManager.SESSIONTYPE_LOCAL, "/", null);
			}
			else if (TODConfig.SESSION_REMOTE.equals(theType)) 
			{
				return new URI(
						SessionTypeManager.SESSIONTYPE_REMOTE, 
						null,
						aConfig.get(TODConfig.COLLECTOR_HOST),
						Util.TOD_SRPC_PORT,
						null,
						null,
						null);
			}
			else if (TODConfig.SESSION_COUNT.equals(theType)) 
			{
				throw new UnsupportedOperationException("Reimplement if needed");
			}
			else throw new RuntimeException("Not handled: "+theType);
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}
	
}
