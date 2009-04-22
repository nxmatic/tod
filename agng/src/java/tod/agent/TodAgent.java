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
package tod.agent;

public class TodAgent
{
	private static boolean itsVmStarted = false;
	private static NativeAgentConfig itsConfig;
	
	private static ExceptionProcessor itsExceptionProcessor;
	private static InstrumentationManager itsInstrumentationManager;
	private static ConnectionManager itsConnectionManager;
	
	private static long itsNextOid = 1;
	
	public static void agVMInit(
			String aPropHost,
			String aPropHostName,
			String aPropPort,
			String aPropCachePath,
			int aPropVerbose)
	{
		System.out.println("[TOD] TodAgent.agVMInit ("+aPropHost+", "+aPropHostName+")");
		itsConfig = new NativeAgentConfig();
		itsConfig.setHost(aPropHost);
		itsConfig.setHostName(aPropHostName);
		itsConfig.setCollectorPort(Integer.parseInt(aPropPort));
		itsConfig.setCachePath(aPropCachePath);
		itsConfig.setVerbosity(aPropVerbose);
		
		String theWorkingSet = System.getProperty("trace-filter");
		if (theWorkingSet!=null)
		{
			itsConfig.setWorkingSet(theWorkingSet);
			System.out.println("[TOD] using local property for the working set");
		}
		else 
			System.out.println("[TOD] using db property for the working set");
		System.out.println("[TOD] Config defined");

		itsConnectionManager = new ConnectionManager(itsConfig);
		itsConnectionManager.connect();
		
		System.out.println("[TOD] Connection done");
		ScopeManager theScopeManager = new ScopeManager(itsConfig);
		itsExceptionProcessor = new ExceptionProcessor(itsConfig, theScopeManager);
		itsInstrumentationManager = new InstrumentationManager(itsConfig, itsConnectionManager, theScopeManager);

	
		
		itsVmStarted = true;
		itsInstrumentationManager.flushTmpTracedMethods();
	
		System.out.println("[TOD] VMStarted");
	}
	
	public static void agOnUnload()
	{
		System.out.println("[TOD] TodAgent.agOnUnload");
		itsConnectionManager.sendFlush();
	}
	
	/**
	 * Transforms the given class.
	 * 
	 * @param aName
	 *            Name of the class
	 * @param aOriginal
	 *            Original bytecode
	 * @return The new bytecode, or null if the class should not be
	 *         instrumented.
	 */
	public static byte[] agClassLoadHook(String aName, byte[] aOriginal)
	{
		return itsInstrumentationManager.instrument(aName, aOriginal);
	}
	
	public static void agExceptionGenerated(
			String aMethodName, 
			String aMethodSignature,
			String aMethodDeclaringClassSignature, 
			int aLocation, 
			Throwable aThrowable)
	{
			itsConfig.log(2,"ExceptionGenerated - start ");
			MethodInfo theMethodInfo = new MethodInfo(aMethodName,aMethodSignature,aMethodDeclaringClassSignature);
			itsExceptionProcessor.agExceptionGenerated( theMethodInfo, aLocation, aThrowable);
			itsConfig.log(2,"ExceptionGenerated - end");
	}
	
	public static boolean isVmStarted()
	{
		return itsVmStarted;
	}
	
	/**
	 * Returns the next object id
	 */
	public synchronized static long agGetNextOid()
	{
		long oid = itsNextOid++;
		
		// Include host id
		oid = (oid << itsConfig.getHostBits()) | itsConfig.getHostId(); 
		
		// We cannot use the 64th bit.
		if (oid >> 63 != 0) 
		{
			System.err.println("OID overflow");
			System.exit(1);
		}

		return oid;
	}
	
	public static int agGetHostId()
	{
		return itsConfig.getHostId();
	}
}
