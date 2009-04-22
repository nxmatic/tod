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
package tod.gui.activities.cflow;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import tod.core.IBookmarks;
import tod.core.IBookmarks.EventBookmark;
import tod.core.database.event.ILogEvent;
import tod.gui.GUIUtils;
import tod.gui.Resources;
import zz.utils.ui.AbstractOptionPanel;
import zz.utils.ui.SimpleColorChooserPanel;
import zz.utils.ui.UIUtils;
import zz.utils.ui.popup.ButtonPopupComponent;

public class BookmarkButton extends ButtonPopupComponent
{
	private ILogEvent itsCurrentEvent;
	private IBookmarks itsBookmarks;

	public BookmarkButton(IBookmarks aBookmarks)
	{
		super(new JButton(Resources.ICON_BOOKMARK.asIcon(20)));
		itsBookmarks = aBookmarks;
		getButton().setToolTipText("Bookmark current event");
		getButton().setMargin(UIUtils.NULL_INSETS);
		setPopupComponent(new BookmarkPopup());
	}

	public void setCurrentEvent(ILogEvent aCurrentEvent)
	{
		itsCurrentEvent = aCurrentEvent;
		getButton().setEnabled(itsCurrentEvent != null);
	}
	
	private class BookmarkPopup extends AbstractOptionPanel
	{
		private JTextField itsNameTextField;
		private SimpleColorChooserPanel itsColorChooserPanel;

		public BookmarkPopup()
		{
		}

		@Override
		protected JComponent createComponent()
		{
			JPanel thePanel = new JPanel(GUIUtils.createStackLayout());
			
			thePanel.add(new JLabel("Choose a name for this event (optional):"));
			itsNameTextField = new JTextField(20);
			thePanel.add(itsNameTextField);
			
			thePanel.add(new JLabel(" "));
			thePanel.add(new JLabel("Choose a color for this event (optional):"));
			JPanel theP1 = new JPanel(new FlowLayout());
			itsColorChooserPanel = new SimpleColorChooserPanel("None");
			theP1.add(itsColorChooserPanel);
			thePanel.add(theP1);
			
			return thePanel;
		}
		
		@Override
		protected void ok()
		{
			super.ok();
			Color theColor = itsColorChooserPanel.pColor().get();
			String theName = itsNameTextField.getText();
			if (theName.length() == 0) theName = null;
			
			itsBookmarks.addBookmark(new EventBookmark(theColor, theName, null, itsCurrentEvent, false));
			hidePopup();
		}
		
		@Override
		protected void cancel()
		{
			super.cancel();
			hidePopup();
		}
	}
}
