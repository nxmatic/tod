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
package tod.gui.activities.editformatters;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tod.gui.IContext;
import tod.gui.activities.ActivityPanel;
import tod.gui.formatter.CustomFormatterRegistry;
import tod.gui.formatter.CustomObjectFormatter;
import tod.gui.kit.SavedSplitPane;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UniversalRenderer;
import zz.utils.ui.crmlist.AbstractJavaCRMListModel;
import zz.utils.ui.crmlist.CRMList;
import zz.utils.ui.crmlist.CRMListModel;

/**
 * This view permits to edit the set of available formatters.
 * @author gpothier
 */
public class EditFormattersActivityPanel extends ActivityPanel<FormattersSeed>
{
	private static final String PROPERTY_SPLITTER_POS = "formattersView.splitterPos";

	private JPanel itsEditorHolder;
	
	public EditFormattersActivityPanel(IContext aContext)
	{
		super(aContext);
	}

	@Override
	public void init()
	{
		super.init();

		JSplitPane theSplitPane = new SavedSplitPane(JSplitPane.HORIZONTAL_SPLIT, getGUIManager(), PROPERTY_SPLITTER_POS);
		theSplitPane.setResizeWeight(0.5);
		
		theSplitPane.setLeftComponent(createSelector());
		
		itsEditorHolder = new JPanel(new StackLayout());
		theSplitPane.setRightComponent(itsEditorHolder);
		
		setLayout(new StackLayout());
		add(theSplitPane);
	}
	
	private void show(CustomObjectFormatter aFormatter)
	{
		itsEditorHolder.removeAll();
		itsEditorHolder.add(new FormatterEditor(getLogBrowser(), aFormatter));
		revalidate();
		repaint();
	}
	
	private JComponent createSelector()
	{
		final CustomFormatterRegistry theRegistry = getGUIManager().getSettings().getCustomFormatterRegistry();
		CRMListModel theModel = new AbstractJavaCRMListModel<CustomObjectFormatter>(theRegistry.getFormatters())
		{

			@Override
			public boolean canMoveElement(int aSourceIndex, int aTargetIndex)
			{
				return false;
			}

			@Override
			protected CustomObjectFormatter newElement()
			{
				CustomObjectFormatter theFormatter = theRegistry.createFormatter();
				theFormatter.setName("<New formatter>");
				return theFormatter;
			}
		};
		
		final CRMList theList = new CRMList(theModel)
		{
			@Override
			protected String getUpLabel()
			{
				return null;
			}
			
			@Override
			protected String getDownLabel()
			{
				return null;
			}
		};
		
		theList.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent aE)
			{
				if (! aE.getValueIsAdjusting())
				{
					EditFormattersActivityPanel.this.show((CustomObjectFormatter) theList.getSelectedValue());
				}
			}
		});
		
		theList.setCellRenderer(new UniversalRenderer<CustomObjectFormatter>()
				{
					@Override
					protected String getName(CustomObjectFormatter aObject)
					{
						return aObject.getName();
					}
				});
		
		return theList;
	}
}
