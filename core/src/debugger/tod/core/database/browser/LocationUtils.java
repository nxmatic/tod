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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

import tod.core.database.event.ICallerSideEvent;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.SourceRange;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.gui.IGUIManager;

/**
 * Utilities related to {@link IStructureDatabase}
 * @author gpothier
 */
public class LocationUtils
{
	/**
	 * Returns the argument types that correspond to the given behavior signature. 
	 */
	public static ITypeInfo[] getArgumentTypes(
			IStructureDatabase aDatabase,
			String aSignature)
	{
		Type[] theASMArgumentTypes = Type.getArgumentTypes(aSignature);
		ITypeInfo[] theArgumentTypes = new ITypeInfo[theASMArgumentTypes.length];
		
		for (int i = 0; i < theASMArgumentTypes.length; i++)
		{
			Type theASMType = theASMArgumentTypes[i];
			theArgumentTypes[i] = aDatabase.getType(theASMType.getDescriptor(), true);
		}
		
		return theArgumentTypes;
	}

	/**
	 * Returns the argument types that correspond to the given behavior signature. 
	 * If some type is not found in the database it is created.
	 */
	public static ITypeInfo[] getArgumentTypes(
			IMutableStructureDatabase aDatabase,
			String aSignature)
	{
		Type[] theASMArgumentTypes = Type.getArgumentTypes(aSignature);
		ITypeInfo[] theArgumentTypes = new ITypeInfo[theASMArgumentTypes.length];
		
		for (int i = 0; i < theASMArgumentTypes.length; i++)
		{
			Type theASMType = theASMArgumentTypes[i];
			theArgumentTypes[i] = aDatabase.getNewType(theASMType.getDescriptor());
					
		}
		
		return theArgumentTypes;
	}
	

	/**
	 * Determines a TOD return type given a method signature
	 */
	public static ITypeInfo getReturnType(
			IStructureDatabase aDatabase,
			String aSignature)
	{
		Type theASMType = Type.getReturnType(aSignature);
		return aDatabase.getType(theASMType.getDescriptor(), true);
	}
	
	/**
	 * Determines a TOD return type given a method signature
	 * If some type is not found in the database it is created.
	 */
	public static ITypeInfo getReturnType(
			IMutableStructureDatabase aDatabase,
			String aSignature)
	{
		Type theASMType = Type.getReturnType(aSignature);
		return aDatabase.getNewType(theASMType.getDescriptor());
	}
	
	

	
	/**
	 * Retrieves a field given a type and a name.
	 * @param aSearchAncestors If false, the field will be searched only in the
	 * specified type. If true, the field will also be searched in ancestors. In the case
	 * of private fields, the first (closest to specified type) matching field is returned. 
	 */
	public static IFieldInfo getField(
			ITypeInfo aType, 
			String aName, 
			boolean aSearchAncestors)
	{
		IClassInfo theClassInfo = (IClassInfo) aType;
		
		while (theClassInfo != null)
		{
			IFieldInfo theField = theClassInfo.getField(aName);
			if (theField != null) return theField;
			
			if (! aSearchAncestors) return null;
			
			theClassInfo = theClassInfo.getSupertype();
		}

		return null;
	}
	
	/**
	 * Searches a behavior in the given type
	 * @param aSearchAncestors See {@link #getField(ITypeInfo, String, boolean)}.
	 */
	public static IBehaviorInfo getBehavior(
			IStructureDatabase aDatabase,
			IClassInfo aClass, 
			String aName, 
			String aSignature, 
			boolean aSearchAncestors)
	{
		ITypeInfo[] theArgumentTypes = getArgumentTypes(aDatabase, aSignature);
		ITypeInfo theReturnType = getReturnType(aDatabase, aSignature);
		
		while (aClass != null)
		{
			IBehaviorInfo theBehavior = aClass.getBehavior(aName, theArgumentTypes, theReturnType);
			if (theBehavior != null) return theBehavior;
			
			if (! aSearchAncestors) return null;
			
			aClass = aClass.getSupertype();
		}

		return null;
	}
	
	/**
	 * Searches a behavior in the given type
	 * @param aSearchAncestors See {@link #getField(ITypeInfo, String, boolean)}.
	 */
	public static IBehaviorInfo getBehavior(
			IStructureDatabase aDatabase,
			String aClassName, 
			String aName, 
			String aSignature, 
			boolean aSearchAncestors)
	{
		IClassInfo theClass = aDatabase.getClass(aClassName, false);
		if (theClass == null) return null;
		else return getBehavior(aDatabase, theClass, aName, aSignature, aSearchAncestors);
	}
	
	/**
	 * Returns the JVM signature of the given behavior.
	 * eg (Ljava/lang/Object;I)V
	 */
	public static String getDescriptor(IBehaviorInfo aBehavior)
	{
		StringBuilder theBuilder = new StringBuilder("(");
		
		for (ITypeInfo theType : aBehavior.getArgumentTypes())
		{
			theBuilder.append(theType.getJvmName());
		}

		theBuilder.append(')');
		theBuilder.append(aBehavior.getReturnType().getJvmName());
		
		return theBuilder.toString();
	}
	
	/**
	 * Returns the source range corresponding to the given event.
	 */
	public static SourceRange getSourceRange (IStructureDatabase aStructureDatabase, ProbeInfo aProbe)
	{
	    IBehaviorInfo theBehavior = aStructureDatabase.getBehavior(aProbe.behaviorId, true);
	    int theLineNumber = theBehavior.getLineNumber(aProbe.bytecodeIndex);
	    ITypeInfo theType = theBehavior.getDeclaringType();
	    
	    String theSourceFile = theBehavior.getSourceFile();
	    if (theSourceFile == null) theSourceFile = theType.getSourceFile();
	    
	    return new SourceRange(theType.getName(), theSourceFile, theLineNumber);
	}
	
	/**
	 * Returns the source range corresponding to the given event.
	 */
	public static SourceRange getSourceRange (IStructureDatabase aStructureDatabase, ILogEvent aEvent)
	{
		if (aEvent instanceof ICallerSideEvent)
		{
			ICallerSideEvent theEvent = (ICallerSideEvent) aEvent;
			ProbeInfo theProbe = theEvent.getProbeInfo();
			return getSourceRange(aStructureDatabase, theProbe);
		}
		else return null;
	}
	
	/**
	 * Tries to show the source code for the given event in the gui manager.
	 */
	public static void gotoSource(IGUIManager aGUIManager, ILogEvent aEvent)
	{
		ProbeInfo theProbe = getProbeInfo(aEvent);
		if (theProbe != null) aGUIManager.gotoSource(theProbe);
	}

	
	/**
	 * Returns the probe info of the given event.
	 */
	public static ProbeInfo getProbeInfo(ILogEvent aEvent)
	{
		if (aEvent instanceof ICallerSideEvent)
		{
			ICallerSideEvent theEvent = (ICallerSideEvent) aEvent;
			return theEvent.getProbeInfo();
		}
		else return null;
	}

	/**
	 * Returns the role of the given event.
	 */
	public static BytecodeRole getEventRole(ILogEvent aEvent)
	{
		ProbeInfo theProbeInfo = getProbeInfo(aEvent);
		return theProbeInfo != null ? theProbeInfo.role : null;
	}
	
	/**
	 * Returns all the advice ids corresponding to the specified location, which
	 * can be either an aspect or an advice.
	 */
	public static int[] getAdviceSourceIds(ILocationInfo aLocation)
	{
		if (aLocation instanceof IAspectInfo)
		{
			IAspectInfo theAspect = (IAspectInfo) aLocation;
			List<IAdviceInfo> theAdvices = theAspect.getAdvices();
			int[] theResult = new int[theAdvices.size()];
			int i=0;
			for(IAdviceInfo theAdvice : theAdvices) theResult[i++] = theAdvice.getId();
			return theResult;
		}
		else if (aLocation instanceof IAdviceInfo)
		{
			IAdviceInfo theAdvice = (IAdviceInfo) aLocation;
			return new int[] {theAdvice.getId()};
		}
		else throw new RuntimeException("Not handled: "+aLocation);
	}
	
	public static List<IBehaviorInfo> getConstructors(IClassInfo aClass)
	{
		List<IBehaviorInfo> theResult = new ArrayList<IBehaviorInfo>();
		for (IBehaviorInfo theBehavior : aClass.getBehaviors())
		{
			if (theBehavior.isConstructor()) theResult.add(theBehavior);
		}
		return theResult;
	}
	
	public static String toString(IBehaviorInfo aBehavior)
	{
		return String.format(
				"%s.%s%s",
				aBehavior.getDeclaringType().getName(),
				aBehavior.getName(),
				aBehavior.getSignature());
	}
	
}
