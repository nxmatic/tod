package tod.plugin.views.objectwatch;

import tod.plugin.views.ExtraView;
import tod.plugin.views.ExtraViewPanel;

public class ObjectWatchView extends ExtraView
{
	@Override
	protected ExtraViewPanel createComponent()
	{
		return new ObjectWatchViewPanel();
	}
}
