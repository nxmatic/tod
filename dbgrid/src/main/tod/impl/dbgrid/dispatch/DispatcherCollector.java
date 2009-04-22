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
package tod.impl.dbgrid.dispatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tod.agent.Output;
import tod.core.ILogCollector;
import tod.core.transport.HighLevelEventWriter;
import tod.impl.dbgrid.DebuggerGridConfig;

/**
 * This collector dispatches the event it receives to 
 * a number of target collectors.
 * It uses a load-balancing scheme.
 * @author gpothier
 */
public class DispatcherCollector implements ILogCollector
{
	private final List<NodeProxy> itsProxies = new ArrayList<NodeProxy>();
	/**
	 * Remaining packets to process before changing to another child
	 */
	private int itsPacketsBeforeChange = 0;
	
	private int itsCurrentProxyIndex;
	private NodeProxy itsCurrentProxy;
	
	private HighLevelEventWriter itsWriter;

	public void addProxy(NodeProxy aProxy)
	{
		itsProxies.add(aProxy);
	}
	
	private void balance()
	{
		if (itsPacketsBeforeChange == 0)
		{
			int theMinChild = Integer.MAX_VALUE;
			itsCurrentProxy = null;
			for(int i=0;i<itsProxies.size();i++)
			{
				NodeProxy theProxy = itsProxies.get((i+itsCurrentProxyIndex) % itsProxies.size());
				int theSize = theProxy.getQueueSize();
				if (theSize < theMinChild)
				{
					itsCurrentProxy = theProxy;
					theMinChild = theSize;
					if (theSize == 0) break; // If the proxy is empty there is no need to continue searching
				}
			}
			// We need a bit of round robin in order to properly balance the
			// dispatch under light load.
			itsCurrentProxyIndex++; 
			
			itsPacketsBeforeChange = DebuggerGridConfig.DISPATCH_BATCH_SIZE;
			
			// Switch writer's target
			itsWriter.setReceiver(itsCurrentProxy.getOutStream());
		}
		
		itsPacketsBeforeChange --;
	}
	
	
	public void arrayWrite(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, Object aTarget, int aIndex, Object aValue)
	{
		balance();
		try
		{
			itsWriter.sendArrayWrite(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aTarget, aIndex, aValue);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void instanceOf(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, Object aObject, int aTypeId, boolean aResult)
	{
		balance();
		try
		{
			itsWriter.sendInstanceOf(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aObject, aTypeId, aResult);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void behaviorExit(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, int aBehaviorId, boolean aHasThrown, Object aResult)
	{
		balance();
		try
		{
			itsWriter.sendBehaviorExit(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aBehaviorId, aHasThrown, aResult);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void exception(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			String aMethodName, String aMethodSignature, String aMethodDeclaringClassSignature,
			int aOperationBytecodeIndex, Object aException)
	{
		balance();
		try
		{
			itsWriter.sendException(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aMethodName, aMethodSignature, aMethodDeclaringClassSignature, aOperationBytecodeIndex, aException);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
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
		balance();
		try
		{
			itsWriter.sendException(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aException);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void fieldWrite(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, int aFieldId, Object aTarget, Object aValue)
	{
		balance();
		try
		{
			itsWriter.sendFieldWrite(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aFieldId, aTarget, aValue);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void instantiation(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, boolean aDirectParent, int aCalledBehaviorId, int aExecutedBehaviorId,
			Object aTarget, Object[] aArguments)
	{
		balance();
		try
		{
			itsWriter.sendInstantiation(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aDirectParent, aCalledBehaviorId, aExecutedBehaviorId, aTarget, aArguments);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void localWrite(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, int aVariableId, Object aValue)
	{
		balance();
		try
		{
			itsWriter.sendLocalWrite(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aVariableId, aValue);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void methodCall(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, boolean aDirectParent, int aCalledBehaviorId, int aExecutedBehaviorId,
			Object aTarget, Object[] aArguments)
	{
		balance();
		try
		{
			itsWriter.sendMethodCall(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aDirectParent, aCalledBehaviorId, aExecutedBehaviorId, aTarget, aArguments);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void newArray(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, Object aTarget, int aBaseTypeId, int aSize)
	{
		balance();
		try
		{
			itsWriter.sendNewArray(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aTarget, aBaseTypeId, aSize);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void output(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			Output aOutput, byte[] aData)
	{
		balance();
		try
		{
			itsWriter.sendOutput(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aOutput, aData);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void superCall(int aThreadId, long aParentTimestamp, short aDepth, long aTimestamp, int[] aAdviceCFlow,
			int aProbeId, boolean aDirectParent, int aCalledBehaviorId, int aExecutedBehaviorId,
			Object aTarget, Object[] aArguments)
	{
		balance();
		try
		{
			itsWriter.sendSuperCall(aThreadId, aParentTimestamp, aDepth, aTimestamp, aAdviceCFlow, aProbeId, aDirectParent, aCalledBehaviorId, aExecutedBehaviorId, aTarget, aArguments);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void register(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable)
	{
		balance();
		try
		{
			itsWriter.sendRegister(aObjectUID, aData, aTimestamp, aIndexable);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void registerRefObject(long aId, long aTimestamp, long aClassId)
	{
		balance();
		try
		{
			itsWriter.sendRegisterRefObject(aId, aTimestamp, aClassId);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}	

	public void registerClass(long aId, long aLoaderId, String aName)
	{
		balance();
		try
		{
			itsWriter.sendRegisterClass(aId, aLoaderId, aName);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void registerClassLoader(long aId, long aClassId)
	{
		balance();
		try
		{
			itsWriter.sendRegisterClassLoader(aId, aClassId);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public int flush()
	{
		throw new UnsupportedOperationException();
	}

	public void clear()
	{
		throw new UnsupportedOperationException();
	}

	public void thread(int aThreadId, long aJVMThreadId, String aName)
	{
		throw new UnsupportedOperationException();
	}
	
	


}
