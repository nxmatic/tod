/*
 * Created on Jun 8, 2007
 */
package tod.plugin.launch;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall3;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import tod.agent.AgentUtils;
import tod.core.config.DeploymentConfig;
import tod.core.config.TODConfig;
import tod.core.session.ConnectionInfo;
import tod.core.session.IProgramLaunch;
import tod.core.session.ISession;
import tod.core.session.SessionCreationException;
import tod.core.session.TODSessionManager;
import tod.plugin.TODPlugin;
import tod.plugin.TODPluginUtils;
import tod.plugin.views.main.MainView;
import zz.utils.Utils;

public class LaunchUtils 
{
	private static final ThreadLocal<LaunchInfo> itsInfo = new ThreadLocal<LaunchInfo>();
	
	public static boolean setup(
			IJavaProject aJavaProject,
			TODConfig aConfig, 
			ILaunch aLaunch) throws CoreException
	{
		return setup(aConfig, new EclipseProgramLaunch(aLaunch, new IProject[] {aJavaProject.getProject()}));
	}
	
	public static boolean setup(
			TODConfig aConfig, 
			IProgramLaunch aLaunch) throws CoreException
{
		MainView theView = TODPluginUtils.getMainView(true);
		
		ISession theSession = null;
		try
		{
			theSession = TODSessionManager.getInstance().getSession(theView.getGUIManager(), aConfig, aLaunch);
		}
		catch (Exception e)
		{
			TODPlugin.logError("Could not create session", e);
			handleException(aConfig, e);
		}
		
		itsInfo.set(new LaunchInfo(theSession, aConfig));
		
		return theSession != null;
	}
	
	private static void handleException(TODConfig aConfig, Exception e)
	{
		ConnectException theConnectException = Utils.findAncestorException(ConnectException.class, e);
		if (theConnectException != null) 
		{
			msgConnectionProblem(aConfig);
			return;
		}
		
		SessionCreationException theSessionCreationException = Utils.findAncestorException(SessionCreationException.class, e);
		if (theSessionCreationException != null)
		{
			msgProblem("Cannot create session", e.getMessage());
			return;
		}
		
		throw new RuntimeException(e);
	}
	
	public static void tearDown()
	{
		itsInfo.set(null);
	}
	
	private static void msgConnectionProblem(TODConfig aConfig)
	{
		String theMessage;
		String theSessionType = aConfig.get(TODConfig.SESSION_TYPE);
		
		if (TODConfig.SESSION_REMOTE.equals(theSessionType))
		{
			theMessage = "No debugging session could be created because of a connection " +
			"error. Check that the database host settings are correct, " +
			"and that the database is up and running.";
		}
		else if (TODConfig.SESSION_LOCAL.equals(theSessionType))
		{
			theMessage = "Could not connect to the local database session. " +
					"This could be caused by a timeout error " +
					"if your machine is under load, please retry. " +
					"If the error persists check the Eclipse log.";
		}
		else 
		{
			theMessage = "Undertermined connection problem. " +
					"Check Eclipse log for details.";
		}
		
		msgProblem("Cannot connect", theMessage);
	}
		
	/**
	 * Displays an error message
	 */
	private static void msgProblem(final String aTitle, final String aMessage)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				MessageDialog.openError(theShell, aTitle, aMessage);
			}
		});
	}
	
	/**
	 * Returns the session created by {@link #setup(IJavaProject, ILaunchConfiguration, ILaunch)}
	 * in this thread.
	 */
	public static ISession getSession()
	{
		return itsInfo.get().session;
	}
	
	public static IVMRunner getVMRunner(IVMRunner aDelegate) 
	{
		return new DelegatedRunner(aDelegate, itsInfo.get());
	}
	
	public static String getLaunchMode(ILaunchConfiguration aConfiguration) throws CoreException
	{
		return getAgentCmd(aConfiguration).getLaunchMode();
	}
	
	/**
	 * Determines the version of the native agent library to use for the given
	 * launch configuration.
	 * The architecture of the target VM is first determined by running a small
	 * program in the target VM.
	 */
	protected static AgentCmd getAgentCmd(ILaunchConfiguration aConfiguration) throws CoreException
	{
		// Determine architecture of target VM
		IVMInstall theVMInstall = JavaRuntime.computeVMInstall(aConfiguration);
		String theOs;
		String theArchName;
		String theVersion;
		
		if (theVMInstall instanceof IVMInstall3)
		{
			IVMInstall3 theInstall3 = (IVMInstall3) theVMInstall;
			Map theMap = theInstall3.evaluateSystemProperties(new String[] {"os.name", "os.arch", "java.version"}, null);
			theOs = (String) theMap.get("os.name");
			theArchName = (String) theMap.get("os.arch");
			theVersion = (String) theMap.get("java.version");
		}
		else
		{
			theOs = System.getProperty("os.name");
			theArchName = System.getProperty("os.arch");
			theVersion = System.getProperty("java.version");
		}
		
		// Parse the version
        String theAgentVersion = null;

        int theMinor = AgentUtils.getJvmMinorVersion(theVersion);
        
        if (theMinor < 4) throw new RuntimeException("JVM version not supported: "+theVersion);
        else if (theMinor == 4) theAgentVersion = "14";
        else theAgentVersion = "15";
        
        // Determine architecture & assemble the agent name.
        Arch theArch = null;
        String theAgentName = DeploymentConfig.getNativeAgentName()+theAgentVersion;
        
		if (theOs.startsWith("Windows")) theArch = Arch.WIN32;
		else if (theOs.startsWith("Mac OS X")) theArch = Arch.MACOS;
		else if (theOs.startsWith("Linux"))
		{
			if (theArchName.equals("x86") || theArchName.equals("i386") || theArchName.equals("i686"))
			{
				theArch = Arch.LINUX;
			}
			else if (theArchName.equals("x86_64") || theArchName.equals("amd64"))
			{
				theArch = Arch.LINUX;
				theAgentName += "_x64";
			}
		}
		
		if (theArch == null)
		{
			throw new RuntimeException("Unsupported architecture: "+theOs+"/"+theArchName);
		}

		return theMinor == 4 ? 
				new Agent14Cmd(theArch, theAgentName) 
				: new Agent15Cmd(theArch, theAgentName);
	}
	
	private static class LaunchInfo
	{
		public final ISession session;
		public final TODConfig config;
		
		public LaunchInfo(ISession aSession, TODConfig aConfig)
		{
			session = aSession;
			config = aConfig;
		}
		
	}


	private static class DelegatedRunner implements IVMRunner
	{
		private IVMRunner itsDelegate;
		private LaunchInfo itsInfo;
		
		public DelegatedRunner(IVMRunner aDelegate, LaunchInfo aInfo)
		{
			itsDelegate = aDelegate;
			itsInfo = aInfo;
		}

		public void run(
				VMRunnerConfiguration aConfiguration, 
				ILaunch aLaunch, 
				IProgressMonitor aMonitor)
				throws CoreException
		{
			if (aMonitor == null) aMonitor = new NullProgressMonitor();
			if (aMonitor.isCanceled()) return;
			
			// Determine which version of the agent to use.
			aMonitor.subTask("Determining architecture of target JVM");
	        AgentCmd theCmd = getAgentCmd(aLaunch.getLaunchConfiguration());
			if (aMonitor.isCanceled()) return;

			// Setup JVM args
			aMonitor.subTask("Setting up extra JVM arguments");
			List<String> theVMArgs = theCmd.getVMArgs(itsInfo);
			if (aConfiguration.getVMArguments() != null) Utils.fillCollection(theVMArgs, aConfiguration.getVMArguments());
			aConfiguration.setVMArguments(theVMArgs.toArray(new String[theVMArgs.size()]));
			
			// Setup BootClassPath
			aMonitor.subTask("Setting up bootclasspath");
			List<String> theBootClassPath = theCmd.getBootClassPath(itsInfo);
			Map theVMSpecific = aConfiguration.getVMSpecificAttributesMap();
			if (theVMSpecific == null)
			{
				theVMSpecific = new HashMap();
				aConfiguration.setVMSpecificAttributesMap(theVMSpecific);
			}
			
			String[] theCurrentCP = (String[]) theVMSpecific.get(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND);
			if (theCurrentCP != null) Utils.fillCollection(theBootClassPath, theCurrentCP);
			
			theVMSpecific.put(
					IJavaLaunchConfigurationConstants.ATTR_BOOTPATH_PREPEND, 
					theBootClassPath.toArray(new String[theBootClassPath.size()]));

			if (aMonitor.isCanceled()) return;
			
			// Setup environment
			aMonitor.subTask("Setting up environment");
			
			if (theCmd.mustSetLibPath())
			{
				String[] theEnv;
				if (aConfiguration.getEnvironment() != null) 
				{
					// TODO: See what we shoud do in this case
					throw new UnsupportedOperationException("Not yet");
				}
				else
				{
					// If no environment has been set, we must explicitly
					// forward the native environment.
					Map<String, String> theNativeEnv = DebugPlugin.getDefault().getLaunchManager().getNativeEnvironmentCasePreserved();
					
					String thePath = theNativeEnv.get(theCmd.getArch().getLibPathEnvKey());
					thePath = thePath == null ? 
							theCmd.getNativeLibraryPath()
							: theCmd.getNativeLibraryPath()+theCmd.getArch().getLibPathSeparator()+thePath;
					theNativeEnv.put(theCmd.getArch().getLibPathEnvKey(), thePath);
					
					theEnv = new String[theNativeEnv.size()];
					int i=0;
					for (Map.Entry<String, String> theEntry : theNativeEnv.entrySet())
					{
						theEnv[i++] = theEntry.getKey()+"="+theEntry.getValue();
					}
				}
				aConfiguration.setEnvironment(theEnv);
			}
			
			if (aMonitor.isCanceled()) return;
			
			itsDelegate.run(aConfiguration, aLaunch, aMonitor);
		}
	}
	
	/**
	 * Abstract base class for the launch command builder.
	 * One subclass per JDK variant.
	 * @author gpothier
	 */
	private static abstract class AgentCmd
	{
		private final Arch itsArch;
		private final String itsLibraryName;
		
		private final String itsJavaLibraryPath;
		private final String itsNativeLibraryPath;
		
		public AgentCmd(Arch aArch, String aLibraryName)
		{
			itsArch = aArch;
			itsLibraryName = aLibraryName;

			itsJavaLibraryPath = TODPlugin.getDefault().getLibraryPath();
			itsNativeLibraryPath = System.getProperty("bcilib.path", itsJavaLibraryPath);
		}

		public Arch getArch()
		{
			return itsArch;
		}

		public String getLibraryName()
		{
			return itsLibraryName;
		}

		public String getJavaLibraryPath()
		{
			return itsJavaLibraryPath;
		}

		public String getNativeLibraryPath()
		{
			return itsNativeLibraryPath;
		}

		public abstract List<String> getVMArgs(LaunchInfo aInfo);
		public abstract List<String> getBootClassPath(LaunchInfo aInfo);
		public abstract String getLaunchMode();
		public abstract boolean mustSetLibPath();
	}
	
	/**
	 * Launch command builder for JDK1.4
	 * @author gpothier
	 */
	private static class Agent14Cmd extends AgentCmd
	{
		public Agent14Cmd(Arch aArch, String aLibraryName)
		{
			super(aArch, aLibraryName);
		}

		@Override
		public List<String> getVMArgs(LaunchInfo aInfo)
		{
			List<String> theVMArgs = new ArrayList<String>();
			
			theVMArgs.add("-noverify");

			TODConfig theConfig = aInfo.config;
			ConnectionInfo theConnectionInfo = aInfo.session.getConnectionInfo();
			
			theVMArgs.add("-Xdebug");
			theVMArgs.add("-Xnoagent");
			
			String theAgentArgs = String.format(
					"%d,%s,%d,%s",
					theConfig.get(TODConfig.AGENT_VERBOSE),
					theConnectionInfo.getHostName(),
					theConnectionInfo.getPort(),
					theConfig.get(TODConfig.CLIENT_NAME));
			
			theVMArgs.add("-Xrun"+getLibraryName()+":"+theAgentArgs);
			
			theVMArgs.add("-Dcollector-host="+theConnectionInfo.getHostName());
			theVMArgs.add("-Dcollector-port="+theConnectionInfo.getPort());
			
			theVMArgs.add(TODConfig.CLIENT_NAME.javaOpt(theConfig));
			theVMArgs.add(TODConfig.AGENT_VERBOSE.javaOpt(theConfig));
			theVMArgs.add(TODConfig.AGENT_CAPTURE_AT_START.javaOpt(theConfig));
			
			return theVMArgs;
		}

		@Override
		public List<String> getBootClassPath(LaunchInfo aInfo)
		{
			List<String> theClassPath = new ArrayList<String>();
			
			String theAgentPath = System.getProperty("agent14.path", getJavaLibraryPath()+"/tod-agent14.jar");
			theClassPath.add(theAgentPath);
	        
			return theClassPath;
		}

		@Override
		public String getLaunchMode()
		{
			return ILaunchManager.RUN_MODE;
		}
		
		@Override
		public boolean mustSetLibPath()
		{
			return true;
		}
	}

	/**
	 * Launch command builder for JDK1.5 and later
	 * @author gpothier
	 */
	private static class Agent15Cmd extends AgentCmd
	{
		public Agent15Cmd(Arch aArch, String aLibraryName)
		{
			super(aArch, aLibraryName);
		}

		@Override
		public List<String> getVMArgs(LaunchInfo aInfo)
		{
			List<String> theVMArgs = new ArrayList<String>();

			theVMArgs.add("-noverify");
			
			String theNativeAgentPath = getNativeLibraryPath()+File.separator+getArch().getLibraryFileName(getLibraryName());
			theVMArgs.add("-agentpath:"+theNativeAgentPath);
			
			// Config
			TODConfig theConfig = aInfo.config;
			ConnectionInfo theConnectionInfo = aInfo.session.getConnectionInfo();
			
			theVMArgs.add("-Dcollector-host="+theConnectionInfo.getHostName());
			theVMArgs.add("-Dcollector-port="+theConnectionInfo.getPort());
			
			theVMArgs.add(TODConfig.CLIENT_NAME.javaOpt(theConfig));
			theVMArgs.add(TODConfig.AGENT_VERBOSE.javaOpt(theConfig));
			theVMArgs.add(TODConfig.AGENT_CAPTURE_AT_START.javaOpt(theConfig));
			
			return theVMArgs;
		}

		@Override
		public List<String> getBootClassPath(LaunchInfo aInfo)
		{
			List<String> theClassPath = new ArrayList<String>();
			
			String theAgentPath = System.getProperty("agent15.path", getJavaLibraryPath()+"/tod-agent15.jar");
			theClassPath.add(theAgentPath);
	        
			return theClassPath;
		}
		
		@Override
		public String getLaunchMode()
		{
			return ILaunchManager.DEBUG_MODE;
		}
		
		@Override
		public boolean mustSetLibPath()
		{
			return false;
		}
	}
	
	/**
	 * Represents a deployment architecture (win32, macos, linux).
	 * @author gpothier
	 */
	private static class Arch
	{
		public static final Arch MACOS = new Arch("lib", ".dylib", null, null);
		public static final Arch WIN32 = new Arch("", ".dll", "PATH", ";");
		public static final Arch LINUX = new Arch("lib", ".so", "LD_LIBRARY_PATH", ":");
		
		private final String itsLibNamePrefix;
		private final String itsLibNameSuffix;
		private final String itsLibPathEnvKey;
		private final String itsLibPathSeparator;
		
		public Arch(
				String aLibNamePrefix,
				String aLibNameSuffix,
				String aLibPathEnvKey,
				String aLibPathSeparator)
		{
			itsLibNamePrefix = aLibNamePrefix;
			itsLibNameSuffix = aLibNameSuffix;
			itsLibPathEnvKey = aLibPathEnvKey;
			itsLibPathSeparator = aLibPathSeparator;
		}

		/**
		 * Returns the filename for a library on this architecture.
		 */
		public String getLibraryFileName(String aBaseName)
		{
			return itsLibNamePrefix+aBaseName+itsLibNameSuffix;
		}
		
		public String getLibPathEnvKey()
		{
			return itsLibPathEnvKey;
		}
		
		public String getLibPathSeparator()
		{
			return itsLibPathSeparator;
		}
	}
}
