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
package tod.impl.bci.asm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tod.core.database.structure.BehaviorKind;
import tod.core.database.structure.IBehaviorInfo.BytecodeTagType;
import tod.core.database.structure.IStructureDatabase.LineNumberInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.impl.database.structure.standard.TagMap;

public class ASMMethodInfo
{
	private int itsMaxLocals;
	private final String itsOwner;
	private final String itsOwnerSuper;
	private final String itsName;
	private final String itsDescriptor;
	private final boolean itsStatic;
	
	private int itsNextFreeVariable;
	
	private int itsCodeSize;
	private List<ASMLineNumberInfo> itsLineNumberInfo = new ArrayList<ASMLineNumberInfo>();
	private List<ASMLocalVariableInfo> itsLocalVariableInfo = new ArrayList<ASMLocalVariableInfo>();
	
	
	private TagMap itsTagMap;
	
	/**
	 * An array of store operations to ignore.
	 * The index is the rank of the store operation within the method.
	 * @see InfoCollector.JSRAnalyserVisitor
	 */
	private boolean[] itsIgnoreStores;
	
	/**
	 * Original pc of each instruction. Indexed by instruction rank.
	 */
	private int[] itsOriginalPc;
	
	/**
	 * This set contains all the classes on which methods are called in this method.
	 */
	private Set<String> itsCalledClasses = new HashSet<String>();
	
	/**
	 * For constructors, whether the constructor calls this(...)
	 */
	private boolean itsCallsThis = false;
	
	public ASMMethodInfo(String aOwner, String aOwnerSuper, String aName, String aDescriptor, boolean aStatic)
	{
		itsOwner = aOwner;
		itsOwnerSuper = aOwnerSuper;
		itsName = aName;
		itsDescriptor = aDescriptor;
		itsStatic = aStatic;
	}

	public int getMaxLocals()
	{
		return itsMaxLocals;
	}

	public void setMaxLocals(int aMaxLocals)
	{
		itsMaxLocals = aMaxLocals;
		itsNextFreeVariable = itsMaxLocals;
	}

//	/**
//	 * Returns the index of a free variable slot for the described method.
//	 */
//	public int getNextFreeVariable(Type aType)
//	{
//		return itsNextFreeVariable;
//	}
//	
//	/**
//	 * Allocates space for a variable of the given type for the
//	 * described method.
//	 */
//	public int createVariable(Type aType)
//	{
//		int theVariable = itsNextFreeVariable;
//		itsNextFreeVariable += aType.getSize();
//		return theVariable;
//	}
//	
	public String getDescriptor()
	{
		return itsDescriptor;
	}

	/**
	 * Returns the name of the owner of this method.
	 */
	public String getOwner()
	{
		return itsOwner;
	}
	
	/**
	 * Returns the name of the superclass of the owner.
	 */
	public String getOwnerSuper()
	{
		return itsOwnerSuper;
	}
	
	public String getName()
	{
		return itsName;
	}

	public boolean isStatic()
	{
		return itsStatic;
	}
	
	public BehaviorKind getKind()
	{
		if ("<init>".equals(getName())) return BehaviorKind.CONSTRUCTOR;
		else if ("<clinit>".equals(getName())) return BehaviorKind.STATIC_INIT;
		else return isStatic() ? BehaviorKind.STATIC_METHOD : BehaviorKind.METHOD;
	}
	
	public int getCodeSize()
	{
		return itsCodeSize;
	}

	public void setCodeSize(int aCodeSize)
	{
		itsCodeSize = aCodeSize;
	}

	public void addLineNumber (ASMLineNumberInfo aInfo)
	{
		itsLineNumberInfo.add(aInfo);
	}
	
	public void addLocalVariable (ASMLocalVariableInfo aInfo)
	{
		itsLocalVariableInfo.add(aInfo);
	}
	
    /**
     * Creates an array of {@link LineNumberInfo} from a list of 
     * {@link ASMLineNumberInfo}
     */
    public LineNumberInfo[] createLineNumberTable ()
    {
    	int theLength = itsLineNumberInfo.size();
        LineNumberInfo[] theTable = new LineNumberInfo[theLength];

        int i=0;
        for (ASMLineNumberInfo theInfo : itsLineNumberInfo)
		{
            theTable[i++] = theInfo.resolve();
        }
        
        return theTable;
    }

    /**
     * Creates an array of {@link LocalVariableInfo} from a list of 
     * {@link ASMLocalVariableInfo}
     */
    public List<LocalVariableInfo> createLocalVariableTable ()
    {
    	List<LocalVariableInfo> theTable = new ArrayList<LocalVariableInfo>();
    	
    	for (ASMLocalVariableInfo theInfo : itsLocalVariableInfo)
    	{
    		try
			{
				theTable.add(theInfo.resolve());
			}
			catch (Exception e)
			{
				System.err.println("[ASMMethodInfo.createLocalVariableTable] WARNING: "+e.getMessage());
				System.err.println(" in: "+itsOwner+"."+itsName+" "+itsDescriptor);
			}
    	}
    	
    	return theTable;
    }
    
    public void setIgnoreStores(boolean[] aIgnoreStores)
	{
		itsIgnoreStores = aIgnoreStores;
	}

	/**
     * Indicates if the n-th store instruction should be ignored 
     * @param aIndex Rank of the store instruction.
     */
    public boolean shouldIgnoreStore(int aIndex)
    {
    	return itsIgnoreStores[aIndex];
    }
    
    public void setOriginalPc(int[] aOriginalPc)
	{
		itsOriginalPc = aOriginalPc;
	}
    
    /**
     * Returns the original Pc (bytecode index) of the nth instruction.
     */
    public int getOriginalPc(int aInstructionRank)
    {
    	return itsOriginalPc[aInstructionRank];
    }

    /**
     * Returns the tag map of the original method, where all pcs are for the
     * original method code.
     */
	public TagMap getOriginalTagMap()
	{
		return itsTagMap;
	}

	public void setTagMap(TagMap aTagMap)
	{
		itsTagMap = aTagMap;
	}
    
	/**
	 * Returns the tag of an instruction in the original method
	 * @param aRank Original rank of the instruction.
	 */
    public <T> T getTag(BytecodeTagType<T> aType, int aRank)
    {
    	int thePc = itsOriginalPc[aRank];
    	return itsTagMap.getTag(aType, thePc);
    }
    
    public void addCalledClass(String aClass)
    {
    	itsCalledClasses.add(aClass);
    }
    
    public Set<String> getCalledClasses()
	{
		return itsCalledClasses;
	}
    
    public void setCallsThis(boolean aCallsThis)
    {
    	itsCallsThis = aCallsThis;
    }
    
    public boolean doesCallsThis()
	{
		return itsCallsThis;
	}
}