/*
 * Created on Apr 23, 2009
 */
package tod.impl.server;

import tod.core.ILogCollector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.server.ITODServerFactory;
import tod.core.server.TODServer;
import tod.impl.bci.asm.ASMDebuggerConfig;
import tod.impl.bci.asm2.ASMInstrumenter2;

public class JavaTODServerFactory2 implements ITODServerFactory
{
	public TODServer create(TODConfig aConfig, IMutableStructureDatabase aStructureDatabase, ILogCollector aLogCollector)
	{
		ASMInstrumenter2 theInstrumenter = new ASMInstrumenter2();
		return new JavaTODServer2(aConfig);
	}
}
