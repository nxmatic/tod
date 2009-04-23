/*
 * Created on Nov 29, 2008
 */
package tod.plugin;

import java.util.List;

import javax.swing.Action;

import tod.core.database.browser.IEventFilter;
import tod.core.database.event.ILogEvent;
import tod.gui.IExtensionPoints;

/**
 * This interface must be implemented by extenders of the
 * ActionsForEvent extension point.
 * See also {@link IExtensionPoints}.
 * @author gpothier
 */
public interface IActionsForEventProvider
{
	/**
	 * See {@link IExtensionPoints#getActionsForEvent(ILogEvent, IEventFilter)}.
	 */
	public List<Action> getActionsForEvent(ILogEvent aEvent, IEventFilter aProducer);
}
