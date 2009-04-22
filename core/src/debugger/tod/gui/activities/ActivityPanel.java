/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.gui.activities;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import tod.core.config.TODConfig;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.ObjectId;
import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.SeedHyperlink;
import tod.gui.activities.objecthistory.ObjectHistorySeed;
import tod.gui.formatter.EventFormatter;
import tod.gui.formatter.ObjectFormatter;
import tod.gui.kit.BusOwnerPanel;
import tod.gui.kit.IOptionsOwner;
import tod.gui.kit.Options;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobSchedulerProvider;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.ISetProperty;
import zz.utils.properties.PropertyUtils;
import zz.utils.properties.PropertyUtils.Connector;
import zz.utils.ui.ZLabel;

/**
 * Base class for views.
 * @see tod.gui.activities.ActivitySeed
 * @author gpothier
 */
public abstract class ActivityPanel<T extends ActivitySeed> extends BusOwnerPanel
implements IOptionsOwner, IJobSchedulerProvider
{
	private final IContext itsContext;
	
	private List<PropertyUtils.Connector> itsConnectors; 

	private ObjectFormatter itsObjectFormatter;
	private EventFormatter itsEventFormatter;
	
	private Options itsOptions;
	
	private T itsSeed;
	
	/**
	 * This flag indicates that this component has been added to a visible hierarchy.
	 */
	private boolean itsAdded = false;
	
	/**
	 * This flag indicates that the seed is connected.
	 */
	private boolean itsSeedConnected = false;
	
	public ActivityPanel(IContext aContext)
	{
		itsContext = aContext;
		itsOptions = new Options(
				getGUIManager().getSettings(), 
				getOptionsName(), 
				getGUIManager().getSettings().getOptions());
		
		initOptions(itsOptions);
	}
	
	/**
	 * Sets the seed displayed by this view.
	 */
	public final void setSeed(T aSeed)
	{
		if (itsSeedConnected && itsSeed != null) 
		{
			disconnectSeed(itsSeed);
			itsSeedConnected = false;
		}
		
		itsSeed = aSeed;
		itsObjectFormatter = itsSeed != null ? new ObjectFormatter(itsSeed.getLogBrowser()) : null;
		itsEventFormatter = itsSeed != null ? new EventFormatter(itsSeed.getLogBrowser()) : null;
		
		if (itsAdded && itsSeed != null)
		{
			connectSeed(itsSeed);
			itsSeedConnected = true;
		}
	}
	
	public T getSeed()
	{
		return itsSeed;
	}
	
	/**
	 * Connects the UI elements of this view to the properties of the seed.
	 * This method is called automatically by {@link ActivityPanel} as needed, ie.
	 * when the seed is changed and when the component appears on screen.
	 * This is a pseudo-abstract method, that does nothing by default. 
	 */
	protected void connectSeed(T aSeed)
	{
	}
	
	/**
	 * Disonnects the UI elements of this view to the properties of the seed.
	 * This method is called automatically by {@link ActivityPanel} as needed, ie.
	 * when the seed is changed and when the component disappears from screen.
	 * This is a pseudo-abstract method, that does nothing by default. 
	 */
	protected void disconnectSeed(T aSeed)
	{
	}
	
	/**
	 * Returns the name under which the options of this view are stored.
	 */
	protected String getOptionsName()
	{
		return getClass().getSimpleName();
	}

	/**
	 * Subclasses can override this method to add options to the options set.
	 */
	protected void initOptions(Options aOptions)
	{
	}
	
	public Options getOptions()
	{
		return itsOptions;
	}
	
	@Override
	public void addNotify()
	{
		if (! itsSeedConnected && itsSeed != null)
		{
			connectSeed(itsSeed);
			itsSeedConnected = true;
		}
		itsAdded = true;
		super.addNotify();
	}
	
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		if (itsSeedConnected && itsSeed != null)
		{
			disconnectSeed(itsSeed);
			itsSeedConnected = false;
		}
		itsAdded = false;
	}
	
	@Override
	public boolean isValidateRoot()
	{
		return true;
	}
	
	/**
	 * Sets up a bidirectional connection between two properties
	 * @see PropertyUtils.Connector
	 */
	protected <V> void connect (IRWProperty<V> aSource, IRWProperty<V> aTarget)
	{
		if (itsConnectors == null) itsConnectors = new ArrayList<PropertyUtils.Connector>(); 
		PropertyUtils.SimpleValueConnector<V> theConnector = new PropertyUtils.SimpleValueConnector<V>(
				aSource, 
				aTarget, 
				true, 
				true);
		
		itsConnectors.add (theConnector);
		theConnector.connect();
	}
	
	protected <V> void disconnect (IRWProperty<V> aSource, IRWProperty<V> aTarget)
	{
		if (itsConnectors != null) for (Iterator<Connector> theIterator = itsConnectors.iterator(); theIterator.hasNext();)
		{
			Connector theConnector = theIterator.next();
			if (theConnector.getSourceProperty() == aSource && theConnector.getTargetProperty() == aTarget)
			{
				theIterator.remove();
				theConnector.disconnect();
				return;
			}
		}
		throw new RuntimeException("Properties were not connected");
	}
	
	/**
	 * Prepares a connection between two set properties. 
	 * The connection is effective only once this component is shown. 
	 * The connection is removed when this component is hidden.
	 * @see PropertyUtils.Connector
	 */
	protected <T> void connectSet (ISetProperty<T> aSource, ISetProperty<T> aTarget, boolean aSymmetric)
	{
		if (itsConnectors == null) itsConnectors = new ArrayList<PropertyUtils.Connector>(); 
		PropertyUtils.SetConnector<T> theConnector = new PropertyUtils.SetConnector<T>(aSource, aTarget, aSymmetric, true);
		itsConnectors.add (theConnector);
	}
	
	public ILogBrowser getLogBrowser()
	{
		return getGUIManager().getSession().getLogBrowser();
	}
	
	public IContext getContext()
	{
		return itsContext;
	}
	
	public IGUIManager getGUIManager()
	{
		return getContext().getGUIManager();
	}
	
	public TODConfig getConfig()
	{
		return getGUIManager().getSession().getConfig();
	}
	
	/**
	 * Helper method to obtain the default job processor for this view.
	 */
	public IJobScheduler getJobScheduler()
	{
		return getGUIManager().getJobScheduler();
	}

	
	/**
	 * Returns an event formatter that can be used in the context
	 * of this view.
	 */
	protected EventFormatter getEventFormatter()
	{
		return itsEventFormatter;
	}


	/**
	 * Returns an object formatter that can be used in the context 
	 * of this view.
	 */
	protected ObjectFormatter getObjectFormatter()
	{
		return itsObjectFormatter;
	}

	/**
	 * This method is called after the object is instantiated.
	 * It should be used to create the user interface.
	 */
	public void init()
	{
	}
	
	/**
	 * Creates a standard panel that shows a title and a link label.
	 */
	protected JComponent createTitledLink (String aTitle, String aLinkName, ActivitySeed aSeed)
	{
		return createTitledPanel(aTitle, SeedHyperlink.create(getGUIManager(), aSeed, aLinkName));
	}
	
	/**
	 * Creates a standard panel that shows a title and another component.
	 */
	protected JComponent createTitledPanel (String aTitle, JComponent aComponent)
	{
		JPanel thePanel = new JPanel(GUIUtils.createSequenceLayout());
		thePanel.add (GUIUtils.createLabel(aTitle));
		thePanel.add (aComponent);
		
		return thePanel;
	}
	
	
	
	/**
	 * Creates a title label, with a big font.
	 */
	protected JComponent createTitleLabel (String aTitle)
	{
		return ZLabel.create(aTitle, FontConfig.STD_HEADER_FONT, Color.BLACK);
	}
	
	/**
	 * Creates a link that jumps to an inspector for the specified object.
	 */
	protected JComponent createInspectorLink (Object aObject)
	{
		if (aObject instanceof ObjectId)
		{
			ObjectId theObjectId = (ObjectId) aObject;
			
			ObjectHistorySeed theSeed = new ObjectHistorySeed(getLogBrowser(), theObjectId);
			
			return SeedHyperlink.create(
					getGUIManager(),
					theSeed,
					itsObjectFormatter.getPlainText(aObject)); 
		}
		else return new JLabel (itsObjectFormatter.getPlainText(aObject));
	}

}
