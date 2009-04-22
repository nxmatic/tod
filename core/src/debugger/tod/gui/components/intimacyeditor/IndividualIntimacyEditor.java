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
package tod.gui.components.intimacyeditor;

import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.gui.GUIUtils;
import tod.gui.Resources;
import tod.gui.components.eventlist.IntimacyLevel;
import zz.utils.ui.UIUtils;

/**
 * Intimacy editor for a single advice/aspect
 * @author gpothier
 */
class IndividualIntimacyEditor extends AbstractIndividualIntimacyEditor
implements ChangeListener
{
	private final IntimacyLevelEditor itsIntimacyLevelEditor;
	private AbstractButton itsFullObliviousButton;
	private AbstractButton[] itsRoleCheckBoxes;

	public IndividualIntimacyEditor(IntimacyLevelEditor aIntimacyLevelEditor)
	{
		itsIntimacyLevelEditor = aIntimacyLevelEditor;
		createUI();
	}

	private void createUI()
	{
		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		
		itsFullObliviousButton = new JToggleButton();
		itsFullObliviousButton.setIcon(Resources.ICON_INTIMACY.asIcon(IntimacyLevelEditor.ROLE_ICON_SIZE));
		itsFullObliviousButton.setSelectedIcon(Resources.ICON_FULL_OBLIVIOUSNESS.asIcon(IntimacyLevelEditor.ROLE_ICON_SIZE));
		itsFullObliviousButton.setMargin(UIUtils.NULL_INSETS);
		itsFullObliviousButton.addChangeListener(this);
		
		add(itsFullObliviousButton);
		
		JPanel theRolesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		itsRoleCheckBoxes = new AbstractButton[IntimacyLevel.ROLES.length];
		
		int i=0;
		for(BytecodeRole theRole : IntimacyLevel.ROLES)
		{
			itsRoleCheckBoxes[i] = new JToggleButton();
			ImageIcon theIcon = GUIUtils.getRoleIcon(theRole).asIcon(IntimacyLevelEditor.ROLE_ICON_SIZE);
			itsRoleCheckBoxes[i].setSelectedIcon(theIcon);
			itsRoleCheckBoxes[i].setIcon(new ImageIcon(GrayFilter.createDisabledImage(theIcon.getImage())));
			itsRoleCheckBoxes[i].setMargin(UIUtils.NULL_INSETS);
			itsRoleCheckBoxes[i].addChangeListener(this);
			
			theRolesPanel.add(itsRoleCheckBoxes[i]);
			i++;
		}
		
		add(theRolesPanel);
	}

	@Override
	public void setValue(IntimacyLevel aLevel)
	{
		if (aLevel == null)
		{
			for (AbstractButton theCheckBox : itsRoleCheckBoxes) theCheckBox.setSelected(false);
			itsFullObliviousButton.setSelected(true);
		}
		else
		{
			int i=0;
			for(BytecodeRole theRole : IntimacyLevel.ROLES)
			{
				itsRoleCheckBoxes[i++].setSelected(aLevel.showRole(theRole));
			}
			
			itsFullObliviousButton.setSelected(false);
		}
	}
	
	@Override
	public IntimacyLevel getValue()
	{
		if (itsFullObliviousButton.isSelected()) return null;
		else
		{
			Set<BytecodeRole> theRoles = new HashSet<BytecodeRole>();
			int i=0;
			for(BytecodeRole theRole : IntimacyLevel.ROLES)
			{
				if (itsRoleCheckBoxes[i++].isSelected()) theRoles.add(theRole);
			}
			return new IntimacyLevel(theRoles);
		}
	}
	
	public void stateChanged(ChangeEvent aE)
	{
		for (AbstractButton theCheckBox : itsRoleCheckBoxes)
		{
			theCheckBox.setEnabled(! itsFullObliviousButton.isSelected());
		}
		
		itsIntimacyLevelEditor.setLevel(getLocationInfo(), getValue());
	}

}