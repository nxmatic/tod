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
package tod.core.database.browser;

public class ShadowId
{
	public final int shadowId;
	public final int adviceSourceId;
	
	public ShadowId(int aAdviceSourceId, int aShadowId)
	{
		adviceSourceId = aAdviceSourceId;
		shadowId = aShadowId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + adviceSourceId;
		result = prime * result + shadowId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ShadowId other = (ShadowId) obj;
		if (adviceSourceId != other.adviceSourceId) return false;
		if (shadowId != other.shadowId) return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		return "srcId: "+adviceSourceId+", shId: "+shadowId;
	}
}