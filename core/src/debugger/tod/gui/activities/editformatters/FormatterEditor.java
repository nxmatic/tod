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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.ILocationInfo;
import tod.gui.GUIUtils;
import tod.gui.components.locationselector.LocationSelectorPanel;
import tod.gui.formatter.CustomObjectFormatter;
import zz.utils.Utils;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;
import zz.utils.ui.PropertyEditor;
import zz.utils.ui.StackLayout;
import zz.utils.ui.crmlist.AbstractJavaCRMListModel;
import zz.utils.ui.crmlist.CRMList;
import zz.utils.ui.popup.StickyPopup;

/**
 * Editor for a {@link CustomObjectFormatter}.
 * @author gpothier
 */
public class FormatterEditor extends JPanel
{
	private final ILogBrowser itsLogBrowser;
	private final CustomObjectFormatter itsFormatter;
	private StickyPopup itsLocationSelectorPopup;
	
	private AbstractJavaCRMListModel itsModel;
	
	private List<String> itsRecognizedTypes = new ArrayList<String>();
	private IRWProperty<String> pName = new SimpleRWProperty<String>()
	{
		@Override
		protected void changed(String aOldValue, String aNewValue)
		{
			itsFormatter.setName(aNewValue);
		}
	};
	
	
	public FormatterEditor(ILogBrowser aLogBrowser, CustomObjectFormatter aFormatter)
	{
		itsLogBrowser = aLogBrowser;
		itsFormatter = aFormatter;
		createUI();
	}

	private void createUI()
	{
		// Put initial state into our intermediate model
		Utils.fillCollection(itsRecognizedTypes, itsFormatter.getRecognizedTypes());
		Collections.sort(itsRecognizedTypes);
		pName.set(itsFormatter.getName());
		
		JTabbedPane theTabbedPane = new JTabbedPane();
		theTabbedPane.addTab("Short formatter", new CodeEditor(itsFormatter)
		{
			@Override
			protected String load()
			{
				return itsFormatter.getShortCode();
			}

			@Override
			protected void save(String aCode)
			{
				itsFormatter.setShortCode(aCode);
			}
		});
		
		theTabbedPane.addTab("Long formatter", new CodeEditor(itsFormatter)
		{
			@Override
			protected String load()
			{
				return itsFormatter.getLongCode();
			}

			@Override
			protected void save(String aCode)
			{
				itsFormatter.setLongCode(aCode);
			}
		});

		setLayout(new BorderLayout());
		add(theTabbedPane, BorderLayout.CENTER);
		add(createTypeSelector(), BorderLayout.EAST);
		
		JPanel theNamePanel = new JPanel(GUIUtils.createSequenceLayout());
		theNamePanel.add(new JLabel("Name: "));
		theNamePanel.add(PropertyEditor.createTextField(pName));
		add(theNamePanel, BorderLayout.NORTH);
	}
	
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		itsFormatter.clearRecognizedTypes();
		for (String theType : itsRecognizedTypes) itsFormatter.addRecognizedType(theType);
	}
	
	/**
	 * Creates the component that permits to select the recognized types
	 * of the formatter
	 * @return
	 */
	private JComponent createTypeSelector()
	{
		CRMList theList = new CRMList()
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
		
		LocationSelectorPanel theLocationSelectorPanel = new LocationSelectorPanel(itsLogBrowser, false)
		{
			@Override
			public void show(ILocationInfo aLocation)
			{
				super.show(aLocation);
				itsLocationSelectorPopup.hide();
				if (aLocation instanceof IClassInfo)
				{
					IClassInfo theClass = (IClassInfo) aLocation;
					itsModel.addElement(theClass.getName());
				}
			}
		};
		
		theLocationSelectorPanel.setPreferredSize(new Dimension(250, 300));
		
		itsLocationSelectorPopup = new StickyPopup(theLocationSelectorPanel, theList.getCreateButton());
		itsModel = new AbstractJavaCRMListModel<String>(itsRecognizedTypes)
		{
			@Override
			protected String newElement()
			{
				itsLocationSelectorPopup.show();
				return null;
			}
		};
		theList.setModel(itsModel);
		
		JPanel thePanel = new JPanel(new BorderLayout());
		thePanel.add(theList, BorderLayout.CENTER);
		thePanel.add(new JLabel("Recognized types"), BorderLayout.NORTH);
		
		return thePanel;
	}
	
	private static abstract class CodeEditor extends JPanel
	{
		private CustomObjectFormatter itsFormatter;
		private JTextArea itsTextArea;
		
		public CodeEditor(CustomObjectFormatter aFormatter)
		{
			itsFormatter = aFormatter;
			createUI();
		}
		
		public CustomObjectFormatter getFormatter()
		{
			return itsFormatter;
		}
		
		private void createUI()
		{
			itsTextArea = new JTextArea();
			itsTextArea.setText(load());
			
			itsTextArea.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent aE)
				{
					save(itsTextArea.getText());
				}
			});
			
			setLayout(new StackLayout());
			add(new JScrollPane(itsTextArea));
		}
		
		protected abstract String load();
		protected abstract void save(String aCode);
	}
}
