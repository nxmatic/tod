/*
 * Created on Dec 13, 2008
 */
package tod.plugin.wst.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

import tod.plugin.launch.TODConfigLaunchTab;

public class TODLaunchTabGroup_Tomcat extends AbstractLaunchConfigurationTabGroup {
	/*
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[7];
		tabs[0] = new ServerLaunchConfigurationTab(new String[] { "org.eclipse.jst.server.tomcat" });
		tabs[0].setLaunchConfigurationDialog(dialog);
		tabs[1] = new JavaArgumentsTab();
		tabs[1].setLaunchConfigurationDialog(dialog);
		tabs[2] = new TODConfigLaunchTab();
		tabs[2].setLaunchConfigurationDialog(dialog);
		tabs[3] = new JavaClasspathTab();
		tabs[3].setLaunchConfigurationDialog(dialog);
		tabs[4] = new SourceLookupTab();
		tabs[4].setLaunchConfigurationDialog(dialog);
		tabs[5] = new EnvironmentTab();
		tabs[5].setLaunchConfigurationDialog(dialog);
		tabs[6] = new CommonTab();
		tabs[6].setLaunchConfigurationDialog(dialog);
		setTabs(tabs);
	}
}