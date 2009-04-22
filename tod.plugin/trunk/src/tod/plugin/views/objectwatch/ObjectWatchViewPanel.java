/*
 * Created on Aug 27, 2008
 */
package tod.plugin.views.objectwatch;

import java.awt.BorderLayout;

import tod.gui.components.objectwatch.ObjectWatchPanel;
import tod.plugin.views.ExtraViewPanel;

public class ObjectWatchViewPanel extends ExtraViewPanel
{
	private ObjectWatchPanel itsObjectWatchPanel;
	
	public ObjectWatchViewPanel()
	{
		createUI();
	}

	private void createUI()
	{
		setLayout(new BorderLayout());
		itsObjectWatchPanel = new ObjectWatchPanel(getContext(), true);
		add(itsObjectWatchPanel, BorderLayout.CENTER);
	}
}
