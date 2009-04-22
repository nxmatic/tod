/*
 * Created on Feb 4, 2009
 */
package java.tod;

/**
 * Contains native methods. Separate from {@link _AgentConfig} because otherwise we cannot load the
 * class before registering the natives (in agent14).
 * @author gpothier
 */
public class _AgConfig
{
	/**
	 * Retrieves the host id that was sent to the native agent.
	 */
	public static native int getHostId();

	public static native String getCollectorHost();
	public static native String getCollectorPort();
	public static native String getClientName();

}
