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



/**
 * Permits to obtain the location info objects that have been registered during a 
 * {@link tod.core.session.ISession}.
 * @deprecated Replaced by {@link IStructureDatabase}
 * @author gpothier
 */
public interface ILocationsRepository
{
	/**
	 * Retrieves a type given its id.
	 */
	public ITypeInfo getType(int aId);
	
	/**
	 * Returns the type object that corresponds to the given name.
	 */
	public ITypeInfo getType(String aName);
	
	/**
	 * Returns all registered types.
	 */
	public Iterable<ITypeInfo> getTypes();
	
	/**
	 * Retrieves a field given its id.
	 */
	public IFieldInfo getField(int aFieldId);
	
	/**
	 * Returns all registered fields.
	 */
	public Iterable<IFieldInfo> getFields();
	
	/**
	 * Retrieves a behavior given its id.
	 */
	public IBehaviorInfo getBehavior(int aBehaviorId);
	
	/**
	 * Returns all registered behaviours.
	 */
	public Iterable<IBehaviorInfo> getBehaviours();
	
	/**
	 * Returns all registered files.
	 */
	public Iterable<String> getFiles();
}
