/*
 * Created on Dec 18, 2008
 */
package tod.plugin.wst.launch;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.wst.server.core.IServer;

import tod.core.session.IProgramLaunch;
import tod.core.session.ISession;
import tod.plugin.launch.EclipseProgramLaunch;

public class WSTAppLaunch extends EclipseProgramLaunch
{
	private final IServer itsServer;

	public WSTAppLaunch(ILaunch aLaunch, IProject aProject, IServer aServer)
	{
		super(aLaunch, new IProject[] {aProject});
		itsServer = aServer;
	}

	public IServer getServer()
	{
		return itsServer;
	}
	
	public static WSTAppLaunch getAppLaunch(ISession aSession)
	{
		Set<IProgramLaunch> theLaunches = aSession.getLaunches();
		for (IProgramLaunch theLaunch : theLaunches)
		{
			if (theLaunch instanceof WSTAppLaunch)
			{
				WSTAppLaunch theServerLaunch = (WSTAppLaunch) theLaunch;
				return theServerLaunch;
			}
		}
		return null;
	}

}
