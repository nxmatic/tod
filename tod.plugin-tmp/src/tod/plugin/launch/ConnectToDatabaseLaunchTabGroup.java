/*
 * Created on Jun 14, 2007
 */
package tod.plugin.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

public class ConnectToDatabaseLaunchTabGroup extends AbstractLaunchConfigurationTabGroup
{
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) 
	{
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
			new TODConnectLaunchTab(),
			new TODConfigLaunchTab(),
//			new SourceLookupTab(),
			new CommonTab(),
		};
		setTabs(tabs);
	}

}
