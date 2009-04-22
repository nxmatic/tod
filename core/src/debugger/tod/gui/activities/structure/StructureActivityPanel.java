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
package tod.gui.activities.structure;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.ILocationInfo;
import tod.gui.IContext;
import tod.gui.activities.ActivityPanel;
import tod.gui.components.locationselector.LocationSelectorPanel;
import tod.gui.kit.SavedSplitPane;
import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.properties.PropertyListener;
import zz.utils.ui.StackLayout;

/**
 * Provides access to the structural database.
 * @author gpothier
 */
public class StructureActivityPanel extends ActivityPanel<StructureSeed>
{
	private static final String PROPERTY_SPLITTER_POS = "structureView.splitterPos";
	private JPanel itsInfoHolder;
	private LocationSelectorPanel itsSelectorPanel;
	
	private IPropertyListener<ILocationInfo> itsSelectedLocationListener = new PropertyListener<ILocationInfo>()
	{
		public void propertyChanged(IProperty<ILocationInfo> aProperty, ILocationInfo aOldValue, ILocationInfo aNewValue)
		{
			showNode(aNewValue);
		}
	};
	
	public StructureActivityPanel(IContext aContext)
	{
		super(aContext);
	}

	@Override
	protected void connectSeed(StructureSeed aSeed)
	{
		aSeed.pSelectedLocation().addHardListener(itsSelectedLocationListener);
		connect(aSeed.pSelectedLocation(), itsSelectorPanel.pSelectedLocation());
	}

	@Override
	protected void disconnectSeed(StructureSeed aSeed)
	{
		aSeed.pSelectedLocation().removeListener(itsSelectedLocationListener);
		disconnect(aSeed.pSelectedLocation(), itsSelectorPanel.pSelectedLocation());
	}

	@Override
	public void init()
	{
		itsSelectorPanel = new LocationSelectorPanel(getLogBrowser(), true);
		itsInfoHolder = new JPanel(new StackLayout());
		
		JSplitPane theSplitPane = new SavedSplitPane(JSplitPane.HORIZONTAL_SPLIT, getGUIManager(), PROPERTY_SPLITTER_POS);
		theSplitPane.setResizeWeight(0.5);
		theSplitPane.setLeftComponent(itsSelectorPanel);
		theSplitPane.setRightComponent(itsInfoHolder);
		
		setLayout(new StackLayout());
		add(theSplitPane);
		
	}
	
	/**
	 * Shows the information corresponding to the given node.
	 */
	public void showNode(ILocationInfo aLocation)
	{
		if (aLocation instanceof IBehaviorInfo)
		{
			IBehaviorInfo theBehavior = (IBehaviorInfo) aLocation;
			
			JTabbedPane theTabbedPane = new JTabbedPane();
			theTabbedPane.addTab("Bytecode", new DisassemblyPanel(theBehavior));
			theTabbedPane.addTab("Line numbers", new LineNumberInfoPanel(theBehavior));
			theTabbedPane.addTab("Local variables", new VariableInfoPanel(theBehavior));
			showPanel(theTabbedPane);
		}
	}
	
	/**
	 * Changes the currently displayed info panel
	 */
	private void showPanel(JComponent aComponent)
	{
		itsInfoHolder.removeAll();
		itsInfoHolder.add(aComponent);
		itsInfoHolder.revalidate();
		itsInfoHolder.repaint();
	}
}
