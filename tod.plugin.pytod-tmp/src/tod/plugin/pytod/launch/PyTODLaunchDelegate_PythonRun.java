package tod.plugin.pytod.launch;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.AbstractLaunchConfigurationDelegate;
import org.python.pydev.debug.ui.launching.InvalidRunException;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.debug.ui.launching.RegularLaunchConfigurationDelegate;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;

import pytod.core.server.PythonTODServer;
import pytod.core.server.PythonTODServerFactory;
import tod.core.config.TODConfig;
import tod.core.session.ConnectionInfo;
import tod.core.session.ISession;
import tod.plugin.launch.LaunchUtils;
import tod.plugin.launch.TODConfigLaunchTab;

/**
 * This delegate permits to run python programs under TOD.
 * It is modeled after {@link RegularLaunchConfigurationDelegate}
 * @author minostro
 */
public class PyTODLaunchDelegate_PythonRun extends RegularLaunchConfigurationDelegate 
{
	/**
	 * Adapted from {@link AbstractLaunchConfigurationDelegate#preLaunchCheck(ILaunchConfiguration, String, IProgressMonitor)}
	 */
	public IProject getProject(ILaunchConfiguration configuration) throws CoreException 
	{
        String theName = configuration.getAttribute(Constants.ATTR_PROJECT, "");
        
        if (theName.length() > 0)
        {
            return ResourcesPlugin.getWorkspace().getRoot().getProject(theName);
        }
        
        return null;
    }
	

	/**
	 * Adapted from {@link AbstractLaunchConfigurationDelegate}.
	 */
    public void launch(ILaunchConfiguration aConfiguration, String aMode,
			ILaunch aLaunch, IProgressMonitor monitor) throws CoreException
	{
		if (monitor == null)
		{
			monitor = new NullProgressMonitor();
		}

		try
		{
			TODConfig theConfig = TODConfigLaunchTab.readConfig(aConfiguration);
			
			// Force python server
			theConfig.set(TODConfig.SERVER_TYPE, PythonTODServerFactory.class.getName());
			
			IProject[] theProjects = new IProject[] { getProject(aConfiguration) };
			if (!LaunchUtils.setup(theProjects, theConfig, aLaunch)) 
			{ 
				throw new RuntimeException("Could not perform setup."); 
			}

			monitor.beginTask("Preparing configuration", 3);
			ISession theSession = LaunchUtils.getSession();
			ConnectionInfo theConnectionInfo = theSession.getConnectionInfo();
			
			//String theHostName = theConnectionInfo.getHostName();
			//int thePort = theConnectionInfo.getPort();
		
			PythonRunnerConfig runConfig = 
				new PythonRunnerConfig(aConfiguration, aMode, getRunnerConfigRun());

			monitor.worked(1);
			try
			{
				PyTODRunner.run(runConfig, aLaunch, monitor, theConnectionInfo);
			}
			catch (IOException e)
			{
				Log.log(e);
				finishLaunchWithError(aLaunch);
				throw new CoreException(PydevDebugPlugin.makeStatus(
						IStatus.ERROR,
						"Unexpected IO Exception in Pydev debugger", 
						null));
			}
		}
		catch (final InvalidRunException e)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					ErrorDialog.openError(
							PyAction.getShell(),
							"Invalid launch configuration",
							"Unable to make launch because launch configuration is not valid",
							PydevPlugin.makeStatus(IStatus.ERROR, e.getMessage(), e));
				}
			});
			finishLaunchWithError(aLaunch);
		}
		finally
		{
			LaunchUtils.tearDown();
		}
	}
    
    private void finishLaunchWithError(ILaunch launch)
	{
		try
		{
			launch.terminate();

			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunch(launch);
		}
		catch (Throwable x)
		{
			PydevPlugin.log(x);
		}
	}

}
