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
package tod.gui.activities.dyncross;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import tod.core.database.browser.ICompoundFilter;
import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.ILogBrowser;
import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.tree.StructureTreeBuilders;
import tod.gui.BrowserData;
import tod.gui.IContext;
import tod.gui.IGUIManager;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.dyncross.DynamicCrosscuttingSeed.Highlight;
import tod.gui.components.LocationTreeTable;
import tod.gui.components.eventlist.IntimacyLevel;
import tod.gui.components.highlighter.EventHighlighter;
import tod.gui.kit.SavedSplitPane;
import zz.utils.list.IList;
import zz.utils.list.IListListener;
import zz.utils.properties.IProperty;
import zz.utils.properties.PropertyListener;
import zz.utils.tree.ITree;
import zz.utils.tree.SimpleTreeNode;
import zz.utils.ui.StackLayout;

/**
 * This view displays the dynamic crosscutting of aspects
 * (aka. aspect murals).
 * @author gpothier
 */
public class DynamicCrosscuttingActivityPanel extends ActivityPanel<DynamicCrosscuttingSeed>
implements IListListener<Highlight>
{
	private static final int COLUMN_HIGHLIGHT = 0;
	
	private MyHighlighter itsHighlighter;
	
	/**
	 * We maintain this temporary binding between the UI and the seed's highlights list.
	 */
	private Map<ILocationInfo, Highlight> itsHighlightsMap = new HashMap<ILocationInfo, Highlight>();
	
	public DynamicCrosscuttingActivityPanel(IContext aContext)
	{
		super(aContext);
	}

	@Override
	protected void connectSeed(DynamicCrosscuttingSeed aSeed)
	{
		connect(aSeed.pStart, itsHighlighter.pStart());
		connect(aSeed.pEnd, itsHighlighter.pEnd());
		
		itsHighlighter.reloadContext();
		
		itsHighlightsMap.clear();
		for (Highlight theHighlight : aSeed.pHighlights)
		{
			itsHighlightsMap.put(theHighlight.getLocation(), theHighlight);
		}
		
		aSeed.pHighlights.addHardListener(this);
		
		setupHighlights();
	}

	@Override
	protected void disconnectSeed(DynamicCrosscuttingSeed aSeed)
	{
		disconnect(aSeed.pStart, itsHighlighter.pStart());
		disconnect(aSeed.pEnd, itsHighlighter.pEnd());
		aSeed.pHighlights.removeListener(this);
	}

	@Override
	public void init()
	{
		super.init();
		
		JSplitPane theSplitPane = new SavedSplitPane(getGUIManager(), "dynamicCrosscuttingView.splitterPos");

		// Left part
		MyTreeTable theTreeTable = new MyTreeTable(StructureTreeBuilders.createAspectTree(
				getLogBrowser().getStructureDatabase(), 
				true));          
		
		Dimension theSize = new HighlightEditor(this).getPreferredSize();
		theTreeTable.setRowHeight(theSize.height + 1);
		theTreeTable.setColumnWidth(COLUMN_HIGHLIGHT, theSize.width);
		
		HighlightCellEditor theEditor = new HighlightCellEditor();
		theTreeTable.setDefaultRenderer(Highlight.class, theEditor);
		theTreeTable.setDefaultEditor(Highlight.class, theEditor);
		
		theTreeTable.pSelectedLocation.addHardListener(new PropertyListener<ILocationInfo>()
				{
					@Override
					public void propertyChanged(
							IProperty<ILocationInfo> aProperty, 
							ILocationInfo aOldValue,
							final ILocationInfo aNewValue)
					{
						if (aNewValue != null) 
						{
							// Delay a bit showing the source, as it causes issues.
							new Thread("DynCC goto source scheduler")
							{
								@Override
								public void run()
								{
									try
									{
										sleep(300);
										getGUIManager().gotoSource(aNewValue);
									}
									catch (InterruptedException e)
									{
										throw new RuntimeException(e);
									}
								}
							}.start();
						}
					}
				});
		
		JScrollPane theScrollPane = new JScrollPane(theTreeTable);
		
		// Right part
		JPanel theRightPanel = new JPanel(new BorderLayout());
//		theRightPanel.add(new LegendPanel(), BorderLayout.SOUTH);
		
		itsHighlighter = new MyHighlighter(getGUIManager(), getLogBrowser());
		theRightPanel.add(itsHighlighter, BorderLayout.CENTER);
		
		theSplitPane.setLeftComponent(theScrollPane);
		theSplitPane.setRightComponent(theRightPanel);
		
		setLayout(new StackLayout());
		add(theSplitPane);
	}
	
	private IEventBrowser createBrowser(Highlight aHighlight)
	{
		ICompoundFilter theUnionFilter = getLogBrowser().createUnionFilter();
		for (int theSourceId : LocationUtils.getAdviceSourceIds(aHighlight.getLocation()))
		{
			if (hasAllRoles(aHighlight))
			{
				theUnionFilter.add(getLogBrowser().createAdviceCFlowFilter(theSourceId));
				theUnionFilter.add(getLogBrowser().createAdviceSourceIdFilter(theSourceId));
			}
			else
			{
				ICompoundFilter theRolesFilter = getLogBrowser().createUnionFilter();
				for (BytecodeRole theRole : aHighlight.getRoles()) 
				{
					if (theRole == BytecodeRole.ADVICE_EXECUTE)
					{
						theRolesFilter.add(getLogBrowser().createAdviceCFlowFilter(theSourceId));
					}

					theRolesFilter.add(getLogBrowser().createIntersectionFilter(
							getLogBrowser().createRoleFilter(theRole),
							getLogBrowser().createAdviceSourceIdFilter(theSourceId)));
				}
				
				theUnionFilter.add(theRolesFilter);
			}
		}
		return getLogBrowser().createBrowser(theUnionFilter);
	}
	
	/**
	 * Whether this highlight has all the available roles.
	 */
	private boolean hasAllRoles(Highlight aHighlight)
	{
		for (BytecodeRole theRole : IntimacyLevel.ROLES)
		{
			if(! aHighlight.getRoles().contains(theRole)) return false;
		}
		return true;
	}
	
	void setHighlight(ILocationInfo aLocation, Highlight aHighlight)
	{
		Highlight thePrevious = itsHighlightsMap.get(aLocation);
		if (thePrevious != null) getSeed().pHighlights.remove(thePrevious);
		itsHighlightsMap.put(aLocation, aHighlight);
		if (aHighlight != null) getSeed().pHighlights.add(aHighlight);
	}
	
	/**
	 * Initial setup of highlights
	 */
	private void setupHighlights()
	{
		for(Highlight theHighlight : getSeed().pHighlights)
		{
			itsHighlighter.pHighlightBrowsers.add(new BrowserData(
					createBrowser(theHighlight),
					theHighlight.getColor(),
					BrowserData.DEFAULT_MARK_SIZE+1));
		}
	}
	
	public void elementAdded(IList<Highlight> aList, int aIndex, Highlight aElement)
	{
		itsHighlighter.pHighlightBrowsers.add(
				aIndex, 
				new BrowserData(createBrowser(aElement), aElement.getColor(), BrowserData.DEFAULT_MARK_SIZE+1));
	}

	public void elementRemoved(IList<Highlight> aList, int aIndex, Highlight aElement)
	{
		itsHighlighter.pHighlightBrowsers.remove(aIndex);
	}
	
	/**
	 * Our subclass of {@link LocationTreeTable} that has two additional columns:
	 * - enable and choose intimacy
	 * - choose color
	 * @author gpothier
	 */
	private class MyTreeTable extends LocationTreeTable
	{
		public MyTreeTable(ITree<SimpleTreeNode<ILocationInfo>, ILocationInfo> aTree)
		{
			super(aTree);
		}

		@Override
		protected int getColumnCount()
		{
			return 1;
		}

		@Override
		protected Class getColumnClass(int aColumn)
		{
			switch(aColumn) 
			{
			case COLUMN_HIGHLIGHT: return Highlight.class;
			default: throw new RuntimeException("Not handled: "+aColumn);
			}
		}

		@Override
		protected Object getValueAt(SimpleTreeNode<ILocationInfo> aNode, ILocationInfo aLocation, int aColumn)
		{
			switch(aColumn) 
			{
			case COLUMN_HIGHLIGHT: 
				Highlight theHighlight = itsHighlightsMap.get(aLocation);
				System.out.println("location: "+aLocation+" -> "+theHighlight);
				return theHighlight;
			default: throw new RuntimeException("Not handled: "+aColumn);
			}
		}

		@Override
		protected boolean isCellEditable(SimpleTreeNode<ILocationInfo> aNode, ILocationInfo aLocation, int aColumn)
		{
			return (aLocation instanceof IAspectInfo) || (aLocation instanceof IAdviceInfo);
		}
	}

//	private class LegendPanel extends JPanel
//	implements IListListener<Highlight>
//	{
//		public LegendPanel()
//		{
//			super(new FlowLayout());
//			for(Highlight theHighlight : itsSeed.pHighlights)
//			{
//				add(new LegendItem(theHighlight, theHighlight.getColor()));
//			}
//		}
//		
//		@Override
//		public void addNotify()
//		{
//			super.addNotify();
//			itsSeed.pHighlights.addHardListener(this);
//		}
//		
//		@Override
//		public void removeNotify()
//		{
//			super.removeNotify();
//			itsSeed.pHighlights.removeListener(this);
//		}
//
//		public void elementAdded(IList<Highlight> aList, int aIndex, Highlight aElement)
//		{
//			add(new LegendItem(aElement, aElement.getColor()), null, aIndex);
//			revalidate();
//			repaint();
//		}
//
//		public void elementRemoved(IList<Highlight> aList, int aIndex, Highlight aElement)
//		{
//			remove(aIndex);
//			revalidate();
//			repaint();
//		}
//		
//	}
//	
//	private static class LegendItem extends JPanel
//	{
//		private final Highlight itsHighlight;
//		
//		public LegendItem(Highlight aHighlight, Color aMarkColor)
//		{
//			super(new FlowLayout(FlowLayout.LEFT));
//			
//			itsHighlight = aHighlight;
//			
//			JPanel theColorPanel = new JPanel(null);
//			theColorPanel.setBackground(aMarkColor);
//			theColorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//			theColorPanel.setPreferredSize(new Dimension(30, 20));
//			add(theColorPanel);
//			
//			add(new JLabel(itsHighlight.toString()));
//		}
//	}
//	
	private class MyHighlighter extends EventHighlighter
	{
		public MyHighlighter(IGUIManager aGUIManager, ILogBrowser aLogBrowser)
		{
			super(aGUIManager, aLogBrowser);
		}

		@Override
		protected void perThread()
		{
			if (getSeed() != null) setMuralPainter(new AdviceCFlowMuralPainter(getSeed().pHighlights));
			super.perThread();
		}
	}
	
	/**
	 * The table cell editor/renderer for the intimacy level column.
	 * @author gpothier
	 */
	private class HighlightCellEditor extends AbstractCellEditor
	implements TableCellRenderer, TableCellEditor
	{
		private HighlightEditor itsRenderer = new HighlightEditor(DynamicCrosscuttingActivityPanel.this);
		private HighlightEditor itsEditor = new HighlightEditor(DynamicCrosscuttingActivityPanel.this);
		private JPanel itsNoValueEditor = new JPanel();
		
		private void setup(
				JComponent aEditor,
				JTable aTable,
				boolean aIsSelected,
				boolean aHasFocus)
		{
			aEditor.setBackground(aIsSelected ? aTable.getSelectionBackground() : aTable.getBackground());
		}
		
		public Component getTableCellRendererComponent(
				JTable aTable,
				Object aValue,
				boolean aIsSelected,
				boolean aHasFocus, 
				int aRow,
				int aColumn)
		{
//			if (aValue == NO_VALUE) return itsNoValueEditor;
			
			setup(itsRenderer, aTable, aIsSelected, aHasFocus);
			itsRenderer.setValue((Highlight) aValue);
			return itsRenderer;
		}

		public Component getTableCellEditorComponent(
				JTable aTable,
				Object aValue,
				boolean aIsSelected,
				int aRow,
				int aColumn)
		{
//			if (aValue == NO_VALUE) return itsNoValueEditor;

			setup(itsEditor, aTable, aIsSelected, true);
			ILocationInfo theLocation = (ILocationInfo) aTable.getModel().getValueAt(aRow, 0);
			itsEditor.setLocationInfo(theLocation);
			itsEditor.setValue((Highlight) aValue);
			return itsEditor;
		}

		public Object getCellEditorValue()
		{
			return itsEditor.getValue();
		}
	}

}
