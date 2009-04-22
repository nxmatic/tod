package tod.plugin.pytod;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorPart;

import tod.core.database.structure.SourceRange;
import tod.core.session.ISession;
import tod.plugin.ISourceRevealer;
import tod.plugin.SourceRevealerUtils;
import tod.utils.TODUtils;

public class PythonSourceRevealer implements ISourceRevealer
{
	public boolean canHandle(SourceRange aSourceRange)
	{
		if (aSourceRange.sourceFile == null) return false;
		return aSourceRange.sourceFile.endsWith(".py");		
	}

	public boolean reveal(ISession aSession, SourceRange aSourceRange) throws CoreException, BadLocationException
	{
		IEditorPart theEditor = findEditor(SourceRevealerUtils.getProjects(aSession), aSourceRange);
		if (theEditor == null) return false;
		
		SourceRevealerUtils.revealLine(theEditor, aSourceRange.startLine);
		return true;
	}
	
	
	public static IEditorPart findEditor(List<IProject> aProjects, SourceRange aSourceRange)
	throws CoreException
	{
		IFile theFile = findFile(aProjects, aSourceRange.sourceFile);
		if (theFile != null) return EditorUtility.openInEditor(theFile, false);
		else
		{
			TODUtils.logf(0, "The type %s has not been found in the available sources "
					+ "of the Eclipse workspace.\n Path were " + "to find the sources was: %s",
					aSourceRange.sourceFile, aProjects);
			return null;
		}
	}
	
	public static IFile findFile(List<IProject> aProjects, String aFileName)
	{
		IWorkspace theWorkspace = ResourcesPlugin.getWorkspace();
		IFile theFile = theWorkspace.getRoot().getFileForLocation(Path.fromOSString(aFileName));
		IProject theProject = theFile.getProject();
		if (aProjects.contains(theProject)) return theFile;
		else return null;
	}
}
