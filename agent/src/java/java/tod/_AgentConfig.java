/*
 * Created on Sep 22, 2008
 */
package java.tod;

import java.tod.io._IO;

import tod.agent.AgentUtils;

/**
 * Agent configuration data that other parts of TOD should not attempt
 * to access as native methods are called.
 * @author gpothier
 */
public class _AgentConfig
{
	/**
	 * True when the JVM is version 1.4
	 */
	public static final boolean JAVA14;
	
	static
	{
		int theMinor = AgentUtils.getJvmMinorVersion();
		if (theMinor < 4) throw new RuntimeException("Unsupported VM version");
		JAVA14 = theMinor == 4;
		
		if (JAVA14) _IO.err("[TOD] Using Java 1.4 compatibility mode.");
	}
	

	/**
	 * The host id assigned by the database to this host.
	 */
	public static final int HOST_ID = _AgConfig.getHostId();
	

}
