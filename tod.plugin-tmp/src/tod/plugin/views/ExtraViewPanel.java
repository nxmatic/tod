/*
 * Created on Aug 27, 2008
 */
package tod.plugin.views;

import javax.swing.JPanel;

import tod.gui.IContext;
import tod.plugin.TODPluginUtils;
import tod.plugin.views.main.MainView;

public abstract class ExtraViewPanel extends JPanel
{
	protected IContext getContext()
	{
		MainView theView = TODPluginUtils.getMainView(false);
		return theView.getContext();
	}
}
