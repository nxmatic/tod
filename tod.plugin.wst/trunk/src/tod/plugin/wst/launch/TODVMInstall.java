/*
 * Created on Dec 24, 2008
 */
package tod.plugin.wst.launch;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstall3;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.LibraryLocation;

import tod.plugin.launch.LaunchUtils;

public class TODVMInstall implements IVMInstall, IVMInstall2, IVMInstall3
{
	private IVMInstall itsDelegate;
	private IVMInstall2 itsDelegate2;
	private IVMInstall3 itsDelegate3;
	
	public TODVMInstall(IVMInstall aDelegate)
	{
		itsDelegate = aDelegate;
		itsDelegate2 = (IVMInstall2) aDelegate;
		itsDelegate3 = (IVMInstall3) aDelegate;
	}		

	public String getJavaVersion()
	{
		return itsDelegate2.getJavaVersion();
	}

	public String getVMArgs()
	{
		return itsDelegate2.getVMArgs();
	}

	public void setVMArgs(String aVmArgs)
	{
		itsDelegate2.setVMArgs(aVmArgs);
	}

	public String getId()
	{
		return itsDelegate.getId();
	}

	public File getInstallLocation()
	{
		return itsDelegate.getInstallLocation();
	}

	public URL getJavadocLocation()
	{
		return itsDelegate.getJavadocLocation();
	}

	public LibraryLocation[] getLibraryLocations()
	{
		return itsDelegate.getLibraryLocations();
	}

	public String getName()
	{
		return itsDelegate.getName();
	}

	public String[] getVMArguments()
	{
		return itsDelegate.getVMArguments();
	}

	public IVMInstallType getVMInstallType()
	{
		return itsDelegate.getVMInstallType();
	}

	public IVMRunner getVMRunner(String aMode)
	{
		return LaunchUtils.getVMRunner(itsDelegate.getVMRunner(aMode));
	}

	public void setInstallLocation(File aInstallLocation)
	{
		itsDelegate.setInstallLocation(aInstallLocation);
	}

	public void setJavadocLocation(URL aUrl)
	{
		itsDelegate.setJavadocLocation(aUrl);
	}

	public void setLibraryLocations(LibraryLocation[] aLocations)
	{
		itsDelegate.setLibraryLocations(aLocations);
	}

	public void setName(String aName)
	{
		itsDelegate.setName(aName);
	}

	public void setVMArguments(String[] aVmArgs)
	{
		itsDelegate.setVMArguments(aVmArgs);
	}

	public Map evaluateSystemProperties(String[] aProperties, IProgressMonitor aMonitor) throws CoreException
	{
		return itsDelegate3.evaluateSystemProperties(aProperties, aMonitor);
	}
	
}