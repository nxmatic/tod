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
package tod.core.transport;

import java.tod.EventCollector;

import tod.agent.BehaviorCallType;
import tod.agent.Output;

/**
 * An interface for collecting low-level events.
 * Modeled after {@link EventCollector}, but adds a timestamp & thread id parameter to each method.
 * @author gpothier
 */
public interface ILowLevelCollector
{
	public void logClInitEnter(int aThreadId, long aTimestamp, int aBehaviorId, BehaviorCallType aCallType);
	public void logClInitExit(int aThreadId, long aTimestamp, int aProbeId, int aBehaviorId);

	public void logBehaviorEnter(int aThreadId, long aTimestamp, int aBehaviorId, BehaviorCallType aCallType, Object aObject, Object[] aArguments);
	public void logBehaviorExit(int aThreadId, long aTimestamp, int aProbeId, int aBehaviorId, Object aResult);
	public void logBehaviorExitWithException(int aThreadId, long aTimestamp, int aBehaviorId, Object aException);

	public void logExceptionGenerated(int aThreadId, long aTimestamp, String aMethodName, String aMethodSignature,
			String aMethodDeclaringClassSignature, int aOperationBytecodeIndex, Object aException);

	public void logLocalVariableWrite(int aThreadId, long aTimestamp, int aProbeId, int aVariableId, Object aValue);
	public void logFieldWrite(int aThreadId, long aTimestamp, int aProbeId, int aFieldId, Object aTarget, Object aValue);
	public void logNewArray(int aThreadId, long aTimestamp, int aProbeId, Object aTarget, int aBaseTypeId, int aSize);
	public void logArrayWrite(int aThreadId, long aTimestamp, int aProbeId, Object aTarget, int aIndex, Object aValue);
	
	public void logInstanceOf(int aThreadId, long aTimestamp, int aProbeId, Object aObject, int aTypeId, boolean aResult);

	public void logBeforeBehaviorCallDry(int aThreadId, long aTimestamp, int aProbeId, int aBehaviorId, BehaviorCallType aCallType);
	public void logAfterBehaviorCallDry(int aThreadId, long aTimestamp);

	public void logBeforeBehaviorCall(int aThreadId, long aTimestamp, int aProbeId, int aBehaviorId, BehaviorCallType aCallType, Object aTarget,
			Object[] aArguments);


	public void logAfterBehaviorCall(int aThreadId, long aTimestamp, int aProbeId, int aBehaviorId, Object aTarget, Object aResult);
	public void logAfterBehaviorCallWithException(int aThreadId, long aTimestamp, int aProbeId, int aBehaviorId, Object aTarget, Object aException);

	public void logOutput(int aThreadId, long aTimestamp, Output aOutput, byte[] aData);

	public void registerThread(int aThreadId, long aJVMThreadId, String aName);
	public void registerObject(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable);
	public void registerClassLoader(long aId, long aClassId);
	public void registerClass(long aId, long aLoaderId, String aName);
	public void registerRefObject(long aId, long aTimestamp, long aClassId);

}