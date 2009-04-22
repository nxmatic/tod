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
package tod.core;


import tod.agent.Output;
import tod.core.transport.HighLevelEventType;

/**
 * Interface for incoming events. It is able to process high-level events
 * ({@link HighLevelEventType}).
 * @author gpothier
 */
public interface ILogCollector
{
	public void methodCall(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehaviorId,
			int aExecutedBehaviorId,
			Object aTarget, 
			Object[] aArguments);
	
	public void instantiation(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehaviorId,
			int aExecutedBehaviorId,
			Object aTarget,
			Object[] aArguments);
	
	public void superCall(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			boolean aDirectParent,
			int aCalledBehaviorId,
			int aExecutedBehaviorId,
			Object aTarget,
			Object[] aArguments);
	
	public void behaviorExit(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			int aBehaviorId,
			boolean aHasThrown,
			Object aResult);
	
	public void fieldWrite(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			int aFieldId,
			Object aTarget,
			Object aValue);
	
	public void newArray(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			Object aTarget,
			int aBaseTypeId, 
			int aSize);
	
	public void arrayWrite(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp, 
			int[] aAdviceCFlow,
			int aProbeId,
			Object aTarget,
			int aIndex, 
			Object aValue);
	
	public void localWrite(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			int aVariableId, 
			Object aValue);
	
	public void instanceOf(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			int aProbeId,
			Object aObject, 
			int aTypeId,
			boolean aResult);
	
	public void exception(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow, 
			String aMethodName,
			String aMethodSignature,
			String aMethodDeclaringClassSignature,
			int aOperationBytecodeIndex,
			Object aException);
	
	public void exception(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow, 
			int aProbeId,
			Object aException);
	
	public void output(
			int aThreadId, 
			long aParentTimestamp,
			short aDepth,
			long aTimestamp,
			int[] aAdviceCFlow,
			Output aOutput, byte[] aData);
	
	public void thread(
			int aThreadId, 
			long aJVMThreadId,
			String aName);
	
	/**
	 * Registers an object whose state cannot be otherwise determined (eg String, Exception)
	 * @param aIndexable Whether the object is indexable, which means that the db should 
	 * deserialize and index it prior to storing it. Example indexable object: String
	 */
	public void register(long aObjectUID, byte[] aData, long aTimestamp, boolean aIndexable);
	
	/**
	 * Register a "normal" object, ie. an object that is only passed by reference.
	 * @param aId Id of the object
	 * @param aTimestamp Timestamp the object was first encountered
	 * @param aClassId Id of the class of the object.
	 */
	public void registerRefObject(long aId, long aTimestamp, long aClassId);

	/**
	 * Registers a class
	 * @param aId Id of the class
	 * @param aLoaderId Id of the {@link ClassLoader} that loaded the class, or 0 for null
	 * @param aName Name of the class.
	 */
	public void registerClass(long aId, long aLoaderId, String aName);

	/**
	 * Registers a class loader.
	 * @param aId Id of the class loader
	 * @param aClassId Id of the class of the class loader.
	 */
	public void registerClassLoader(long aId, long aClassId);
	
	/**
	 * Clears the database.
	 */
	public void clear();
	
	/**
	 * Flushes buffered events.
	 */
	public int flush();

	
}
