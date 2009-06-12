/*
 * Created on May 27, 2009
 */
package java.tod.util;

import tod2.agent.AgentDebugFlags;
import tod2.agent.io._ByteBuffer;

public final class IntDeltaSender
{
	private int itsLastValueSent = 0;
	
	private int itsCount = 0;
	private int itsDeltaByteCount = 0;
	private int itsDeltaShortCount = 0;

	private void updateProfile(int aDelta)
	{
        if (AgentDebugFlags.COLLECT_PROFILE)
        {
            if (aDelta >= Byte.MIN_VALUE && aDelta <= Byte.MAX_VALUE) itsDeltaByteCount++;
            else if (aDelta >= Short.MIN_VALUE && aDelta <= Short.MAX_VALUE) itsDeltaShortCount++;
            
            itsCount++;
        }
	}
	
	public void send(_ByteBuffer aBuffer, int aValue)
	{
		int theDelta = aValue - itsLastValueSent;
		itsLastValueSent = aValue;
	    
	    if (theDelta >= Byte.MIN_VALUE && theDelta <= Byte.MAX_VALUE)
	    {
            aBuffer.put((byte) theDelta);
	    }
	    else
	    {
	        aBuffer.putInt(aValue);
	    }
        
	    updateProfile(theDelta);
	}
	
	public void send(_ByteBuffer aBuffer, int aValue, byte aSendByteCmd, byte aSendFullCmd)
	{
		int theDelta = aValue - itsLastValueSent;
		itsLastValueSent = aValue;
	    
	    if (theDelta >= Byte.MIN_VALUE && theDelta <= Byte.MAX_VALUE)
	    {
	    	aBuffer.put(aSendByteCmd);
            aBuffer.put((byte) theDelta);
	    }
	    else
	    {
	    	aBuffer.put(aSendFullCmd);
	        aBuffer.putInt(aValue);
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
        b.append("\n");
        return b.toString();
	}
}
