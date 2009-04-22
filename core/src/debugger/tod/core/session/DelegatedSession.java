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

import javax.swing.JComponent;

import tod.core.config.TODConfig;
import tod.core.database.browser.ILogBrowser;
import zz.utils.properties.IRWProperty;

/**
 * A session that delegates to another session.
 * @author gpothier
 */
public abstract class DelegatedSession implements ISession
{
	private ISession itsDelegate;

	public DelegatedSession(ISession aDelegate)
	{
		itsDelegate = aDelegate;
	}

	public ISession getDelegate()
	{
		return itsDelegate;
	}

	public TODConfig getConfig()
	{
		return itsDelegate.getConfig();
	}

	public void disconnect()
	{
		itsDelegate.disconnect();
	}

	public void flush()
	{
		itsDelegate.flush();
	}

	public ILogBrowser getLogBrowser()
	{
		return itsDelegate.getLogBrowser();
	}

	public URI getUri()
	{
		return itsDelegate.getUri();
	}

	public JComponent createConsole()
	{
		return itsDelegate.createConsole();
	}

	public ConnectionInfo getConnectionInfo()
	{
		return itsDelegate.getConnectionInfo();
	}
	
	public boolean isAlive()
	{
		return itsDelegate.isAlive();
	}

	public IRWProperty<Boolean> pCaptureEnabled()
	{
		return itsDelegate.pCaptureEnabled();
	}
}
