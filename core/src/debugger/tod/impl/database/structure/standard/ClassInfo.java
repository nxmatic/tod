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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import tod.core.database.browser.LocationUtils;
import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IFieldInfo;
import tod.core.database.structure.IMemberInfo;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableFieldInfo;
import tod.core.database.structure.IShareableStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ILocationInfo.ISerializableLocationInfo;
import tod.core.database.structure.IStructureDatabase.LineNumberInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.impl.database.structure.standard.StructureDatabase.ClassNameInfo;
import zz.utils.Utils;

/**
 * Default implementation of {@link IClassInfo}.
 * @author gpothier
 */
public class ClassInfo extends TypeInfo 
implements IMutableClassInfo, ISerializableLocationInfo
{
	private static final long serialVersionUID = -2583314414851419966L;

	private transient ClassNameInfo itsClassNameInfo;
	private String itsJvmName;
	
	private boolean itsHasBytecode = false;
	
	/**
	 * Instrumented bytecode
	 */
	private transient byte[] itsBytecode;
	
	/**
	 * Original (non-instrumented) bytecode.
	 */
	private transient byte[] itsOriginalBytecode;
	
	private boolean itsHasSMAP = false;
	private transient String itsSMAP;
	
	private boolean itsInScope;
	private boolean itsInterface;
	
	private String itsChecksum;
	
	/**
	 * Id of the supertype.
	 * Important: we keep the id and not the object for serialization purposes.
	 */
	private int itsSupertypeId;
	
	/**
	 * Ids of the interfaces implemented by this class.
	 * Important: we keep the ids and not the objects for serialization purposes.
	 */
	private int[] itsInterfacesIds;
	
	private transient Map<String, IMutableFieldInfo> itsFieldsMap = 
		new HashMap<String, IMutableFieldInfo>();
	
	private transient Map<String, IMutableBehaviorInfo> itsBehaviorsMap = 
		new HashMap<String, IMutableBehaviorInfo>();
	
	/**
	 * Whether this class info can be disposed.
	 * At the start of the system,
	 * and when all debugged VMs are disconnected from the database,
	 * every class is marked disposable.
	 * Once operation starts, classes are marked not disposable
	 * as they are used or added to the database. This permits to free the space
	 * used by old versions of classes that are not used anymore, while preserving
	 * various versions when classes are redefined at runtime.
	 */
	private boolean itsDisposable = false;
		
	private long itsStartTime;

	public ClassInfo(IShareableStructureDatabase aDatabase, ClassNameInfo aClassNameInfo, String aName, int aId)
	{
		super(aDatabase, aId, aName);
		assert aDatabase != null;
		itsClassNameInfo = aClassNameInfo;
		//Thread.currentThread().getContextClassLoader().
		itsJvmName = Type.getObjectType(getName().replace('.', '/')).getDescriptor();
		
//		System.out.println(String.format("[Struct] class info [id: %d, name: %s]", aId, aName));
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
		if (StructureDatabaseUtils.isSaving())
		{
			out.writeBoolean(true);
			out.writeObject(itsBytecode);
			out.writeObject(itsOriginalBytecode);
			out.writeObject(itsFieldsMap);
			out.writeObject(itsBehaviorsMap);
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
			itsBytecode = (byte[]) in.readObject();
			itsOriginalBytecode = (byte[]) in.readObject();
			itsFieldsMap = (Map<String, IMutableFieldInfo>) in.readObject();
			itsBehaviorsMap = (Map<String, IMutableBehaviorInfo>) in.readObject();
		}
	}
 
	protected void fireClassChanged()
	{
		IShareableStructureDatabase theDatabase = getDatabase();
		if (theDatabase instanceof StructureDatabase)
		{
			StructureDatabase d = (StructureDatabase) theDatabase;
			d.fireClassChanged(this);
		}
	}
	
	public String getJvmName()
	{
		return itsJvmName;
	}
	
	public void setup(
			boolean aIsInterface, 
			boolean aInScope, 
			String aChecksum, 
			IClassInfo[] aInterfaces, 
			IClassInfo aSuperclass)
	{
		itsInterface = aIsInterface;
		itsInScope = aInScope;
		itsChecksum = aChecksum;
		setInterfaces(aInterfaces);
		setSupertype(aSuperclass);
		
		fireClassChanged();
	}
	
	byte[] _getBytecode()
	{
		return itsBytecode;
	}
	
	byte[] _getOriginalBytecode()
	{
		return itsOriginalBytecode;
	}
	
	public byte[] getBytecode()
	{
		if (itsBytecode == null && itsHasBytecode)
		{
			assert ! isOriginal();
			itsBytecode = getDatabase()._getClassBytecode(getId());
		}
		return itsBytecode;
	}
	
	public byte[] getOriginalBytecode()
	{
		if (itsOriginalBytecode == null && itsHasBytecode)
		{
			assert ! isOriginal();
			itsOriginalBytecode = getDatabase()._getClassOriginalBytecode(getId());
		}
		return itsOriginalBytecode;
	}
	

	public void setBytecode(byte[] aBytecode, byte[] aOriginalBytecode)
	{
		assert isOriginal();
		assert (aBytecode == null) == (aOriginalBytecode == null);
		itsBytecode = aBytecode;
		itsOriginalBytecode = aOriginalBytecode;
		itsHasBytecode = itsBytecode != null;
	}
	
	String _getSMAP()
	{
		return itsSMAP;
	}

	public String getSMAP()
	{
		if (itsSMAP == null && itsHasSMAP)
		{
			assert ! isOriginal();
			itsSMAP = getDatabase()._getClassSMAP(getId());
		}
		return itsSMAP;
	}
	
	public void setSMAP(String aSmap)
	{
		assert isOriginal();
		itsSMAP = aSmap;
		itsHasSMAP = itsSMAP != null;
	}

	/**
	 * Same as {@link #getDatabase()} but casts to {@link StructureDatabase}.
	 * This is only for registration methods, that are used only where the original
	 * structure database exists.
	 */
	public StructureDatabase getStructureDatabase()
	{
		return (StructureDatabase) super.getDatabase();
	}
	
	@Override
	public void setDatabase(IShareableStructureDatabase aDatabase, boolean aIsOriginal)
	{
		super.setDatabase(aDatabase, aIsOriginal);
		if (aIsOriginal)
		{
			if (itsBehaviorsMap == null) itsBehaviorsMap = new HashMap<String, IMutableBehaviorInfo>();
			if (itsFieldsMap == null) itsFieldsMap = new HashMap<String, IMutableFieldInfo>();
			if (itsClassNameInfo == null) itsClassNameInfo = ((StructureDatabase) getDatabase()).new ClassNameInfo();
		}
	}
	
	protected IMemberInfo[] getMembers()
	{
		List<IMemberInfo> theMembers = new ArrayList<IMemberInfo>();
		Utils.fillCollection(theMembers, getBehaviorsMap().values());
		Utils.fillCollection(theMembers, getFieldsMap().values());
		return theMembers.toArray(new IMemberInfo[theMembers.size()]);
	}
	
	public boolean isInScope()
	{
		return itsInScope;
	}
	
	public boolean isInterface()
	{
		return itsInterface;
	}
	
	public long getStartTime()
	{
		return itsStartTime;
	}
	
	public String getChecksum()
	{
		return itsChecksum;
	}

	public boolean isDisposable()
	{
		return itsDisposable;
	}

	public void setDisposable(boolean aDisposable)
	{
		itsDisposable = aDisposable;
	}

	/**
	 * Registers the given field info object.
	 */
	public void register(IMutableFieldInfo aField)
	{
		getFieldsMap().put (aField.getName(), aField);
		getStructureDatabase().registerField(aField);
	}
	
	/**
	 * Registers the given behavior info object.
	 */
	public void register(IMutableBehaviorInfo aBehavior)
	{
		getBehaviorsMap().put(getKey(aBehavior), aBehavior);
		getStructureDatabase().registerBehavior(aBehavior);
	}
	
	public IMutableFieldInfo getField(String aName)
	{
		return getFieldsMap().get(aName);
	}
	
	Map<String, IMutableFieldInfo> _getFieldsMap()
	{
		return itsFieldsMap;
	}
	
	private Map<String, IMutableFieldInfo> getFieldsMap()
	{
		if (itsFieldsMap == null)
		{
			assert ! isOriginal();
			itsFieldsMap = getDatabase()._getClassFieldMap(getId());
		}
		return itsFieldsMap;
	}
	
	public IMutableBehaviorInfo getBehavior(String aName, ITypeInfo[] aArgumentTypes, ITypeInfo aReturnType)
	{
		return getBehaviorsMap().get(getBehaviorKey(aName, aArgumentTypes, aReturnType));
	}
	
	public IMutableBehaviorInfo getBehavior(String aName, String aDescriptor)
	{
		ITypeInfo[] theArgumentTypes = LocationUtils.getArgumentTypes(getDatabase(), aDescriptor);
		ITypeInfo theReturnType = LocationUtils.getReturnType(getDatabase(), aDescriptor);

		return getBehavior(aName, theArgumentTypes, theReturnType);
	}
	
	Map<String, IMutableBehaviorInfo> _getBehaviorsMap()
	{
		return itsBehaviorsMap;
	}
	
	private Map<String, IMutableBehaviorInfo> getBehaviorsMap()
	{
		if (itsBehaviorsMap == null)
		{
			assert ! isOriginal();
			itsBehaviorsMap = getDatabase()._getClassBehaviorsMap(getId());
		}
		return itsBehaviorsMap;
	}
	
	public IMutableBehaviorInfo getNewBehavior(String aName, String aDescriptor, boolean aStatic)
	{
		ITypeInfo[] theArgumentTypes = LocationUtils.getArgumentTypes(getDatabase(), aDescriptor);
		ITypeInfo theReturnType = LocationUtils.getReturnType(getDatabase(), aDescriptor);
		
		IMutableBehaviorInfo theBehavior = getBehavior(aName, theArgumentTypes, theReturnType);
		if (theBehavior == null)
		{
			int theId = itsClassNameInfo.getBehaviorId(aName, theArgumentTypes, theReturnType);
			theBehavior = new BehaviorInfo(
					getStructureDatabase(), 
					theId,
					this,
					aName,
					aStatic,
					aDescriptor,
					theArgumentTypes,
					theReturnType);
			
			register(theBehavior);
		}
		
		return theBehavior;
	}

	public IMutableBehaviorInfo addBehavior(int aId, String aName, String aDescriptor, boolean aStatic)
	{
		BehaviorInfo theBehavior = getStructureDatabase().getBehavior(aId, false);
		if (theBehavior != null)
		{
			throw new IllegalArgumentException("There is already a behavior with id "+aId);
		}
		
		ITypeInfo[] theArgumentTypes = LocationUtils.getArgumentTypes(getDatabase(), aDescriptor);
		ITypeInfo theReturnType = LocationUtils.getReturnType(getDatabase(), aDescriptor);
		
		theBehavior = new BehaviorInfo(
				getStructureDatabase(), 
				aId,
				this,
				aName,
				aStatic,
				aDescriptor,
				theArgumentTypes,
				theReturnType);
		
		register(theBehavior);
		return theBehavior;
	}
	
	public IMutableFieldInfo getNewField(String aName, ITypeInfo aType, boolean aStatic)
	{
		IMutableFieldInfo theField = getField(aName);
		if (theField == null)
		{
			int theId = itsClassNameInfo.getFieldId(aName, aType);
			theField = new FieldInfo(getStructureDatabase(), theId, this, aName, aType, aStatic);
			
			register(theField);
		}
	
		return theField;
	}
	
	public IMutableFieldInfo addField(int aId, String aName, ITypeInfo aType, boolean aStatic)
	{
		FieldInfo theField = getStructureDatabase().getField(aId, false);
		if (theField != null)
		{
			throw new IllegalArgumentException("There is already a field with id "+aId);
		}
		
		theField = new FieldInfo(getStructureDatabase(), aId, this, aName, aType, aStatic);
		register(theField);
		return theField;
	}
	
	public Iterable<IFieldInfo> getFields()
	{
		return (Iterable) getFieldsMap().values();
	}
	
	public Iterable<IBehaviorInfo> getBehaviors()
	{
		return (Iterable) getBehaviorsMap().values();
	}
	
	public IClassInfo[] getInterfaces()
	{
		if (itsInterfacesIds == null) return new IClassInfo[0];
		
		IClassInfo[] theResult = new ClassInfo[itsInterfacesIds.length];
		for(int i=0;i<itsInterfacesIds.length;i++)
		{
			theResult[i] = getDatabase().getClass(itsInterfacesIds[i], true);
		}
		return theResult;
	}

	private void setInterfaces(IClassInfo[] aInterfaces)
	{
		itsInterfacesIds = new int[aInterfaces.length];
		for(int i=0;i<itsInterfacesIds.length;i++)
		{
			itsInterfacesIds[i] = aInterfaces[i].getId();
		}		
	}

	private void setSupertype(IClassInfo aSupertype)
	{
		itsSupertypeId = aSupertype != null ? aSupertype.getId() : 0;
	}

	public IClassInfo getSupertype()
	{
		return itsSupertypeId != 0 ? getDatabase().getClass(itsSupertypeId, true) : null;
	}

	public int getSize()
	{
		return 1;
	}

	public boolean isArray()
	{
		return false;
	}

	public boolean isPrimitive()
	{
		return false;
	}

	public boolean isVoid()
	{
		return false;
	}
	
	public static String getKey(IBehaviorInfo aBehavior)
	{
		return getBehaviorKey(aBehavior.getName(), aBehavior.getArgumentTypes(), aBehavior.getReturnType());
	}
	
	/**
	 * Returns a key (signature) for identifying a behavior.
	 */
	public static String getBehaviorKey(String aName, ITypeInfo[] aArgumentTypes, ITypeInfo aReturnType)
	{
		StringBuilder theBuilder = new StringBuilder("b");
		theBuilder.append(aName);
		theBuilder.append('|');
		for (ITypeInfo theType : aArgumentTypes)
		{
			theBuilder.append('|');
			theBuilder.append(theType.getName());
		}
		theBuilder.append('/');
		theBuilder.append(aReturnType.getName());
		
		return theBuilder.toString();
	}

	/**
	 * Returns a key (signature) for identifying a field.
	 */
	public static String getFieldKey(String aName, ITypeInfo aType)
	{
		return "f" + aName + "|" + aType.getName();
	}
	
	@Override
	public String toString()
	{
		return "Class ("+getId()+", "+getName()+")";
	}
	
	public ClassInfo createUncertainClone()
	{
		ClassInfo theClone = (ClassInfo) super.clone();
		theClone.changeName(getName()+ "?");
		return theClone;
	}

	public Object getDefaultInitialValue()
	{
		return null;
	}
}
