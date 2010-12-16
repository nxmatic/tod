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

import java.tod.io._IO;
import java.tod.util._StringBuilder;

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
	
	public volatile static long t = System.nanoTime() << TIMESTAMP_ADJUST_SHIFT;
	
	private static final boolean PRINT_DELTA = false;
	
	private long itsLastTimestamp;
	private long itsTotalDeltas;
	private long itsTimeSincePrint;
	private int itsCount;

	@Override
	public void run()
	{
		try
		{
			while(true)
			{
				update();
				sleep(1);
				
				if (PRINT_DELTA)
				{
					if (itsLastTimestamp != 0)
					{
						long theDelta = t - itsLastTimestamp;
						itsTotalDeltas += theDelta;
						itsCount++;
						
						itsTimeSincePrint += theDelta;
						if (itsTimeSincePrint > 1000000000L)
						{
							_StringBuilder theBuilder = new _StringBuilder();
							theBuilder.append("Avg timestamp delta: ");
							theBuilder.append(itsTotalDeltas / itsCount);
							theBuilder.append("ns");
							_IO.out(theBuilder.toString());
							
							itsTimeSincePrint = 0;
						}
					}
					itsLastTimestamp = t;
				}
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
		return t;
	}
}
