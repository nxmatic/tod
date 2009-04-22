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
package tod.impl.bci.asm;



public class ASMSession //extends AbstractSession
{
//	private final ASMDebuggerConfig itsConfig;
//	private final ILogCollector itsCollector;
//	
//	private final ILogBrowser itsBrowser;
//	private final ILocationsRepository itsLocationsRepository;
//	
//	private MyLogReceiver itsLogReceiver;
//	private MyNativePeer itsNativePeer;
//
//	private String itsCachedClassesPath;
//	private File itsCachedLocationsPath;
//
//	public ASMSession(
//			URI aUri, 
//			String aGlobalWorkingSet,
//			String aIdentificationWorkingSet,
//			String aTraceWorkingSet,
//			ILogCollector aCollector,
//			ILogBrowser aBrowser,
//			ILocationsRepository aLocationsRepository)
//	{
//		super(aUri);
//		
//		itsCollector = aCollector;
//		itsBrowser = aBrowser;
//		itsLocationsRepository = aLocationsRepository;
//		
//		if (aUri != null)
//		{
//			File theCachePath = new File(aUri);
//			itsCachedClassesPath = theCachePath.getPath();
//			
//			itsCachedLocationsPath = new File(theCachePath, "loc.dat");
//		}
//		
//		itsConfig = new ASMDebuggerConfig(
//				new PrintThroughCollector(itsCollector),
////				itsCollector,
//				itsCachedLocationsPath, 
//				"[-tod.** -remotebci.** +tod.test.** +tod.demo.**]",
//				"[-java.** -javax.** -sun.** -com.sun.**]");
//
//		try
//		{
//			itsLogReceiver = new MyLogReceiver (itsConfig, new ServerSocket(8058));
//			itsNativePeer = new MyNativePeer(8059);
//		}
//		catch (IOException e)
//		{
//			throw new RuntimeException(e);
//		}
//	}
//	
//	public ILogBrowser getLogBrowser()
//	{
//		return itsBrowser;
//	}
//
//	public ILocationsRepository getLocations()
//	{
//		return itsLocationsRepository;
//	}
//	
//	public void disconnect()
//	{
//		itsNativePeer.interrupt();
//		itsLogReceiver.interrupt();
//	}
//	
//	public String getCachedClassesPath()
//	{
//		return itsCachedClassesPath;
//	}
//
//	private class MyNativePeer extends NativeAgentPeer
//	{
//		private static final int EXCEPTION_GENERATED = 20;
//		private static final byte OBJECT_HASH = 1;
//		private static final byte OBJECT_UID = 2;
//
//		private boolean itsFinished = false;
//		
//		public MyNativePeer(int aPort) throws IOException
//		{
//			super (aPort, true, null, itsConfig.getInstrumenter());
//		}
//
//		@Override
//		protected String cfgCachePath()
//		{
//			return getCachedClassesPath();
//		}
//		
//		@Override
//		protected boolean cfgSkipCoreClasses()
//		{
//			return false;
//		}
//		
//		@Override
//		protected void processExceptionGenerated(
//				long aTimestamp, 
//				long aThreadId,
//				String aClassName,
//				String aMethodName, 
//				String aMethodSignature, 
//				int aBytecodeIndex,
//				Object aException)
//		{
//			int theTypeId = itsConfig.getLocationPool().getTypeId(aClassName);
//			int theBehaviorId = itsConfig.getLocationPool().getBehaviorId(
//					theTypeId, 
//					aMethodName, 
//					aMethodSignature);
//			
//			itsConfig.getCollector().logExceptionGenerated(
//					aTimestamp, 
//					aThreadId, 
//					theBehaviorId, 
//					aBytecodeIndex, 
//					aException);
//		}
//
//		@Override
//		protected boolean accept()
//		{
//			return ! itsFinished;
//		}
//		
//		@Override
//		protected void disconnected()
//		{
//			itsFinished = true;
//			disconnect();
//		}
//	}
//	
//	private class MyLogReceiver extends LogReceiver
//	{
//		private boolean itsFinished = false;
//
//		public MyLogReceiver(ASMDebuggerConfig aConfig, ServerSocket aServerSocket)
//		{
//			super (aConfig.getCollector(), aServerSocket);
//		}
//		
//		@Override
//		protected boolean accept()
//		{
//			return ! itsFinished;
//		}
//		
//		@Override
//		protected void disconnected()
//		{
//			itsFinished = true;
//			disconnect();
//		}
//	}
}