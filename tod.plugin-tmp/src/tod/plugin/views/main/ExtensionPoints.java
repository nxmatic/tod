/*
 * Created on Nov 29, 2008
 */
package tod.plugin.views.main;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import tod.core.database.browser.IEventFilter;
import tod.core.database.event.ILogEvent;
import tod.gui.IExtensionPoints;
import tod.plugin.ExtensionUtils;
import tod.plugin.IActionsForEventProvider;

public class ExtensionPoints implements IExtensionPoints
{
	public List<Action> getActionsForEvent(ILogEvent aEvent, IEventFilter aProducer)
	{
		List<IActionsForEventProvider> theProviders = ExtensionUtils.getExtensions(
				"tod.plugin.ActionsForEvent", 
				IActionsForEventProvider.class);
		
		List<Action> theResult = new ArrayList<Action>();
		
		for (IActionsForEventProvider theProvider : theProviders)
		{
			List<Action> theActions = theProvider.getActionsForEvent(aEvent, aProducer);
			if (theActions != null) theResult.addAll(theActions);
		}
		
		return theResult.size() > 0 ? theResult : null;
	}
}
