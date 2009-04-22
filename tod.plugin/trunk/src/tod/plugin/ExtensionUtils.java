/*
 * Created on Nov 29, 2008
 */
package tod.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import tod.core.database.browser.IEventFilter;
import tod.core.database.event.ILogEvent;

public class ExtensionUtils
{
	/**
	 * Retrieves all the implementations of a given extension point.
	 * This is valid only for extension points that have an attribute 
	 * of Java type named "class".
	 */
	public static <T> List<T> getExtensions(String aExtensionPoint, Class<T> aClass)
	{
		List<T> theResult = new ArrayList<T>();
		IExtensionRegistry theRegistry = Platform.getExtensionRegistry();
		IConfigurationElement[] theExtensions = 
			theRegistry.getConfigurationElementsFor(aExtensionPoint);

		for (IConfigurationElement theElement : theExtensions)
		{
			T theExtension;
			try
			{
				theExtension = (T) theElement.createExecutableExtension("class");
				theResult.add(theExtension);
			}
			catch (CoreException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return theResult;
	}
}
