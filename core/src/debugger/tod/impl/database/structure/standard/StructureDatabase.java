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

import gnu.trove.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import tod.Util;
import tod.core.DebugFlags;
import tod.core.config.ClassSelector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IAdviceInfo;
import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IAspectInfo;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableFieldInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.SourceRange;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.core.database.structure.IClassInfo.Bytecode;
import tod.core.database.structure.IMutableStructureDatabase.LastIds;
import tod.impl.bci.asm2.BCIUtils;
import tod.tools.parsers.ParseException;
import tod.tools.parsers.workingset.WorkingSetFactory;
import tod.utils.remote.RemoteStructureDatabase;
import zz.utils.RandomAccessInputStream;
import zz.utils.RandomAccessOutputStream;
import zz.utils.Utils;

/**
 * Standard implementation of {@link IStructureDatabase}
 * @author gpothier
 */
public class StructureDatabase 
implements IShareableStructureDatabase
{
	private static final long serialVersionUID = -3929708435718445343L;
	
	public static ThreadLocal<Boolean> SAVING = new ThreadLocal<Boolean>()
	{
		@Override
		protected Boolean initialValue()
		{
			return false;
		}
	};
	


	/**
	 * Class ids below this value are reserved.
	 */
	public static final int FIRST_CLASS_ID = 100;
	
	private final TODConfig itsConfig;
	
	private final String itsId;
	
	private final boolean itsAllowHomonymClasses;
	
	/**
	 * The file that stores this structure database
	 */
	private final RandomAccessFile itsFile;
	
	/**
	 * Stores the offset into the file of the bytecode of each class. 
	 */
	private TLongArrayList itsByteCodeOffsets = new TLongArrayList(1000);
	
	/**
	 * Next free ids.
	 */
	private final Ids itsIds;
	
	/**
	 * Maps class names to {@link ClassNameInfo} objects that keep track
	 * of all the versions of a same class.
	 */
	private final Map<String, ClassNameInfo> itsClassNameInfos;
	private final Map<String, ClassInfo> itsClassInfos;
	
	private List<BehaviorInfo> itsBehaviors;
	private List<FieldInfo> itsFields;
	private List<ClassInfo> itsClasses;
	
	private List<ProbeInfo> itsProbes;
	
	private Map<Long, ProbeInfo> itsExceptionProbesMap = new HashMap<Long, ProbeInfo>();
	
	private List<AdviceInfo> itsAdvices = new ArrayList<AdviceInfo>(100); 
	
	private Map<String, IAspectInfo> itsAspectInfoMap = 
		new HashMap<String, IAspectInfo>();
	
	private final IClassInfo itsUnknownClass = new ClassInfo(this, null, "Unknown", -1);
	
	private transient List<Listener> itsListeners = new ArrayList<Listener>();
	
	private final MethodGroupManager itsMethodGroupManager; 

	private transient ClassSelector itsGlobalSelector;
	private transient ClassSelector itsTraceSelector;
	private transient ClassSelector itsIdSelector;
	
	protected StructureDatabase(TODConfig aConfig, String aId, File aFile, Ids aIds) throws IOException
	{
		itsConfig = aConfig;
		itsAllowHomonymClasses = itsConfig.get(TODConfig.ALLOW_HOMONYM_CLASSES);
		itsId = aId;
		boolean theFileExists = aFile.exists();
		itsFile = new RandomAccessFile(aFile, theFileExists ? "r" : "rw");
		itsIds = aIds;
		
		itsTraceSelector = parseWorkingSet(itsConfig.get(TODConfig.SCOPE_TRACE_FILTER));
		itsGlobalSelector = parseWorkingSet(itsConfig.get(TODConfig.SCOPE_GLOBAL_FILTER));
		itsIdSelector = parseWorkingSet(itsConfig.get(TODConfig.SCOPE_ID_FILTER));
		System.out.println("SCOPE_ID_FILTER: "+itsConfig.get(TODConfig.SCOPE_ID_FILTER));
		
		itsClassNameInfos = itsAllowHomonymClasses ? new HashMap<String, ClassNameInfo>(1000) : null;
		itsClassInfos = itsAllowHomonymClasses ? null : new HashMap<String, ClassInfo>(1000);
		
		if (theFileExists) 
		{
			load();
			itsMethodGroupManager = null;
		}
		else 
		{
			itsFile.seek(8); // The first long is used to store the offset of the serialized data
			itsMethodGroupManager = new MethodGroupManager(this);
			itsBehaviors = new ArrayList<BehaviorInfo>(10000);
			itsFields = new ArrayList<FieldInfo>(10000);
			itsClasses = new ArrayList<ClassInfo>(1000);
			
			itsProbes = new ArrayList<ProbeInfo>(10000);
			itsProbes.add(null);
		}
	}
	
	private void load() throws IOException
	{
		itsFile.seek(0);
		long theOffset =  itsFile.readLong();
		itsFile.seek(theOffset);
		
		RandomAccessInputStream theStream = new RandomAccessInputStream(itsFile);
		ObjectInputStream ois = new ObjectInputStream(theStream);
		
		try
		{
			itsByteCodeOffsets = (TLongArrayList) ois.readObject();
			itsBehaviors = (List<BehaviorInfo>) ois.readObject();
			itsFields = (List<FieldInfo>) ois.readObject();
			itsClasses = (List<ClassInfo>) ois.readObject();
			itsProbes = (List<ProbeInfo>) ois.readObject();
			
			reown();
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
	public static boolean isSaving()
	{
		return SAVING.get();
	}


	public void save() throws IOException
	{
		long theOffset = itsFile.getFilePointer();
		itsFile.seek(0);
		itsFile.writeLong(theOffset);
		itsFile.seek(theOffset);
		
		// Write serialized data
		RandomAccessOutputStream theStream = new RandomAccessOutputStream(itsFile);
		ObjectOutputStream oos = new ObjectOutputStream(theStream);
		
		try
		{
			SAVING.set(true);
			
			oos.writeObject(itsByteCodeOffsets);
			oos.writeObject(itsBehaviors);
			oos.writeObject(itsFields);
			oos.writeObject(itsClasses);
			oos.writeObject(itsProbes);			
		}
		finally
		{
			SAVING.set(true);
		}
	}
	

	
	private static String transformClassName(String aName)
	{
		return aName.replace('.', '/');
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		
		itsTraceSelector = parseWorkingSet(itsConfig.get(TODConfig.SCOPE_TRACE_FILTER));
		itsGlobalSelector = parseWorkingSet(itsConfig.get(TODConfig.SCOPE_GLOBAL_FILTER));
		itsIdSelector = parseWorkingSet(itsConfig.get(TODConfig.SCOPE_ID_FILTER));
	}


	/**
	 * Creates a structure database at the location specified in the given config.
	 */
	public static StructureDatabase create(TODConfig aConfig)
	{
		File theFile = new File(aConfig.get(TODConfig.STRUCTURE_DATABASE_LOCATION));
		return create(aConfig, theFile);
	}
	
	/**
	 * Creates a new structure database at the specified location.
	 * @param aFile Location where the structure database must be stored.
	 * The file should not exist.
	 */
	public static StructureDatabase create(TODConfig aConfig, File aFile)
	{
		try
		{
			aFile.getParentFile().mkdirs();
			
			// Generate a new id.
			long theTime = System.nanoTime();
			String theId = Utils.md5String(BigInteger.valueOf(theTime).toByteArray());
			
			StructureDatabase theDatabase = new StructureDatabase(aConfig, theId, aFile, new Ids());
			return theDatabase;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Call this method after deserializing a {@link StructureDatabase} to
	 * set the database field on all locations.
	 */
	public void reown()
	{
		for (AdviceInfo theAdvice : itsAdvices) if (theAdvice != null) theAdvice.setDatabase(this, true);
		for (ClassInfo theClass : itsClasses) if (theClass != null) theClass.setDatabase(this, true);

		for (BehaviorInfo theBehavior : itsBehaviors) if (theBehavior != null) 
		{
			theBehavior.setDatabase(this, true);
			ClassInfo theClass = (ClassInfo) theBehavior.getDeclaringType();
			theClass._getBehaviorsMap().put(ClassInfo.getKey(theBehavior), theBehavior);
		}
		
		for (FieldInfo theField : itsFields) if (theField != null) 
		{
			theField.setDatabase(this, true);
		}
	}
	
	public String getId()
	{
		return itsId;
	}
	
	public TODConfig getConfig()
	{
		return itsConfig;
	}
	
	private ClassSelector parseWorkingSet(String aWorkingSet)
	{
		try
		{
			return WorkingSetFactory.parseWorkingSet(aWorkingSet);
		}
		catch (ParseException e)
		{
			throw new RuntimeException("Cannot parse selector: "+aWorkingSet, e);
		}
	}
	
	void fireMonitoringModeChanged(BehaviorMonitoringModeChange aChange)
	{
		for (Listener theListener : itsListeners) theListener.monitoringModeChanged(aChange);
	}

	public IClassInfo getClass(String aName, String aChecksum, boolean aFailIfAbsent)
	{
		aName = transformClassName(aName);
		ClassNameInfo theClassNameInfo = itsClassNameInfos.get(aName);
		if (theClassNameInfo == null && itsFile != null)
		{
			
		}
		
		return null;
	}
	
	public IClassInfo getUnknownClass()
	{
		return itsUnknownClass;
	}

	public ClassInfo getClass(String aName, boolean aFailIfAbsent)
	{
		aName = transformClassName(aName);
		if (itsAllowHomonymClasses)
		{
			ClassNameInfo theClassNameInfo = itsClassNameInfos.get(aName);
			if (theClassNameInfo == null) 
			{
				if (aFailIfAbsent) throw new RuntimeException("Class not found: "+aName);
				else return null;
			}
			return theClassNameInfo.getLatest();
		}
		else
		{
			ClassInfo theClassInfo = itsClassInfos.get(aName);
			if (theClassInfo == null) 
			{
				if (aFailIfAbsent) throw new RuntimeException("Class not found: "+aName);
				else return null;
			}
			return theClassInfo;
			
		}
	}

	public ClassInfo[] getClasses(String aName)
	{
		aName = transformClassName(aName);
		if (itsAllowHomonymClasses)
		{
			ClassNameInfo theClassNameInfo = itsClassNameInfos.get(aName);
			if (theClassNameInfo == null) 
			{
				return new ClassInfo[0];
			}
			return theClassNameInfo.getAll();
		}
		else
		{
			ClassInfo theClass = getClass(aName, false);
			return theClass != null ? new ClassInfo[] { theClass } : new ClassInfo[0];
		}
	}
	
	void fireClassChanged(IClassInfo aClass)
	{
		for (Listener theListener : itsListeners) theListener.classChanged(aClass);
	}

	protected void registerClass(IClassInfo aClass)
	{
		itsIds.registerClassId(aClass.getId());
		Utils.listSet(itsClasses, aClass.getId(), (ClassInfo) aClass);
		
		if (itsAllowHomonymClasses)
		{
			ClassNameInfo theClassNameInfo = getClassNameInfo(aClass.getName());
			theClassNameInfo.addClass((ClassInfo) aClass);
		}
		else
		{
			ClassInfo theClassInfo = itsClassInfos.put(aClass.getName(), (ClassInfo) aClass);
			if (theClassInfo != null) throw new RuntimeException("Class already exists: "+aClass.getName()+", enable allow homonym classes.");
		}
		
		for (Listener theListener : itsListeners) theListener.classAdded(aClass);
	}
	
	protected void registerBytecode(int aClassId, byte[] aBytecode, byte[] aOriginalBytecode)
	{
		try
		{
			itsByteCodeOffsets.ensureCapacity(aClassId+1);
			itsByteCodeOffsets.setQuick(aClassId, itsFile.getFilePointer());
			itsFile.writeInt(aBytecode.length);
			itsFile.write(aBytecode);
			itsFile.writeInt(aOriginalBytecode.length);
			itsFile.write(aOriginalBytecode);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	protected ClassNameInfo getClassNameInfo(String aName)
	{
		if (! itsAllowHomonymClasses) return null;
		
		aName = transformClassName(aName);
		ClassNameInfo theClassNameInfo = itsClassNameInfos.get(aName);
		if (theClassNameInfo == null)
		{
			theClassNameInfo = new ClassNameInfo();
			itsClassNameInfos.put(aName, theClassNameInfo);
		}
		return theClassNameInfo;
	}
	
	public ClassInfo getNewClass(String aName)
	{
		aName = transformClassName(aName);
		ClassInfo theClass = getClass(aName, false);
		if (theClass == null)
		{
			theClass = new ClassInfo(this, getClassNameInfo(aName), aName, itsIds.nextClassId());
			registerClass(theClass);
		}
		
		return theClass;
	}
	
	public ClassInfo addClass(int aId, String aName)
	{
		aName = transformClassName(aName);
		ClassInfo theClass = getClass(aId, false);
		if (theClass != null)
		{
			throw new IllegalArgumentException("There is already a class with id "+aId);
		}
		
		theClass = new ClassInfo(this, getClassNameInfo(aName), aName, aId);
		registerClass(theClass);
		return theClass;
	}
	
	public BehaviorInfo getBehavior(int aId, boolean aFailIfAbsent)
	{
		BehaviorInfo theBehavior = aId >= 0 ? Utils.listGet(itsBehaviors, aId) : null;
		if (theBehavior == null && aFailIfAbsent) throw new RuntimeException("Behavior not found: "+aId);
		return theBehavior;
	}
	
	public IBehaviorInfo[] getBehaviors()
	{
		List<IBehaviorInfo> theBehaviors = new ArrayList<IBehaviorInfo>();
		for (IBehaviorInfo theBehavior : itsBehaviors)
		{
			if (theBehavior != null) theBehaviors.add(theBehavior);
		}
		return theBehaviors.toArray(new IBehaviorInfo[theBehaviors.size()]);
	}
	
	public void registerBehavior(IBehaviorInfo aBehavior)
	{
		itsIds.registerBehaviorId(aBehavior.getId());

		Utils.listSet(itsBehaviors, aBehavior.getId(), (BehaviorInfo) aBehavior);
		if (DebugFlags.LOG_REGISTERED_BEHAVIORS) 
		{
			System.out.println(String.format(
					"Reg.b. %d: %s.%s",
					aBehavior.getId(),
					aBehavior.getDeclaringType().getName(),
					Util.getFullName(aBehavior)));
		}
		
		for (Listener theListener : itsListeners) theListener.behaviorAdded(aBehavior);
	}

	public ClassInfo getClass(int aId, boolean aFailIfAbsent)
	{
		ClassInfo theClass = Utils.listGet(itsClasses, aId);
		if (theClass == null && aFailIfAbsent) throw new RuntimeException("Class not found: "+aId);
		return theClass;
	}

	public FieldInfo getField(int aId, boolean aFailIfAbsent)
	{
		FieldInfo theField = Utils.listGet(itsFields, aId);
		if (theField == null && aFailIfAbsent) throw new RuntimeException("Field not found: "+aId);
		return theField;
	}
	
	public void registerField(IFieldInfo aField)
	{
		itsIds.registerFieldId(aField.getId());
		Utils.listSet(itsFields, aField.getId(), (FieldInfo) aField);
		for (Listener theListener : itsListeners) theListener.fieldAdded(aField);
	}

	public ITypeInfo getNewType(String aName)
	{
		return getType(this, aName, true, false);
	}
	
	public ITypeInfo getType(String aName, boolean aFailIfAbsent)
	{
		return getType(this, aName, false, aFailIfAbsent);
	}
	
	public IArrayTypeInfo getArrayType(ITypeInfo aBaseType, int aDimensions)
	{
		return new ArrayTypeInfo(this, aBaseType, aDimensions);
	}


	public ITypeInfo getType(int aId, boolean aFailIfAbsent)
	{
		if (aId > 0 && aId <= PrimitiveTypeInfo.TYPES.length) return PrimitiveTypeInfo.get(aId);
		else return getClass(aId, aFailIfAbsent);
	}

	public static ITypeInfo getType(
			IMutableStructureDatabase aStructureDatabase, 
			String aName, 
			boolean aCreateIfAbsent, 
			boolean aFailIfAbsent)
	{
		aName = transformClassName(aName);
		Type theType = Type.getType(aName);
		switch(theType.getSort())
		{
		case Type.OBJECT:
		{ 
			String theClassName = theType.getClassName();
			return aCreateIfAbsent ? 
					aStructureDatabase.getNewClass(theClassName) 
					: aStructureDatabase.getClass(theClassName, aFailIfAbsent);
		}
			
		case Type.ARRAY:
		{
			ITypeInfo theElementType = getType(
					aStructureDatabase,
					theType.getElementType().getDescriptor(), 
					aCreateIfAbsent, 
					aFailIfAbsent);
			
			int theDimensions = theType.getDimensions();
			
			return new ArrayTypeInfo(
					null, // That should be safe... if there is a problem we'll see what we do
					theElementType,
					theDimensions);			
		}
		
		case Type.VOID: return PrimitiveTypeInfo.VOID;
		case Type.BOOLEAN: return PrimitiveTypeInfo.BOOLEAN;
		case Type.BYTE: return PrimitiveTypeInfo.BYTE;
		case Type.CHAR: return PrimitiveTypeInfo.CHAR;
		case Type.DOUBLE: return PrimitiveTypeInfo.DOUBLE;
		case Type.FLOAT: return PrimitiveTypeInfo.FLOAT;
		case Type.INT: return PrimitiveTypeInfo.INT;
		case Type.LONG: return PrimitiveTypeInfo.LONG;
		case Type.SHORT: return PrimitiveTypeInfo.SHORT;
			
		default:
			// This is not a "normal" failure, so always throw exception
			throw new RuntimeException("Not handled: "+theType);
		}
	}
	
	public IClassInfo[] getClasses()
	{
		List<IClassInfo> theClasses = new ArrayList<IClassInfo>();
		for (IClassInfo theClass : itsClasses)
		{
			if (theClass != null) theClasses.add(theClass);
		}
		return theClasses.toArray(new IClassInfo[theClasses.size()]);
	}
	

	
	public Stats getStats()
	{
		return new Stats(itsClasses.size(), itsBehaviors.size(), itsFields.size(), itsProbes.size());
	}
	
	public int getBehaviorId(String aClassName, String aMethodName, String aMethodSignature)
	{
		return StructureDatabaseUtils.getBehaviorId(this, aClassName, aMethodName, aMethodSignature);
	}
	
	public ProbeInfo getProbeInfo(int aProbeId)
	{
		return itsProbes.get(aProbeId);
	}
	
	protected void registerProbe(ProbeInfo aProbe)
	{
		if (aProbe.behaviorId == -1) return; // This was a temp probe, it will be updated later
		BehaviorInfo theBehavior = getBehavior(aProbe.behaviorId, true);
		theBehavior.addProbe(aProbe);
	}

	public int addProbe(int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId)
	{
		int theId = itsProbes.size(); // we add a null element in the constructor, so first id is 1
		ProbeInfo theProbe = new ProbeInfo(theId, aBehaviorId, aBytecodeIndex, aRole, aAdviceSourceId);
		itsProbes.add(theProbe);
		registerProbe(theProbe);
		return theId;
	}
	
	
	
	public void addProbe(
			int aId,
			int aBehaviorId,
			int aBytecodeIndex,
			BytecodeRole aRole,
			int aAdviceSourceId)
	{
		ProbeInfo theProbe = Utils.listGet(itsProbes, aId);
		if (theProbe != null)
		{
			throw new IllegalArgumentException("There is already a probe with id "+aId);
		}
		theProbe = new ProbeInfo(aId, aBehaviorId, aBytecodeIndex, aRole, aAdviceSourceId);
		Utils.listSet(itsProbes, aId, theProbe);
		registerProbe(theProbe);
	}

	public void setProbe(int aProbeId, int aBehaviorId, int aBytecodeIndex, BytecodeRole aRole, int aAdviceSourceId)
	{
		ProbeInfo theProbe = new ProbeInfo(aProbeId, aBehaviorId, aBytecodeIndex, aRole, aAdviceSourceId);
		itsProbes.set(aProbeId, theProbe);
		registerProbe(theProbe);
	}

	public ProbeInfo getNewExceptionProbe(int aBehaviorId, int aBytecodeIndex)
	{
		long theKey = (((long) aBehaviorId) << 32) | aBytecodeIndex;
		ProbeInfo theProbe = itsExceptionProbesMap.get(theKey);
		if (theProbe == null)
		{
			int theId = addProbe(aBehaviorId, aBytecodeIndex, null, -1);
			theProbe = itsProbes.get(theId);
			assert theProbe != null : String.format("bid: %d, index: %d, probeId: %d", aBehaviorId, aBytecodeIndex, theId);
			itsExceptionProbesMap.put(theKey, theProbe);
		}
		return theProbe;
	}

	public int getProbeCount()
	{
		return itsProbes.size();
	}
	
	public BehaviorMonitoringModeChange getBehaviorMonitoringModeChange(int aVersion)
	{
		return itsMethodGroupManager.getChange(aVersion);
	}

	public IAdviceInfo getAdvice(int aAdviceId)
	{
		return Utils.listGet(itsAdvices, aAdviceId);
	}
	
	public Map<String, IAspectInfo> getAspectInfoMap()
	{
		return itsAspectInfoMap;
	}

	public void setAdviceSourceMap(Map<Integer, SourceRange> aMap)
	{
		for (Map.Entry<Integer, SourceRange> theEntry : aMap.entrySet())
		{
			int theId = theEntry.getKey();
			SourceRange theRange = theEntry.getValue();
			assert theRange != null;
			
			// Fill advice source map
			AdviceInfo thePrevious = Utils.listGet(itsAdvices, theId); 
			if (thePrevious != null && ! thePrevious.getSourceRange().equals(theRange))
			{
				Utils.rtex(
						"Advice source inconsistency for id %d (prev.: %s, new: %s)", 
						theId,
						thePrevious,
						theRange);
			}
			
			// Fill aspect info map
			if (thePrevious == null)
			{
				AdviceInfo theAdviceInfo = new AdviceInfo(this, theId, theRange);
				Utils.listSet(itsAdvices, theId, theAdviceInfo);
				
				AspectInfo theAspectInfo = (AspectInfo) itsAspectInfoMap.get(theRange.sourceFile);
				if (theAspectInfo == null)
				{
					theAspectInfo = new AspectInfo(this, itsIds.nextAspectId(), theRange.sourceFile);
					itsAspectInfoMap.put(theRange.sourceFile, theAspectInfo);
				}
				theAspectInfo.addAdvice(theAdviceInfo);
			}
		}
	}

	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public Bytecode _getClassBytecode(int aClassId)
	{
		ClassInfo theClass = getClass(aClassId, true);
		Bytecode theBytecode = theClass._getBytecode();
		if (theBytecode == null)
		{
			readClassBytecode(aClassId);
			theBytecode = theClass._getBytecode();
		}
		return theBytecode;
	}
	
	private void readClassBytecode(int aClassId)
	{
		try
		{
			itsFile.seek(itsByteCodeOffsets.get(aClassId));
			int l = itsFile.readInt();
			byte[] theBytecode = new byte[l];
			itsFile.readFully(theBytecode);
			l = itsFile.readInt();
			byte[] theOriginalBytecode = new byte[l];
			itsFile.readFully(theOriginalBytecode);
			
			ClassInfo theClass = getClass(aClassId, true);
			theClass._setBytecode(theBytecode, theOriginalBytecode);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public String _getClassSMAP(int aClassId)
	{
		return getClass(aClassId, true)._getSMAP();
	}

	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public Map<String, IMutableFieldInfo> _getClassFieldMap(int aClassId)
	{
		return getClass(aClassId, true)._getFieldsMap();
	}
	
	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public Map<String, IMutableBehaviorInfo> _getClassBehaviorsMap(int aClassId)
	{
		return getClass(aClassId, true)._getBehaviorsMap();
	}

	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public List<LocalVariableInfo> _getBehaviorLocalVariableInfo(int aBehaviorId)
	{
		return getBehavior(aBehaviorId, true)._getLocalVariables();
	}
	
	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public LineNumberInfo[] _getBehaviorLineNumberInfo(int aBehaviorId)
	{
		return getBehavior(aBehaviorId, true)._getLineNumbers();
	}
	
	/**
	 * This method is used to retrieve the value of transient fields on the remote side
	 * (see {@link RemoteStructureDatabase}).
	 */
	public TagMap _getBehaviorTagMap(int aBehaviorId)
	{
		return getBehavior(aBehaviorId, true)._getTagMap();
	}
	
	public List<ProbeInfo> _getBehaviorProbes(int aBehaviorId)
	{
		return getBehavior(aBehaviorId, true)._getProbes();
	}
	
	public IClassInfo _getBehaviorClass(int aBehaviorId, boolean aFailIfAbsent)
	{
		BehaviorInfo theBehavior = getBehavior(aBehaviorId, aFailIfAbsent);
		return theBehavior != null ? theBehavior.getDeclaringType() : null;
	}

	public IClassInfo _getFieldClass(int aFieldId, boolean aFailIfAbsent)
	{
		FieldInfo theField = getField(aFieldId, aFailIfAbsent);
		return theField != null ? theField.getDeclaringType() : null;
	}
	
	public void addListener(Listener aListener)
	{
		itsListeners.add(aListener);
	}

	public void removeListener(Listener aListener)
	{
		itsListeners.remove(aListener);
	}

	public boolean isInScope(String aClassName)
	{
		return BCIUtils.acceptClass(aClassName, itsGlobalSelector)
			&& BCIUtils.acceptClass(aClassName, itsTraceSelector);
	}
	
	public boolean isInIdScope(String aClassName)
	{
		return BCIUtils.acceptClass(aClassName, itsIdSelector);
	}
	
	public Ids getIds()
	{
		return itsIds;
	}

	public LastIds getLastIds()
	{
		return new LastIds(itsIds.getLastClassId(), itsIds.getLastBehaviorId(), itsIds.getLastFieldId());
	}

	public void setLastIds(LastIds aIds)
	{
		itsIds.setLastClassId(aIds.classId);
		itsIds.setLastBehaviorId(aIds.behaviorId);
		itsIds.setLastFieldId(aIds.fieldId);
	}



	static class Ids implements Serializable
	{
		private static final long serialVersionUID = -8031089051309554360L;
		
		/**
		 * Ids below FIRST_CLASS_ID are reserved; Ids 1 to 9 are for primitive types.
		 */
		private int itsNextFreeClassId = FIRST_CLASS_ID;
		private int itsNextFreeBehaviorId = 1;
		private int itsNextFreeFieldId = 1;
		private int itsNextFreeAspectId = 1;

		public synchronized int nextClassId()
		{
			return itsNextFreeClassId++;
		}
		
		public int getLastClassId()
		{
			return itsNextFreeClassId-1;
		}
		
		public synchronized int nextBehaviorId()
		{
			return itsNextFreeBehaviorId++;
		}
		
		public int getLastBehaviorId()
		{
			return itsNextFreeBehaviorId-1;
		}
		
		public synchronized int nextFieldId()
		{
			return itsNextFreeFieldId++;
		}
		
		public int getLastFieldId()
		{
			return itsNextFreeFieldId-1;
		}
		
		public synchronized int nextAspectId()
		{
			return itsNextFreeAspectId++;
		}
		
		/**
		 * Notifies this id manager that a class with the given id has been
		 * registered without asking for an id here.
		 */
		public synchronized void registerClassId(int aId)
		{
			setLastClassId(aId);
		}
		
		public synchronized void setLastClassId(int aId)
		{
			if (aId >= itsNextFreeClassId) itsNextFreeClassId = aId+1;
		}

		/**
		 * Notifies this id manager that a behavior with the given id has been
		 * registered without asking for an id here.
		 */
		public synchronized void registerBehaviorId(int aId)
		{
			setLastBehaviorId(aId);
		}
		
		public synchronized void setLastBehaviorId(int aId)
		{
			if (aId >= itsNextFreeBehaviorId) itsNextFreeBehaviorId = aId+1;
		}
		
		/**
		 * Notifies this id manager that a field with the given id has been
		 * registered without asking for an id here.
		 */
		public synchronized void registerFieldId(int aId)
		{
			setLastFieldId(aId);
		}
		
		public synchronized void setLastFieldId(int aId)
		{
			if (aId >= itsNextFreeFieldId) itsNextFreeFieldId = aId+1;
		}
	}
	
	/**
	 * Information associated to a class name. Note that several classes can share
	 * the same class name: there can be several versions of the same class.
	 * @author gpothier
	 */
	public class ClassNameInfo implements Serializable
	{
		private static final long serialVersionUID = 5559242268240172292L;

		/**
		 * Maps class checksum to class info.
		 */
		private HashMap<String, ClassInfo> itsChecksumToClassMap =
			new HashMap<String, ClassInfo>();
		
		/**
		 * List of classes in the order they were added to the database.
		 */
		private List<ClassInfo> itsChronologicalClasses =
			new ArrayList<ClassInfo>();
		
		/**
		 * Maps of the ids of all the members of this set of classes.
		 * It is used so that homonym classes all have the same ids
		 * for "compatible" members. 
		 */
		private Map<String, Integer> itsIdMap =
			new HashMap<String, Integer>();
		
		public void addClass(ClassInfo aClass)
		{
			itsChecksumToClassMap.put(aClass.getChecksum(), aClass);
			itsChronologicalClasses.add(aClass);
		}
		
		/**
		 * Gets the latest registered class for this name.
		 */
		public ClassInfo getLatest()
		{
			if (itsChronologicalClasses.isEmpty()) return null;
			return itsChronologicalClasses.get(itsChronologicalClasses.size()-1);
		}
		
		/**
		 * Returns all the classes registered with this name, in chronological order.
		 */
		public ClassInfo[] getAll()
		{
			return itsChronologicalClasses.toArray(new ClassInfo[itsChronologicalClasses.size()]);
		}
		
		/**
		 * Returns an id for a particular field.
		 * The ids map is first checked, and if no corresponding
		 * id exists a new one is created.
		 * @param aName Name of the field.
		 * @param aType Type of the field.
		 */
		public int getFieldId(String aName, ITypeInfo aType)
		{
			String theKey = ClassInfo.getFieldKey(aName, aType);
			Integer theId = itsIdMap.get(theKey);
			if (theId == null)
			{
				theId = itsIds.nextFieldId();
				itsIdMap.put(theKey, theId);
			}
			return theId;
		}
		
		/**
		 * Returns an id for a particular behavior.
		 * The ids map is first checked, and if no corresponding
		 * id exists a new one is created.
		 * @param aName Name of the behavior.
		 * @param aType Type of the behavior's arguments.
		 */
		public int getBehaviorId(String aName, ITypeInfo[] aArgumentTypes, ITypeInfo aReturnType)
		{
			String theKey = ClassInfo.getBehaviorKey(aName, aArgumentTypes, aReturnType);
			Integer theId = itsIdMap.get(theKey);
			if (theId == null)
			{
				theId = itsIds.nextBehaviorId();
				itsIdMap.put(theKey, theId);
			}
			return theId;
		}
	}
}
