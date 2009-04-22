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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import tod.core.database.event.ILogEvent;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.IEventSeed;
import zz.utils.SimpleComboBoxModel;

/**
 * This panel permits to create and select bookmarks.
 * @author gpothier
 */
public class BookmarkPanel extends JPanel
{
	private IEventSeed itsCurrentSeed;
	private JComboBox itsComboBox;
	private SimpleComboBoxModel itsModel = new SimpleComboBoxModel(new ArrayList());
	
	private Map<String, ILogEvent> itsBookmarks = new HashMap<String, ILogEvent>();

	public BookmarkPanel()
	{
		createUI();
	}

	private void createUI()
	{
		itsComboBox = new JComboBox(itsModel);
		itsComboBox.setEditable(true);
		itsComboBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				selectBookmark((String) itsComboBox.getSelectedItem());
			}
		});
		
		itsComboBox.setToolTipText(
				"<html>" +
				"<b>Bookmarks.</b> Type a name and press enter <br>" +
				"to create a new bookmark, or select an existing <br>" +
				"bookmark in the list.");
		
		itsComboBox.setEnabled(false);
		
		add(itsComboBox);
	}
	
	public void setView(ActivityPanel aView)
	{
		if (aView.getSeed() instanceof IEventSeed)
		{
			IEventSeed theSeed = (IEventSeed) aView.getSeed();
			itsCurrentSeed = theSeed;
		}
		else itsCurrentSeed = null;
		
		itsComboBox.setSelectedIndex(-1);
		itsComboBox.setEnabled(itsCurrentSeed != null);
	}
	
	private void selectBookmark(String aName)
	{
		if (itsCurrentSeed == null) return;
		if (aName == null || aName.length() == 0) return;
		
		ILogEvent theEvent = itsBookmarks.get(aName);
		if (theEvent == null)
		{
			theEvent = itsCurrentSeed.pEvent().get();
			itsBookmarks.put(aName, theEvent);
			itsModel.getList().add(aName);
			Collections.sort(itsModel.getList());
			itsModel.fireContentsChanged();
			itsComboBox.getEditor().setItem(null);
		}
		else
		{
//			itsCurrentSeed.selectEvent(theEvent, new EventSelectedMsg.SM_SelectInBookmarks(aName));
			itsCurrentSeed.pEvent().set(theEvent);
		}
		
		itsComboBox.setSelectedItem(null);
	}
	
}
