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
package tod.gui.components;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import tod.Util;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IMemberInfo;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;
import zz.utils.tree.ITree;
import zz.utils.tree.SimpleTreeNode;
import zz.utils.treetable.JTreeTable;
import zz.utils.treetable.ZTreeTableModel;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UniversalRenderer;

/**
 * A tree/table for location nodes.
 * The first column of the table is the tree. More columns can be defined
 * by subclasses.
 * @author gpothier
 */
public abstract class LocationTreeTable extends JPanel
{
	private final JTreeTable itsTreeTable;
	private ITree<SimpleTreeNode<ILocationInfo>, ILocationInfo> itsTree;
	
	public final IRWProperty<ILocationInfo> pSelectedLocation = new SimpleRWProperty<ILocationInfo>();

	public LocationTreeTable(ITree<SimpleTreeNode<ILocationInfo>, ILocationInfo> aTree)
	{
		super(new StackLayout());
		
		itsTreeTable = new JTreeTable();
		
		itsTreeTable.getTree().setRootVisible(false);
		itsTreeTable.getTree().setShowsRootHandles(true);
		itsTreeTable.setTableHeader(null);
		
		itsTreeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent aE)
			{
				int theRow = itsTreeTable.getSelectedRow();
				if (theRow == -1) 
				{
					pSelectedLocation.set(null);
				}
				else
				{
					TreePath thePath = itsTreeTable.getTree().getPathForRow(theRow);
					SimpleTreeNode<ILocationInfo> theNode = (SimpleTreeNode) thePath.getLastPathComponent();					
					pSelectedLocation.set(theNode.pValue().get());
				}
			}
		});
		
		itsTreeTable.getTree().setCellRenderer(new MyTreeRenderer());

		add(itsTreeTable);
		setTree(aTree);
	}
	
	public IRWProperty<ILocationInfo> pSelectedLocation()
	{
		return pSelectedLocation;
	}
	
	public void setTree(ITree<SimpleTreeNode<ILocationInfo>, ILocationInfo> aTree)
	{
		itsTree = aTree;
		itsTreeTable.setTreeTableModel(new MyModel(itsTree));
		for(int i=0;i<getColumnCount();i++)
		{
			int theWidth = getPreferredColumnWidth(i);
			if (theWidth != -1) 
			{
				getColumn(i).setPreferredWidth(theWidth);
				getColumn(i).setMaxWidth(theWidth);
			}
		}
	}
	
	public void setDefaultRenderer(Class aClass, TableCellRenderer aRenderer)
	{
		itsTreeTable.setDefaultRenderer(aClass, aRenderer);
	}
	
	public void setDefaultEditor(Class aClass, TableCellEditor aRenderer)
	{
		itsTreeTable.setDefaultEditor(aClass, aRenderer);
	}
	
	public TableColumn getColumn(int aColumn)
	{
		return itsTreeTable.getColumnModel().getColumn(aColumn+1);
	}
	
	public void setRowHeight(int aHeight)
	{
		itsTreeTable.setRowHeight(aHeight);
	}
	
	public void setColumnWidth(int aColumn, int aWidth)
	{
		getColumn(aColumn).setMinWidth(aWidth);
		getColumn(aColumn).setMaxWidth(aWidth);
	}
	
	/**
	 * Number of additional columns.
	 */
	protected int getColumnCount()
	{
		return 0;
	}
	
	/**
	 * Returns the class of an additional column
	 * (first additional column has index 0).
	 */
	protected Class getColumnClass(int aColumn)
	{
		return Object.class;
	}
	
	/**
	 * Returns the name of an additional column
	 * (first additional column has index 0).
	 */
	protected String getColumnName(int aColumn)
	{
		return ""+aColumn;
	}
	
	/**
	 * Returns the preferred width for a particular column, or -1 if
	 * it should not be set.
	 */
	protected int getPreferredColumnWidth(int aColumn)
	{
		return -1;
	}
	
	/**
	 * Whether a cell of an additional column is editable
	 * @param aLocation The location info of the cell's line.
	 * @param aColumn The index of the additional column
	 * (first additional column has index 0).
	 */
	protected boolean isCellEditable(SimpleTreeNode<ILocationInfo> aNode, ILocationInfo aLocation, int aColumn)
	{
		return false;
	}

	protected Object getValueAt(SimpleTreeNode<ILocationInfo> aNode, ILocationInfo aLocation, int aColumn)
	{
		return null;
	}
	
	protected void setValueAt(Object aValue, ILocationInfo aLocation, int aColumn)
	{
	}


	
	/**
	 * Aspect tree model.
	 * @author gpothier
	 */
	private class MyModel extends ZTreeTableModel<SimpleTreeNode<ILocationInfo>, ILocationInfo>
	{

		public MyModel(ITree<SimpleTreeNode<ILocationInfo>, ILocationInfo> aTree)
		{
			super(aTree);
		}

		public int getColumnCount()
		{
			return LocationTreeTable.this.getColumnCount()+1;
		}

		public Class getColumnClass(int aColumn)
		{
			if (aColumn == 0) return null; // handled by jtreetable
			else return LocationTreeTable.this.getColumnClass(aColumn-1);
		}

		public String getColumnName(int aColumn)
		{
			if (aColumn == 0) return "Location";
			else return LocationTreeTable.this.getColumnName(aColumn-1);
		}
		
		@Override
		public boolean isCellEditable(Object aNode, int aColumn)
		{
			if (aColumn == 0) return super.isCellEditable(aNode, aColumn);
			else 
			{
				SimpleTreeNode<ILocationInfo> theNode = (SimpleTreeNode<ILocationInfo>) aNode;
				return LocationTreeTable.this.isCellEditable(theNode, theNode.pValue().get(), aColumn-1);
			}
		}
		
		@Override
		public Object getValueFor(
				SimpleTreeNode<ILocationInfo> aNode,
				ILocationInfo aValue,
				int aColumn)
		{
			if (aColumn == 0) return aValue;
			else return LocationTreeTable.this.getValueAt(aNode, aNode.pValue().get(), aColumn-1);
		}
		
		@Override
		public void setValueAt(Object aValue, Object aNode, int aColumn)
		{
			if (aColumn != 0)
			{
				SimpleTreeNode<ILocationInfo> theNode = (SimpleTreeNode<ILocationInfo>) aNode;
				LocationTreeTable.this.setValueAt(aValue, theNode.pValue().get(), aColumn-1);
			}
		}
	}
	
	/**
	 * Renderer for the locations tree.
	 * @author gpothier
	 */
	private static class MyTreeRenderer extends UniversalRenderer<SimpleTreeNode<ILocationInfo>>
	{
		@Override
		protected String getName(SimpleTreeNode<ILocationInfo> aNode)
		{
			ILocationInfo theLocation = aNode.pValue().get();
			
			if (theLocation instanceof IClassInfo)
			{
				IClassInfo theClass = (IClassInfo) theLocation;
				return Util.getSimpleName(theClass.getName());
			}
			else if (theLocation instanceof IMemberInfo)
			{
				IMemberInfo theMember = (IMemberInfo) theLocation;
				return Util.getFullName(theMember);
			}
			else
			{
				return theLocation.getName();
			}
		}
	}

}
