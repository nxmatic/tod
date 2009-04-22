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
package tod.gui;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Action;

import tod.tools.scheduling.IJobScheduler;
import tod.tools.scheduling.IJobSchedulerProvider;
import tod.tools.scheduling.Scheduled;
import tod.tools.scheduling.IJobScheduler.JobPriority;

import zz.utils.ArrayStack;
import zz.utils.ItemAction;
import zz.utils.ReverseIteratorWrapper;

/**
 * Implements the web browser-like navigation: backward and forward stack of
 * seeds.
 * @author gpothier
 */
public class BrowserNavigator<S>
implements IJobSchedulerProvider
{
	private final IJobScheduler itsJobScheduler;
	
	private ArrayStack<S> itsBackwardSeeds = new ArrayStack<S>(50);
	private ArrayStack<S> itsForwardSeeds = new ArrayStack<S>();
	
	private Action itsBackwardAction = new BackwardAction();
	private Action itsForwardAction = new ForwardAction();
	
	private S itsCurrentSeed;
	
	public BrowserNavigator(IJobScheduler aJobScheduler)
	{
		itsJobScheduler = aJobScheduler;
	}

	public IJobScheduler getJobScheduler()
	{
		return itsJobScheduler;
	}

	public S getCurrentSeed()
	{
		return itsCurrentSeed;
	}

	protected void setSeed (S aSeed)
	{
		itsCurrentSeed = aSeed;
	}
	
	public Iterable<S> getBackwardSeeds()
	{
		return new Iterable<S>()
		{
			public Iterator<S> iterator()
			{
				return new ReverseIteratorWrapper<S>(itsBackwardSeeds);
			}
		};
	}
	
	public Iterable<S> getForwardSeeds()
	{
		return itsForwardSeeds;
	}
	
	/**
	 * Jumps to the previous view
	 */
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	public void backward()
	{
		if (! itsBackwardSeeds.isEmpty())
		{
			S theSeed = itsBackwardSeeds.pop();
			if (itsCurrentSeed != null) itsForwardSeeds.push(itsCurrentSeed);
			setSeed(theSeed);
			updateActions();
		}
	}
	
	/**
	 * Jump backward to the specified seed. An exception is thrown if the seed is not in
	 * the backward stack
	 * @param aSeed
	 */
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	public void backToSeed(S aSeed)
	{
		while(! itsBackwardSeeds.isEmpty())
		{
			S theSeed = itsBackwardSeeds.pop();
			if (itsCurrentSeed != null) itsForwardSeeds.push(itsCurrentSeed);
			itsCurrentSeed = theSeed;
			if (itsCurrentSeed == aSeed)
			{
				setSeed(itsCurrentSeed);
				updateActions();
				return;
			}
		}
		throw new RuntimeException("Seed not found: "+aSeed);
	}

	/**
	 * Jumps to the view that was active before jumping backwards
	 */
	@Scheduled(value = JobPriority.EXPLICIT, cancelOthers = true)
	public void forward()
	{
		if (! itsForwardSeeds.isEmpty())
		{
			S theSeed = itsForwardSeeds.pop();
			if (itsCurrentSeed != null) itsBackwardSeeds.push(itsCurrentSeed);
			setSeed(theSeed);
			updateActions();
		}
	}
	
	/**
	 * Opens a view for the given seed.
	 */
	public void open (S aSeed)
	{
		if (itsCurrentSeed != null) itsBackwardSeeds.push(itsCurrentSeed);
		itsForwardSeeds.clear();
		setSeed(aSeed);
		updateActions();
	}
	
	/**
	 * Clears the forward/backward history of this navigator.
	 */
	public void clear()
	{
		setSeed(null);
		itsBackwardSeeds.clear();
		itsForwardSeeds.clear();
		updateActions();
	}
	
	private void updateActions()
	{
		itsBackwardAction.setEnabled(itsBackwardAction.isEnabled());
		itsForwardAction.setEnabled(itsForwardAction.isEnabled());
	}
	
	/**
	 * Returns an action that corresponds to the {@link #backward()} operation.
	 */
	public Action getBackwardAction()
	{
		return itsBackwardAction;
	}
	
	/**
	 * Returns an action that corresponds to the {@link #forward()} operation.
	 */
	public Action getForwardAction()
	{
		return itsForwardAction;
	}
	
	private class BackwardAction extends ItemAction
	{
		public BackwardAction()
		{
			setTitle("<");
		}
		
		public void actionPerformed(ActionEvent aE)
		{
			backward();
		}
		
		public boolean isEnabled()
		{
			return ! itsBackwardSeeds.isEmpty();
		}
	}
	
	private class ForwardAction extends ItemAction
	{
		public ForwardAction()
		{
			setTitle(">");
		}
		
		public void actionPerformed(ActionEvent aE)
		{
			forward();
		}
		
		public boolean isEnabled()
		{
			return ! itsForwardSeeds.isEmpty();
		}
	}
}
