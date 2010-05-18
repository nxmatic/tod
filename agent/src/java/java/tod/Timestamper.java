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
package java.tod;

import tod2.agent.util.BitUtilsLite;

/**
 * A thread that maintains the current timestamp, with a granularity.
 * Permits to avoid too many system calls for obtaining the timestamp.
 * @author gpothier
 */
public class Timestamper extends Thread
{
	/**
	 * Number of bits to shift timestamp values.
	 */
	public static final int TIMESTAMP_ADJUST_SHIFT = TimestampCalibration.shift;
	
	/**
	 * Number of bits of original timestamp values that are considered inaccurate.
	 */
	public static final int TIMESTAMP_ADJUST_INACCURACY = TimestampCalibration.inaccuracy;
	
	/**
	 * Mask of artificial timestamp bits.
	 */
	public static final long TIMESTAMP_ADJUST_MASK = 
		BitUtilsLite.pow2(TIMESTAMP_ADJUST_INACCURACY+TIMESTAMP_ADJUST_SHIFT)-1;
	

	static
	{
		// Start the thread
		new Timestamper();
	}
	
	private Timestamper()
	{
		super("[TOD] Timestamper");
		setDaemon(true);
		start();
	}
	
	public transient static long t = System.nanoTime() << TIMESTAMP_ADJUST_SHIFT;
	
	/**
	 * An int to quickly check if the timestamp has changed.
	 * Contains the lower 32 bits of timestamp.
	 */
	public transient static int lt = (int) t;
	
	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				update();
				sleep(1);
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static long update()
	{
		long theT = System.nanoTime() << TIMESTAMP_ADJUST_SHIFT; 
		t = theT;
		lt = (int) theT; 
		return t;
	}
}
