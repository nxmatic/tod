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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tod.Util;
import tod.core.database.structure.BehaviorKind;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.IStructureDatabase.LineNumberInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;


/**
 * Base class for behaviour (method/constructor) information.
 * @author gpothier
 */
public class BehaviorInfo extends MemberInfo implements IMutableBehaviorInfo
{
	private static final long serialVersionUID = 8645425455286128491L;
	
	private BehaviorKind itsBehaviourKind;
	private HasTrace itsHasTrace = HasTrace.UNKNOWN;
	
	private final String itsSignature;
	private final ITypeInfo[] itsArgumentTypes;
	private final ITypeInfo itsReturnType;

	private int itsCodeSize;
	
	private boolean itsHasLineNumberTable = false;
	private transient LineNumberInfo[] itsLineNumberTable;
	
	private boolean itsHasLocalVariableTable = false;
	private transient List<LocalVariableInfo> itsLocalVariableTable;
	
	private boolean itsHasTagMap = false;
	private transient TagMap itsTagMap;
	
	private transient List<ProbeInfo> itsProbes = new ArrayList<ProbeInfo>();

	public BehaviorInfo(
			IShareableStructureDatabase aDatabase, 
			int aId, 
			ClassInfo aType, 
			String aName,
			boolean aStatic,
			String aSignature,
			ITypeInfo[] aArgumentTypes,
			ITypeInfo aReturnType)
	{
		super(aDatabase, aId, aType, aName, aStatic);
		itsSignature = aSignature;
		itsArgumentTypes = aArgumentTypes;
		itsReturnType = aReturnType;
		
		if ("<init>".equals(getName())) itsBehaviourKind = BehaviorKind.CONSTRUCTOR;
		else if ("<clinit>".equals(getName())) itsBehaviourKind = BehaviorKind.STATIC_INIT;
		else if (isStatic()) itsBehaviourKind = BehaviorKind.STATIC_METHOD;
		else itsBehaviourKind = BehaviorKind.METHOD;
		
//		System.out.println(String.format(
//				"[Struct] behavior info [id: %d, sig: %s.%s(%s)]",
//				aId,
//				aType.getName(),
//				aName,
//				aSignature));
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
		if (StructureDatabaseUtils.isSaving())
		{
			out.writeBoolean(true);
			out.writeObject(itsLineNumberTable);
			out.writeObject(itsLocalVariableTable);
			out.writeObject(itsTagMap);
			out.writeObject(itsProbes);
		}
		else
		{
			out.writeBoolean(false);
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		if (in.readBoolean())
		{
			itsLineNumberTable = (LineNumberInfo[]) in.readObject();
			itsLocalVariableTable = (List<LocalVariableInfo>) in.readObject();
			itsTagMap = (TagMap) in.readObject();
			itsProbes = (List<ProbeInfo>) in.readObject();
		}
	}

	public void addProbe(ProbeInfo aProbe)
	{
		itsProbes.add(aProbe);
	}
	
	public void setup(
			boolean aTraced,
			int aCodeSize,
			LineNumberInfo[] aLineNumberInfos,
			TagMap aTagMap)
	{
		itsHasTrace = aTraced ? HasTrace.YES : HasTrace.NO;
		itsCodeSize = aCodeSize;
		
		itsLineNumberTable = aLineNumberInfos;
		itsHasLineNumberTable = itsLineNumberTable != null;
		
		itsTagMap = aTagMap;
		itsHasTagMap = itsTagMap != null;
	}
	
	
	
	@Override
	public IClassInfo getDeclaringType()
	{
		return (IClassInfo) super.getDeclaringType();
	}
	
	public HasTrace hasTrace()
	{
		return itsHasTrace;
	}
	
	public BehaviorKind getBehaviourKind()
	{
		return itsBehaviourKind;
	}

	public ITypeInfo[] getArgumentTypes()
	{
		return itsArgumentTypes;
	}
	
	public String getSignature()
	{
		return itsSignature;
	}

	public ITypeInfo getReturnType()
	{
		return itsReturnType;
	}

	public LocalVariableInfo getLocalVariableInfo (int aPc, int aIndex)
	{
		int thePc = aPc+22; // TODO: the size of the instrumentation is not always 22, can be less (eg. 21), and probably more.
    	if (itsHasLocalVariableTable) for (LocalVariableInfo theInfo : getLocalVariables())
    	{
    		if (theInfo.match(thePc, aIndex)) return theInfo;
    	}
    	return null;
	}
    
    public LocalVariableInfo getLocalVariableInfo (int aSymbolIndex)
    {
        return itsHasLocalVariableTable && aSymbolIndex < getLocalVariables().size() ?
        		getLocalVariables().get(aSymbolIndex)
                : null;
    }
    
    public int getCodeSize()
    {
    	return itsCodeSize;
    }
    
    public int getLineNumber (int aBytecodeIndex)
    {
        if (itsHasLineNumberTable && getLineNumbers().length > 0)
        {
        	int theResult = getLineNumbers()[0].getLineNumber();
            
            for (LineNumberInfo theInfo : getLineNumbers())
			{
                if (aBytecodeIndex < theInfo.getStartPc()) break;
                theResult = theInfo.getLineNumber();
			}
            return theResult;
        }
        else return -1;
    }
    
    /**
     * Adds a range of numbers to the list.
     * @param aStart First number to add, inclusive
     * @param aEnd Last number to add, exclusive.
     */
    private void addRange(List<Integer> aList, int aStart, int aEnd)
    {
    	for(int i=aStart;i<aEnd;i++) aList.add(i);
    }
    
	public int[] getBytecodeLocations(int aLine)
	{
        if (itsHasLineNumberTable && getLineNumbers().length > 0)
        {
        	List<Integer> theLocations = new ArrayList<Integer>();

        	int thePreviousPc = -1;
        	int theCurrentLine = -1;
            for (LineNumberInfo theInfo : getLineNumbers())
            {
            	if (thePreviousPc == -1)
            	{
            		thePreviousPc = theInfo.getStartPc();
            		theCurrentLine = theInfo.getLineNumber();
            		continue;
            	}
            	
            	if (theCurrentLine == aLine)
            	{
            		addRange(theLocations, thePreviousPc, theInfo.getStartPc());
            	}

            	thePreviousPc = theInfo.getStartPc();
            	theCurrentLine = theInfo.getLineNumber();
            }
            
            if (theCurrentLine == aLine)
            {
            	addRange(theLocations, thePreviousPc, getCodeSize());
            }
            
            // TODO: do something to include only valid bytecode indexes.
            int[] theResult = new int[theLocations.size()];
            for (int i=0;i<theResult.length;i++) theResult[i] = theLocations.get(i);
            
            return theResult;
        }
        else return null;
	}
	
	public <T> T getTag(BytecodeTagType<T> aType, int aBytecodeIndex)
	{
		return itsHasTagMap ? getTagMap().getTag(aType, aBytecodeIndex) : null;
	}

	public List<LocalVariableInfo> _getLocalVariables()
    {
    	return itsLocalVariableTable;
    }
	
	public List<LocalVariableInfo> getLocalVariables()
	{
		if (! itsHasLocalVariableTable) return Collections.EMPTY_LIST;
		
		if (itsLocalVariableTable == null)
		{
			assert ! isOriginal();
			itsLocalVariableTable = getDatabase()._getBehaviorLocalVariableInfo(getId());
		}
		return itsLocalVariableTable;
	}
	
	public void addLocalVariableInfo(LocalVariableInfo aInfo)
	{
		if (itsLocalVariableTable == null) itsLocalVariableTable = new ArrayList<LocalVariableInfo>();
		itsHasLocalVariableTable = true;
		itsLocalVariableTable.add(aInfo);
	}
	
	LineNumberInfo[] _getLineNumbers()
	{
		return itsLineNumberTable; 
	}
	
	public LineNumberInfo[] getLineNumbers()
	{
		if (itsLineNumberTable == null && itsHasLineNumberTable)
		{
			assert ! isOriginal();
			itsLineNumberTable = getDatabase()._getBehaviorLineNumberInfo(getId());
		}
		return itsLineNumberTable;
	}
	
	TagMap _getTagMap()
	{
		return itsTagMap;
	}
    
	private TagMap getTagMap()
	{
		if (itsTagMap == null && itsHasTagMap)
		{
			assert ! isOriginal();
			itsTagMap = getDatabase()._getBehaviorTagMap(getId());
		}
		return itsTagMap;
	}
	
	List<ProbeInfo> _getProbes()
	{
		return itsProbes;
	}
	
	public ProbeInfo[] getProbes()
	{
		if (itsProbes == null)
		{
			assert ! isOriginal();
			itsProbes = getDatabase()._getBehaviorProbes(getId());
		}
		return itsProbes.toArray(new ProbeInfo[itsProbes.size()]);
	}
	
    public boolean isConstructor()
    {
    	return getBehaviourKind() == BehaviorKind.CONSTRUCTOR;
    }

    public boolean isStaticInit()
    {
    	return getBehaviourKind() == BehaviorKind.STATIC_INIT;
    }
    
    @Override
	public boolean isStatic()
    {
    	return getBehaviourKind() == BehaviorKind.STATIC_INIT
    		|| getBehaviourKind() == BehaviorKind.STATIC_METHOD;
    }
	
	@Override
	public String toString()
	{
		return String.format("%s.%s (bid: %d, cid: %d)", getDeclaringType().getName(), Util.getFullName(this), getId(), getDeclaringType().getId());
	}
	
}
