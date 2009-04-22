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
package tod.gui.activities.dyncross;

import java.awt.Color;
import java.util.Set;

import tod.core.database.browser.ILogBrowser;
import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.gui.activities.ActivityPanel;
import tod.gui.activities.ActivitySeed;
import zz.utils.list.IList;
import zz.utils.list.ZArrayList;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

public class DynamicCrosscuttingSeed extends ActivitySeed
{
	public final IList<Highlight> pHighlights = new ZArrayList<Highlight>();
	public final IRWProperty<Long> pStart = new SimpleRWProperty<Long>();
	public final IRWProperty<Long> pEnd = new SimpleRWProperty<Long>();

	public DynamicCrosscuttingSeed(ILogBrowser aLog)
	{
		super(aLog);
	}

	@Override
	public Class< ? extends ActivityPanel> getComponentClass()
	{
		return DynamicCrosscuttingActivityPanel.class;
	}
	
	@Override
	public String getKindDescription()
	{
		return "Dynamic crosscutting";
	}

	@Override
	public String getShortDescription()
	{
		return null;
	}

	/**
	 * Represents any kind of Aspect stuff that can be highlighted (full aspect,
	 * or individual advice).
	 * @author gpothier
	 */
	public static class Highlight
	{
		private final Color itsColor;
		private final Set<BytecodeRole> itsRoles;
		
		/**
		 * The highlighted location (should be {@link IAdviceInfo} or {@link IAspectInfo}
		 */
		private final ILocationInfo itsLocation;
		
		public Highlight(Color aColor, Set<BytecodeRole> aRoles, ILocationInfo aLocation)
		{
			itsColor = aColor;
			itsRoles = aRoles;
			itsLocation = aLocation;
		}

		public Color getColor()
		{
			return itsColor;
		}
		
		public Set<BytecodeRole> getRoles()
		{
			return itsRoles;
		}
		
		public ILocationInfo getLocation()
		{
			return itsLocation;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((itsColor == null) ? 0 : itsColor.hashCode());
			result = prime * result + ((itsLocation == null) ? 0 : itsLocation.hashCode());
			result = prime * result + ((itsRoles == null) ? 0 : itsRoles.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final Highlight other = (Highlight) obj;
			if (itsColor == null)
			{
				if (other.itsColor != null) return false;
			}
			else if (!itsColor.equals(other.itsColor)) return false;
			if (itsLocation == null)
			{
				if (other.itsLocation != null) return false;
			}
			else if (!itsLocation.equals(other.itsLocation)) return false;
			if (itsRoles == null)
			{
				if (other.itsRoles != null) return false;
			}
			else if (!itsRoles.equals(other.itsRoles)) return false;
			return true;
		}
	}
}
