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

import java.awt.Image;
import java.util.Collection;

import javax.swing.JComponent;

import tod.gui.components.eventsequences.mural.EventMural;

import zz.utils.ItemAction;
import zz.utils.properties.IRWProperty;

/**
 * A view of an horizontal event sequence.
 * Each indivudual components of the view must be requested
 * through corresponding methods. For instance, to obtain the main
 * graphic object (the one that displays the events), use
 * {@link #getEventStripe()};
 * for obtaining the available actions, use {@link #getActions()}.
 * @author gpothier
 */
public interface IEventSequenceView 
{
	/**
	 * Starting timestamp of the displayed time range.
	 */
	public IRWProperty<Long> pStart ();
	
	/**
	 * Ending timestamp of the displayed time range.
	 */
	public IRWProperty<Long> pEnd ();
	
	/**
	 * Sets the timestamp bounds of this view.  
	 */
	public void setLimits(long aFirstTimestamp, long aLastTimestamp);	
	
	/**
	 * Returns the horizontal stripe that displays events.
	 */
	public EventMural getEventStripe();
	
	/**
	 * Returns a collection of available actions for this sequence view.
	 */
	public Collection<ItemAction> getActions();
	
	/**
	 * Returns an icon representing this sequence view.
	 */
	public Image getIcon();
	
	/**
	 * Returns the title of this sequence view.
	 */
	public String getTitle();
	
	/**
	 * Returns the timestamp of the first event in this view.
	 */
	public long getFirstTimestamp();
	
	/**
	 * Returns the timestamp of the last event in this view.
	 */
	public long getLastTimestamp();

}
