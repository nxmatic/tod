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
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import tod.core.database.event.ILogEvent;
import tod.core.database.event.IParentEvent;
import tod.gui.GUIUtils;
import tod.gui.kit.AsyncPanel;
import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.Scheduled;
import tod.tools.scheduling.IJobScheduler.JobPriority;
import zz.utils.ui.UIUtils;

/**
 * Represents a stack item in the control flow.
 * @author gpothier
 */
public abstract class AbstractStackNode extends AsyncPanel
{
	private final CallStackPanel itsCallStackPanel;
	
	/**
	 * The event currently selected in this frame.
	 * Note that the frame itself displays the parent of this event.
	 */
	private final ILogEvent itsEvent;
	
	private IParentEvent itsFrameEvent;
	
	/**
	 * Indicates if this stack node corresponds to the currently
	 * selected stack frame. 
	 */
	private boolean itsCurrentStackFrame;

	private boolean itsMouseOver = false;
	
	public AbstractStackNode(
			IJobScheduler aJobScheduler,
			ILogEvent aEvent,
			CallStackPanel aCallStackPanel)
	{
		super(aJobScheduler, JobPriority.AUTO);
		itsEvent = aEvent;
		itsCallStackPanel = aCallStackPanel;
	}
	
	public ILogEvent getEvent()
	{
		return itsEvent;
	}

	@Override
	protected void runJob()
	{
		itsFrameEvent = itsEvent.getParent();
	}

	public IParentEvent getFrameEvent()
	{
		return itsFrameEvent;
	}
	
	public void setCurrentStackFrame(boolean aCurrentStackFrame)
	{
		itsCurrentStackFrame = aCurrentStackFrame;
		repaint();
	}

	protected abstract JComponent createHeader();

	@Override
	protected void updateSuccess()
	{
		setLayout(GUIUtils.createStackLayout());

		if (! getFrameEvent().isDirectParent())
		{
			JComponent theDots = createDots();
			theDots.addMouseListener(this);
			add(theDots);
		}		
		
		JComponent theHeader = createHeader();
		theHeader.addMouseListener(this);
		add(theHeader);
	}
	
	private JComponent createDots()
	{
		return GUIUtils.createLabel("···");
	}
	
	@Override
	protected void paintComponent(Graphics aG)
	{
		Color theColor = Color.ORANGE;
		if (itsCurrentStackFrame) theColor = theColor.darker();
		if (! itsMouseOver || itsCurrentStackFrame) 
			theColor = UIUtils.getLighterColor(theColor);
		aG.setColor(theColor);
		aG.fillRect(0, 0, getWidth(), getHeight());
	}

	@Override
	public void mouseEntered(MouseEvent aE)
	{
		itsMouseOver = true;
		repaint();
	}

	
	@Override
	public void mouseExited(MouseEvent aE)
	{
		itsMouseOver = false;
		repaint();
	}

	@Override
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	public void mousePressed(MouseEvent aE)
	{
		if (! itsCurrentStackFrame)
		{
			//removed old behavior
			//Bus.get(this).postMessage(new EventSelectedMsg(getEvent(), SelectionMethod.SELECT_IN_CALL_STACK));
			itsCallStackPanel.selectEvent(itsEvent);
		}
		
		aE.consume();
	}
}
