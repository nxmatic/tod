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
package tod.gui.activities.threads;

import tod.core.database.browser.ILogBrowser;
import tod.gui.IGUIManager;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import tod.gui.kit.html.HtmlText;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * This seed provides a view that lets the user browse all events that occured in
 * each thread.
 * @author gpothier
 */
public class ThreadsSeed extends ActivitySeed
{
	/**
	 * Start of the displayed range.
	 */
	private IRWProperty<Long> pRangeStart = new SimpleRWProperty<Long>();
	
	/**
	 * End of the displayed range.
	 */
	private IRWProperty<Long> pRangeEnd = new SimpleRWProperty<Long>();

	public ThreadsSeed(ILogBrowser aLog)
	{
		super(aLog);
		pRangeStart().set(aLog.getFirstTimestamp());
		pRangeEnd().set(aLog.getLastTimestamp());
	}

	@Override
	public Class< ? extends ActivityPanel> getComponentClass()
	{
		return ThreadsActivityPanel.class;
	}

	public IRWProperty<Long> pRangeStart()
	{
		return pRangeStart;
	}

	public IRWProperty<Long> pRangeEnd()
	{
		return pRangeEnd;
	}

	@Override
	public String getKindDescription()
	{
		return "Threads summary";
	}

	@Override
	public String getShortDescription()
	{
		return null;
	}

}
