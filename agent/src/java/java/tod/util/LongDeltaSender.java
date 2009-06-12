/*
 * Created on May 27, 2009
 */
package java.tod.util;

import tod2.agent.AgentDebugFlags;
import tod2.agent.io._ByteBuffer;

public final class LongDeltaSender
{
	private long itsLastValueSent = 0;
	
	private int itsCount = 0;
	private int itsDeltaByteCount = 0;
	private int itsDeltaShortCount = 0;
	private int itsDeltaIntCount = 0;

	private void updateProfile(long aDelta)
	{
        if (AgentDebugFlags.COLLECT_PROFILE)
        {
            if (aDelta >= Byte.MIN_VALUE && aDelta <= Byte.MAX_VALUE) itsDeltaByteCount++;
            else if (aDelta >= Short.MIN_VALUE && aDelta <= Short.MAX_VALUE) itsDeltaShortCount++;
            else if (aDelta >= Integer.MIN_VALUE && aDelta <= Integer.MAX_VALUE) itsDeltaIntCount++;
            
            itsCount++;
        }
	}
	
	public void send(_ByteBuffer aBuffer, long aValue)
	{
		long theDelta = aValue - itsLastValueSent;
		itsLastValueSent = aValue;
	    
	    if (theDelta >= Byte.MIN_VALUE && theDelta <= Byte.MAX_VALUE)
	    {
            aBuffer.put((byte) theDelta);
	    }
	    else
	    {
	        aBuffer.putLong(aValue);
	    }
        
	    updateProfile(theDelta);
	}
	
	public void send(_ByteBuffer aBuffer, long aValue, byte aSendByteCmd, byte aSendFullCmd)
	{
		long theDelta = aValue - itsLastValueSent;
		itsLastValueSent = aValue;
	    
	    if (theDelta >= Byte.MIN_VALUE && theDelta <= Byte.MAX_VALUE)
	    {
	    	aBuffer.put(aSendByteCmd);
            aBuffer.put((byte) theDelta);
	    }
	    else
	    {
	    	aBuffer.put(aSendFullCmd);
	        aBuffer.putLong(aValue);
	    }
        
	    updateProfile(theDelta);
	}
	
	@Override
	public String toString()
	{
		_StringBuilder b = new _StringBuilder();
        b.append(itsCount);
        b.append(" - b: ");
        b.append(itsDeltaByteCount);
        b.append(" - s: ");
        b.append(itsDeltaShortCount);
        b.append(" - i: ");
        b.append(itsDeltaIntCount);
        b.append("\n");
        return b.toString();
	}

}
