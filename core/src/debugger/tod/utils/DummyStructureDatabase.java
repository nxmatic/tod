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
package tod.utils;

import java.io.IOException;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.SourceRange;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.SnapshotProbeInfo;

public class DummyStructureDatabase 
implements IMutableStructureDatabase
{
	private static DummyStructureDatabase INSTANCE = new DummyStructureDatabase();

	public static DummyStructureDatabase getInstance()
	{
		return INSTANCE;
	}

	private DummyStructureDatabase()
	{
	}
	
	public TODConfig getConfig()
	{
		return null;
	}

	public IMutableBehaviorInfo getBehavior(int aId, boolean aFailIfAbsent)
	{
		return null;
	}

	public int getBehaviorId(String aClassName, String aMethodName, String aMethodSignature)
	{
		return 0;
	}

	public IBehaviorInfo[] getBehaviors()
	{
		return null;
	}

	public IMutableClassInfo getNewClass(String aName)
	{
		return null;
	}

	public int getBehaviorSignatureId(IBehaviorInfo aBehavior)
	{
		return 0;
	}

	public IMutableClassInfo getClass(int aId, boolean aFailIfAbsent)
	{
		return null;
	}

	public IMutableClassInfo getClass(String aName, boolean aFailIfAbsent)
	{
		return null;
	}

	public IClassInfo getClass(String aName, String aChecksum, boolean aFailIfAbsent)
	{
		return null;
	}

	public IClassInfo getUnknownClass()
	{
		return null;
	}

	public IClassInfo[] getClasses(String aName)
	{
		return null;
	}

	public IFieldInfo getField(int aId, boolean aFailIfAbsent)
	{
		return null;
	}

	public String getId()
	{
		return "dummy";
	}

	public ITypeInfo getType(String aName, boolean aFailIfAbsent)
	{
		return null;
	}

	public IClassInfo[] getClasses()
	{
		return null;
	}

	public Stats getStats()
	{
		return null;
	}

	public ITypeInfo getNewType(String aName)
	{
		return null;
	}

	public IArrayTypeInfo getArrayType(ITypeInfo aBaseType, int aDimensions)
	{
		return null;
	}

	public ITypeInfo getType(int aId, boolean aFailIfAbsent)
	{
		return null;
	}

	public int addProbe(int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId)
	{
		return 0;
	}

	public void setProbe(int aProbeId, int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId)
	{
	}

	public ProbeInfo getProbeInfo(int aProbeId)
	{
		return null;
	}

	public int getProbeCount()
	{
		return 0;
	}

	public void setAdviceSourceMap(Map<Integer, SourceRange> aMap)
	{
	}

	public IAdviceInfo getAdvice(int aAdviceId)
	{
		return null;
	}

	public Map<String, IAspectInfo> getAspectInfoMap()
	{
		return null;
	}

	public ProbeInfo getNewExceptionProbe(int aBehaviorId, int aBytecodeIndex)
	{
		return null;
	}

	public IMutableClassInfo addClass(int aId, String aName)
	{
		return null;
	}

	public void addProbe(
			int aId,
			int aBehaviorId,
			int aBytecodeIndex,
			BytecodeRole aRole,
			int aAdviceSourceId)
	{
	}

	public SnapshotProbeInfo getNewSnapshotProbe(int aBehaviorId, int aProbeIndex, String aSignature)
	{
		return null;
	}

	public SnapshotProbeInfo getSnapshotProbeInfo(int aProbeId)
	{
		return null;
	}

	public void registerSnapshotSignature(String aSignature)
	{
	}

	public Iterable<String> getRegisteredSnapshotSignatures()
	{
		return null;
	}

	public void addListener(Listener aListener)
	{
	}

	public void removeListener(Listener aListener)
	{
	}

	public boolean isInIdScope(String aClassName)
	{
		return false;
	}

	public boolean isInScope(String aClassName)
	{
		return false;
	}

	public void replayModeChanges(int aClassId)
	{
	}
	
	public void save() throws IOException
	{
	}
}
