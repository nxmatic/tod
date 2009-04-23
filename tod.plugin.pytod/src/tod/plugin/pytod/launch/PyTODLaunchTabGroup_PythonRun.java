package tod.plugin.pytod.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.RefreshTab;
import org.python.pydev.debug.ui.ArgumentsTab;
import org.python.pydev.debug.ui.InterpreterTab;
import org.python.pydev.debug.ui.MainModuleTab;
import org.python.pydev.plugin.PydevPlugin;

import tod.plugin.launch.TODConfigLaunchTab;

public class PyTODLaunchTabGroup_PythonRun extends AbstractLaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new MainModuleTab(), 
				new ArgumentsTab(),
				new TODConfigLaunchTab(),
				new InterpreterTab(PydevPlugin.getPythonInterpreterManager()),
				new RefreshTab(), 
				new EnvironmentTab(), 
				new CommonTab() };
		setTabs(tabs);
	}
}