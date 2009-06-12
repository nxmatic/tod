/*
 * Created on May 13, 2009
 */
package tod2.agent;

import java.tod.TracedMethods;

/**
 * Constants defining the different monitoring mode of methods.
 * @see TracedMethods
 * @author gpothier
 */
public class MonitoringMode
{
	/**
	 * Monitoring mode constant meaning a method does not emit any event 
	 */
	public static final int NONE = 0;
	
	/**
	 * Monitoring mode constant meaning a method emits entry and exit events
	 */
	public static final int ENVELOPPE = 1;
	
	/**
	 * Monitoring mode constant meaning a method emits all events 
	 */
	public static final int FULL = 2;
	
	/**
	 * Monitoring mode constant meaning the method has a special monitoring mode
	 * specified separately (not used yet). 
	 */
	public static final int SPECIAL = 3;
	
	public static String toString(int aMode)
	{
		switch(aMode)
		{
		case NONE: return "NONE";
		case ENVELOPPE: return "ENVELOPPE";
		case FULL: return "FULL";
		case SPECIAL: return "SPECIAL";
		default: return "bad mode: "+aMode;
		}
	}

}
