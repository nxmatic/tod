/*
 * Created on Jun 14, 2007
 */
package tod.plugin.launch;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import tod.plugin.TODPlugin;

/**
 * A launch tab that permits to specify the location of the database
 * to connect to, for the "ConnectToDatabase" launch type.
 * Inspired from JavaConnectTab
 * @author gpothier
 */
public class TODConnectLaunchTab extends AbstractJavaMainTab 
{
	
	private Text itsAddressField;
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
    	Composite comp = new Composite(parent, SWT.NONE);
    	comp.setLayout(new GridLayout(1, false));
    	comp.setFont(font);
    	GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 1;
		comp.setLayoutData(gd);
		
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 0;
		comp.setLayout(layout);
		createProjectEditor(comp);
		createVerticalSpacer(comp, 1);
		
		Group group = new Group(comp, SWT.NONE);
		group.setText("Connection properties");
		group.setLayout(new GridLayout(2, false));
		group.setFont(parent.getFont());
    	gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		group.setLayoutData(gd);

		Label theLabel = new Label(group, SWT.NONE);
		theLabel.setText("host:");
		
		itsAddressField = new Text(group, SWT.BORDER);
		itsAddressField.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent aE)
			{
				updateLaunchConfigurationDialog();
			}
		});
		itsAddressField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		setControl(comp);
	}
	

	public void initializeFrom(ILaunchConfiguration config) 
	{
		super.initializeFrom(config);
		
		String theAddress = null;
		try
		{
			theAddress = config.getAttribute(ConnectToDatabaseLaunchConstants.DATABASE_ADDRESS, "localhost");
		}
		catch (CoreException e)
		{
			TODPlugin.logError("Cannot read database address", e);
		}
		itsAddressField.setText(theAddress);
	}
	

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText().trim());
		config.setAttribute(ConnectToDatabaseLaunchConstants.DATABASE_ADDRESS, itsAddressField.getText());
	}
	

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement = getContext();
		if (javaElement != null) initializeJavaProject(javaElement, config);
		config.setAttribute(ConnectToDatabaseLaunchConstants.DATABASE_ADDRESS, "localhost");
	}

	public boolean isValid(ILaunchConfiguration config) {	
		setErrorMessage(null);
		setMessage(null);	
		String name = fProjText.getText().trim();
		if (name.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists()) {
				setErrorMessage(LauncherMessages.JavaConnectTab_Project_does_not_exist_14); 
				return false;
			}
		}
		
		String theAddress = itsAddressField.getText();
		if (theAddress == null || theAddress.length() == 0)
		{
			setErrorMessage("Invalid address"); 
			return false;			
		}

		return true;
	}

	public String getName() {
		return "Connection";
	}			

//	public Image getImage() {
//		return DebugUITools.getImage(IDebugUIConstants.IMG_LCL_DISCONNECT);
//	}
		
	public void propertyChange(PropertyChangeEvent event) {
		updateLaunchConfigurationDialog();
	}
}
