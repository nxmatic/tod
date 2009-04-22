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
 * An enumeration of the various behaviour types.
 * @author gpothier
 */
public enum BehaviorKind
{
	METHOD("method"), 
	STATIC_METHOD("static method"), 
	CONSTRUCTOR("constructor"), 
	STATIC_INIT("static block");
	
	private String itsName;
	
	BehaviorKind (String aName)
	{
		itsName = aName;
	}
	
	public String getName ()
	{
		return itsName;
	}

	/**
	 * Cached values; call to values() is costly. 
	 */
	public static final BehaviorKind[] VALUES = values();
}
