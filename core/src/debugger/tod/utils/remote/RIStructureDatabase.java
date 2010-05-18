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
package tod.utils.remote;

import java.util.List;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableFieldInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IClassInfo.Bytecode;
import tod.core.database.structure.IStructureDatabase.LineNumberInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.database.structure.IStructureDatabase.Stats;
import tod.impl.database.structure.standard.TagMap;
import zz.utils.srpc.IRemote;

/**
 * A clone of {@link IStructureDatabase} that is used to create a remotely
 * accessible locations repository.
 * @author gpothier
 */
public interface RIStructureDatabase extends IRemote
{
	public void addListener(RIStructureDatabaseListener aListener);
	
	public TODConfig getConfig();
	public String getId();
	public IClassInfo getClass(String aName, String aChecksum, boolean aFailIfAbsent);
	public IClassInfo[] getClasses(String aName);
	public IClassInfo[] getClasses();
	public IClassInfo getClass(String aName, boolean aFailIfAbsent);
	public IClassInfo getClass(int aId, boolean aFailIfAbsent);
	public IClassInfo[] getClasses(int[] aIds, boolean aFailIfAbsent);
	public IClassInfo getNewClass(String aName);
	public ITypeInfo getType(String aName, boolean aFailIfAbsent);
	public Stats getStats();
	public ProbeInfo[] getProbeInfos(int aAvailableCount);
	public int getNewExceptionProbeInfo(int aBehaviorId, int aBytecodeIndex);
	public IAdviceInfo getAdvice(int aAdviceId);
	public Map<String, IAspectInfo> getAspectInfoMap();
	
	public Bytecode _getClassBytecode(int aClassId);
	public String _getClassSMAP(int aClassId);
	public Map<String, IMutableFieldInfo> _getClassFieldMap(int aClassId);
	public Map<String, IMutableBehaviorInfo> _getClassBehaviorsMap(int aClassId);
	public List<LocalVariableInfo> _getBehaviorLocalVariableInfo(int aBehaviorId);
	public LineNumberInfo[] _getBehaviorLineNumberInfo(int aBehaviorId);
	public TagMap _getBehaviorTagMap(int aBehaviorId);
	public List<ProbeInfo> _getBehaviorProbes(int aBehaviorId);
	public IClassInfo _getBehaviorClass(int aBehaviorId, boolean aFailIfAbsent);
	public IClassInfo _getFieldClass(int aFieldId, boolean aFailIfAbsent);
}
