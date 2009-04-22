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
package tod.core.database.structure;

import java.util.Map;

import tod.core.database.structure.IBehaviorInfo.BytecodeRole;

/**
 * Writable extension of {@link IStructureDatabase}.
 * @author gpothier
 */
public interface IMutableStructureDatabase extends IStructureDatabase
{
	/**
	 * This method either creates a new uninitialized class, or
	 * returns the latest added class of the specified name.
	 * If the class is created it is automatically assigned an id and added
	 * to the database.
	 */
	public IMutableClassInfo getNewClass(String aName);
	
	/**
	 * Adds a new class with a specific id.
	 * This is for platforms where ids are not assigned by the structure
	 * database (eg. python).
	 * Othwerwise, use {@link #getNewClass(String)}.
	 */
	public IMutableClassInfo addClass(int aId, String aName);
	
	/**
	 * Same as {@link #getType(String, boolean)}, but if the type is a class and
	 * does not exist, it is created as by {@link #getNewClass(String)}.
	 */
	public ITypeInfo getNewType(String aName);
	
	/**
	 * Override so as to provide mutable version.
	 */
	public IMutableClassInfo getClass(String aName, boolean aFailIfAbsent);
	
	/**
	 * Override so as to provide mutable version.
	 */
	public IMutableClassInfo getClass(int aId, boolean aFailIfAbsent);
	
	/**
	 * Override so as to provide mutable version.
	 */
	public IMutableBehaviorInfo getBehavior(int aId, boolean aFailIfAbsent);

	/**
	 * Creates a new probe and returns its id. 
	 */
	public int addProbe(int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId);
	
	/**
	 * Creates a new probe with the specified id.
	 */
	public void addProbe(int aId, int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId);
	
	/**
	 * Changes the probe info for the given id.
	 */
	public void setProbe(int aProbeId, int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId);
	
	/**
	 * Retrieves the probe at the given location, or create a new one if necessary.
	 * The probes created by this method should only be used for exception processing
	 * (when an exception generated event is received, we have no probe id, but we
	 * have behavior and bytecode index).
	 */
	public ProbeInfo getNewExceptionProbe(int aBehaviorId, int aBytecodeIndex);
	
	/**
	 * Sets the map that maps advice ids to source ranges for a given class.
	 * Several calls with overlapping advice ids can be made, provided there is no
	 * inconsistency.
	 */
	public void setAdviceSourceMap(Map<Integer, SourceRange> aMap);

}
