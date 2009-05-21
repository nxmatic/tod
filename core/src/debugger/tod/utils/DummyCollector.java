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
package tod.utils;

import tod.core.ILogCollector;

public class DummyCollector implements ILogCollector
{

	public void behaviorExit(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, int aBehaviorId, boolean aHasThrown, Object aResult)
	{
	}

	public void exception(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			String aMethodName, String aMethodSignature, String aMethodDeclaringClassSignature,
			int aOperationBytecodeIndex, Object aException)
	{
	}

	public void exception(
			int aThreadId,
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aException)
	{
	}

	public void fieldWrite(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, int aFieldId, Object aTarget, Object aValue)
	{
	}

	public void newArray(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow, int aProbeId, Object aTarget, int aBaseTypeId, int aSize)
	{
	}

	public void arrayWrite(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, Object aTarget, int aIndex, Object aValue)
	{
	}

	public void instantiation(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, boolean aDirectParent, int aCalledBehaviorId,
			int aExecutedBehaviorId, Object aTarget, Object[] aArguments)
	{
	}

	public void localWrite(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, int aVariableId, Object aValue)
	{
	}

	public void methodCall(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, boolean aDirectParent, int aCalledBehaviorId,
			int aExecutedBehaviorId, Object aTarget, Object[] aArguments)
	{
	}

	public void superCall(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp,
			int[] aAdviceCFlow, int aProbeId, boolean aDirectParent, int aCalledBehaviorid,
			int aExecutedBehaviorId, Object aTarget, Object[] aArguments)
	{
	}


	public void instanceOf(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, Object aObject, int aTypeId, boolean aResult)
	{
	}

	public void thread(int aThreadId, long aJVMThreadId, String aName)
	{
	}

	public void register(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable)
	{
	}

	public void registerClass(long aId, long aLoaderId, String aName)
	{
	}

	public void registerClassLoader(long aId, long aClassId)
	{
	}

	public void registerRefObject(long aId, long aTimestamp, long aClassId)
	{
	}

	public void clear()
	{
	}

	public int flush()
	{
		return 0;
	}
	
}