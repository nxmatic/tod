/*
 * Created on Dec 24, 2008
 */
package tod.plugin.wst.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jst.server.generic.core.internal.GenericServerLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import tod.plugin.launch.LaunchUtils;
import tod.plugin.launch.TODConfigLaunchTab;

public class TODLaunchDelegate_GenericServer extends GenericServerLaunchConfigurationDelegate
{
	@Override
	public IVMRunner getVMRunner(ILaunchConfiguration aConfiguration, String aMode) throws CoreException
	{
		return LaunchUtils.getVMRunner(super.getVMRunner(aConfiguration, aMode));
	}
	
	@Override
	public IVMInstall verifyVMInstall(ILaunchConfiguration aConfiguration) throws CoreException
	{
		return new TODVMInstall(super.verifyVMInstall(aConfiguration));
	}
	
	@Override
	public void launch(ILaunchConfiguration aConfiguration, String aMode, ILaunch aLaunch, IProgressMonitor aMonitor)
			throws CoreException
	{
		try
		{
			IServer theServer = ServerUtil.getServer(aConfiguration);
			WSTServerLaunch theLaunch = new WSTServerLaunch(aLaunch, theServer);
			if (LaunchUtils.setup(TODConfigLaunchTab.readConfig(aConfiguration), theLaunch))
			{
				super.launch(aConfiguration, LaunchUtils.getLaunchMode(aConfiguration), aLaunch, aMonitor);
			}
		}
		finally
		{
			LaunchUtils.tearDown();
		}
	}
}
