///*
//TOD - Trace Oriented Debugger.
//Copyright (c) 2006-2008, Guillaume Pothier
//All rights reserved.
//
//This program is free software; you can redistribute it and/or 
//modify it under the terms of the GNU General Public License 
//version 2 as published by the Free Software Foundation.
//
//This program is distributed in the hope that it will be useful, 
//but WITHOUT ANY WARRANTY; without even the implied warranty of 
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
//General Public License for more details.
//
//You should have received a copy of the GNU General Public License 
//along with this program; if not, write to the Free Software 
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
//MA 02111-1307 USA
//
//Parts of this work rely on the MD5 algorithm "derived from the 
//RSA Data Security, Inc. MD5 Message-Digest Algorithm".
//*/
//package tod.impl.dbgrid;
//
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.PrintStream;
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import tod.Util;
//import tod.core.config.DeploymentConfig;
//import tod.core.config.TODConfig;
//import tod.utils.TODUtils;
//import zz.utils.StreamPipe;
//import zz.utils.srpc.RIRegistry;
//
///**
// * Manages (launches, monitors and controls) an external database process.
// * 
// * @author gpothier
// */
//public class DBProcessManager
//{
//	private static DBProcessManager itsDefault;
//
//	/**
//	 * Returns a default instance of {@link DBProcessManager}.
//	 */
//	public static DBProcessManager getDefault()
//	{
//		try
//		{
//			if (itsDefault == null)
//			{
//				itsDefault = new DBProcessManager(new TODConfig());
//				FileOutputStream theLogStrean = new FileOutputStream("db.log");
//				itsDefault.addOutputStream(theLogStrean);
//				itsDefault.addErrorStream(theLogStrean);
//			}
//			return itsDefault;
//		}
//		catch (FileNotFoundException e)
//		{
//			throw new RuntimeException(e);
//		}
//	}
//
//	/**
//	 * Classpath to use to launch the database. This is public because
//	 * the plugin sets it when initialized.
//	 */
//	public static String cp = ".";// + File.pathSeparator +
//	// System.getenv("classpath");
//	public static String lib = ".";
//
//	private TODConfig itsConfig;
//	private Process itsProcess;
//	private RIGridMaster itsMaster;
//
//	private List<IDBProcessListener> itsListeners = new ArrayList<IDBProcessListener>();
//
//	private List<PrintStream> itsOutputPrintStreams = new ArrayList<PrintStream>();
//	private List<PrintStream> itsErrorPrintStreams = new ArrayList<PrintStream>();
//	private Map<OutputStream, PrintStream> itsOutputStreams =
//			new HashMap<OutputStream, PrintStream>();
//	private Map<OutputStream, PrintStream> itsErrorStreams =
//			new HashMap<OutputStream, PrintStream>();
//
//	private KeepAliveThread itsKeepAliveThread;
//	private boolean itsAlive = false;
//
//	private Thread itsShutdownHook = new Thread("Shutdown hook (DBProcessManager)")
//	{
//		@Override
//		public void run()
//		{
//			if (itsProcess != null) itsProcess.destroy();
//			itsProcess = null;
//		}
//	};
//
//	public DBProcessManager(TODConfig aConfig)
//	{
//		itsConfig = aConfig;
//		Runtime.getRuntime().addShutdownHook(itsShutdownHook);
//	}
//
//	@Override
//	protected void finalize() throws Throwable
//	{
//		Runtime.getRuntime().removeShutdownHook(itsShutdownHook);
//		itsShutdownHook.run();
//	}
//
//	public void addListener(IDBProcessListener aListener)
//	{
//		itsListeners.add(aListener);
//	}
//
//	public void removeListener(IDBProcessListener aListener)
//	{
//		itsListeners.remove(aListener);
//	}
//
//	protected void fireStarted()
//	{
//		for (IDBProcessListener theListener : itsListeners)
//		{
//			theListener.started();
//		}
//	}
//
//	protected void fireStopped()
//	{
//		for (IDBProcessListener theListener : itsListeners)
//		{
//			theListener.stopped();
//		}
//	}
//
//	public TODConfig getConfig()
//	{
//		return itsConfig;
//	}
//
//	/**
//	 * Sets the configuration for subsequent launches.
//	 * 
//	 * @param aConfig
//	 */
//	public void setConfig(TODConfig aConfig)
//	{
//		itsConfig = aConfig;
//	}
//
//	/**
//	 * Adds a stream that will receive the output from the database process.
//	 * 
//	 * @see StreamPipe
//	 */
//	public void addOutputStream(OutputStream aStream)
//	{
//		PrintStream thePrintStream;
//		if (aStream instanceof PrintStream) thePrintStream = (PrintStream) aStream;
//		else
//		{
//			thePrintStream = new PrintStream(aStream);
//			itsOutputStreams.put(aStream, thePrintStream);
//		}
//		itsOutputPrintStreams.add(thePrintStream);
//	}
//
//	/**
//	 * Removes a stream previously added with
//	 * {@link #addOutputStream(PrintStream)}
//	 */
//	public void removeOutputStream(OutputStream aStream)
//	{
//		PrintStream thePrintStream = itsOutputStreams.remove(aStream);
//		itsOutputPrintStreams.remove(thePrintStream);
//	}
//
//	/**
//	 * Adds a stream that will receive the error output from the database
//	 * process.
//	 * 
//	 * @see StreamPipe
//	 */
//	public void addErrorStream(OutputStream aStream)
//	{
//		PrintStream thePrintStream;
//		if (aStream instanceof PrintStream) thePrintStream = (PrintStream) aStream;
//		else
//		{
//			thePrintStream = new PrintStream(aStream);
//			itsErrorStreams.put(aStream, thePrintStream);
//		}
//		itsErrorPrintStreams.add(thePrintStream);
//	}
//
//	/**
//	 * Removes a stream previously added with
//	 * {@link #addErrorStream(PrintStream)}
//	 */
//	public void removeErrorStream(OutputStream aStream)
//	{
//		PrintStream thePrintStream = itsErrorStreams.remove(aStream);
//		itsErrorPrintStreams.remove(thePrintStream);
//	}
//
//	/**
//	 * Returns the {@link RIGridMaster} representing the database.
//	 */
//	public RIGridMaster getMaster()
//	{
//		return itsMaster;
//	}
//
//	private String getJavaExecutable()
//	{
//		String theOs = System.getProperty("os.name");
//
//		String theJVM = System.getProperty("java.home") + "/bin/java";
//		if (theOs.contains("Windows")) theJVM += "w.exe";
//
//		return theJVM;
//	}
//
//	private void createProcess()
//	{
//		try
//		{
//			if (itsKeepAliveThread != null) itsKeepAliveThread.kill();
//
//			if (itsProcess != null) 
//			{
//				itsProcess.destroy();
//				Thread.sleep(500);
//			}
//			printOutput("--- Preparing...");
//			boolean theJDWP = false;
//
//			Long theHeapSize = getConfig().get(TODConfig.LOCAL_SESSION_HEAP);
//			String theJVM = getJavaExecutable();
//			ProcessBuilder theBuilder =
//					new ProcessBuilder(
//							theJVM,
//							"-Xmx" + theHeapSize,
//							"-ea",
//							"-Djava.library.path=" + lib,
//							"-cp",
//							cp,
//							"-Dmaster-host=localhost",
//							"-Dpage-buffer-size=" + (theHeapSize / 2),
//							TODConfig.MASTER_TIMEOUT.javaOpt(10),
//							TODConfig.AGENT_VERBOSE.javaOpt(getConfig()),
//							TODConfig.SCOPE_TRACE_FILTER.javaOpt(getConfig()),
//							TODConfig.CLIENT_NAME.javaOpt(getConfig()),
//							TODConfig.COLLECTOR_PORT.javaOpt(getConfig()),
//							TODConfig.WITH_ASPECTS.javaOpt(getConfig()),
//							TODConfig.WITH_BYTECODE.javaOpt(getConfig()),
//							TODConfig.SERVER_TYPE.javaOpt(getConfig()),
//							"-Ddb.scope.check=" + DeploymentConfig.getDbScopeCheck(),
//							// theJDWP ?
//							// "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
//							// : "",
//							"tod.impl.dbgrid.GridMaster",
//							"0");
//
//			StringBuilder theCommand = new StringBuilder();
//			for (String theArg : theBuilder.command())
//			{
//				theCommand.append('"');
//				theCommand.append(theArg);
//				theCommand.append("\" ");
//			}
//			System.out.println("[DBProcessManager] Command: " + theCommand);
//
//			theBuilder.redirectErrorStream(false);
//
//			setAlive(true);
//			printOutput("--- Starting process...");
//			printOutput("Classpath: " + cp);
//			itsProcess = theBuilder.start();
//			ProcessOutWatcher theWatcher = new ProcessOutWatcher(itsProcess.getInputStream());
//			ProcessErrGrabber theGrabber = new ProcessErrGrabber(itsProcess.getErrorStream());
//
//			printOutput("--- Waiting grid master...");
//			boolean theReady = theWatcher.waitReady();
//
//			if (!theReady) { throw new RuntimeException("Could not start event database:\n--\n"
//					+ theGrabber.getText() + "\n--"); }
//
//			theGrabber.stopCapture();
//			printOutput("--- Ready.");
//			
//			RIRegistry theRegistry = Util.getRemoteSRPCRegistry("localhost", Util.TOD_SRPC_PORT);
//			itsMaster = (RIGridMaster) theRegistry.lookup(GridMaster.SRPC_ID);
//			itsMaster.setConfig(getConfig());
//
//			itsKeepAliveThread = new KeepAliveThread(this);
//		}
//		catch (Exception e)
//		{
//			throw new RuntimeException(e);
//		}
//	}
//
//	/**
//	 * Starts the database process and creates the grid master. This method
//	 * waits until the grid master is ready, or some failure occurs (in which
//	 * case it throws a {@link RuntimeException}).
//	 */
//	public void start()
//	{
//		createProcess();
//	}
//
//	/**
//	 * Stops the database process.
//	 */
//	public void stop()
//	{
//		if (itsProcess != null) itsProcess.destroy();
//		itsProcess = null;
//	}
//
//	/**
//	 * Whether the process is alive.
//	 */
//	public boolean isAlive()
//	{
//		return itsAlive;
//	}
//
//	public void setAlive(boolean aAlive)
//	{
//		if (aAlive != itsAlive)
//		{
//			itsAlive = aAlive;
//			if (itsAlive) fireStarted();
//			else fireStopped();
//		}
//	}
//
//	private void printOutput(String aString)
//	{
//		for (PrintStream theStream : itsOutputPrintStreams)
//		{
//			theStream.println(aString);
//		}
//	}
//
//	private void printError(String aString)
//	{
//		for (PrintStream theStream : itsErrorPrintStreams)
//		{
//			theStream.println(aString);
//		}
//	}
//
//	/**
//	 * A thread that monitors the JVM process' output stream
//	 * 
//	 * @author gpothier
//	 */
//	private class ProcessOutWatcher extends Thread
//	{
//		private InputStream itsStream;
//		private boolean itsReady = false;
//		private CountDownLatch itsLatch = new CountDownLatch(1);
//
//		public ProcessOutWatcher(InputStream aStream)
//		{
//			super("LocalGridSession - Output Watcher");
//			itsStream = aStream;
//			start();
//		}
//
//		@Override
//		public void run()
//		{
//			try
//			{
//				BufferedReader theReader = new BufferedReader(new InputStreamReader(itsStream));
//				while (true)
//				{
//					String theLine = theReader.readLine();
//					if (theLine == null) 
//					{
//						itsLatch.countDown();
//						break;
//					}
//
//					printOutput(theLine);
////					System.out.println("[GridMaster process] "+theLine);
//
//					if (theLine.startsWith(GridMaster.READY_STRING))
//					{
//						itsReady = true;
//						itsLatch.countDown();
//						System.out.println("[DBProcessManager] GridMaster process ready.");
//					}
//				}
//			}
//			catch (IOException e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//
//		/**
//		 * Waits until the Grid master is ready, or a timeout occurs
//		 * 
//		 * @return Whether the grid master is ready.
//		 */
//		public boolean waitReady()
//		{
//			try
//			{
//				itsLatch.await(itsConfig.get(TODConfig.DB_PROCESS_TIMEOUT), TimeUnit.SECONDS);
//				interrupt();
//				return itsReady;
//			}
//			catch (InterruptedException e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//	}
//
//	private class ProcessErrGrabber extends Thread
//	{
//		private InputStream itsStream;
//		private StringBuilder itsBuilder = new StringBuilder();
//
//		public ProcessErrGrabber(InputStream aStream)
//		{
//			super("LocalGridSession - Error grabber");
//			itsStream = aStream;
//			start();
//		}
//
//		/**
//		 * Stops capturing output, and prints it instead.
//		 */
//		public void stopCapture()
//		{
//			itsBuilder = null;
//		}
//
//		@Override
//		public void run()
//		{
//			try
//			{
//				BufferedReader theReader = new BufferedReader(new InputStreamReader(itsStream));
//				while (true)
//				{
//					String theLine = theReader.readLine();
//					if (theLine == null) break;
//
//					printError(theLine);
//					System.err.println("[GridMaster process] " + theLine);
//
//					StringBuilder theBuilder = itsBuilder; // To avoid
//					// concurrency
//					// issues
//					if (theBuilder != null) itsBuilder.append("> " + theLine + "\n");
//				}
//			}
//			catch (IOException e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//
//		public String getText()
//		{
//			return itsBuilder.toString();
//		}
//	}
//
//	private static class KeepAliveThread extends Thread
//	{
//		private WeakReference<DBProcessManager> itsManager;
//
//		public KeepAliveThread(DBProcessManager aManager)
//		{
//			super("KeepAliveThread");
//			itsManager = new WeakReference<DBProcessManager>(aManager);
//			setDaemon(true);
//			start();
//		}
//
//		public synchronized void kill()
//		{
//			itsManager = null;
//		}
//
//		@Override
//		public synchronized void run()
//		{
//			try
//			{
//				TODUtils.log(0, "Starting keepalive thread");
//
//				while (itsManager != null)
//				{
//					DBProcessManager theManager = itsManager.get();
//					if (theManager == null)
//					{
//						TODUtils.log(0, "DBProcessManager was garbage collected, stopping keepalive thread");
//						break;
//					}
//
//					boolean theAlive = false;
//					for (int i=0;i<3;i++)
//					{
//						RIGridMaster theMaster = theManager.getMaster();
//						try
//						{
//							if (theMaster != null)
//							{
//								theMaster.keepAlive();
//								theAlive = true;
//							}
//							break; // we continue the loop only if an exception occurred.
//						}
//						catch (Exception e)
//						{
//							System.err.println("[KeepAliveThread] Error in try #"+(i+1));
//							e.printStackTrace();
//							Thread.sleep(500);
//						}
//					}
//					
//					if (! theAlive) theManager.itsMaster = null;
//
//					theManager.setAlive(theAlive);
//
//					theManager = null; // We don't want to prevent GC
//					wait(2000);
//				}
//
//				TODUtils.log(0, "Stopped keepalive thread");
//			}
//			catch (InterruptedException e)
//			{
//				throw new RuntimeException(e);
//			}
//		}
//	}
//
//	/**
//	 * A listener of the state of the process.
//	 * 
//	 * @author gpothier
//	 */
//	public interface IDBProcessListener
//	{
//		public void started();
//
//		public void stopped();
//	}
//
//}
