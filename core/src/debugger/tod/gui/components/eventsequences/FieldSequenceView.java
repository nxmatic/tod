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
package tod.gui.components.eventsequences;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import tod.core.database.browser.IObjectInspector;
import tod.core.database.event.IFieldWriteEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMemberInfo;
import tod.gui.FontConfig;
import tod.gui.IGUIManager;
import tod.gui.SeedHyperlink;
import tod.gui.activities.cflow.CFlowSeed;
import zz.utils.ui.ZLabel;

public class FieldSequenceView extends AbstractMemberSequenceView
{
	public static final Color FIELD_COLOR = Color.BLUE;
	
	private IFieldInfo itsField;

	
	public FieldSequenceView(IGUIManager aGUIManager, IObjectInspector aInspector, IFieldInfo aField)
	{
		super(aGUIManager, FIELD_COLOR, aInspector);
		itsField = aField;
	}

	public String getTitle()
	{
		return "field " + itsField.getName();
	}

	@Override
	protected JComponent getBaloon(ILogEvent aEvent)
	{
		IFieldWriteEvent theEvent = (IFieldWriteEvent) aEvent;
		return createFieldWriteBaloon(theEvent);
	}
	
	private JComponent createFieldWriteBaloon(IFieldWriteEvent aEvent)
	{
		JPanel theContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));

		// Create hyperlink to call event
		CFlowSeed theSeed = new CFlowSeed(getLogBrowser(), aEvent);
		SeedHyperlink theHyperlink = SeedHyperlink.create(getGUIManager(), theSeed, "set");
		theContainer.add (theHyperlink);
		
		// Colon
		theContainer.add (ZLabel.create(": ", FontConfig.TINY_FONT, Color.BLACK));
		
		// Value
		theContainer.add(createBaloon(aEvent.getValue()));
		
		return theContainer;
	}

	@Override
	public IMemberInfo getMember()
	{
		return itsField;
	}
}
