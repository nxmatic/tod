/*
TOD - Trace Oriented Debugger.
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.dbgrid;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import tod.core.DebugFlags;
import tod.core.config.TODConfig;
import tod.impl.bci.asm2.ASMInstrumenter2;
import tod.impl.database.structure.standard.StructureDatabase;
import tod.impl.database.structure.standard.StructureDatabaseUtils;
import tod.impl.server.NativeAgentPeer;
import tod2.agent.AgentConfig;
import tod2.agent.io._ByteBuffer;
import zz.utils.net.Server;

/**
 * A process that accepts connections from a debugged VM and writes the trace to a file.
 * @author gpothier
 */
public class Recorder extends Server
{
	private static final TODConfig itsConfig = new TODConfig(); 
	
	private StructureDatabase itsStructureDatabase = StructureDatabase.create(itsConfig);
	private ASMInstrumenter2 itsInstrumenter = new ASMInstrumenter2(itsConfig, itsStructureDatabase);
	
	private final File itsEventsFile;
	private final File itsDbFile;
	private final OutputStream itsFileOut;
	
	private boolean itsJavaConnected = false;
	private boolean itsNativeConnected = false;

	public Recorder() throws FileNotFoundException
	{
		super(itsConfig.getPort(), DebugFlags.TOD_SERVER_DAEMON);

		itsEventsFile = new File(itsConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/events.raw");
		itsEventsFile.delete();
		
		itsDbFile = new File(itsConfig.get(TODConfig.DB_RAW_EVENTS_DIR)+"/db.raw");
		itsDbFile.delete();
		
		itsFileOut = new BufferedOutputStream(new FileOutputStream(itsEventsFile));
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
	
	protected synchronized void acceptJavaConnection(Socket aSocket) throws IOException
	{
		if (itsJavaConnected) throw new RuntimeException("A client is already connected");
		itsJavaConnected = true;
		
		new MyReceiver(aSocket).start();
	}
	
	protected synchronized void acceptNativeConnection(final Socket aSocket)
	{
		if (itsNativeConnected) throw new RuntimeException("A client is already connected");
		itsNativeConnected = true;
		
		new MyNativePeer(aSocket, 1);
	}
	
	private class MyNativePeer extends NativeAgentPeer
	{
		public MyNativePeer(Socket aSocket, int aHostId)
		{
			super(itsConfig, aSocket, itsInstrumenter, aHostId);
		}
		
		@Override
		protected void processFlush()
		{
		}
		
		@Override
		protected void disconnected()
		{
		}
	}
	
	private class MyReceiver extends Thread
	{
		private DataInputStream itsDataIn;
		private DataOutputStream itsDataOut;
		
		public MyReceiver(Socket aSocket) throws IOException
		{
			itsDataIn = new DataInputStream(new BufferedInputStream(aSocket.getInputStream()));
			itsDataOut = new DataOutputStream(new BufferedOutputStream(aSocket.getOutputStream()));
		}

		@Override
		public void run()
		{
			try
			{
				String theHostName = _ByteBuffer.getString(itsDataIn);
				System.out.println("Received hostname: "+theHostName);
				
				int theCount = 0;
				byte[] theBuffer = new byte[4096];
				while(true)
				{
					int theRead;
					try
					{
						theRead = itsDataIn.read(theBuffer);
					}
					catch (IOException e)
					{
						System.err.println(e.getMessage());
						theRead = -1;
					}
					
					theCount += theRead;

					if (theRead == 0) continue;
					else if (theRead > 0) itsFileOut.write(theBuffer, 0, theRead);
					else
					{
						itsDataIn.close();
						itsDataOut.close();
						break;
					}
				}
				
				itsFileOut.flush();
				itsFileOut.close();
				System.out.println("Received "+theCount+" bytes");
				
				StructureDatabaseUtils.saveDatabase(itsStructureDatabase, itsDbFile);
				System.exit(0);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException
	{
		new Recorder();
		System.out.println("Ready.");
	}
}
