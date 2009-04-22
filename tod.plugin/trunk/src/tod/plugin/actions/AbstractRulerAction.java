/*
 * Created on Jan 30, 2007
 */
package tod.plugin.actions;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.actions.ActionDelegateHelper;
import org.eclipse.jdt.internal.debug.ui.actions.ToggleBreakpointAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.session.ISession;
import tod.gui.IGUIManager;
import tod.plugin.TODPluginUtils;

public abstract class AbstractRulerAction extends Action //implements IUpdate
{
	private ITextEditor itsEditor;
	private IVerticalRulerInfo itsRulerInfo;
	
	
	public AbstractRulerAction(ITextEditor aEditor, IVerticalRulerInfo aRulerInfo)
	{
		itsEditor = aEditor;
		itsRulerInfo = aRulerInfo;
	}

	protected ITextEditor getEditor()
	{
		return itsEditor;
	}

	protected IVerticalRulerInfo getRulerInfo()
	{
		return itsRulerInfo;
	}

	protected boolean shouldEnable()
	{
		return false;
	}
	
	public final void update()
	{
		System.out.println("AbstractRulerAction.update()");
//		setEnabled(shouldEnable());
//		setEnabled(true);
	}

	
	protected IGUIManager getGUIManager(boolean aShow)
	{
		return TODPluginUtils.getMainView(aShow).getGUIManager();
	}

	/**
	 * Returns the current editor line.
	 */
	protected int getCurrentLine()
	{
		return getRulerInfo().getLineOfLastMouseButtonActivity()+1;
	}
	
	/**
	 * Returns the method that corresponds to the current line.
	 */
	protected IMethod getCurrentMethod()
	{
		return getMethod(getCurrentLine());
	}
	
	protected IBehaviorInfo getCurrentBehavior()
	{
		try
		{
			return (IBehaviorInfo) TODPluginUtils.getLocationInfo(
					getSession(), 
					getCurrentMethod());
		}
		catch (JavaModelException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Retrieves the current session.
	 */
	protected ISession getSession()
	{
		return getGUIManager(false).getSession();
	}
	
	/**
	 * Returns the Java type at the specified line of the specified editor.
	 * Inspired from {@link ToggleBreakpointAdapter#getType} 
	 */
	protected IMethod getMethod(int aLine)
	{
		try
		{
			IDocument theDocument = getEditor().getDocumentProvider().getDocument(getEditor().getEditorInput());
			IRegion theRegion = theDocument.getLineInformation(aLine);
			ITextSelection theSelection = new TextSelection(theDocument, theRegion.getOffset(), 0);
			
			IMember theMember = ActionDelegateHelper.getDefault().getCurrentMember(theSelection);
			
			if (theMember instanceof IMethod)
			{
				IMethod theMethod = (IMethod) theMember;
				return theMethod;
			}
			else return null;
		}
		catch (BadLocationException e)
		{
			return null;
		}
	}

}
