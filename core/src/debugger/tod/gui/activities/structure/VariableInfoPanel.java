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

import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import zz.utils.SimpleListModel;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UniversalRenderer;

/**
 * This panel displays the local variable info table of a behavior
 * @author gpothier
 */
public class VariableInfoPanel extends JPanel
{
	private final IBehaviorInfo itsBehavior;

	public VariableInfoPanel(IBehaviorInfo aBehavior)
	{
		itsBehavior = aBehavior;
		createUI();
	}

	private void createUI()
	{
		List<LocalVariableInfo> theLocalVariables = itsBehavior.getLocalVariables();
		
		JList theList = theLocalVariables != null ? 
				new JList(new SimpleListModel(theLocalVariables))
				: new JList();
				
		theList.setCellRenderer(new UniversalRenderer<LocalVariableInfo>()
				{
					@Override
					protected String getName(LocalVariableInfo aObject)
					{
						return String.format(
								"%s %s (%d-%d): %d",
								aObject.getVariableTypeName(),
								aObject.getVariableName(),
								aObject.getStartPc(),
								aObject.getStartPc()+aObject.getLength(),
								aObject.getIndex());
					}
				});
		
		setLayout(new StackLayout());
		add(new JScrollPane(theList));
	}
	
}
