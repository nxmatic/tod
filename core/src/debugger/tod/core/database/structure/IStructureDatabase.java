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

import java.io.Serializable;
import java.tod.TracedMethods;
import java.util.Map;

import tod.core.config.TODConfig;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.impl.database.structure.standard.AspectInfo;
import tod2.agent.MonitoringMode;

/**
 * The structure database contains static information about the
 * debugged program. In particular, it contains a list of types
 * and behaviors.
 * The structure database is thightly coupled with the instrumentation:
 * ids of types and behaviors are hardcoded in the instrumented code
 * and must match those used in the currently used structure database.
 * Therefore the structure database has a unique identifier, which is sent
 * to the native agent during setup, so that the agent can ensure that cached
 * versions of instrumented classes match the structure database.
 * <p>
 * It is possible for the database to contain various classes with the same name.
 * This can occurr for three reasons:
 * <li>Several classes with the same name but different contents exist in the
 * codebase and are loaded by different class loaders (this is both improbable
 * and quite risky for the developper...)
 * <li>A class is modified between different sessions, ie. the developper changes
 * the source code of the class and relaunches the program. In this case the 
 * old class info can be deleted.
 * <li>A class is redefined during a given session (edit & continue). This
 * is more problematic, because it is necessary to keep all the versions.
 * Although the structure of the class should not change (at least in current JDK),
 * the content of behaviors can change.  
 * @author gpothier
 */
public interface IStructureDatabase
{
	/**
	 * Returns the unique identifier of this database. 
	 */
	public String getId();
	
	/**
	 * Returns the configuration used by this structure database.
	 */
	public TODConfig getConfig();
	
	/**
	 * Returns a class info that represents an unknown class.
	 */
	public IClassInfo getUnknownClass();
	
	/**
	 * Returns the information for a class of the given name and checksum.
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public IClassInfo getClass(String aName, String aChecksum, boolean aFailIfAbsent);
	
	/**
	 * Returns all the class info that have the specified name.
	 * It is possible to have several classes with the same name in case
	 * of class redefinition, or classloader hacking.
	 */
	public IClassInfo[] getClasses(String aName);
	
	/**
	 * Returns the latest registered class with the given name.
	 * Note that there can be many classes with the same name 
	 * (see {@link #getClasses(String)}).
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public IClassInfo getClass(String aName, boolean aFailIfAbsent);
	
	/**
	 * Returns the classinfo corresponding to the specified id.
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public IClassInfo getClass(int aId, boolean aFailIfAbsent);
	
	/**
	 * Returns all the registered classes (so, no primitive types nor array types).
	 */
	public IClassInfo[] getClasses();
	
	/**
	 * Returns the type that corresponds to the given name. If the type 
	 * corresponds to a class, returns the latest registered class info 
	 * for this name.
	 * @param aName The JVM type name (eg. I, J, Ljava.lang.Object;, etc.)
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public ITypeInfo getType(String aName, boolean aFailIfAbsent);

	/**
	 * Returns the type corresponding to the specified id. This can either be a class
	 * or a primitive type. Array types have no id and therefore can't be retrieved
	 * through this method.
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public ITypeInfo getType(int aId, boolean aFailIfAbsent);
	
	/**
	 * Creates (or obtains a cached) array type.
	 */
	public IArrayTypeInfo getArrayType(ITypeInfo aBaseType, int aDimensions);
	
	/**
	 * Returns the field that corresponds to the given id.
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public IFieldInfo getField(int aId, boolean aFailIfAbsent);
	
	/**
	 * Returns the behavior that corresponds to the given id.
	 * @param aFailIfAbsent If true, the method throws an exception if the requested element
	 * is not found. Otherwise, returns null if the requested element is not found.
	 */
	public IBehaviorInfo getBehavior(int aId, boolean aFailIfAbsent);
	
	/**
	 * Returns all the currently registered behaviors.
	 */
	public IBehaviorInfo[] getBehaviors();
	
	/**
	 * Returns the id of the specified behavior. This method is used for resolving 
	 * exceptions.
	 * @param aMethodSignature JVM signature of the method.
	 */
	public int getBehaviorId(String aClassName, String aMethodName, String aMethodSignature);
	
	/**
	 * Returns the {@link ProbeInfo} corresponding to the given probe id.
	 */
	public ProbeInfo getProbeInfo(int aProbeId);
	
	/**
	 * Returns the number of installed probes.
	 */
	public int getProbeCount();
	
	/**
	 * Returns an ordered iterable of all mode changes performed.
	 * This is aimed to be used in conjunction with {@link TracedMethods} versioning.
	 * @param aVersion The version number that was created in response to the change.
	 */
	public BehaviorMonitoringModeChange getBehaviorMonitoringModeChange(int aVersion);
	
	/**
	 * Returns the information (location of the source code) for the specified advice source id.
	 * @param aAdviceId An advice source id.
	 * @return The advice info, or null if not available.
	 */
	public IAdviceInfo getAdvice(int aAdviceId);
	
	/**
	 * Returns a map that maps aspect source file names to an {@link AspectInfo}
	 * structure.
	 * TODO: For now this structure is expected to be small but if we want to
	 * be serious about aspects we must do that in another way.  
	 */
	public Map<String, IAspectInfo> getAspectInfoMap();

	/**
	 * Returns statistics about registered locations
	 */
	public Stats getStats();
	
	public void addListener(Listener aListener);
	public void removeListener(Listener aListener);

	public boolean isInScope(String aClassName);
	public boolean isInIdScope(String aClassName);

	/**
	 * A listener that is notified of changes in the database.
	 * @author gpothier
	 */
	public interface Listener
	{
		public void classAdded(IClassInfo aClass);
		public void classChanged(IClassInfo aClass);
		public void behaviorAdded(IBehaviorInfo aBehavior);
		public void fieldAdded(IFieldInfo aField);
		
		/**
		 * @see MonitoringMode
		 */
		public void monitoringModeChanged(BehaviorMonitoringModeChange aChange);
	}

	public static class BehaviorMonitoringModeChange implements Serializable
	{
		private static final long serialVersionUID = 69242304676431987L;

		public final int behaviorId;
		
		/**
		 * One of the constants in {@link MonitoringMode}.
		 */
		public final int mode;
		
		public BehaviorMonitoringModeChange(int aBehaviorId, int aMode)
		{
			behaviorId = aBehaviorId;
			mode = aMode;
		}
	}

	
	public static class Stats implements Serializable
	{
		private static final long serialVersionUID = -2910977890794945414L;
		
		public final int nTypes;
		public final int nBehaviors;
		public final int nFields;
		public final int nProbes;

		public Stats(int aTypes, int aBehaviors, int aFields, int aProbes)
		{
			nTypes = aTypes;
			nBehaviors = aBehaviors;
			nFields = aFields;
			nProbes = aProbes;
		}
		
		@Override
		public String toString()
		{
			return String.format(
					"Location repository stats: %d types, %d behaviors, %d fields, %d probes",
					nTypes,
					nBehaviors,
					nFields,
					nProbes);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + nBehaviors;
			result = prime * result + nFields;
			result = prime * result + nProbes;
			result = prime * result + nTypes;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final Stats other = (Stats) obj;
			if (nBehaviors != other.nBehaviors) return false;
			if (nFields != other.nFields) return false;
			if (nProbes != other.nProbes) return false;
			if (nTypes != other.nTypes) return false;
			return true;
		}

	}


	/**
	 * Represents an entry of a method's LineNumberTable attribute.
	 * @see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#22856 
	 * @author gpothier
	 */
	public static class LineNumberInfo implements Serializable
	{
		private static final long serialVersionUID = 5859320809004004345L;
		
		private short itsStartPc;
		private short itsLineNumber;
	
		public LineNumberInfo(short aStartPc, short aLineNumber)
		{
			itsStartPc = aStartPc;
			itsLineNumber = aLineNumber;
		}
	
		public short getLineNumber()
		{
			return itsLineNumber;
		}
	
		public short getStartPc()
		{
			return itsStartPc;
		}
		
		@Override
		public String toString()
		{
			return String.format("line %d - pc %d", itsLineNumber, itsStartPc);
		}
	}

	/**
	 * Represents an entry of a method's LocalVariableTable attribute. 
	 * @see http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#5956
	 * @author gpothier
	 */
	public static class LocalVariableInfo implements Serializable
	{
		private static final long serialVersionUID = 8459382258589171019L;
		
		private short itsStartPc;
		private short itsLength;
		private String itsVariableName;
		private String itsVariableTypeName;
		private short itsIndex;
	
		public LocalVariableInfo(int aStartPc, int aLength, String aVariableName, String aVariableTypeName,
				int aIndex)
		{
			itsStartPc = (short) aStartPc;
			itsLength = (short) aLength;
			itsVariableName = aVariableName;
			itsVariableTypeName = aVariableTypeName;
			itsIndex = (short) aIndex;
		}
	
		/**
		 * Index of the local variable's storage in the frame's local variables array
		 * @return
		 */
		public short getIndex()
		{
			return itsIndex;
		}
	
		/**
		 * Index of first bytecoed where this local variable can be used.
		 */
		public short getStartPc()
		{
			return itsStartPc;
		}
	
		/**
		 * Length of the bytecode span where this variable can be used.
		 */
		public short getLength()
		{
			return itsLength;
		}
	
		/**
		 * Name of the variable.
		 */
		public String getVariableName()
		{
			return itsVariableName;
		}
	
		/**
		 * Variable's type name.
		 */
		public String getVariableTypeName()
		{
			return itsVariableTypeName;
		}
	
		/**
		 * Indicates if this entry matches the local variable at the specified index
		 * for the specified bytecode position
		 * @param aPc A position in the bytecode where the variable is used.
		 * @param aIndex Index of the local variable in the frame's local variables array.
		 */
		public boolean match(int aPc, int aIndex)
		{
			return aIndex == getIndex() && available(aPc);
		}
	
		/**
		 * Indicates if this entry is available at the specified bytecode position.
		 * @param aPc A position in the bytecode where the variable is used.
		 */
		public boolean available(int aPc)
		{
			return aPc >= getStartPc() && aPc <= getStartPc() + getLength();
		}
		
		@Override
		public boolean equals(Object aObj)
		{
			if (aObj instanceof LocalVariableInfo)
			{
				LocalVariableInfo theOther = (LocalVariableInfo) aObj;
				return theOther.itsIndex == itsIndex
						&& theOther.itsStartPc == itsStartPc
						&& theOther.itsLength == itsLength;
			}
			else return false;
		}
		
		@Override
		public String toString()
		{
			return String.format(
					"%s %s (%d-%d): %d",
					getVariableTypeName(),
					getVariableName(),
					getStartPc(),
					getStartPc()+getLength(),
					getIndex());
		}
	}

	/**
	 * Contains the information for a given probe.
	 * A probe is a point of instrumentation in the code.
	 * @author gpothier
	 */
	public static class ProbeInfo implements Serializable
	{
		private static final long serialVersionUID = -2555314414321419466L;
		
		public static final ProbeInfo NULL = new ProbeInfo(-1, -1, -1, null, -1);

		public final int id;
		
		/**
		 * Id of the behavior that contains the probe.
		 */
		public final int behaviorId;
		
		/**
		 * Bytecode location of the probe.
		 */
		public final int bytecodeIndex;
		
		public final BytecodeRole role;
		
		/**
		 * Id of the advice that caused the generation of the code in which
		 * lies the probe.
		 * This is for the AspectJ extension of TOD.
		 */
		public final int adviceSourceId;

		public ProbeInfo(int aId, int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId)
		{
			id = aId;
			adviceSourceId = aAdviceSourceId;
			behaviorId = aBehaviorId;
			role = aRole;
			bytecodeIndex = aBytecodeIndex;
		}

		@Override
		public String toString()
		{
			return "bid: "+behaviorId+", bi: "+bytecodeIndex+", aid: "+adviceSourceId;
		}
	}

}
