/*
 * Created on Dec 14, 2007
 */
package tod.agent;

/**
 * Holds the current configuration of the native agent.
 * 
 * @author gpothier
 */
public class NativeAgentConfig
{
	private String itsHost;

	private String itsHostName;

	private int itsCollectorPort;

	private String itsCachePath;
	
	public static String DefaultCachePath = System.getProperty("user.home")+"/tmp/tod";

	private int itsVerbosity;

	private int itsHostId;

	private boolean itsSkipCoreClasses;

	private boolean itsCaptureExceptions;

	private int itsHostBits;

	private String itsWorkingSet;

	private String itsStructDbId;

	private String itsClassCachePrefix;

	public int getVerbosity()
	{
		return itsVerbosity;
	}

	public void setVerbosity(int aVerbosity)
	{
		itsVerbosity = aVerbosity;
	}

	public String getHost()
	{
		return itsHost;
	}

	public void setHost(String aHost)
	{
		itsHost = aHost;
	}

	public String getHostName()
	{
		return itsHostName;
	}

	public void setHostName(String aHostName)
	{
		itsHostName = aHostName;
	}

	public int getCollectorPort()
	{
		return itsCollectorPort;
	}

	public void setCollectorPort(int aCollectorPort)
	{
		itsCollectorPort = aCollectorPort;
	}

	public String getCachePath()
	{
		if (itsCachePath==null) itsCachePath=DefaultCachePath;
		return itsCachePath;
	}

	public void setCachePath(String aCachePath)
	{
		itsCachePath = aCachePath;
	}

	public int getHostId()
	{
		return itsHostId;
	}

	public void setHostId(int aHostId)
	{
		itsHostId = aHostId;
	}

	public boolean getSkipCoreClasses()
	{
		return itsSkipCoreClasses;
	}

	public void setSkipCoreClasses(boolean aSkipCoreClasses)
	{
		itsSkipCoreClasses = aSkipCoreClasses;
	}

	public boolean getCaptureExceptions()
	{
		return itsCaptureExceptions;
	}

	public void setCaptureExceptions(boolean aCaptureExceptions)
	{
		itsCaptureExceptions = aCaptureExceptions;
	}

	public int getHostBits()
	{
		return itsHostBits;
	}

	public void setHostBits(int aHostBits)
	{
		itsHostBits = aHostBits;
	}

	public String getWorkingSet()
	{
		return itsWorkingSet;
	}

	public void setWorkingSet(String aWorkingSet)
	{
		itsWorkingSet = aWorkingSet;
	}

	public String getStructDbId()
	{
		return itsStructDbId;
	}

	public void setStructDbId(String aStructDbId)
	{
		itsStructDbId = aStructDbId;
	}

	public String getClassCachePrefix()
	{
		return itsClassCachePrefix;
	}

	public void setClassCachePrefix(String aClassCachePrefix)
	{
		itsClassCachePrefix = aClassCachePrefix;
	}

	/**
	 * Prints the specified message if the current verbosity level is >= the
	 * specified level.
	 */
	public void log(int aLevel, String aMessage)
	{
		if (getVerbosity() >= aLevel) System.out.println(aMessage);
	}

	public void logf(int aLevel, String aText, Object... aArgs)
	{
		log(aLevel, String.format(aText, aArgs));
	}

}
