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
import java.tod.io._SocketChannel;
import java.tod.transport.IOThread;
import java.tod.transport.LowLevelEventWriter;
import java.tod.transport.NakedLinkedList;
import java.util.ArrayList;
import java.util.List;

import tod.agent.AgentConfig;
import tod.agent.AgentDebugFlags;
import tod.agent.Command;
import tod.agent.io._ByteBuffer;
import tod.agent.io._GrowingByteBuffer;
import tod.agent.io._IOException;
import tod.agent.util._ArrayList;
import tod.agent.util._IntStack;


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
	
	public EventCollector(String aHostname, int aPort, String aClientName) throws _IOException
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
		
		itsIOThread = new IOThread(this, itsChannel);
		
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
		ThreadData theThreadData = new ThreadData(theId);
		itsThreadData.set(theThreadData);
		
		if (theThreadData.isSending()) throw new RuntimeException();
		
    	LowLevelEventWriter theWriter = theThreadData.packetStart(0);
    	theWriter.sendThread(theJvmId, theCurrentThread.getName());
        theThreadData.packetEnd();
        
		return theThreadData;
	}
	
	private ThreadData getThreadData()
	{
		return itsThreadData != null ? itsThreadData.get() : null;
	}

	public void logClInitEnter(
			int aBehaviorId, 
			_BehaviorCallType aCallType,
			Object aObject, 
			Object[] aArguments)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendClInitEnter(
    			theTimestamp,
    			aBehaviorId,
    			aCallType);
    	
        theThread.packetEnd();

		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logClInitEnter(th: %d, ts: %d, bid: %d, ct: %s)",
				theThread.getId(),
				theTimestamp,
				aBehaviorId,
				aCallType);
		
//		theThread.pushEnter();		
	}
	
	public void logBehaviorEnter(
			int aBehaviorId, 
			_BehaviorCallType aCallType,
			Object aObject, 
			Object[] aArguments)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendBehaviorEnter(
    			theTimestamp,
    			aBehaviorId,
    			aCallType,
    			aObject,
    			aArguments);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logBehaviorEnter(th: %d, ts: %d, bid: %d, ct: %s, tgt: %s, args: %s)",
				theThread.getId(),
				theTimestamp,
				aBehaviorId,
				aCallType,
				formatObj(aObject),
				formatArgs(aArguments));
		
//		theThread.pushEnter();
	}
	
	private String formatObj(Object aObject)
	{
		if (aObject == null) return "null";
		else return aObject.getClass().getName();
	}
	
	private String formatArgs(Object[] aArgs)
	{
		if (aArgs == null) return "[-]";
		StringBuilder theBuilder = new StringBuilder();
		theBuilder.append("[");
		for (Object theArg : aArgs)
		{
			theBuilder.append(formatObj(theArg));
			theBuilder.append(", ");
		}
		theBuilder.append("]");
		
		return theBuilder.toString();
	}
	

	public void logClInitExit(
			int aProbeId, 
			int aBehaviorId,
			Object aResult)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendClInitExit(
    			theTimestamp,
    			aProbeId, 
    			aBehaviorId);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logClInitExit(th: %d, ts: %d, pid: %d, bid: %d, res: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aBehaviorId,
				formatObj(aResult));

//		theThread.popEnter();
	}
	
	public void logBehaviorExit(
			int aProbeId, 
			int aBehaviorId,
			Object aResult)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendBehaviorExit(
    			theTimestamp,
    			aProbeId,
    			aBehaviorId,
    			aResult);
    	
        theThread.packetEnd();

		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logBehaviorExit(th: %d, ts: %d, pid: %d, bid: %d, res: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aBehaviorId,
				formatObj(aResult));
		
//		theThread.popEnter();
	}
	
	
	public void logBehaviorExitWithException(
			int aBehaviorId, 
			Object aException)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendBehaviorExitWithException(
    			theTimestamp,
    			aBehaviorId,
    			aException);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logBehaviorExitWithException(th: %d, ts: %d, bid: %d, ex: %s)",
				theThread.getId(),
				theTimestamp,
				aBehaviorId,
				aException);
		
//		theThread.popEnter();		
	}
	
	/**
	 * Sets the ignore next exception flag of the current thread.
	 * This is called by instrumented classes.
	 */
	public void ignoreNextException()
	{
		getThreadData().ignoreNextException();
	}
	
	public void logExceptionGenerated(
			String aMethodName,
			String aMethodSignature,
			String aMethodDeclaringClassSignature, 
			int aOperationBytecodeIndex,
			Object aException)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendExceptionGenerated(
    			theTimestamp,
    			aMethodName,
    			aMethodSignature,
    			aMethodDeclaringClassSignature,
    			aOperationBytecodeIndex,
    			aException);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logExceptionGenerated(th: %d, ts: %d, mn: %s, sig: %s, dt: %s, ex: %s)",
				theThread.getId(),
				theTimestamp,
				aMethodName,
				aMethodSignature,
				aMethodDeclaringClassSignature,
				aException);
        
	}
	
	public void logFieldWrite(
			int aProbeId, 
			int aFieldId,
			Object aTarget, 
			Object aValue)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendFieldWrite(
    			theTimestamp,
    			aProbeId, 
    			aFieldId,
    			aTarget, 
    			aValue);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logFieldWrite(th: %d, ts: %d, pid: %d, fid: %d, tgt: %s, val: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aFieldId,
				formatObj(aTarget),
				formatObj(aValue));
	}
	
	public void logNewArray(
			int aProbeId, 
			Object aTarget,
			int aBaseTypeId,
			int aSize)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendNewArray(
    			theTimestamp,
    			aProbeId, 
    			aTarget,
    			aBaseTypeId,
    			aSize);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logNewArray(th: %d, ts: %d, pid: %d, tgt: %s, btid: %d, sz: %d)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				formatObj(aTarget),
				aBaseTypeId,
				aSize);
	}
	

	public void logArrayWrite(
			int aProbeId, 
			Object aTarget,
			int aIndex,
			Object aValue)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendArrayWrite(
    			theTimestamp,
    			aProbeId, 
    			aTarget,
    			aIndex,
    			aValue);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logArrayWrite(th: %d, ts: %d, pid: %d, tgt: %s, i: %d, val: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				formatObj(aTarget),
				aIndex,
				formatObj(aValue));
	}
	
	public void logInstanceOf(
			int aProbeId, 
			Object aObject,
			int aTypeId,
			int aResult)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();
		
		if (theThread.isSending()) return;
		LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
		
		theWriter.sendInstanceOf(
				theTimestamp,
				aProbeId, 
				aObject,
				aTypeId,
				aResult != 0);
		
		theThread.packetEnd();
		
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logInstanceOf(th: %d, ts: %d, pid: %d, obj: %s, t: %d, r: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				formatObj(aObject),
				aTypeId,
				aResult);
	}
	
	public void logLocalVariableWrite(
			int aProbeId, 
			int aVariableId,
			Object aValue)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendLocalVariableWrite(
    			theTimestamp,
    			aProbeId, 
    			aVariableId, 
    			aValue);
    	
        theThread.packetEnd();

		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logLocalVariableWrite(th: %d, ts: %d, pid: %d, vid: %d, val: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aVariableId,
				formatObj(aValue));
	}
	
	public void logBeforeBehaviorCallDry(
			int aProbeId, 
			int aBehaviorId,
			_BehaviorCallType aCallType)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendBeforeBehaviorCallDry(
    			theTimestamp,
    			aProbeId, 
    			aBehaviorId,
    			aCallType);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logBeforeBehaviorCallDry(th: %d, ts: %d, pid: %d, bid: %d, ct: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aBehaviorId,
				aCallType);

		theThread.pushCall(aBehaviorId);
	}
	
	public void logBeforeBehaviorCall(
			int aProbeId, 
			int aBehaviorId,
			_BehaviorCallType aCallType,
			Object aTarget, 
			Object[] aArguments)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendBeforeBehaviorCall(
    			theTimestamp,
    			aProbeId, 
    			aBehaviorId,
    			aCallType,
    			aTarget,
    			aArguments);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logBeforeBehaviorCall(th: %d, ts: %d, pid: %d, bid: %d, ct: %s, tgt: %s, args: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aBehaviorId,
				aCallType,
				formatObj(aTarget),
				formatArgs(aArguments));

		theThread.pushCall(aBehaviorId);
	}

	public void logAfterBehaviorCallDry()
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	theWriter.sendAfterBehaviorCallDry(theTimestamp);
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logAfterBehaviorCallDry(th: %d, ts: %d)",
				theThread.getId(),
				theTimestamp);
		
		theThread.popCall();		
	}
	
	public void logAfterBehaviorCall(
			int aProbeId, 
			int aBehaviorId, 
			Object aTarget,
			Object aResult)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendAfterBehaviorCall(
    			theTimestamp,
    			aProbeId, 
    			aBehaviorId,
    			aTarget,
    			aResult);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logAfterBehaviorCall(th: %d, ts: %d, pid: %d, bid: %d, tgt: %s, res: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aBehaviorId,
				formatObj(aTarget),
				formatObj(aResult));

		theThread.popCall();
	}
	
	public void logAfterBehaviorCallWithException(
			int aProbeId, 
			int aBehaviorId, 
			Object aTarget, 
			Object aException)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendAfterBehaviorCallWithException(
    			theTimestamp,
    			aProbeId, 
    			aBehaviorId,
    			aTarget,
    			aException);
    	
        theThread.packetEnd();
        
		if (AgentDebugFlags.COLLECTOR_LOG) printf(
				"logAfterBehaviorCallWithException(th: %d, ts: %d, pid: %d, bid: %d, tgt: %s, ex: %s)",
				theThread.getId(),
				theTimestamp,
				aProbeId,
				aBehaviorId,
				formatObj(aTarget),
				aException);

		theThread.popCall();
	}
	
	public void logOutput(
			_Output aOutput, 
			byte[] aData)
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;
		
		ThreadData theThread = getThreadData();
		long theTimestamp = theThread.timestamp();

		if (theThread.isSending()) return;
    	LowLevelEventWriter theWriter = theThread.packetStart(theTimestamp);
    	
    	theWriter.sendOutput(
    			theTimestamp,
    			aOutput,
    			aData);
    	
        theThread.packetEnd();
	}

	/**
	 * Returns the id of the currently called behavior (as registered).
	 */
	public int getCurrentCalledBehavior()
	{
		if (! AgentReady.COLLECTOR_READY) return 0;
		ThreadData theThread = getThreadData();
		
		return theThread != null ? theThread.getCurrentCall() : 0;
	}

	private ThreadData getControlThreadData()
	{
		if (itsControlThreadData == null)
		{
			itsControlThreadData = new ThreadData(-1);
		}
		return itsControlThreadData;
	}
	
	/**
	 * Sends a request to clear the database.
	 */
	public void clear()
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;

		ThreadData theThread = getThreadData();

		if (theThread.isSending()) throw new RuntimeException();
    	LowLevelEventWriter theWriter = theThread.packetStart(0);
    	theWriter.sendClear();
        theThread.packetEnd();
	}
	
	/**
	 * Sends a request to flush buffered events
	 */
	public void flush()
	{
		if (AgentDebugFlags.COLLECTOR_IGNORE_ALL) return;

		ThreadData theThread = getControlThreadData();
		synchronized (theThread)
		{
			if (theThread.isSending()) throw new RuntimeException();
	    	LowLevelEventWriter theWriter = theThread.packetStart(0);
	    	theWriter.sendFlush();
	    	theThread.packetEnd();
		}
	}
	
	/**
	 * Sends {@link Command#CMD_END}
	 */
	public void end()
	{
		ThreadData theThread = getControlThreadData();
		synchronized (theThread)
		{
			if (theThread.isSending()) throw new RuntimeException();
			LowLevelEventWriter theWriter = theThread.packetStart(0);
			theWriter.sendEnd();
			theThread.packetEnd();
		}
	}
	
	/**
	 * Sends {@link Command#DBEV_CAPTURE_ENABLED}
	 */
	public void evCaptureEnabled(boolean aEnabled)
	{
		ThreadData theThread = getControlThreadData();
		synchronized (theThread)
		{
			if (theThread.isSending()) throw new RuntimeException();
			LowLevelEventWriter theWriter = theThread.packetStart(0);
			theWriter.sendEvCaptureEnabled(aEnabled);
			theThread.packetEnd();
		}
	}
	
	private class ThreadData 
	{
		/**
		 * Internal thread id.
		 * These are different than JVM thread ids, which can potentially
		 * use the whole 64 bits range. Internal ids are sequential.
		 */
		private int itsId;
		
		private long itsTimestamp = 0;
		
		/**
		 * Number of events for which the timestamp was approximated.
		 */
		private int itsSeqCount = 0;
		
		/**
		 * This flag permits to avoid reentrancy issues.
		 */
		private boolean itsInCFlow = false;
		
		/**
		 * When this flag is true the next exception generated event
		 * is ignored. This permits to avoid reporting EG events that
		 * are caused by the instrumentation.
		 */
		private boolean itsIgnoreNextException = false;

		private final LowLevelEventWriter itsWriter;
		
		private boolean itsSending = false;
		
		private long itsFirstTimestamp;
		private long itsLastTimestamp;
		
		/**
		 * Our own entry in the LRU list
		 */
		private NakedLinkedList.Entry<ThreadData> itsEntry;
		
		/**
		 * This stack contains the behavior ids of called methods
		 */
		private _IntStack itsCallStack = new _IntStack(128);
		
		public ThreadData(int aId)
		{
			itsId = aId;
			itsWriter = new LowLevelEventWriter(itsIOThread.createBuffer(itsId));
		}
		
		public int getId()
		{
			return itsId;
		}

		/**
		 * This method is called at the beginning of each logXxxx method
		 * to check that we are not already inside event collection code.
		 * 
		 * @return False if we are not top-level
		 */
		public boolean enter()
		{
			if (itsInCFlow) return false;
			
			itsInCFlow = true;
			return true;
		}
		
		public void exit()
		{
			assert itsInCFlow == true;
			itsInCFlow = false;
		}

		/**
		 * Returns an approximate value of the current time.
		 * Ensures that no two successive calls return the same value, so that
		 * all the events of the thread have distinct timestamp values.
		 */
		public long timestamp()
		{
			long ts = Timestamper.t;
			if (ts > itsTimestamp) 
			{
				itsTimestamp = ts;
				itsSeqCount = 0;
			}
			else 
			{
				itsTimestamp += 1;
				itsSeqCount++;
				if (itsSeqCount > 100)
				{
					ts = Timestamper.update();
					if (ts > itsTimestamp) 
					{
						itsTimestamp = ts;
						itsSeqCount = 0;
					}
				}
			}
			
			return itsTimestamp;
		}
		
		/**
		 * Sets the ignore next exception flag.
		 */
		public void ignoreNextException()
		{
			itsIgnoreNextException = true;
		}
		
		/**
		 * Checks if the ignore next exception flag is set, and resets it.
		 */
		public boolean checkIgnoreNextException()
		{
			boolean theIgnoreNext = itsIgnoreNextException;
			itsIgnoreNextException = false;
			return theIgnoreNext;
		}

		public LowLevelEventWriter packetStart(long aTimestamp)
		{
			if (itsSending) throw new RuntimeException();
			itsSending = true;
			
			if (itsFirstTimestamp == 0) itsFirstTimestamp = aTimestamp;
			if (aTimestamp != 0) itsLastTimestamp = aTimestamp;
			
			return itsWriter;
		}
		
		public boolean isSending()
		{
			return itsSending;
		}
		
		public synchronized void packetEnd()
		{
			if (! itsSending) throw new RuntimeException();
			itsSending = false;
		}
		
		public NakedLinkedList.Entry<ThreadData> getEntry()
		{
			return itsEntry;
		}
		
		public void pushCall(int aBehaviorId)
		{
			itsCallStack.push(aBehaviorId);
		}
		
		public void popCall()
		{
			itsCallStack.pop();
		}
		
		public int getCurrentCall()
		{
			return itsCallStack.isEmpty() ? 0 : itsCallStack.peek();
		}
		
//		public void pushEnter()
//		{
//			itsCallStack.push(0);
//			System.out.println("ThreadData.pushEnter()");
//		}
//		
//		public void popEnter()
//		{
//			int v = itsCallStack.pop();
//			if (v != 0) throw new RuntimeException("Call stack error");
//			System.out.println("ThreadData.popEnter()");
//		}
//		
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
