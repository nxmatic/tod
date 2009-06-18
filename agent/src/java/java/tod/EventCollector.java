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
package java.tod;

import java.io.PrintStream;
import java.tod.io._IO;
import java.tod.io._IOException;
import java.tod.io._SocketChannel;
import java.tod.transport.IOThread;
import java.tod.util._ArrayList;
import java.tod.util._StringBuilder;

import tod2.agent.AgentConfig;
import tod2.agent.AgentDebugFlags;
import tod2.agent.Command;
import tod2.agent.io._ByteBuffer;
import tod2.agent.io._GrowingByteBuffer;


/**
 * Instrumented code calls the log* methods of this class, which serializes the
 * event data and sends it through a socket.
 * @author gpothier
 */
public final class EventCollector 
{
	public static final EventCollector INSTANCE;
	
	static
	{
		_IO.out("[TOD] Init EventCollector");
		EventCollector c = null; // Just to have it compiling (because of final keyword)
		try
		{
			// Force loading of TOD
			TOD.captureEnabled();
			
			int thePort = Integer.parseInt(_AgConfig.getCollectorPort());
			String theHost = _AgConfig.getCollectorHost();
			String theClientName = _AgConfig.getClientName();
			
			if (thePort == 0)
			{
				_IO.err("[TOD] Must specify database port");
				System.exit(1);
			}
			
			if (theHost == null)
			{
				_IO.err("[TOD] Must specify database host");
				System.exit(1);
			}
			
			if (theClientName == null)
			{
				_IO.err("[TOD] Must specify client name");
				System.exit(1);
			}

			c = new EventCollector(theHost, thePort, theClientName);
		}
		catch (Throwable e)
		{
			_IO.err("[TOD] FATAL: Got exception");
			System.exit(1);
		}
		INSTANCE = c;
		
		// Force loading of a few classes to allow execution of ArrayList.toString
		Class f;
		f = Void.class;
		f = InternalError.class;
	}
	
	private static PrintStream itsPrintStream = AgentDebugFlags.EVENT_INTERPRETER_PRINT_STREAM;
	
	private ThreadLocal<ThreadData> itsThreadData = null;
	
	private _SocketChannel itsChannel;
	
	private final String itsCollectorHost;
	private final int itsCollectorPort;
	private final String itsClientName;
	
	/**
	 * A dummy thread data that is used for control messages.
	 */
	private ThreadData itsControlThreadData = null;
	
	private _ArrayList<ThreadData> itsThreadDataList = new _ArrayList<ThreadData>();
	private IOThread itsIOThread;

	
	private static int itsCurrentThreadId = 1;
	
	public EventCollector(String aHostname, int aPort, String aClientName)
	{
		itsCollectorHost = aHostname;
		itsCollectorPort = aPort;
		itsClientName = aClientName;
	}
	
	void init()
	{
		itsThreadData = new ThreadLocal<ThreadData>() 
		{
			@Override
			protected ThreadData initialValue()
			{
				return createThreadData();
			}
		};
		
		// Send initialization
		_ByteBuffer theBuffer = _GrowingByteBuffer.allocate(200);
		theBuffer.putIntB(AgentConfig.CNX_JAVA);
		theBuffer.putString(itsClientName);
		theBuffer.flip();
		try
		{
			itsChannel = _SocketChannel.open(itsCollectorHost, itsCollectorPort);
			itsChannel.write(theBuffer);
		}
		catch (_IOException e)
		{
			throw new RuntimeException(e);
		}
		
		itsIOThread = new IOThread(itsChannel);
		
		AgentReady.COLLECTOR_READY = true;

		try
		{
			if ((_AgentConfig.HOST_ID & ~AgentConfig.HOST_MASK) != 0) 
				throw new RuntimeException("Host id overflow");
		}
		catch (UnsatisfiedLinkError e)
		{
			_IO.err("ABORTING:");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private synchronized int getNextThreadId()
	{
		return itsCurrentThreadId++;
	}

	private ThreadData createThreadData()
	{
		Thread theCurrentThread = Thread.currentThread();
		int theId = (getNextThreadId() << AgentConfig.HOST_BITS) | _AgentConfig.HOST_ID;
		long theJvmId = _AgentConfig.JAVA14 ? theId : theCurrentThread.getId();
		ThreadData theThreadData = new ThreadData(theId, itsIOThread);
		itsThreadData.set(theThreadData);
		
		theThreadData.sendThread(theJvmId, theCurrentThread.getName());
		
		_StringBuilder b = new _StringBuilder();
		b.append("[TOD] New thread: ");
		b.append(theCurrentThread.getName());
		_IO.out(b.toString());
		
		if (theCurrentThread.getName().startsWith("[TOD]"))
		{
			throw new Error("POlop");
		}
		
		return theThreadData;
	}
	
	/**
	 * Returns the {@link ThreadData} object for the current thread.
	 */
	public ThreadData getThreadData()
	{
		ThreadData theThreadData = itsThreadData.get();
		theThreadData.checkTracedMethodsVersion();
		return theThreadData;
	}
	
	public static ThreadData _getThreadData()
	{
		return INSTANCE.getThreadData();
	}

	
	/**
	 * Sets the ignore next exception flag of the current thread.
	 * This is called by instrumented classes.
	 */
	public void ignoreNextException()
	{
		getThreadData().ignoreNextException();
	}
	
	private ThreadData getControlThreadData()
	{
		if (itsControlThreadData == null)
		{
			itsControlThreadData = new ThreadData(-1, itsIOThread);
		}
		return itsControlThreadData;
	}
	
	/**
	 * Sends a request to clear the database.
	 */
	public void clear()
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;

		ThreadData theThreadData = getThreadData();

		theThreadData.sendClear();
		theThreadData.flushBuffer();
	}
	
	/**
	 * Sends a request to flush buffered events
	 */
	public void flush()
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;

		ThreadData theThreadData = getControlThreadData();
		synchronized (theThreadData)
		{
			theThreadData.sendFlush();
			theThreadData.flushBuffer();
		}
	}
	
	/**
	 * Sends {@link Command#CMD_END}
	 */
	public void end()
	{
		ThreadData theThreadData = getControlThreadData();
		synchronized (theThreadData)
		{
			theThreadData.sendEnd();
			theThreadData.flushBuffer();
		}
	}
	
	/**
	 * Sends {@link Command#DBEV_CAPTURE_ENABLED}
	 */
	public void evCaptureEnabled(boolean aEnabled)
	{
		ThreadData theThreadData = getControlThreadData();
		synchronized (theThreadData)
		{
			theThreadData.sendEvCaptureEnabled(aEnabled);
			theThreadData.flushBuffer();
		}
	}
	
	private static void printf(String aString, Object... aArgs)
	{
		print(String.format(aString, aArgs));
	}
	
	private static void print(String aString)
	{
		itsPrintStream.println(aString);
	}

}
