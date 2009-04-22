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
package tod.gui.components.eventlist;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import tod.core.database.structure.IBehaviorInfo.BytecodeRole;

/**
 * Defines the level of intimacy for aspect debugging.
 * @author gpothier
 */
public final class IntimacyLevel implements Serializable
{
	public static final BytecodeRole[] ROLES = {
		BytecodeRole.ADVICE_ARG_SETUP,
		BytecodeRole.CONTEXT_EXPOSURE,
		BytecodeRole.ADVICE_TEST,
		BytecodeRole.ADVICE_EXECUTE,
	};
	
	public static final IntimacyLevel FULL_INTIMACY = new IntimacyLevel(ROLES);
	public static final IntimacyLevel FULL_OBLIVIOUSNESS = null;
	
	private Set<BytecodeRole> itsRoles;
	
	/**
	 * Minimum intimacy level
	 */
	public IntimacyLevel()
	{
		itsRoles = new HashSet<BytecodeRole>();
	}
	
	private IntimacyLevel(BytecodeRole... aRoles)
	{
		itsRoles = new HashSet<BytecodeRole>();
		for (BytecodeRole theRole : aRoles) itsRoles.add(theRole);
	}
	
	public IntimacyLevel(Set<BytecodeRole> aRoles)
	{
		itsRoles = aRoles;
	}
	
	/**
	 * Whether the given role should be shown in this intimacy level.
	 */
	public boolean showRole(BytecodeRole aRole)
	{
		return itsRoles.contains(aRole);
	}

	public Set<BytecodeRole> getRoles()
	{
		return itsRoles;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itsRoles == null) ? 0 : itsRoles.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		IntimacyLevel other = (IntimacyLevel) obj;
		if (itsRoles == null)
		{
			if (other.itsRoles != null) return false;
		}
		else if (!itsRoles.equals(other.itsRoles)) return false;
		return true;
	}

	/**
	 * Whether the given role is one of those in {@link #ROLES}
	 */
	public static boolean isKnownRole(BytecodeRole aRole)
	{
		for (BytecodeRole theRole : ROLES)
		{
			if (aRole == theRole) return true;
		}
		return false;
	}
	
}
