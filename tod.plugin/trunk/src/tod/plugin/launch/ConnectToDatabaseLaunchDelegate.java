/*
 * Created on Jun 14, 2007
 */
package tod.plugin.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;

import tod.core.config.TODConfig;
import tod.core.session.ISession;
import tod.core.session.TODSessionManager;
import tod.impl.dbgrid.RemoteGridSession;
import tod.plugin.TODPlugin;
import tod.plugin.TODPluginUtils;
import tod.plugin.views.main.MainView;

public class ConnectToDatabaseLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate
{
	public void launch(
			ILaunchConfiguration aConfiguration, 
			String aMode, 
			ILaunch aLaunch, 
			IProgressMonitor aMonitor) throws CoreException
	{
		if (aMonitor == null) aMonitor = new NullProgressMonitor();
		
		TODConfig theConfig = TODConfigLaunchTab.readConfig(aConfiguration);
		
		theConfig.set(TODConfig.COLLECTOR_HOST, getAddress(aConfiguration));
		
		MainView theView = TODPluginUtils.getMainView(true);
		
		aMonitor.worked(1);
		IJavaProject theJavaProject = getJavaProject(aConfiguration);
		ISession theSession = new RemoteGridSession(theView.getGUIManager(), null, theConfig, true); 
		theSession.getLaunches().add(new EclipseProgramLaunch(aLaunch, theJavaProject));
		
		TODSessionManager.getInstance().pCurrentSession().set(theSession);

		aMonitor.done();
	}
	
	private static String getAddress(ILaunchConfiguration aConfig)
	{
		try
		{
			return aConfig.getAttribute(ConnectToDatabaseLaunchConstants.DATABASE_ADDRESS, "localhost");
		}
		catch (CoreException e)
		{
			TODPlugin.logError("Cannot read database address", e);
			return null;
		}
	}
	
}
