package tod.plugin.views;

import tod.plugin.views.main.MainView;


/**
 * Base class for extra views.
 * The main view is {@link MainView}. Extra views are linked to the main view,
 * ie selecting an event in an extra view puts shows a control flow view in the main view.
 * @author gpothier
 *
 */
public abstract class ExtraView extends AbstractAWTView
{
	@Override
	protected abstract ExtraViewPanel createComponent();
}
