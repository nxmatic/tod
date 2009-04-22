/*
 * Created on Jun 8, 2007
 */
package tod.plugin.ajdt.launch;

import org.eclipse.ajdt.internal.launching.LTWApplicationLaunchConfigurationDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMRunner;

import tod.plugin.launch.LaunchUtils;
import tod.plugin.launch.TODConfigLaunchTab;


/**
 * Launch delegate for launch type: org.eclipse.jdt.launching.localJavaApplication
 * @author gpothier
 */
public class TODLaunchDelegate_AJDT_LoadTime extends LTWApplicationLaunchConfigurationDelegate
{
	@Override
	public IVMRunner getVMRunner(ILaunchConfiguration aConfiguration, String aMode) throws CoreException
	{
		return LaunchUtils.getVMRunner(super.getVMRunner(aConfiguration, aMode));
	}
	
	@Override
	public void launch(ILaunchConfiguration aConfiguration, String aMode, ILaunch aLaunch, IProgressMonitor aMonitor)
			throws CoreException
	{
		try
		{
			IJavaProject theJavaProject = getJavaProject(aConfiguration);
			if (LaunchUtils.setup(theJavaProject, TODConfigLaunchTab.readConfig(aConfiguration), aLaunch))
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
