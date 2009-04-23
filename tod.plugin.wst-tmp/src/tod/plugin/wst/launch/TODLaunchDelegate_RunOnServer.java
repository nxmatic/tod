/*
 * Created on Dec 18, 2008
 */
package tod.plugin.wst.launch;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.model.ModuleArtifactDelegate;
import org.eclipse.wst.server.ui.internal.actions.RunOnServerLaunchConfigurationDelegate;

import tod.core.session.IProgramLaunch;
import tod.core.session.ISession;
import tod.core.session.TODSessionManager;
import tod.plugin.launch.EclipseProgramLaunch;

public class TODLaunchDelegate_RunOnServer extends RunOnServerLaunchConfigurationDelegate
{
	@Override
	public void launch(ILaunchConfiguration aConfiguration, String aMode, ILaunch aLaunch, IProgressMonitor aMonitor) 
			throws CoreException
	{
		ISession theSession = TODSessionManager.getInstance().pCurrentSession().get();
		WSTServerLaunch theServerLaunch = WSTServerLaunch.getServerLaunch(theSession);
		
		String theServerId = aConfiguration.getAttribute(ATTR_SERVER_ID, (String)null);
		IServer theServer = ServerCore.findServer(theServerId);
		
		if (theServer != theServerLaunch.getServer()) 
			throw new RuntimeException("Trying to launch on a server that is not currently being debugged by TOD");

		
		// Find projects
		String moduleArt = aConfiguration.getAttribute(ATTR_MODULE_ARTIFACT, (String)null);
		String moduleArtifactClass = aConfiguration.getAttribute(ATTR_MODULE_ARTIFACT_CLASS, (String)null);
		
		IModule module = null;
		ModuleArtifactDelegate moduleArtifact = null;
		
		try 
		{
			Class c = Class.forName(moduleArtifactClass);
			moduleArtifact = (ModuleArtifactDelegate) c.newInstance();
			moduleArtifact.deserialize(moduleArt);
			module = moduleArtifact.getModule();
		} 
		catch (Throwable t) 
		{
			throw new RuntimeException("Could not load module artifact delegate class");
		}
		

		WSTAppLaunch theLaunch = new WSTAppLaunch(aLaunch, module.getProject(), theServer);
		theSession.getLaunches().add(theLaunch);
		
		super.launch(aConfiguration, aMode, aLaunch, aMonitor);
	}
	
}
