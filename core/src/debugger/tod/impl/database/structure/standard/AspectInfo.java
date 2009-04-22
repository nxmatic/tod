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
package tod.impl.database.structure.standard;

import java.util.ArrayList;
import java.util.List;

import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ILocationInfo.ISerializableLocationInfo;

public class AspectInfo extends LocationInfo 
implements IAspectInfo, ISerializableLocationInfo
{
	private static final long serialVersionUID = 3481200655789101417L;
	private final String itsSourceFile;
	private final List<IAdviceInfo> itsAdvices = new ArrayList<IAdviceInfo>();
	
	
	public AspectInfo(IShareableStructureDatabase aDatabase, int aId, String aSourceFile)
	{
		super(aDatabase, aId);
		itsSourceFile = aSourceFile;
		
		changeName(aSourceFile.substring(aSourceFile.lastIndexOf('.')+1));
	}
	
	@Override
	public String getSourceFile()
	{
		return itsSourceFile;
	}
	
	public void addAdvice(AdviceInfo aAdviceInfo)
	{
		itsAdvices.add(aAdviceInfo);
	}

	public List<IAdviceInfo> getAdvices()
	{
		return itsAdvices;
	}
	

}
