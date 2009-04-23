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
package tod.plugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Utility for determining the {@link org.eclipse.jdt.core.IJavaElement} that corresponds
 * to a {@link ITextSelection}.
 * @author gpothier
 */
public class TextSelectionUtils
{
	/**
	 * Retrieves the Java Element that corresponds to the given text selection.
	 * If no JavaElement is found, this method returns null.
	 */
	public static IJavaElement getJavaElement(IWorkbenchPart part, ITextSelection selection)
	{
		if (part instanceof IEditorPart)
		{
			IEditorInput ei = ((IEditorPart) part).getEditorInput();
			int offset = selection.getOffset();
			return getElementAt(ei, offset);
		}
		else return null;
	}
	
	private static IJavaElement getElementAt(IEditorInput input, int offset)
	{
		if (input instanceof IClassFileEditorInput)
		{
			try
			{
				return ((IClassFileEditorInput) input).getClassFile().getElementAt(offset);
			}
			catch (JavaModelException ex)
			{
				return null;
			}
		}

		IWorkingCopyManager manager = JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit = manager.getWorkingCopy(input);
		if (unit != null)
		{
			try
			{
				if (unit.isConsistent()) return unit.getElementAt(offset);
			}
			catch (JavaModelException ex)
			{
			}
		}
		return null;
	}


}
