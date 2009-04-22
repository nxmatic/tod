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
package tod.gui.kit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import tod.gui.BrowserNavigator;
import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.gui.Resources;
import tod.gui.activities.ActivitySeed;
import tod.gui.kit.html.HtmlBody;
import tod.gui.kit.html.HtmlComponent;
import tod.gui.kit.html.HtmlDoc;
import tod.gui.kit.html.HtmlElement;
import tod.gui.kit.html.HtmlRaw;
import tod.gui.kit.html.HtmlText;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.ui.ScrollablePanel;
import zz.utils.ui.StackLayout;
import zz.utils.ui.UIUtils;
import zz.utils.ui.popup.ButtonPopupComponent;

/**
 * Base for the buttons that permits to navigate forward or backward in
 * the seeds stacks from a {@link BrowserNavigator}.
 * @author gpothier
 */
public abstract class AbstractNavButton extends JPanel
{
	private final BrowserNavigator<ActivitySeed> itsNavigator;
	private NavStackPanel itsNavStackPanel;
	private ButtonPopupComponent itsNavPopupButton;
	
	public AbstractNavButton(BrowserNavigator<ActivitySeed> aNavigator)
	{
		itsNavigator = aNavigator;
		itsNavStackPanel = new NavStackPanel();
		createUI();
	}
	
	public BrowserNavigator<ActivitySeed> getNavigator()
	{
		return itsNavigator;
	}

	private void createUI()
	{
		setLayout(new BorderLayout());
		
		JButton theBackButton = new JButton(getAction());
		add(theBackButton, BorderLayout.CENTER);
		
		JButton theArrowButton = new JButton(Resources.ICON_TRIANGLE_DOWN.asIcon(10));
		theArrowButton.setMargin(UIUtils.NULL_INSETS);
		
		itsNavPopupButton = new ButtonPopupComponent(itsNavStackPanel, theArrowButton);
		
		itsNavPopupButton.ePopupShowing().addListener(new IEventListener<Void>()
		{
			public void fired(IEvent< ? extends Void> aEvent, Void aData)
			{
				itsNavStackPanel.setup();
			}
		});
		add(itsNavPopupButton, BorderLayout.EAST);
	}
	
	private static HtmlElement createShortDesc(ActivitySeed aSeed)
	{
		String theDescription = aSeed.getShortDescription();
		if (theDescription == null) return null;
		
		HtmlText theText = HtmlText.create(theDescription, FontConfig.SMALL, Color.BLACK);
		theText.addExtraStyle("left", "10");
		return theText;
	}
	
	private static HtmlElement createKindDesc(ActivitySeed aSeed)
	{
		return HtmlText.create(aSeed.getKindDescription(), FontConfig.NORMAL, HtmlText.FONT_WEIGHT_BOLD, Color.BLACK);
	}
	
	protected abstract Action getAction();
	
	protected abstract Iterable<ActivitySeed> getSeedsStack();
	
	/**
	 * The panel that shows the backward navigation stack.
	 * @author gpothier
	 */
	private class NavStackPanel extends JPanel
	{
		private JPanel itsSeedsPanel;
		
		public NavStackPanel()
		{
			createUI();
		}

		private void createUI()
		{
			removeAll();
			setLayout(new StackLayout());
			
			itsSeedsPanel = new ScrollablePanel(GUIUtils.createStackLayout())
			{
				@Override
				public boolean getScrollableTracksViewportWidth()
				{
					return true;
				}
			};
			itsSeedsPanel.setBackground(Color.white);
			JScrollPane theScrollPane = new JScrollPane(itsSeedsPanel)
			{
				@Override
				public Dimension getPreferredSize()
				{
					Dimension theSize = super.getPreferredSize();
					theSize.height += 50; // Hack...
					if (theSize.width > 300) theSize.width = 300;
					if (theSize.height > 500) theSize.height = 500;
					return theSize;
				}
			};
			add(theScrollPane);
		}
		
		private void setup()
		{
			createUI(); // temp
			
			itsSeedsPanel.removeAll();
			
			Iterator<ActivitySeed> theIterator = getSeedsStack().iterator();

			// The seeds are grouped by class.
			Class theGroupClass = null;
			List<ActivitySeed> theSeedGroup = new ArrayList<ActivitySeed>();
			
			while(theIterator.hasNext())
			{
				ActivitySeed theSeed = theIterator.next();
				Class theClass = theSeed.getClass();
				if (theGroupClass == null) theGroupClass = theClass;

				if (theClass.equals(theGroupClass))
				{
					theSeedGroup.add(theSeed);
				}
				else
				{
					setupGroup(theSeedGroup);
					theSeedGroup.clear();
					theSeedGroup.add(theSeed);
					theGroupClass = theClass;
				}
			}
			
			if (! theSeedGroup.isEmpty()) setupGroup(theSeedGroup);
			
			revalidate();
			repaint();
		}

		private void setupGroup(List<ActivitySeed> aGroup)
		{
			if (aGroup.size() == 1)
			{
				ActivitySeed theSeed = aGroup.get(0);
				itsSeedsPanel.add(new SeedPanel(theSeed, true));
			}
			else
			{
				assert ! aGroup.isEmpty();
				ActivitySeed theFirstSeed = aGroup.get(0);
				itsSeedsPanel.add(new HtmlComponent(HtmlDoc.create(createKindDesc(theFirstSeed))));
				
				for (ActivitySeed theSeed : aGroup)
				{
					itsSeedsPanel.add(new SeedPanel(theSeed, false));
				}
			}
		}
		
	}
	
	/**
	 * This panel represents a single seed.
	 * @author gpothier
	 */
	private class SeedPanel extends HtmlComponent
	implements MouseListener
	{
		private final ActivitySeed itsSeed;

		public SeedPanel(ActivitySeed aSeed, boolean aShowKindDesc)
		{
			addMouseListener(this);
			itsSeed = aSeed;
			
			HtmlBody theBody = new HtmlBody();
			
			HtmlElement theShortDesc = createShortDesc(aSeed);
			if (aShowKindDesc || theShortDesc == null) 
			{
				theBody.add(createKindDesc(aSeed));
				theBody.add(HtmlRaw.create("<br>"));
			}
			if (theShortDesc != null) theBody.add(theShortDesc);
			
			setDoc(HtmlDoc.create(theBody));
			
			setBackground(Color.WHITE);
		}
		
		public void mouseEntered(MouseEvent aE)
		{
			setBackground(UIUtils.getLighterColor(Color.BLUE));
		}
		
		public void mouseExited(MouseEvent aE)
		{
			setBackground(Color.WHITE);
		}
		
		public void mousePressed(MouseEvent aE)
		{
			itsNavPopupButton.hidePopup();
			itsNavigator.backToSeed(itsSeed);
		}

		public void mouseClicked(MouseEvent aE)
		{
		}

		public void mouseReleased(MouseEvent aE)
		{
		}
	}
}
