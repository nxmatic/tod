/*
 * Created on Apr 23, 2009
 */
package tod.impl.server;

import java.net.Socket;

import tod.core.config.TODConfig;
import tod.core.server.TODServer;
import zz.utils.properties.IRWProperty;

public class JavaTODServer2 extends TODServer
{
	public JavaTODServer2(TODConfig aConfig)
	{
		super(aConfig);
	}

	@Override
	public IRWProperty<Boolean> pCaptureEnabled()
	{
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	protected void accepted(Socket aSocket)
	{
		throw new RuntimeException("Not yet implemented");
	}

}
