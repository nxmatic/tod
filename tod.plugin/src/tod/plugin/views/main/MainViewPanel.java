/*
TOD plugin - Eclipse pluging for TOD
Copyright (C) 2006 Guillaume Pothier (gpothier@dcc.uchile.cl)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.plugin.views.main;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import tod.core.session.TODSessionManager;
import tod.gui.IExtensionPoints;
import tod.gui.MinerUI;
import tod.gui.activities.ActivitySeed;
import tod.gui.activities.ActivitySeedFactory;
import tod.plugin.TODPluginUtils;
import zz.utils.properties.IProperty;
import zz.utils.properties.PropertyListener;
import zz.utils.ui.StackLayout;

public class MainViewPanel extends MinerUI
{
	private final MainView itsTraceNavigatorView;
	private final ExtensionPoints itsExtensionPoints = new ExtensionPoints();

	public MainViewPanel(MainView aTraceNavigatorView)
	{
		itsTraceNavigatorView = aTraceNavigatorView;
		TODSessionManager.getInstance().pCurrentSession().addHardListener(new PropertyListener<ISession>()
				{
					public void propertyChanged(
							IProperty<ISession> aProperty, 
							ISession aOldValue, 
							final ISession aNewValue)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								setSession(aNewValue);
							}
						});
					}
				});

		setSession(TODSessionManager.getInstance().pCurrentSession().get());
	}
	
	@Override
	protected void createActions(ActionToolbar aToolbar, ActionCombo aActionCombo)
	{
		super.createActions(aToolbar, aActionCombo);
		
		// Add a button that permits to jump to the exceptions view.
		aActionCombo.add(new MyAction(
				"Drop session",
				"<html>" +
				"<b>Drop current session.</b> Clears all recorded event <br>" +
				"and starts a new, clean session.")
		{
			public void actionPerformed(ActionEvent aE)
			{
				TODSessionManager.getInstance().killSession();
			}
		});
	}
	
	public void showElement (IJavaElement aElement)
	{
		try
		{
			ILocationInfo theLocationInfo = TODPluginUtils.getLocationInfo(getSession(), aElement);
			ActivitySeed theSeed = ActivitySeedFactory.getDefaultSeed(getLogBrowser(), theLocationInfo);
			openSeed(theSeed, false);
		}
		catch (JavaModelException e)
		{
			throw new RuntimeException("Could not show element", e);
		}
	}
	
	
	
	public void gotoSource(ILocationInfo aLocation)
	{
	    itsTraceNavigatorView.gotoSource(getSession(), aLocation);
	}

	public void gotoSource(ProbeInfo aProbe)
	{
	    itsTraceNavigatorView.gotoSource(getSession(), aProbe);
	}

	public <T> T showDialog(DialogType<T> aDialog)
	{
		if (aDialog instanceof ErrorDialogType)
		{
			final ErrorDialogType theDialog = (ErrorDialogType) aDialog;
			final int[] theResult = new int[1];
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					theResult[0] = ErrorDialog.openError(
							theShell, 
							theDialog.getTitle(), 
							theDialog.getText(), 
							new Status(
									IStatus.ERROR,
									"tod.plugin",
									0,
									"Exception",
									null));
				}
			});

			return null;//theResult[0] != Dialog.CANCEL;		
		}
		else if (aDialog instanceof OkCancelDialogTYpe)
		{
			OkCancelDialogTYpe theDialog = (OkCancelDialogTYpe) aDialog;
			throw new UnsupportedOperationException();
		}
		else if (aDialog instanceof YesNoDialogType)
		{
			final YesNoDialogType theDialog = (YesNoDialogType) aDialog;
			final boolean[] theResult = new boolean[1];
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					theResult[0] = MessageDialog.openQuestion(
							theShell,
							theDialog.getTitle(),
							theDialog.getText());
				}
			});
			return (T) (Boolean) theResult[0];
		}
		else throw new IllegalArgumentException("Not handled: "+aDialog);
	}

	public void showPostIt(final JComponent aComponent, final Dimension aSize)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				Shell theMainShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				Shell thePostItShell = new Shell(theMainShell, SWT.DIALOG_TRIM | SWT.RESIZE);
				thePostItShell.setText("TOD PostIt");
				
				Composite theEmbedded = new Composite(thePostItShell, SWT.EMBEDDED | SWT.CENTER);
				thePostItShell.setLayout(new FillLayout());

				Frame theFrame = SWT_AWT.new_Frame(theEmbedded);
				theFrame.setLayout(new StackLayout());
				theFrame.add(aComponent);
				
				thePostItShell.setSize(aSize.width+20, aSize.height+40);
//				thePostItShell.pack();
//				thePostItShell.setSize(thePreferredSize.width, thePreferredSize.height);
				
				thePostItShell.setVisible(true);
			}
		});
	}

	public IExtensionPoints getExtensionPoints()
	{
		return itsExtensionPoints;
	}
}
