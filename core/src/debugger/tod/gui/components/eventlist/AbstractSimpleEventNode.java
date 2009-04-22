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
package tod.gui.components.eventlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import tod.core.DebugFlags;
import tod.core.config.TODConfig;
import tod.core.database.browser.LocationUtils;
import tod.core.database.event.ICallerSideEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;
import tod.gui.GUIUtils;
import tod.gui.IGUIManager;
import tod.gui.kit.Bus;
import tod.gui.kit.html.HtmlBody;
import tod.gui.kit.html.HtmlComponent;
import tod.gui.kit.html.HtmlDoc;
import tod.gui.kit.html.HtmlParentElement;
import tod.gui.kit.html.HtmlText;
import tod.gui.kit.messages.EventActivatedMsg;
import tod.gui.kit.messages.EventSelectedMsg;
import tod.gui.kit.messages.EventActivatedMsg.ActivationMethod;
import tod.gui.kit.messages.EventSelectedMsg.SelectionMethod;
import tod.tools.scheduling.IJobSchedulerProvider;
import tod.tools.scheduling.Scheduled;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.Utils;
import zz.utils.primitive.IntArray;
import zz.utils.ui.ResourceUtils.ImageResource;

/**
 * Base class for simple event nodes that display a block of html.
 * @author gpothier
 */
public abstract class AbstractSimpleEventNode extends AbstractEventNode
{
	private static final int ROLE_ICON_SIZE = 15;
	
	private HtmlComponent itsHtmlComponent;
	private HtmlDoc itsDoc;
	
	public AbstractSimpleEventNode(IGUIManager aGUIManager, EventListPanel aListPanel)
	{
		super(aGUIManager, aListPanel);
		
		itsDoc = new HtmlDoc();
		itsHtmlComponent = new HtmlComponent();
		itsHtmlComponent.setOpaque(false);
		itsHtmlComponent.setDoc(itsDoc);
		itsHtmlComponent.addMouseListener(this);
	}
	
	/**
	 * Default UI creation. 
	 * The html component is placed at the center of a {@link BorderLayout}.
	 */
	protected void createUI()
	{
		super.createUI();
		updateHtml();
		setupRoleIcons();
	}
	
	private void setupRoleIcons()
	{
		if (! getConfig().get(TODConfig.WITH_ASPECTS)) return;
		BytecodeRole theRole = LocationUtils.getEventRole(getEvent());
		if (theRole == null) return;

		ImageResource theIcon = GUIUtils.getRoleIcon(theRole);
		if (theIcon == null) return;

		addToGutter(new JLabel(theIcon.asIcon(ROLE_ICON_SIZE)));
	}
	
	protected void updateHtml()
	{
		HtmlBody theBody = itsDoc.getRoot();
		theBody.clear();
		createHtmlUI(theBody);
		itsDoc.update(theBody);
	}
	
	protected abstract void createHtmlUI(HtmlBody aBody);
	
	/**
	 * Adds debugging info to the given element, if debugging info is enabled.
	 */
	protected void createDebugInfo(HtmlParentElement aParent)
	{
		if (DebugFlags.SHOW_DEBUG_GUI)
		{
			String theLocation = "?";
			if (getEvent() instanceof ICallerSideEvent)
			{
				ICallerSideEvent theCallerSideEvent = (ICallerSideEvent) getEvent();
				theLocation = ""+theCallerSideEvent.getOperationBytecodeIndex();
			}
			
			int theSourceId = -1;
			int theShadowId = -10;
			if (getEvent() instanceof ICallerSideEvent)
			{
				ICallerSideEvent theCallerSideEvent = (ICallerSideEvent) getEvent();
				theSourceId = theCallerSideEvent.getAdviceSourceId();
				
				IBehaviorInfo theOperationBehavior = theCallerSideEvent.getOperationBehavior();
				
				if (theOperationBehavior != null)
				{
					Integer theTag = theOperationBehavior.getTag(
							BytecodeTagType.INSTR_SHADOW, 
							theCallerSideEvent.getOperationBytecodeIndex());
					
					if (theTag != null) theShadowId = theTag;
				}
			}
			
			aParent.add(HtmlText.createf(
					" (ts: %d, loc: %s, th: %d, d: %d, asid: %d, sid: %d)",
					getEvent().getTimestamp(),
					theLocation,
					getEvent().getThread().getId(),
					getEvent().getDepth(),
					theSourceId,
					theShadowId));
		}
	}
	
	@Override
	protected JComponent getCenterComponent()
	{
		return itsHtmlComponent;
	}
	
	@Override
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	public void mousePressed(MouseEvent aEvent)
	{
		if (getListPanel() == null) return;
		
		getListPanel().pSelectedEvent().set(getEvent());
		ILogEvent theEvent = getEvent();
		Bus.get(this).postMessage(new EventSelectedMsg(theEvent, SelectionMethod.SELECT_IN_LIST));
		aEvent.consume();			
		
		if (aEvent.isPopupTrigger())
		{
			JPopupMenu theMenu = createPopupMenu();
			if (theMenu != null) theMenu.show(this, aEvent.getX(), aEvent.getY());
		}
		else
		{
			if (aEvent.getClickCount() == 2)
			{
				Bus.get(this).postMessage(new EventActivatedMsg(getEvent(), ActivationMethod.DOUBLE_CLICK));
				getListPanel().eventActivated(getEvent());
			}
		}
	}
	
	private JPopupMenu createPopupMenu()
	{
		List<Action> theActions = getGUIManager().getExtensionPoints().getActionsForEvent(getEvent(), null);
		if (theActions == null || theActions.isEmpty()) return null;
		
		JPopupMenu theMenu = new JPopupMenu();
		Map<String, JMenu> theSubmenusMap = new HashMap<String, JMenu>();
		for (Action theAction : theActions) 
		{
			String theName = (String) theAction.getValue(Action.NAME);
			int i = theName.indexOf('/');
			if (i >= 0)
			{
				String thePrefix = theName.substring(0, i);
				String theSuffix = theName.substring(i+1);
				
				JMenu theSubmenu = theSubmenusMap.get(thePrefix);
				if (theSubmenu == null)
				{
					theSubmenu = new JMenu(thePrefix);
					theSubmenusMap.put(thePrefix, theSubmenu);
					theMenu.add(theSubmenu);
				}
			
				theSubmenu.add(new ActionWrapper(theSuffix, theAction));
			}
			else
			{
				theMenu.add(theAction);
			}
		}
		
		return theMenu;
	}
	
	/**
	 * Called when this event is selected.
	 */
	protected void selected()
	{
//		updateHtml();
		repaint();
	}

	/**
	 * Called when this node is deselected.
	 */
	protected void deselected()
	{
//		updateHtml();
		repaint();
	}
	
	protected boolean isSelected()
	{
		if (getListPanel() == null) return false;
		return Utils.equalOrBothNull(getEvent(), getListPanel().pSelectedEvent().get());
	}
	
	@Override
	protected void paintComponent(Graphics aG)
	{
		aG.setColor(isSelected() ? Color.YELLOW : Color.WHITE);
		aG.fillRect(0, 0, getWidth(), getHeight());
	}
	
	private static class ActionWrapper implements Action
	{
		private String itsName;
		private Action itsDelegate;
		
		public ActionWrapper(String aName, Action aDelegate)
		{
			itsName = aName;
			itsDelegate = aDelegate;
		}

		public void addPropertyChangeListener(PropertyChangeListener aListener)
		{
			itsDelegate.addPropertyChangeListener(aListener);
		}

		public Object getValue(String aKey)
		{
			return Action.NAME.equals(aKey) ? itsName : itsDelegate.getValue(aKey);
		}

		public boolean isEnabled()
		{
			return itsDelegate.isEnabled();
		}

		public void putValue(String aKey, Object aValue)
		{
			throw new UnsupportedOperationException();
		}

		public void removePropertyChangeListener(PropertyChangeListener aListener)
		{
			itsDelegate.removePropertyChangeListener(aListener);
		}

		public void setEnabled(boolean aB)
		{
			itsDelegate.setEnabled(aB);
		}

		public void actionPerformed(ActionEvent aE)
		{
			itsDelegate.actionPerformed(aE);
		}
	}
}
