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
package tod.impl.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.python.modules.synchronize;

import tod.core.ILogCollector;
import tod.core.bci.IInstrumenter;
import tod.core.config.TODConfig;
import tod.core.database.structure.IStructureDatabase;
import tod.core.server.TODServer;
import tod.core.transport.CollectorLogReceiver;
import tod.core.transport.LogReceiver;
import tod.impl.database.structure.standard.HostInfo;
import tod2.agent.AgentConfig;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

public class JavaTODServer extends TODServer
{
	private final IInstrumenter itsInstrumenter;
	private final IStructureDatabase itsStructureDatabase;
	private final ILogCollector itsLogCollector;
	
	private Map<String, ClientConnection> itsConnections = 
		new HashMap<String, ClientConnection>();
	
	private int itsCurrentHostId = 1;
	private LogReceiver itsReceiver;
	
	private boolean itsUpdatingCapture = false;
	private final IRWProperty<Boolean> pCaptureEnabled = 
		new SimpleRWProperty<Boolean>(this, (Boolean) null)
		{
			@Override
			protected Object canChange(Boolean aOldValue, Boolean aNewValue)
			{
				if (itsReceiver == null) return aNewValue == null ? ACCEPT : REJECT;
				else return aNewValue != null ? ACCEPT : REJECT;
			}
			
			@Override
			protected void changed(Boolean aOldValue, Boolean aNewValue)
			{
				if (itsUpdatingCapture) return;
				if (aNewValue == null) return;
				else itsReceiver.sendEnableCapture(aNewValue);
			}
		};


	public JavaTODServer(
			TODConfig aConfig, 
			IInstrumenter aInstrumenter,
			IStructureDatabase aStructureDatabase,
			ILogCollector aLogCollector)
	{
		super(aConfig);

		itsInstrumenter = aInstrumenter;
		itsStructureDatabase = aStructureDatabase;
		itsLogCollector = aLogCollector;
	}
	
	@Override
	public void setConfig(TODConfig aConfig)
	{
		super.setConfig(aConfig);
		itsInstrumenter.setConfig(aConfig);
	}
	
	public IStructureDatabase getStructureDatabase()
	{
		return itsStructureDatabase;
	}

	/**
	 * Disconnects from all currently connected VMs.
	 */
	@Override
	public synchronized void disconnect()
	{
		for(ClientConnection theConnection : itsConnections.values())
		{
			theConnection.getLogReceiver().disconnect();
		}
		itsConnections.clear();
		
		super.disconnect();
	}

	/**
	 * This method is called when target VMs are disconnected.
	 */
	@Override
	protected void disconnected()
	{
		super.disconnected();
		itsCurrentHostId = 1;
	}

	/**
	 * Disconnects from the given host
	 */
	protected synchronized void disconnect(String aHostname)
	{
		ClientConnection theConnection = itsConnections.get(aHostname);

		// The connection can be null if only the native agent
		// was connected.
		if (theConnection != null)
		{
			LogReceiver theReceiver = theConnection.getLogReceiver();
			theReceiver.disconnect();
			itsConnections.remove(aHostname);
			if (itsConnections.size() == 0) disconnected();
		}
	}
	
	
	@Override
	public IRWProperty<Boolean> pCaptureEnabled()
	{
		return pCaptureEnabled;
	}

	@Override
	protected void accepted(Socket aSocket)
	{
		try
		{
			DataInputStream theStream = new DataInputStream(aSocket.getInputStream());
			int theSignature = theStream.readInt();
			if (theSignature == AgentConfig.CNX_NATIVE) acceptNativeConnection(aSocket);
			else if (theSignature == AgentConfig.CNX_JAVA) acceptJavaConnection(aSocket);
			else throw new RuntimeException("Bad signature: 0x"+Integer.toHexString(theSignature));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected synchronized void acceptJavaConnection(Socket aSocket)
	{
		try
		{
			if (itsReceiver != null)
			{
				itsReceiver.eException().removeListener(getExceptionEvent());
			}
			
			itsReceiver = createReceiver(
					new HostInfo(itsCurrentHostId++),
					new BufferedInputStream(aSocket.getInputStream()), 
					new BufferedOutputStream(aSocket.getOutputStream()),
					true,
					getStructureDatabase(),
					itsLogCollector);
			
			itsReceiver.eException().addListener(getExceptionEvent());
		}
		catch (IOException e1)
		{
			throw new RuntimeException(e1);
		}
		
		
		String theHostName = itsReceiver.waitHostName();
		if (itsConnections.containsKey(theHostName))
		{
			try
			{
				aSocket.close();
				throw new RuntimeException("Host already connected: "+theHostName);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		itsConnections.put(theHostName, new ClientConnection(itsReceiver));
		System.out.println("Accepted (java) connection from "+theHostName);
		
		if (itsConnections.size() == 1) connected();

		notifyAll();
	}
	
	protected LogReceiver createReceiver(
			HostInfo aHostInfo, 
			InputStream aInStream, 
			OutputStream aOutStream, 
			boolean aStart,
			IStructureDatabase aStructureDatabase,
			ILogCollector aCollector)
	{
		return new CollectorLogReceiver(
				getConfig(), 
				aHostInfo, 
				aInStream, 
				aOutStream, 
				aStart, 
				aStructureDatabase, 
				aCollector)
		{
			@Override
			protected synchronized void eof()
			{
				super.eof();
				JavaTODServer.this.disconnected();
			}

			@Override
			protected void processEvCaptureEnabled(boolean aEnabled)
			{
				itsUpdatingCapture = true;
				pCaptureEnabled.set(aEnabled);
				itsUpdatingCapture = false;
			}
		};
	}
	
	protected synchronized void acceptNativeConnection(final Socket aSocket)
	{
		Thread theThread = new Thread("Connection peering")
		{
			@Override
			public synchronized void run()
			{
				try
				{
					NativeAgentPeer thePeer = new MyNativePeer(aSocket, itsCurrentHostId++);
					thePeer.waitConfigured();
					String theHostName = thePeer.getHostName();
					
					while(! aSocket.isClosed())
					{
						ClientConnection theConnection = itsConnections.get(theHostName);
						if (theConnection == null) wait(1000);
						else
						{
							theConnection.setNativeAgentPeer(thePeer);
							break;
						}
					}
					
					System.out.println("Accepted (native) connection from "+theHostName);
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		};
		theThread.setDaemon(true);
		theThread.start();
	}
	
	private class MyNativePeer extends NativeAgentPeer
	{
		public MyNativePeer(Socket aSocket, int aHostId)
		{
			super(
					getConfig(), 
					aSocket, 
					new SynchronizedInstrumenter(itsInstrumenter),
					aHostId);
		}
		
		@Override
		protected void processFlush()
		{
			disconnected();
		}
		
		@Override
		protected void disconnected()
		{
			if (getHostName() != null)
			{
				JavaTODServer.this.disconnect(getHostName());
				super.disconnected();
			}
		}
	}
	
	private static class SynchronizedInstrumenter implements IInstrumenter
	{
		private IInstrumenter itsDelegate;

		public SynchronizedInstrumenter(IInstrumenter aDelegate)
		{
			itsDelegate = aDelegate;
		}

		public synchronized InstrumentedClass instrumentClass(String aClassName, byte[] aBytecode, boolean aUseJava14)
		{
			return itsDelegate.instrumentClass(aClassName, aBytecode, aUseJava14);
		}
		
		public synchronized void setConfig(TODConfig aConfig)
		{
			itsDelegate.setConfig(aConfig);
		}
		
		public synchronized Iterable<String> getSpecialCaseClasses()
		{
			return itsDelegate.getSpecialCaseClasses();
		}
	}
	
	/**
	 * Groups both connection entities of a client (log receiver
	 * and native peer).
	 * @author gpothier
	 */
	private static class ClientConnection
	{
		private LogReceiver itsLogReceiver;
		private NativeAgentPeer itsNativeAgentPeer;
		
		public ClientConnection(LogReceiver aLogReceiver)
		{
			itsLogReceiver = aLogReceiver;
		}

		public NativeAgentPeer getNativeAgentPeer()
		{
			return itsNativeAgentPeer;
		}

		public void setNativeAgentPeer(NativeAgentPeer aNativeAgentPeer)
		{
			itsNativeAgentPeer = aNativeAgentPeer;
		}

		public LogReceiver getLogReceiver()
		{
			return itsLogReceiver;
		}

	}


}
