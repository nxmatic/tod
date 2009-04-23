package tod.plugin.pytod;

import java.io.File;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import tod.Util;
import tod.impl.dbgrid.DBProcessManager;
import zz.eclipse.utils.EclipseUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class PyTODPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "tod.plugin.pytod";

	// The shared instance
	private static PyTODPlugin plugin;
	
	private String itsLibraryPath;
	
	private String itsHunterPath;
	
	/**
	 * The constructor
	 */
	public PyTODPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception 
	{
		super.start(context);
	

		String theDevPath = Util.workspacePath;
		if (theDevPath == null)
		{
			String theBase = getLibraryPath();
			
			DBProcessManager.cp += File.pathSeparator
				+theBase+"/tod-pytod-db.jar"+File.pathSeparator
				+theBase+"/freehep-xdr-2.0.3.jar";
			
			itsHunterPath = theBase+"/pytod-core";
		}
		else
		{
			DBProcessManager.cp += File.pathSeparator
				+theDevPath+"/TOD-pytod-db/bin"+File.pathSeparator
				+theDevPath+"/TOD-pytod-db/lib/freehep-xdr-2.0.3.jar";
			
			itsHunterPath = theDevPath+"/python-project/src";
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception 
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static PyTODPlugin getDefault() 
	{
		return plugin;
	}
	
	public String getLibraryPath()
	{
		if (itsLibraryPath == null)
			itsLibraryPath = EclipseUtils.getLibraryPath(this);
		
		return itsLibraryPath;
	}
	
	/**
	 * Returns the path to the base of the python trace capture scripts.
	 */
	public String getHunterPath()
	{
		return itsHunterPath;
	}

}
