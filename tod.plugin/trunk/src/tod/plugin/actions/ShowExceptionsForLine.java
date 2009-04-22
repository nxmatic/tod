/*
 * Created on Jan 16, 2007
 */
package tod.plugin.actions;

import javax.swing.SwingUtilities;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IBehaviorInfo;
import tod.gui.IGUIManager;
import tod.plugin.TODPluginUtils;

/**
 * Handler for the "Show exceptions in TOD" ruler action.
 * @author gpothier
 */
public class ShowExceptionsForLine extends AbstractRulerActionDelegate
{
	@Override
	protected IAction createAction(
			ITextEditor aEditor, 
			IVerticalRulerInfo aRulerInfo)
	{
		return new ShowExceptionsForLineAction(aEditor, aRulerInfo);
	}

	private static class ShowExceptionsForLineAction extends AbstractRulerAction
	{
		public ShowExceptionsForLineAction(ITextEditor aEditor, IVerticalRulerInfo aRulerInfo)
		{
			super(aEditor, aRulerInfo);
		}

		@Override
		public void run()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					IGUIManager theGUIManager = getGUIManager(true);
					ILogBrowser theLogBrowser = theGUIManager.getSession().getLogBrowser();
					
					theGUIManager.showEventsForLine(
							getCurrentBehavior(), 
							getCurrentLine(), 
							theLogBrowser.createExceptionGeneratedFilter());
				}
			});
		}
		
		@Override
		protected boolean shouldEnable()
		{
			return getCurrentMethod() != null;
		}
		
	}
	
}
