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

import java.io.Serializable;


/**
 * Base interface for location info (ie structural information). 
 * Locations can be types, fields or methods
 * @author gpothier
 */
public interface ILocationInfo
{
	/**
	 * Returns the id of this location.
	 */
	public int getId();

	public String getName();
	
	/**
	 * Returns the database that owns this info.
	 */
	public IStructureDatabase getDatabase();
	
	/**
	 * Returns the name of the file that contains the source code of this location.
	 */
	public String getSourceFile();
	
	/**
	 * Interface for location info implementations that are serializable.
	 * Such implementation should have their reference to the owner database
	 * transient, so that upon arriving at a new location they can be bound
	 * to a local database.
	 * @author gpothier
	 */
	public interface ISerializableLocationInfo extends Serializable, ILocationInfo
	{
		public void setDatabase(IShareableStructureDatabase aDatabase, boolean aIsOriginal);
	}

}