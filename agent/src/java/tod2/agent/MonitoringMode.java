/*
 * Created on May 13, 2009
 */
package tod2.agent;

import java.tod.TracedMethods;
import java.tod.util._StringBuilder;

/**
 * Constants defining the different monitoring mode of methods.
 * @see TracedMethods
 * @author gpothier
 */
public class MonitoringMode
{
	/**
	 * For methods do not emit any event 
	 */
	public static final int INSTRUMENTATION_NONE = 0;
	
	/**
	 * For methods that emit entry and exit events
	 */
	public static final int INSTRUMENTATION_ENVELOPPE = 1;
	
	/**
	 * For methods that emit all events 
	 */
	public static final int INSTRUMENTATION_FULL = 2;
	
	public static final int CALL_UNMONITORED = 0 << 4;
	public static final int CALL_MONITORED = 1 << 4;
	
	/**
	 * For method groups for which we cannot know beforehand
	 * whether events will be emitted or not.
	 * Eg., a group that has a native override. 
	 */
	public static final int CALL_UNKNOWN = 2 << 4;
	
	public static final int MASK_INSTRUMENTATION = 0x0f;
	public static final int MASK_CALL = 0xf0;
	
	public static String toString(int aMode)
	{
		int theInstrumentation = aMode & MASK_INSTRUMENTATION;
		int theCall = aMode & MASK_CALL;
		
		_StringBuilder theBuilder = new _StringBuilder();
		
		switch(theInstrumentation)
		{
		case INSTRUMENTATION_NONE: 
			theBuilder.append("INSTRUMENTATION_NONE");
			break;
			
		case INSTRUMENTATION_ENVELOPPE: 
			theBuilder.append("INSTRUMENTATION_ENVELOPPE");
			break;
			
		case INSTRUMENTATION_FULL: 
			theBuilder.append("INSTRUMENTATION_FULL");
			break;
			
		default: return "bad mode: "+theInstrumentation;
		}
		
		theBuilder.append(' ');

		switch(theCall)
		{
		case CALL_UNMONITORED: 
			theBuilder.append("CALL_UNMONITORED");
			break;
			
		case CALL_MONITORED: 
			theBuilder.append("CALL_MONITORED");
			break;
			
		case CALL_UNKNOWN: 
			theBuilder.append("CALL_UNKNOWN");
			break;
			
		default: return "bad mode: "+theCall;
		}
		
		return theBuilder.toString();
	}
	
}
