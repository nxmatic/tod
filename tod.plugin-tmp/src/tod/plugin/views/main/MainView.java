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

import javax.swing.JComponent;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;

import tod.core.database.event.ILogEvent;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.plugin.SourceRevealerUtils;
import tod.plugin.views.AbstractAWTView;

/**
 * This view is the trace navigator.
 * @author gpothier
 */
public class MainView extends AbstractAWTView implements ISelectionListener
{
	/**
	 * Id of the view as defined in plugin.xml
	 */
	public static final String VIEW_ID = "tod.plugin.views.main.MainView";
	
	private MainViewPanel itsEventViewer;
	
	/**
	 * This flag permits to avoid infinite recursion or misbehaviors
	 * of selection in java source vs. {@link #gotoEvent(ILogEvent)}.
	 */
	private boolean itsMoving = false;

	@Override
	public void createPartControl(Composite parent) 
	{
		System.out.println("Add listener");
		ISelectionService theSelectionService = getViewSite().getWorkbenchWindow().getSelectionService();
		theSelectionService.addPostSelectionListener(this);
		
		super.createPartControl(parent);
	}
	
	@Override
	public void dispose()
	{
		ISelectionService theSelectionService = getViewSite().getWorkbenchWindow().getSelectionService();
		theSelectionService.removePostSelectionListener(this);
	}
	
	/**
	 * Called when the selected element in the workbench changes.
	 */
	public void selectionChanged(IWorkbenchPart aPart, ISelection aSelection)
	{
	    if (itsMoving) return;
	    itsMoving = true;
	    
//		IJavaElement theJavaElement = null;
//		
//		if (aSelection instanceof IStructuredSelection)
//		{
//			IStructuredSelection theSelection = (IStructuredSelection) aSelection;
//			Object theElement = theSelection.getFirstElement();
//			if (theElement instanceof IJavaElement)
//			{
//				theJavaElement = (IJavaElement) theElement;
//			}
//		}
//		else if (aSelection instanceof ITextSelection)
//		{
//			ITextSelection theTextSelection = (ITextSelection) aSelection;
//			theJavaElement = TextSelectionUtils.getJavaElement(aPart, theTextSelection);
//		}
//		
//		if (theJavaElement != null) itsEventViewer.showElement(theJavaElement);
		itsMoving = false;
	}
	
	public void gotoSource(ISession aSession, ILocationInfo aLocation)
	{
	    if (itsMoving) return;
	    itsMoving = true;
	    
	    SourceRevealerUtils.reveal(aSession, aLocation);
	    
	    itsMoving = false;
	}

	public void gotoSource(ISession aSession, ProbeInfo aProbe)
	{
		if (itsMoving) return;
		itsMoving = true;
		
		SourceRevealerUtils.reveal(aSession, aProbe);
		
		itsMoving = false;
	}
	

	
	@Override
	protected JComponent createComponent()
	{
		itsEventViewer = new MainViewPanel(this);
		return itsEventViewer;
	}

	public IGUIManager getGUIManager()
	{
		return itsEventViewer;
	}
	
	public IContext getContext()
	{
		return itsEventViewer;
	}
		
}