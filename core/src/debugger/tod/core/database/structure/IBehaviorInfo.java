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

import java.util.ArrayList;
import java.util.List;

import tod.core.database.structure.IStructureDatabase.LineNumberInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;

public interface IBehaviorInfo extends IMemberInfo
{
	/**
	 * The type of a behavior is always a class.
	 */
	public IClassInfo getDeclaringType();
	
	/**
	 * Whether this behavior is traced, ie. emits at least behavior
	 * enter and behavior exit events.
	 * The possible values are YES, NO and UNKNOWN, the latter being returned
	 * when the behavior has been created but not yet set up.
	 */
	public HasTrace hasTrace();
	
	/**
	 * Indicates the kind of behavior represented by this object
	 * (method, constructor, etc.)
	 * @see BehaviorKind
	 */
	public BehaviorKind getBehaviourKind();

	/**
	 * Returns the types of the arguments to this behavior
	 */
	public ITypeInfo[] getArgumentTypes();
	
	/**
	 * Returns the JVM signature of this behavior.
	 */
	public String getDescriptor();

	/**
	 * Returns the type of the return value of this behavior. 
	 */
	public ITypeInfo getReturnType();

	/**
     * Returns the local variable symbolic information for a given bytecode index
     * and variable slot
     * @param aPc Bytecode index
     * @param aIndex Position of the variable's slot in the frame.
     */
	public LocalVariableInfo getLocalVariableInfo (int aPc, int aIndex);
    
    /**
     * Returns the symbolic variable information at the specified index. 
     */
    public LocalVariableInfo getLocalVariableInfo (int aSymbolIndex);
    
    /**
     * Returns the line number that corresponds to the specified bytecode index
     * according to the line number table. If the table is not available, returns -1.
     */
    public int getLineNumber (int aBytecodeIndex);
    
    /**
     * Returns the size of the bytecode of the method (after instrumentation).
     */
    public int getCodeSize();
    
    /**
     * Returns the tag associated to the specified bytecode.
     * @param aType The type of requested tag (one of the constants in {@link BytecodeTagType}).
     * @param aBytecodeIndex The index of the bytecode.
     * @return The value of the tag, or null if not found.
     */
    public <T> T getTag(BytecodeTagType<T> aType, int aBytecodeIndex);
    
	/**
	 * Returns an array of all valid bytecode locations corresponding
	 * to the specified line in this method.
	 * @param aLine The source code line, relative to the whole file
	 * containing the method.
	 * @return Array of valid bytecodes, or null if no line number info is
	 * available.
	 */
	public int[] getBytecodeLocations(int aLine);

	/**
	 * Returns the raw local variables info of the behavior.
	 */
    public List<LocalVariableInfo> getLocalVariables();
    
    /**
     * Returns the raw line number info of the behavior.
     */
    public LineNumberInfo[] getLineNumbers();
    
    /**
     * Indicates if this behavior is a constructor. 
     */
    public boolean isConstructor();

    /**
     * Indicates if this behavior is a static class initializer 
     */
    public boolean isStaticInit();
    
    /**
     * Indicates if this behavior is static.
     */
    public boolean isStatic();
    
    /**
     * Indicates if this behavior is abstract.
     */
    public boolean isAbstract();
    
    /**
     * Indicates if this behavior is native.
     */
    public boolean isNative();
    
    /**
     * Returns all the probes of this method.
     */
    public ProbeInfo[] getProbes();

    /**
     * An enumeration of all possible bytecode tag types.
     * Bytecode tags are information associated to each bytecode in a behavior.
     * @author gpothier
     */
    public static class BytecodeTagType<T> 
    {
    	public static final BytecodeTagType<Integer> SOURCE_POSITION = new BytecodeTagType<Integer>("srcPos");
    	
    	/**
    	 * The role of the bytecode (eg. base code, advice, arg. setup...)
    	 */
    	public static final BytecodeTagType<BytecodeRole> ROLE = new BytecodeTagType<BytecodeRole>("role");
    	public static final BytecodeTagType<Integer> INSTR_SHADOW = new BytecodeTagType<Integer>("shadow");
    	
    	/**
    	 * The id of the advice that caused the generation of the bytecode, if any.
    	 * More information about the advice can be obtained through {@link IClassInfo#getAdviceSource(int)}
    	 */
    	public static final BytecodeTagType<Integer> ADVICE_SOURCE_ID = new BytecodeTagType<Integer>("source");
    	
    	public static final BytecodeTagType[] ALL = {SOURCE_POSITION, ROLE, INSTR_SHADOW, ADVICE_SOURCE_ID};
    	
    	private final String itsName;

		public BytecodeTagType(String aName)
		{
			itsName = aName;
		}
		
		public String getName()
		{
			return itsName;
		}
    }
    
    /**
     * Enumerates the different possible roles of a bytecode. For instance, base code
     * which comes straight from a class source file, or inlined adviced code produced
     * by a weaver.
     * @author gpothier
     */
    public enum BytecodeRole 
    {
    	UNKNOWN,
    	BASE_CODE,
    	
    	WOVEN_CODE,
    	ASPECTJ_CODE(WOVEN_CODE),
    	
    	/**
    	 * Code inserted by TOD during the instrumentation
    	 */
    	TOD_CODE(WOVEN_CODE),
    	
    	ADVICE_ARG_SETUP(ASPECTJ_CODE), //aka aspect instance selection
    	CONTEXT_EXPOSURE(ASPECTJ_CODE),
    	PARAMETER_BACKUP(ASPECTJ_CODE),
    	ADVICE_TEST(ASPECTJ_CODE),
    	ADVICE_EXECUTE(ASPECTJ_CODE),
    	AFTER_THROWING_HANDLER(ASPECTJ_CODE),
    	EXCEPTION_SOFTENER(ASPECTJ_CODE),
    	INLINED_ADVICE(ASPECTJ_CODE);
    	
    	private final BytecodeRole itsParentRole;
    	private final List<BytecodeRole> itsChildrenRoles = new ArrayList<BytecodeRole>();
    	
    	BytecodeRole()
		{
    		itsParentRole = null;
		}
    	
    	BytecodeRole(BytecodeRole aParentRole)
    	{
    		itsParentRole = aParentRole;
    		itsParentRole.itsChildrenRoles.add(this);
    	}
    }
    
	public enum HasTrace
	{
		YES, NO, UNKNOWN;
	}
	
 
}
