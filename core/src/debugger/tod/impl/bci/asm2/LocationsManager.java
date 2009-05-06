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
package tod.impl.bci.asm2;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import tod.core.database.structure.IMutableStructureDatabase;

/**
 * Manages the locations that are created during the 
 * instrumentation of a behavior.
 * The temporary locations created by this manager have a {@link Label}
 * instead of a concrete bytecode index so that the calculation
 * of the concrete bytecode index can be done after the instrumentation is
 * finished. 
 * @author gpothier
 */
public class LocationsManager
{
	private final IMutableStructureDatabase itsStructureDatabase;
	private List<TmpLocationInfo> itsLocations = new ArrayList<TmpLocationInfo>();
	
	public LocationsManager(IMutableStructureDatabase aStructureDatabase)
	{
		itsStructureDatabase = aStructureDatabase;
	}
	
	/**
	 * Creates a new location at the end of the given list.
	 */
	public int createLocation(InsnList aInsns)
	{
		int theId = itsStructureDatabase.addProbe(-1, -1, null, -1);
		Label theLabel = new Label();
		aInsns.add(new LabelNode(theLabel));
		itsLocations.add(new TmpLocationInfo(theId, theLabel));
		return theId;
	}
	
	/**
	 * Creates a new location before the given node in the given list.
	 */
	public int createLocation(InsnList aInsns, AbstractInsnNode aNode)
	{
		int theId = itsStructureDatabase.addProbe(-1, -1, null, -1);
		Label theLabel = new Label();
		aInsns.insertBefore(aNode, new LabelNode(theLabel));
		itsLocations.add(new TmpLocationInfo(theId, theLabel));
		return theId;
	}
	
	public List<TmpLocationInfo> getLocations()
	{
		return itsLocations;
	}
	
	public static class TmpLocationInfo
	{
		public final int id;
		public final Label label;

		public TmpLocationInfo(int aId, Label aLabel)
		{
			id = aId;
			label = aLabel;
		}
	}
}
