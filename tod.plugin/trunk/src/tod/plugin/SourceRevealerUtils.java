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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.IProgramLaunch;
import tod.core.session.ISession;
import tod.plugin.launch.EclipseProgramLaunch;
import tod.utils.TODUtils;
import zz.eclipse.utils.EclipseUtils;

/**
 * Utility class that permits to asynchronously reveal particualr source
 * locations
 * 
 * @author gpothier
 */
public class SourceRevealerUtils
{
	private static SourceRevealerUtils INSTANCE = new SourceRevealerUtils();

	public static SourceRevealerUtils getInstance()
	{
		return INSTANCE;
	}

	private SourceRevealerUtils()
	{
	}

	private ISourceRevealer itsCurentRevealer;

	private boolean itsRevealScheduled = false;

	private void reveal(
			final RevealKind aKind,
			ISourceRevealer aRevealer, 
			final ISession aSession, 
			final Object aTarget)
	{
		itsCurentRevealer = aRevealer;
		if (!itsRevealScheduled)
		{
			itsRevealScheduled = true;
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					try
					{
						aKind.reveal(itsCurentRevealer, aSession, aTarget);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					itsRevealScheduled = false;
				}
			});
		}
	}

	
	/**
	 * Opens a given location. Should be safe for JDT and PDE projects.
	 */
	private static void reveal(
			RevealKind aKind,
			ISession aSession, 
			Object aTarget)
	{
		TODUtils.log(1, "[SourceRevealerUtils.reveal]" + aTarget);
		List<ISourceRevealer> theRevealers = ExtensionUtils.getExtensions(
				"tod.plugin.SourceRevealer", 
				ISourceRevealer.class);

		// Find the best revealer
		List<Integer> theKeys = new ArrayList<Integer>();
		Map<Integer, ISourceRevealer> theOrderedMap = new HashMap<Integer, ISourceRevealer>();
		for (ISourceRevealer theRevealer : theRevealers) 
		{
			int theConfidence = aKind.canHandle(theRevealer, aSession, aTarget);
			while (theOrderedMap.containsKey(theConfidence)) theConfidence--;
			theKeys.add(theConfidence);
			theOrderedMap.put(theConfidence, theRevealer);
		}

		Collections.sort(theKeys);
		
		// Reveal
		for (int i=theKeys.size()-1;i>=0;i--)
		{
			ISourceRevealer theRevealer = theOrderedMap.get(theKeys.get(i));
			getInstance().reveal(aKind, theRevealer, aSession, aTarget);
			break; //TODO: try other revealers if this one doesn't work.
		}
	}

	/**
	 * Opens a given location.
	 */
	public static void reveal(ISession aSession, ILocationInfo aLocation)
	{
		reveal(KIND_LOCATION, aSession, aLocation);
	}

	/**
	 * Opens a given probe. 
	 */
	public static void reveal(ISession aSession, ProbeInfo aProbe)
	{
		reveal(KIND_PROBE, aSession, aProbe);
	}

	
	public static IFile findFile(List<IJavaProject> aJavaProjects, String aName)
	{
		for (IJavaProject theJavaProject : aJavaProjects)
		{
			IProject theProject = theJavaProject.getProject();
			IFile[] theFiles = EclipseUtils.findFiles(aName, theProject);
			if (theFiles.length > 0) return theFiles[0];
		}

		return null;
	}

	/**
	 * Similar to {@link #findFile(List, String)}, but only searches in source
	 * folders.
	 */
	public static IFile findSourceFile(List<IJavaProject> aJavaProjects, String aName)
	{
		for (IJavaProject theJavaProject : aJavaProjects)
		{
			try
			{
				IFile[] theFiles = EclipseUtils.findSourceFiles(aName, theJavaProject);
				if (theFiles.length > 0) return theFiles[0];
			}
			catch (Exception e)
			{
				System.out.println("Revealer exception....");
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * Returns all the java projects of the given session.
	 */
	public static List<IJavaProject> getJavaProjects(ISession aSession)
	{
		List<IProject> theProjects = getProjects(aSession);
		List<IJavaProject> theJavaProjects = new ArrayList<IJavaProject>();
		
    	for (IProject theProject : theProjects)
		{
    		IJavaProject theJavaProject = JavaCore.create(theProject);
			if (theJavaProject != null && theJavaProject.exists())
			{
				theJavaProjects.add(theJavaProject);
			}
		}

	    return theJavaProjects;
	}

	/**
	 * Returns all the projects of the given session.
	 */
	public static List<IProject> getProjects(ISession aSession)
	{
		List<IProject> theProjects = new ArrayList<IProject>();
		
		Set<IProgramLaunch> theLaunches = aSession.getLaunches();
		for (IProgramLaunch theLaunch : theLaunches)
		{
			EclipseProgramLaunch theEclipseLaunch = (EclipseProgramLaunch) theLaunch;
			theProjects.addAll(theEclipseLaunch.getProjects());
		}
		
		return theProjects;
	}

	/**
	 * Reveals the specified line in the given editor
	 */
	public static void revealLine(IEditorPart aEditor, int aLine) throws BadLocationException
	{
		if (aEditor instanceof ITextEditor)
		{
			ITextEditor theTextEditor = (ITextEditor) aEditor;
			IDocumentProvider theProvider = theTextEditor.getDocumentProvider();
			IDocument theDocument = theProvider.getDocument(theTextEditor.getEditorInput());
			int theStart = theDocument.getLineOffset(aLine-1);
			theTextEditor.selectAndReveal(theStart, 0);
		}
	}
	
	private static abstract class RevealKind
	{
		public abstract int canHandle(ISourceRevealer aRevealer, ISession aSession, Object aTarget);
		public abstract boolean reveal(ISourceRevealer aRevealer, ISession aSession, Object aTarget) throws CoreException, BadLocationException;
	}
	
	private static final RevealKind KIND_LOCATION = new RevealKind()
	{
		@Override
		public int canHandle(ISourceRevealer aRevealer, ISession aSession, Object aTarget)
		{
			return aRevealer.canHandle(aSession, (ILocationInfo) aTarget);
		}

		@Override
		public boolean reveal(ISourceRevealer aRevealer, ISession aSession, Object aTarget) throws CoreException, BadLocationException
		{
			return aRevealer.reveal(aSession, (ILocationInfo) aTarget);
		}
	};
	
	private static final RevealKind KIND_PROBE = new RevealKind()
	{
		@Override
		public int canHandle(ISourceRevealer aRevealer, ISession aSession, Object aTarget)
		{
			return aRevealer.canHandle(aSession, (ProbeInfo) aTarget);
		}

		@Override
		public boolean reveal(ISourceRevealer aRevealer, ISession aSession, Object aTarget)
				throws CoreException,
				BadLocationException
		{
			return aRevealer.reveal(aSession, (ProbeInfo) aTarget);
		}
	};
	

}
