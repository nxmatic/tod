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
package tod.tools.scheduling;

import javax.swing.SwingUtilities;

/**
 * A job that runs some code in the Swing event thread
 * when the main work is finished
 * @author gpothier
 */
public abstract class SwingJob implements Runnable
{
	public final void run()
	{
		work();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				update();
			}
		});
	}
	
	/**
	 * Performs the main job, in a worker thread.
	 */
	protected abstract void work();
	
	/**
	 * This method is executed in the Swing event thread once the
	 * main job is finished.
	 */
	protected abstract void update();
}
