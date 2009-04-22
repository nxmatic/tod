/*
 * Created on Apr 22, 2008
 */
package tod.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.core.IJavaProject;

import tod.core.session.IProgramLaunch;

public class EclipseProgramLaunch implements IProgramLaunch
{
	private final ILaunch itsLaunch;
	private List<IProject> itsProjects = new ArrayList<IProject>();

	public EclipseProgramLaunch(ILaunch aLaunch, IJavaProject aJavaProject)
	{
		itsLaunch = aLaunch;
		itsProjects.add(aJavaProject.getProject());
	}
	
	public EclipseProgramLaunch(ILaunch aLaunch, IProject... aProjects)
	{
		itsLaunch = aLaunch;
		for (IProject theProject : aProjects) itsProjects.add(theProject);
	}

	public ILaunch getLaunch()
	{
		return itsLaunch;
	}
	
	public List<IProject> getProjects()
	{
		return itsProjects;
	}
}
