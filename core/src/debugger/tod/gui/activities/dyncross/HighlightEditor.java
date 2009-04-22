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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.gui.GUIUtils;
import tod.gui.Resources;
import tod.gui.activities.dyncross.DynamicCrosscuttingSeed.Highlight;
import tod.gui.components.eventlist.IntimacyLevel;
import tod.gui.components.intimacyeditor.IntimacyLevelEditor;
import zz.utils.Utils;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.ui.SimpleColorChooserPanel;
import zz.utils.ui.UIUtils;
import zz.utils.ui.popup.ButtonPopupComponent;

/**
 * Intimacy editor for a single advice/aspect
 * @author gpothier
 */
class HighlightEditor extends JPanel
implements ChangeListener
{
	private static final Border BORDER = BorderFactory.createLineBorder(Color.BLACK);
	
	private final DynamicCrosscuttingActivityPanel itsDynamicCrosscuttingView;
	
	/**
	 * The currently edited location
	 */
	private ILocationInfo itsLocation;
	
	/**
	 * The currently edited highlight.
	 */
	private Highlight itsHighlight;
	
	private Color itsSelectedColor;
	
	private JPanel itsSelectedColorPanel;
	private ButtonPopupComponent itsButton;

	private AbstractButton[] itsRoleCheckBoxes;
	
	private int itsChanging = 0;
	
	public HighlightEditor(DynamicCrosscuttingActivityPanel aDynamicCrosscuttingView)
	{
		itsDynamicCrosscuttingView = aDynamicCrosscuttingView;
		createUI();
	}

	private void createUI()
	{
		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		
		itsSelectedColorPanel = new JPanel(null);
		itsSelectedColorPanel.setPreferredSize(new Dimension(20, 20));

		add(itsSelectedColorPanel);

		JButton theButton = new JButton(Resources.ICON_TRIANGLE_DOWN.asIcon(9));
		theButton.setMargin(UIUtils.NULL_INSETS);
		
		SimpleColorChooserPanel thePopup = new SimpleColorChooserPanel("Disable");
		thePopup.eChanged().addListener(new IEventListener<Color>() 
			{
				public void fired(IEvent< ? extends Color> aEvent, Color aData)
				{
					selectColor(aData);
				}
			});
		itsButton = new ButtonPopupComponent(thePopup, theButton);
		add(itsButton);

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
	
	protected ILocationInfo getLocationInfo()
	{
		return itsLocation;
	}

	public void setLocationInfo(ILocationInfo aLocation)
	{
		itsLocation = aLocation;
	}
	
	public void setValue(Highlight aHighlight)
	{
		itsChanging++;
		itsHighlight = aHighlight;

		setSelectedColor(itsHighlight != null ? itsHighlight.getColor() : null);
		
		if (itsHighlight == null)
		{
			for (AbstractButton theCheckBox : itsRoleCheckBoxes) theCheckBox.setSelected(false);
		}
		else
		{
			int i=0;
			for(BytecodeRole theRole : IntimacyLevel.ROLES)
			{
				itsRoleCheckBoxes[i++].setSelected(itsHighlight.getRoles().contains(theRole));
			}
		}
		itsChanging--;
	}
	
	private void setSelectedColor(Color aColor)
	{
		itsSelectedColor = aColor;
		if (itsSelectedColor == null)
		{
			itsSelectedColorPanel.setBorder(null);
			itsSelectedColorPanel.setBackground(getBackground());
		}
		else
		{
			itsSelectedColorPanel.setBorder(BORDER);
			itsSelectedColorPanel.setBackground(itsSelectedColor);
		}
		
		for (AbstractButton theButton : itsRoleCheckBoxes) theButton.setEnabled(itsSelectedColor != null);
	}
	
	public Highlight getValue()
	{
		if (itsSelectedColor == null) return null;
		
		Set<BytecodeRole> theRoles = new HashSet<BytecodeRole>();
		int i=0;
		for(BytecodeRole theRole : IntimacyLevel.ROLES)
		{
			if (itsRoleCheckBoxes[i++].isSelected()) theRoles.add(theRole);
		}
		
		return new Highlight(itsSelectedColor, theRoles, itsLocation);
	}
	
	public void stateChanged(ChangeEvent aE)
	{
		assert itsChanging >= 0;
		if (itsChanging == 0)
		{
			Highlight theNewHighlight = getValue();
			if (Utils.different(theNewHighlight, itsHighlight))
			{
				itsHighlight = theNewHighlight;
				itsDynamicCrosscuttingView.setHighlight(itsLocation, itsHighlight);
			}
		}
	}
	
	private void selectColor(Color aColor)
	{
		itsChanging++;
		
		// By default select everything.
		if (itsSelectedColor == null && aColor != null)
		{
			for (AbstractButton theButton : itsRoleCheckBoxes) theButton.setSelected(true);
		}
		
		setSelectedColor(aColor);
		itsButton.hidePopup();
		
		itsChanging--;
		
		stateChanged(null);
	}

}