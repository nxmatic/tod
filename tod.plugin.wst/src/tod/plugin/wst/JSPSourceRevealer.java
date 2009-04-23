/*
 * Created on Jun 27, 2008
 */
package tod.plugin.wst;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorPart;

import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.SourceRange;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;
import tod.plugin.ISourceRevealer;
import tod.plugin.SourceRevealerUtils;
import tod.plugin.wst.launch.WSTServerLaunch;
import tod.tools.parsers.ParseException;
import tod.tools.parsers.smap.SMAP;
import tod.tools.parsers.smap.SMAPFactory;
import tod.tools.parsers.smap.SMAP.SourceLoc;

public class JSPSourceRevealer implements ISourceRevealer
{

	public int canHandle(ISession aSession, ILocationInfo aLocation)
	{
		return CANT;
	}

	public int canHandle(ISession aSession, ProbeInfo aProbe)
	{
		IStructureDatabase theStructureDatabase = aSession.getLogBrowser().getStructureDatabase();
		String theSourceFile = LocationUtils.getSourceRange(theStructureDatabase, aProbe).sourceFile;
		if (theSourceFile == null) return CANT;
		else if (theSourceFile.endsWith("_jsp.java")) return SPECIALIZED;		
		else if (theSourceFile.endsWith(".jsp")) return NORMAL;
		else return CANT;
	}

	public boolean reveal(ISession aSession, ILocationInfo aLocation) throws CoreException, BadLocationException
	{
		return false;
	}

	public boolean reveal(ISession aSession, ProbeInfo aProbe) throws CoreException, BadLocationException
	{
		IStructureDatabase theStructureDatabase = aSession.getLogBrowser().getStructureDatabase();
		SourceRange theSourceRange = LocationUtils.getSourceRange(theStructureDatabase, aProbe);
		
		if (theSourceRange.sourceFile.endsWith("_jsp.java")) 
		{
			try
			{
				IBehaviorInfo theBehavior = theStructureDatabase.getBehavior(aProbe.behaviorId, true);
				IClassInfo theClass = theBehavior.getDeclaringType();
				String theSMAPString = theClass.getSMAP();
				if (theSMAPString != null)
				{
					SMAP theSMAP = SMAPFactory.parseSMAP(theSMAPString);
					SourceLoc theSourceLoc = theSMAP.getSourceLoc(theSourceRange.startLine);
					SourceRange theInputRange = new SourceRange(
							theSourceRange.typeName,
							theSourceLoc.fileInfo.fileName,
							theSourceLoc.lineNumber);
					
					return reveal(aSession, theInputRange);
				}
				else return false;
			}
			catch (ParseException e)
			{
				throw new RuntimeException(e);
			}
		}
		else if (theSourceRange.sourceFile.endsWith(".jsp"))
		{
			// TODO: what do we do here?
			return false;
		}
		else return false;
	}

	public boolean reveal(ISession aSession, SourceRange aSourceRange) throws CoreException, BadLocationException
	{
//		WSTAppLaunch theLaunch = WSTAppLaunch.getAppLaunch(aSession);
		WSTServerLaunch theServerLaunch = WSTServerLaunch.getServerLaunch(aSession); 
		ISourceLookupResult theResult = DebugUITools.lookupSource(aSourceRange.sourceFile, theServerLaunch.getLaunch().getSourceLocator());
		Object theSourceElement = theResult.getSourceElement();
		IFile theFile = null;
		if (theSourceElement instanceof IFile) theFile = (IFile) theSourceElement;
		
		IEditorPart theEditor = null;
		if (theFile != null) theEditor = EditorUtility.openInEditor(theFile, false);
		
		if (theEditor == null) return false;
		
		SourceRevealerUtils.revealLine(theEditor, aSourceRange.startLine);
		return true;
	}
	
}
