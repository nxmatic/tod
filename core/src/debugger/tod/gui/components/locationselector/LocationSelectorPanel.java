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
package tod.gui.components.locationselector;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.database.structure.tree.PackageInfo;
import tod.core.database.structure.tree.StructureTreeBuilders;
import tod.gui.components.LocationTreeTable;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.PropertyUtils;
import zz.utils.properties.SimpleRWProperty;
import zz.utils.tree.SimpleTreeNode;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UniversalRenderer;

/**
 * This panel permits to select a location in the structure database.
 * @author gpothier
 */
public class LocationSelectorPanel extends JPanel
{
	private final IRWProperty<ILocationInfo> pSelectedLocation = new SimpleRWProperty<ILocationInfo>()
	{
		@Override
		protected void changed(ILocationInfo aOldValue, ILocationInfo aNewValue)
		{
			show(aNewValue);
		}
	};
	
	private final ILogBrowser itsLogBrowser;
	private final boolean itsShowMembers;
	
	private final Map<ILocationInfo, Long> itsCountsCache = new HashMap<ILocationInfo, Long>();
	
	/**
	 * Total number of events in the database at the time the cache was valid.
	 */
	private long itsEventCountForCache;
	
	public LocationSelectorPanel(ILogBrowser aLogBrowser, boolean aShowMembers)
	{
		itsLogBrowser = aLogBrowser;
		itsShowMembers = aShowMembers;
		createUI();
	}

	private void createUI()
	{
		JTabbedPane theTabbedPane = new JTabbedPane();
		
		theTabbedPane.addTab("Packages", new JScrollPane(new TreeSelector()));
		theTabbedPane.addTab("Behaviors", new JScrollPane(new BigJList(new BehaviorListModel(getStructureDatabase()))));
		theTabbedPane.addTab("Fields", new JScrollPane(new BigJList(new FieldListModel(getStructureDatabase()))));
		theTabbedPane.addTab("Probes", new JScrollPane(new BigJList(new ProbeListModel(getStructureDatabase()))));
		
		setLayout(new StackLayout());
		add(theTabbedPane);
	}

	public ILogBrowser getLogBrowser()
	{
		return itsLogBrowser;
	}
	
	public IStructureDatabase getStructureDatabase()
	{
		return itsLogBrowser.getStructureDatabase();
	}
	
	/**
	 * The property that contains the currently selected location node.
	 */
	public IRWProperty<ILocationInfo> pSelectedLocation()
	{
		return pSelectedLocation;
	}
	
	/**
	 * Called when the user selects a location.
	 */
	public void show(ILocationInfo aLocation)
	{
		System.out.println("LocationSelectorPanel.show(): "+aLocation);
		pSelectedLocation.set(aLocation);
	}
	
	private void checkCacheValid()
	{
		long theCurrentCount = getLogBrowser().getEventsCount();
		if (theCurrentCount != itsEventCountForCache)
		{
			itsEventCountForCache = theCurrentCount;
			itsCountsCache.clear();
		}
	}
	
	private long getEventCountAt(SimpleTreeNode<ILocationInfo> aNode)
	{
		ILocationInfo theLocation = aNode.pValue().get();
		
		checkCacheValid();
		Long theCount = itsCountsCache.get(theLocation);
		if (theCount == null) theCount = computeEventCountAt(aNode);
		
		return theCount;
	}
		
		
	private long computeEventCountAt(SimpleTreeNode<ILocationInfo> aNode)
	{
		ILocationInfo theLocation = aNode.pValue().get();

		if (theLocation instanceof IBehaviorInfo)
		{
			IBehaviorInfo theBehavior = (IBehaviorInfo) theLocation;
			long theCount = getLogBrowser().getEventCounts(new IBehaviorInfo[] {theBehavior})[0];
			itsCountsCache.put(theLocation, theCount);
			return theCount;
		}
		else if (theLocation instanceof IClassInfo)
		{
			IClassInfo theClass = (IClassInfo) theLocation;
			long theCount = getLogBrowser().getEventCounts(new IClassInfo[] {theClass})[0];
			itsCountsCache.put(theLocation, theCount);
			return theCount;
		}
		else if (theLocation instanceof PackageInfo)
		{
			long theTotal = 0;
			IClassInfo[] theClasses = getClasses(aNode);
			long[] theCounts = getLogBrowser().getEventCounts(theClasses);
			
			for(int i=0;i<theCounts.length;i++)
			{
				theTotal += theCounts[i];
				itsCountsCache.put(theClasses[i], theCounts[i]);
			}
			
			return theTotal;
		}
		else
		{
			System.err.println("Not handled: "+theLocation);
			return -1;
		}
	}
	
	private IClassInfo[] getClasses(SimpleTreeNode<ILocationInfo> aNode)
	{
		List<IClassInfo> theClasses = new ArrayList<IClassInfo>();
		getClasses(theClasses, aNode);
		return theClasses.toArray(new IClassInfo[theClasses.size()]);
	}
	
	private void getClasses(List<IClassInfo> aResult, SimpleTreeNode<ILocationInfo> aNode)
	{
		ILocationInfo theLocation = aNode.pValue().get();

		if (theLocation instanceof IClassInfo)
		{
			IClassInfo theClass = (IClassInfo) theLocation;
			aResult.add(theClass);
		}
		else if (theLocation instanceof PackageInfo)
		{
			for (SimpleTreeNode<ILocationInfo> theChild : aNode.pChildren()) getClasses(aResult, theChild);
		}
		else
		{
			System.err.println("Not handled: "+theLocation);
		}
	}
	
	/**
	 * Presents all the classes/behaviors in a tree.
	 * @author gpothier
	 */
	private class TreeSelector extends LocationTreeTable
	{
		public TreeSelector()
		{
			super(StructureTreeBuilders.createClassTree(
					getStructureDatabase(), 
					itsShowMembers, 
					itsShowMembers));

//			getTree().setShowsRootHandles(false);
			PropertyUtils.connect(pSelectedLocation(), LocationSelectorPanel.this.pSelectedLocation, true);
			setDefaultRenderer(Long.class, new UniversalRenderer<Long>()
			{
				@Override
				protected void setupLabel(JLabel aLabel, Long aValue)
				{
					long v = aValue.longValue();
//					return v < 1000000 ? String.format("%,d", v) : String.format("%,.1fM", v/1000000.0);
					String theLabel = String.format("%,d", v);
					aLabel.setText(theLabel);
					aLabel.setHorizontalAlignment(JLabel.RIGHT);
				}
			});
		}
		
		@Override
		public int getColumnCount()
		{
			return 1;
		}

		@Override
		public Class getColumnClass(int aColumn)
		{
			switch(aColumn)
			{
			case 0: return Long.class;
			default: throw new RuntimeException("No such column: "+aColumn);
			}
		}

		@Override
		public String getColumnName(int aColumn)
		{
			switch(aColumn)
			{
			case 0: return "# ev";
			default: throw new RuntimeException("No such column: "+aColumn);
			}
		}
		
		@Override
		protected int getPreferredColumnWidth(int aColumn)
		{
			switch(aColumn)
			{
			case 0: return 80;
			default: throw new RuntimeException("No such column: "+aColumn);
			}
		}
			
		@Override
		protected Object getValueAt(SimpleTreeNode<ILocationInfo> aNode, ILocationInfo aLocation, int aColumn)
		{
			switch(aColumn)
			{
			case 0: return getEventCountAt(aNode);
			default: throw new RuntimeException("No such column: "+aColumn);
			}
		}
	}
	
	/**
	 * Peer of {@link BigListModel}
	 * @author gpothier
	 */
	private static class BigJList extends JList
	{
		public BigJList()
		{
		}

		public BigJList(BigListModel aDataModel)
		{
			super(aDataModel);
		}

		@Override
		public BigListModel getModel()
		{
			return (BigListModel) super.getModel();
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			getModel().setHideAway(true);
			Dimension thePreferredSize = super.getPreferredSize();
			getModel().setHideAway(false);
			return thePreferredSize;
		}
	}
	
	/**
	 * A list model for huge list for which we do not want the contents
	 * to be retrieved just for calculating the preferred size.
	 * @author gpothier
	 */
	private static abstract class BigListModel extends AbstractListModel
	{
		/**
		 * This flag permits to simulate we are empty
		 * during the call to getPreferredSize, otherwise the full
		 * model is scanned.
		 */
		private boolean itsHideAway = false;
		
		public void setHideAway(boolean aHideAway)
		{
			itsHideAway = aHideAway;
		}
		
		public final Object getElementAt(int aIndex)
		{
			if (itsHideAway) return "A";
			else return getElementAt0(aIndex);
		}
		
		protected abstract Object getElementAt0(int aIndex);
	}
	


	private static class BehaviorListModel extends BigListModel
	{
		private IStructureDatabase itsStructureDatabase;
		private int itsSize;
		
		public BehaviorListModel(IStructureDatabase aStructureDatabase)
		{
			itsStructureDatabase = aStructureDatabase;
			itsSize = itsStructureDatabase.getStats().nBehaviors;
		}

		@Override
		protected Object getElementAt0(int aIndex)
		{
			IBehaviorInfo theBehavior = itsStructureDatabase.getBehavior(aIndex, false);
			return theBehavior != null ?
					""+aIndex+" "+theBehavior.getDeclaringType().getName()+"."+theBehavior.getName()
					: ""+aIndex;
		}

		public int getSize()
		{
			return itsSize;
		}
	}
	
	
	private static class FieldListModel extends BigListModel
	{
		private IStructureDatabase itsStructureDatabase;
		private int itsSize;
		
		public FieldListModel(IStructureDatabase aStructureDatabase)
		{
			itsStructureDatabase = aStructureDatabase;
			itsSize = itsStructureDatabase.getStats().nFields;
		}
		
		@Override
		protected Object getElementAt0(int aIndex)
		{
			IFieldInfo theField = itsStructureDatabase.getField(aIndex, false);
			return theField != null ?
					""+aIndex+" "+theField.getDeclaringType().getName()+"."+theField.getName()
					: ""+aIndex;
		}
		
		public int getSize()
		{
			return itsSize;
		}
	}
	

	private static class ProbeListModel extends BigListModel
	{
		private IStructureDatabase itsStructureDatabase;
		private int itsSize;
		
		public ProbeListModel(IStructureDatabase aStructureDatabase)
		{
			itsStructureDatabase = aStructureDatabase;
			itsSize = itsStructureDatabase.getStats().nProbes;
		}

		@Override
		protected Object getElementAt0(int aIndex)
		{
			ProbeInfo theProbeInfo = itsStructureDatabase.getProbeInfo(aIndex);
			return theProbeInfo != null ?
					""+aIndex+" "+theProbeInfo
					: ""+aIndex;
		}

		public int getSize()
		{
			return itsSize;
		}
	}
	
	
}
