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
package tod.core.server;

import tod.core.DebugFlags;
import tod.core.config.TODConfig;
import tod.core.session.ISession;
import tod.utils.ConfigUtils;
import tod.utils.TODUtils;
import zz.utils.net.Server;
import zz.utils.notification.IEvent;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;
import zz.utils.properties.IProperty;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * A TOD server accepts connections from debugged VMs and process instrumentation
 * requests as well as logged events.
 * The actual implementation of the instrumenter and database are left
 * to delegates.
 * @author gpothier
 */
public abstract class TODServer extends Server
{
	private TODConfig itsConfig;
	private IRWProperty<Boolean> pConnected = new SimpleRWProperty<Boolean>();
	private IFireableEvent<Throwable> eException = new SimpleEvent<Throwable>();
	
	public TODServer(TODConfig aConfig)
	{
		super(aConfig.getPort(), true, DebugFlags.TOD_SERVER_DAEMON);
		TODUtils.logf(0, "TODServer on port: %d", getPort());

		itsConfig = aConfig;
	}
	
	public void setConfig(TODConfig aConfig)
	{
		itsConfig = aConfig;
	}

	public TODConfig getConfig()
	{
		return itsConfig;
	}
	
	/**
	 * This property indicates if the server is connected to a debuggee VM or not.
	 */
	public IProperty<Boolean> pConnected()
	{
		return pConnected;
	}
	
	/**
	 * Causes this server to stop accepting connections.
	 */
	@Override
	public void close()
	{
		System.out.println("Server disconnecting...");
		super.close();
		System.out.println("Server disconnected.");
	}
	
	/**
	 * Disconnects from all currently connected VMs.
	 * Subclasses should override and call super.
	 */
	public synchronized void disconnect()
	{
		disconnected();
	}

	/**
	 * This method is called when target VMs are disconnected.
	 */
	protected void disconnected()
	{
		pConnected.set(false);
	}

	/**
	 * This method is called when a client connects to the server when there
	 * are no open connections (ie, when the server passes from the "no client
	 * connected" state to the "client(s) connected" state).
	 */
	protected void connected()
	{
		pConnected.set(true);
	}
	
	/**
	 * See {@link ISession#pCaptureEnabled()}.
	 */
	public abstract IRWProperty<Boolean> pCaptureEnabled();
	
	public IEvent<Throwable> eException()
	{
		return eException;
	}
	
	protected IFireableEvent<Throwable> getExceptionEvent()
	{
		return eException;
	}
	
	/**
	 * Retrieves the {@link ITODServerFactory} to use for the given config.
	 */
	public static ITODServerFactory getFactory(TODConfig aConfig)
	{
		try
		{
			String theClassName = aConfig.get(TODConfig.SERVER_TYPE);
			Class<?> theClass = Class.forName(theClassName);
			return (ITODServerFactory) theClass.newInstance();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
